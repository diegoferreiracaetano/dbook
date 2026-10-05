# Desempenho e resiliência

O que o sistema promete sob carga, como medir, e o que protege a API quando uma dependência (o banco, o Redis, o Bedrock, uma fila) fica lenta ou cai.

## Testes de carga (k6)

Em `loadtest/`, escritos em JavaScript, rodam contra uma API local (ou de homologação) com o mesmo script de dados de sempre:

```bash
./scripts/seed-flights.sh 300          # o admin de seed e voos
REQUESTLIMITS_RATELIMITENABLED=false ./gradlew bootRun    # sem o limite por IP: o k6 vem de um IP só
docker run --rm -i -e BASE_URL=http://host.docker.internal:8080 -v "$PWD/loadtest:/lt" grafana/k6 run /lt/search.js
```

| Script | O que faz | Metas (reprova se não cumprir) |
|---|---|---|
| `search.js` | 100 buscas por segundo (`RATE`, `DURATION`) em 5 dias de voos GRU→GIG | p95 < 300 ms, p99 < 800 ms, menos de 1 % de erro |
| `booking-race.js` | 50 clientes reservam **o mesmo assento** no mesmo instante (`CUSTOMERS`) | **exatamente 1** reserva criada, **todas as outras 409**, nenhum 5xx |
| `checkout.js` | 5 compras por segundo: cadastro, login, reserva de um assento próprio e pagamento (`RATE`) | reserva p95 < 400 ms, pagamento p95 < 600 ms, menos de 1 % de erro |

A corrida é a que mais importa: não mede tempo, mede a **promessa** do fluxo de reserva (o assento é de um só), e o limiar é uma contagem.

### Medidas (2026-10-05, notebook, API + Postgres + Redis locais, 300 voos)

| Cenário | Resultado |
|---|---|
| Busca, 100/s por 30 s, **com** o cache | p95 5,7 ms, p99 10 ms, 0 erro; 2.994 acertos de cache e 7 faltas |
| Busca, 100/s por 30 s, **sem** o cache | p95 14 ms, 0 erro; duas consultas ao banco por busca |
| Corrida, 50 clientes, 1 assento | 1 vitória, 49 × `409`, 0 outro status |
| Compra, 5/s por 60 s | reserva p95 47 ms, pagamento p95 51 ms, 0 erro (as chamadas de cadastro e login pesam 0,8–1 s: é o custo 12 do BCrypt, de propósito) |
| Pool depois da carga | `hikaricp_connections_max` 10, `pending` 0, `timeout_total` 0 |

Os números são de uma máquina e de uma base pequenas: o valor deles é a **relação** (o cache tira a consulta do caminho) e o piso das metas, não a promessa de produção. Rode de novo antes de um lançamento, contra o ambiente de verdade.

> **Armadilha achada ao medir:** a variável de ambiente de `request-limits.rate-limit-enabled` é `REQUESTLIMITS_RATELIMITENABLED` (o Spring tira os hífens do nome, o sublinhado só separa níveis). Com `REQUEST_LIMITS_...` ela não pega, e o k6, que vem de um IP só, é recusado com `429` depois de 300 requisições por minuto, exatamente como o limite foi feito para fazer.

## O pool de conexões

`spring.datasource.hikari.*` no `application.yml`:

| Chave | Valor | Por quê |
|---|---|---|
| `maximum-pool-size` (`DB_POOL_SIZE`) | 10 | poucas conexões, bem usadas: um banco faz mais trabalho com 10 conexões ativas do que com 100 brigando por CPU e disco (a regra do Hikari: núcleos × 2 + discos). O pool pequeno também protege o banco: **instâncias × pool** tem de caber em `max_connections`, com folga para migração, `psql` e o `pg_dump` |
| `minimum-idle` | igual ao máximo | pool de tamanho fixo: sem o custo de abrir conexão no meio de um pico |
| `connection-timeout` | 3 s | quem não consegue uma conexão em 3 s falha em vez de empilhar atrás dos outros |
| `leak-detection-threshold` | 20 s | uma conexão emprestada há mais que isso é logada com a pilha de quem a pegou (vazamento, ou uma chamada de rede dentro de uma transação) |

Métricas (já exportadas): `hikaricp_connections_{active,idle,pending,max}`, `hikaricp_connections_acquire_seconds` (a espera por uma conexão) e `hikaricp_connections_usage_seconds` (quanto tempo cada uma fica emprestada); alerta `DatabasePoolSaturated` (conexões pendentes por 2 minutos) com [runbook](runbooks.md#databasepoolsaturated). O teste `TheConnectionPoolIsSizedAndObservedTest` garante que o pool tem o tamanho dito e que os medidores existem.

## N+1: listas que crescem em consultas

Uma lista que faz uma consulta a mais por linha é invisível com três linhas num teste e lenta com três mil em produção. O `QueryCounter` dos testes conta as consultas SQL de cada operação, e os testes de `presentation/performance/` falham quando uma lista **passa a custar mais consultas só porque tem mais linhas**: mesmas consultas com 1 e com 5 linhas, para a busca de voos, "minhas viagens", a lista de reservas e a de voos do portal e a busca de hotéis.

O primeiro rodar já achou duas:

| Lista | Antes | Depois |
|---|---|---|
| Busca pública de voos | 1 + 2 por voo (a capacidade livre, mais a companhia e os aeroportos de cada voo) | **2** consultas: os voos com companhia e aeroportos num `@EntityGraph`, e a capacidade livre de todos numa só (`SeatAvailability.availableSeatsOf`) |
| "Minhas viagens" (`GET /v1/bookings`) | 1 + 5 por reserva (voo, assento, pagamento, capacidade, avaliação) | número fixo de consultas: `@EntityGraph` na reserva, e assentos, avaliações e capacidade pedidos de todas as reservas de uma vez |

A regra para quem escreve uma lista: a porta devolve **a lista inteira** (`findAllById`, `findByBookingIds`, `availableSeatsOf`) e o caso de uso junta em memória por id; nunca um `repositorio.find(id)` dentro de um `map`.

## O cache da busca de voos

A busca pública é a leitura mais repetida da API, e a resposta só muda quando o catálogo muda. `RedisFlightSearchCache` guarda o resultado de cada rota e dia por 30 s (`flight-search-cache.ttl-seconds`, e `flight-search-cache.enabled` liga e desliga).

- **Invalidação:** mudar um voo (criar, editar, cancelar), uma companhia ou um aeroporto chama `invalidateAll()` no adaptador de persistência, e ela vale **quando a transação confirma**. Invalidar é um `INCR` de uma "época" que toda chave carrega: as cópias velhas ficam inalcançáveis na hora e expiram sozinhas, sem varrer o Redis. Quem já tinha lido do banco um instante antes da mudança grava a cópia sob a época antiga, que ninguém mais lê.
- **O que fica velho:** os assentos tomados no meio tempo só aparecem quando a cópia expira (até 30 s). A reserva é sempre conferida contra os assentos de verdade, então uma cópia velha só pode fazer uma reserva dar `409`, nunca dar certo errado. É um combinado, e os 30 s são a conta dele.
- **Falha aberta:** com o Redis fora, a busca vai ao banco (`dbook_cache_total{cache="flight-search",outcome="error"}` e o alerta `CacheUnavailable` que já existia).
- **Serialização à mão:** o `ObjectMapper` da API não tem o módulo do Kotlin e não reconstrói um `Flight`; `FlightSnapshot` escreve e lê cada campo, e um teste compara todos eles (um cache que zera um campo em silêncio é pior que nenhum).
- Os testes rodam com o cache **desligado** (reservam e buscam logo em seguida, esperando ver o assento sumir) e os do cache o ligam.

## O modelo de IA: tempo-limite, disjuntor e anteparo

Antes, uma chamada ao Bedrock sem limite de tempo segurava uma thread do servidor pelo tempo que o SDK quisesse (com novas tentativas), e um Bedrock fora do ar segurava **todas**. Agora (`BedrockConfig`, `BedrockGuard`):

| Proteção | Como | Resultado |
|---|---|---|
| **Tempo-limite** | `apiCallTimeout` 10 s no total e `apiCallAttemptTimeout` 6 s por tentativa (`ai.bedrock.timeout-seconds`) | uma resposta lenta vira falha em vez de espera sem fim |
| **Disjuntor** (*circuit breaker*) | janela das 10 últimas chamadas; com 5 ou mais e 50 % de falhas, abre por 30 s; depois deixa passar 2 chamadas de teste | com o modelo fora, as sugestões respondem `503` **na hora** em vez de cada pedido esperar o tempo-limite inteiro |
| **Anteparo** (*bulkhead*) | no máximo 10 chamadas em andamento; a 11ª é recusada na hora, sem fila | um modelo lento pode tomar algumas threads do servidor, nunca todas |

Só conta como falha o que é da **chamada** (`SdkException`, inclusive o estouro de tempo); uma resposta do modelo que não dá para usar (JSON quebrado) não é o modelo fora do ar. A busca comum nunca depende disso: é o motivo de o `503` ser aceitável. Medidores: `dbook_ai_bedrock_calls_total{outcome=success|failure|circuit_open|bulkhead_full}` e o `dbook_ai_circuit_state` (0 fechado, 1 aberto, 2 meio-aberto); alerta `AiCircuitOpen` com [runbook](runbooks.md#aicircuitopen).

## As threads dos jobs agendados (anteparo do consumidor de fila)

Os consumidores de fila fazem *long polling* (esperam até 20 s por uma mensagem), então um job pode segurar a sua thread por muito tempo. O achado: com `@EnableScheduling` e nenhum agendador próprio, os jobs rodavam no agendador do **broker do WebSocket** (o único `TaskScheduler` do contexto, que tem batimentos para mandar a cada socket aberto). Uma fila lenta atrasaria os batimentos de todos, e os jobs se atrasavam entre si.

`SchedulingConfig` dá aos jobs um `taskScheduler` próprio de 6 threads (`scheduling.pool-size`), com um nome (`dbook-sched-`) que aparece nos *dumps* de thread. São 4 jobs hoje (o relé do outbox e a sua limpeza, o consumidor de expiração de reservas e o de notificações), então um job travado segura a sua thread e nenhuma outra. O teste `EachScheduledJobHasAThreadOfItsOwnTest` **conta os `@Scheduled` do código** e reprova se o pool for menor que o número de jobs (quem adicionar um quinto sem subir o pool é avisado), prova que os jobs rodam nesse pool e que jobs travados não seguram outro.

## O que ainda não foi feito

- **Teste de carga no CI:** o k6 é manual (uma máquina de CI não é um ambiente de medida estável). Quando houver homologação, rodar o `search.js` e a corrida a cada *deploy*.
- **Réplica de leitura** e **particionar** `booking`/`price_history`: só quando o volume pedir; as listas já têm índice e paginação.
- **Cache de outras leituras** (destinos em destaque, menor preço): o mesmo desenho serve; entra quando os painéis mostrarem que pesam.

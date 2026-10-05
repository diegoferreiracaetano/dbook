# CRM de clientes

O portal administrativo precisa **achar** um cliente, ver o **quadro completo** dele, registrar contexto e agir, com rastro e respeitando a LGPD. Tudo isso mora no conceito `crm` (só leitura, mais as notas) sob `/v1/admin/customers`. Os endpoints estão em [Endpoints](endpoints.md#crm-clientes-v1admincustomers-requer-a-permissão-customer_read).

## Glossário

| Termo | Significa |
|---|---|
| **Cliente** | conta com papel `CLIENT`. O CRM **só enxerga clientes**: o id de um membro da equipe é `404`, igual a um id inexistente (a equipe está em `/v1/admin/staff`) |
| **Visão 360º** | `GET /customers/{id}`: perfil, status e totais (reservas por status, quanto pagou, nota média dada) |
| **Read model** | o que a tela mostra, montado por SQL (`CustomerSummary`, `CustomerProfile`), sem passar por entidade JPA: consulta separada do agregado |
| **Nota** | texto interno da equipe sobre o cliente. O cliente nunca a vê |
| **Anonimizar** | tirar de uma conta tudo o que identifica a pessoa, sem apagar o que a lei manda guardar |

## Quem pode o quê

| Permissão | Papéis | O que abre |
|---|---|---|
| `CUSTOMER_READ` | `SUPPORT`, `SUPER_ADMIN` | busca, visão 360º, listas, notas (leitura) |
| `CUSTOMER_NOTE` | `SUPPORT`, `SUPER_ADMIN` | escrever, editar e apagar notas |
| `CUSTOMER_BLOCK` | `SUPPORT`, `SUPER_ADMIN` | bloquear e desbloquear |
| `CUSTOMER_EXPORT` | `SUPER_ADMIN` | exportar CSV |
| `CUSTOMER_ERASE` | `SUPER_ADMIN` | anonimizar |

## Busca

`GET /v1/admin/customers` combina texto, status, período de cadastro e "tem reservas", com página, ordenação e total. Decisões:

- **Por `page`/`size` com total, não por cursor**: a tela precisa de "página 3 de 120" e de ordenar por coluna, e cursor não faz nenhum dos dois. A auditoria, que só cresce, segue por cursor.
- **Sem caixa e sem acento**: `jose` acha `José`. A migration V31 cria a função `immutable_unaccent` (a `unaccent` do Postgres não é `IMMUTABLE`, e um índice exige isso) e índices trigram sobre `immutable_unaccent(lower(...))` de nome e e-mail.
- **`%`, `_` e `\` valem como caracteres**, não como curingas.
- **A ordenação vem de um enum fechado**: nenhum texto digitado chega ao `ORDER BY`. Desempate por id, para a ordem ser estável entre páginas.
- **Um `SQL` só para busca e exportação** (`CustomerSql`): os dois nunca discordam sobre o que um filtro significa.

### Medido com 50 000 clientes

`./scripts/seed-customers.sh` carrega 50 000 clientes (5% bloqueados, 20% sem nunca ter entrado), 60 mil reservas em todos os status, 20 mil pagamentos e 8 mil avaliações. `scripts/explain-customers.sql` roda as consultas como o código as monta, sob `EXPLAIN (ANALYZE)`. Postgres 16, máquina de desenvolvimento, base quente:

| Consulta | Plano | Tempo |
|---|---|---|
| listagem padrão, página 1 | `idx_app_user_client_created` (o índice parcial já entrega na ordem), contagem de reservas por `idx_booking_customer` | 0,3 ms |
| texto comum (um sobrenome em ~2% dos clientes), página 1 | percorre `idx_app_user_client_created` filtrando (20 resultados saem cedo) | 4,8 ms |
| a **contagem** da mesma busca | **trigram**: `BitmapOr` de `idx_app_user_name_trgm` e `idx_app_user_email_trgm` | 2,7 ms |
| texto seletivo (um e-mail, em maiúsculas) | **trigram** no e-mail, `Sort` de 1 linha | 10 ms |
| "tem reservas" e bloqueado | `EXISTS` vira *semi join* em `idx_booking_customer` | 0,4 ms |
| **página funda**, `OFFSET 40000` | índice, 20 linhas com a contagem | 32 ms |
| visão 360º | uma consulta, índices de `booking`, `payment` (o prefixo do índice único) e `review` | 0,3 ms |

**O que a medição corrigiu:** a primeira versão calculava a contagem de reservas junto com as colunas, e uma subconsulta por linha roda também nas linhas que o `OFFSET` joga fora: a página em `OFFSET 40000` levou **286 ms** (40 020 execuções). Hoje o `LIMIT` fica dentro, nas colunas simples, e a contagem é calculada só para as 20 linhas devolvidas (32 ms, e o resto é o custo de andar 40 mil entradas do índice, o preço de paginar por `offset`).

**Limites que ficam:** páginas muito fundas continuam custando proporcional ao `offset` (a tela não leva ninguém a 2 000 páginas, e `size ≤ 100`). O sobrenome comum prefere o índice de ordem; é o planejador escolhendo o barato, e o trigram assume quando o termo é raro.

**Contra N+1** (testado com um contador de `jdbc.query`): a visão 360º custa **1** consulta, e cada lista, **2** (contagem + página), qualquer que seja o histórico do cliente.

## Notas

`POST/GET/PATCH/DELETE /v1/admin/customers/{id}/notes`. Fixadas primeiro, depois as mais novas; até 2000 caracteres.

- **Só o autor edita** o que escreveu. **O autor ou um `SUPER_ADMIN` apaga.** Outro membro do suporte recebe `403`.
- **Remoção lógica**: a linha fica, mas nenhuma lista nem edição a enxerga de novo (`404`).
- **A auditoria nunca guarda o texto** (é dado pessoal livre): só que a nota foi escrita, editada ou apagada, qual e por quem.

## Bloquear

`POST /v1/admin/customers/{id}/block` com o `reason` (no mínimo 10 caracteres, aparado) e `.../unblock`. Bloquear **encerra as sessões**; o cliente vê `403 ACCOUNT_BLOCKED` ao tentar entrar. Auditado com o motivo e o estado antes e depois.

## Quem abriu a ficha

Abrir a visão 360º grava `CUSTOMER_VIEWED`, porque é acesso a dado pessoal e a empresa precisa responder quem viu o quê. Uma pessoa reabrindo a mesma ficha em **5 minutos** conta como um acesso só (a consulta ao registro filtra por ator, alvo e janela). Se a gravação falhar, a visão também falha: acesso sem rastro não existe.

## LGPD

### Exportar (portabilidade do titular)

`GET /v1/users/me/export` devolve, para quem pede, tudo o que o sistema guarda sobre ele (perfil, reservas, pagamentos, avaliações), **todas as páginas, sem teto**: cortar uma exportação de portabilidade seria omitir dado em silêncio. A exportação fica registrada.

### Exportar em massa (equipe)

`GET /v1/admin/customers/export` (só `SUPER_ADMIN`). CSV em UTF-8 **com BOM** (o Excel lê os acentos), no máximo **50 000 linhas**, lido do banco aos poucos (`fetchSize`) e escrito direto na resposta: a memória não cresce com o tamanho do resultado.

- **O registro é gravado antes do primeiro byte**, com o filtro usado. Uma exportação sem rastro não acontece.
- **Proteção contra *CSV injection*:** um cliente que se cadastrou como `=HYPERLINK(...)` atacaria quem abrisse o arquivo numa planilha. Toda célula de texto que começa com `=`, `+`, `-`, `@`, tabulação ou retorno ganha uma `'` na frente, que a planilha lê como texto.
- O corpo é escrito num *async dispatch*, onde o filtro de JWT não roda de novo e o contexto (sem sessão) se perde. O `SecurityConfig` libera **só esse despacho** (`DispatcherType.ASYNC`): a autorização já foi decidida quando a requisição chegou, e o cliente não consegue provocar um despacho assíncrono.

### Anonimizar (direito ao esquecimento)

`POST /v1/admin/customers/{id}/anonymize` (staff, `SUPER_ADMIN`) e `DELETE /v1/users/me` (o próprio cliente, com a senha) usam **o mesmo código** (`CustomerAnonymizer`): mudam quem pede e como prova, não o que acontece. É **irreversível**, por isso a equipe informa um motivo e a frase `ANONYMIZE <id>`, que um id errado não satisfaz; o cliente confirma com a senha (uma senha errada conta no mesmo limite do login).

| O que acontece | Como |
|---|---|
| nome e e-mail | viram `Anonymous customer` e `anonymized-<id>@anonymous.invalid` (`.invalid` é reservado: nunca recebe e-mail) |
| acesso | conta bloqueada, senha trocada por um hash que nenhuma senha satisfaz, sessões encerradas; `app_user.anonymized_at` marca o fato, e **desbloquear** é recusado |
| **notas** sobre o cliente | apagadas |
| **consultas à IA** (`ai_suggestion_log`) | apagadas (é texto que ele digitou) |
| **nome do titular** nos pagamentos | vira `ANONYMIZED` |
| **reservas e pagamentos** | **ficam** (obrigação fiscal), agora ligados a uma conta anônima |
| **avaliações** | ficam, sem autor identificável |
| a auditoria | já nunca guardou nome nem e-mail (lista permitida), então nada a limpar |
| o **e-mail original** | **não pode ser cadastrado de novo**: fica só o hash SHA-256 (`anonymized_email`), e o registro recusa o endereço (`409`, igual a "já existe") |

**Por que o e-mail não volta:** sem isso, quem foi bloqueado por fraude apagaria a conta e voltaria com o mesmo endereço, sem histórico. O hash não revela o endereço e dá para recusá-lo; é a base de legítimo interesse para prevenção a fraude. O custo: quem apagou a conta por vontade própria também não volta com o mesmo e-mail.

**Fora do escopo (de propósito):** backups e logs de aplicação (têm retenção própria); avaliações já publicadas não têm o texto removido (o conteúdo fica, o autor não); apagar a conta de equipe (fecha-se bloqueando).

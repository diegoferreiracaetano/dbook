# Dashboard de negócio

Os números que a equipe olha para saber como o negócio vai: reservas, receita, clientes novos, conversão e ocupação. **Sem um glossário, um número vira discussão**, então o significado de cada um vem primeiro, e o código segue o glossário à risca. Os endpoints estão em [Endpoints](endpoints.md#dashboard-v1admindashboard).

## Glossário

| Termo | Significa | Detalhe |
|---|---|---|
| **Período** | os dias de `from` a `to`, **os dois incluídos**, contados no fuso de **São Paulo** | uma reserva paga às 23:30 do dia 30 em São Paulo é do dia 30, mesmo já sendo dia 31 em UTC. No máximo **400 dias**. Sem `from` e `to`: os últimos 30 dias, até hoje |
| **Reserva paga** | a reserva que teve a entrada `CONFIRMED` no histórico de status no período | a data é a do **pagamento**, não a da criação (`booking_status_history`, do [M31](reembolso.md#a-linha-do-tempo)) |
| **Receita bruta** | a soma do `Booking.price` das reservas pagas no período | o preço **congelado** na reserva, não o atual do voo |
| **Reembolsado** | a soma dos reembolsos **concluídos** no período | conta no dia em que **concluiu**, não no da reserva |
| **Receita líquida** | receita bruta menos reembolsado | é a "receita" do plano |
| **Reservas por status** | as reservas **criadas** no período, pelo status de agora | |
| **Clientes novos** | contas `CLIENT` criadas no período | a equipe não conta |
| **Conversão** | das reservas criadas no período, quantas **foram pagas** (alguma vez, mesmo reembolsadas depois) | `null` se não houve reserva |
| **Expiração** | das reservas criadas no período, quantas o **sistema cancelou** por falta de pagamento | o cancelamento sem ator (o job de expiração, M20) |
| **Ocupação** | os assentos reservados dos voos que **partem** no período, sobre a capacidade deles | só voos à venda; a reserva pendente também ocupa assento |
| **Rota** | origem e destino | `top-routes` ordena por reservas pagas, depois por receita, depois por código |

Uma taxa é `null` quando não há o que dividir (um período sem reservas não tem "0% de conversão": não tem conversão). A receita das séries é a líquida, e o mesmo reembolso aparece no dia em que foi concluído.

Uma limitação a saber: as reservas anteriores à V35 ganharam uma entrada de criação com o momento da migração, então as **pagas** antes dela ficam atribuídas a esse dia.

## Endpoints (`DASHBOARD_READ`: `SUPPORT`, `CATALOG_MANAGER`, `SUPER_ADMIN`)

- `GET /v1/admin/dashboard/summary?from=&to=`
- `GET /v1/admin/dashboard/timeseries?metric=REVENUE|BOOKINGS|NEW_CUSTOMERS&granularity=DAY|WEEK&from=&to=`: um ponto por dia ou por **semana** (a data é a segunda-feira; a primeira pode ser anterior ao período), com os baldes vazios como zero, para o gráfico não ter buraco.
- `GET /v1/admin/dashboard/top-routes?limit=&from=&to=` (`limit` de 1 a 50, padrão 10)

## O cache (o primeiro do projeto)

Cada resposta é guardada no **Redis por 60 segundos** (`dashboard.cache-ttl-seconds`), com a chave montada dos parâmetros (`dbook:dashboard:summary:2026-09-01:2026-09-30`). Um número pode ter um minuto, e ninguém percebe; a tela é aberta várias vezes seguidas, e cada consulta lê todo o histórico de reservas. Decisões:

- **O cache nunca atrapalha a resposta, só a velocidade.** Com o Redis fora do ar a resposta é calculada normalmente e a falha é contada; uma cópia ilegível é tratada como ausente (como o limitador de login, que também deixa passar).
- **Métrica `dbook_cache_total{cache="dashboard",outcome="hit|miss|error"}`**, para ver quanto o cache vale.
- **Só aqui, de propósito.** O cache fica atrás de uma porta do domínio (`DashboardCache`), e o que vai nele são dados de leitura que podem envelhecer sem prejuízo. Cachear algo que muda uma regra de negócio exigiria invalidação; este não.
- Os valores são serializados em JSON. Um tipo com **um campo só** não volta do JSON sem um criador (a mesma armadilha dos DTOs), por isso o `TopRoutes` carrega também o `limit`.

## Desempenho (EXPLAIN com 60 mil reservas e 100 mil linhas de histórico)

`./scripts/seed-customers.sh` (agora também grava o histórico, com as reservas espalhadas por 400 dias) e `scripts/explain-dashboard.sql`. Postgres 16, máquina de desenvolvimento:

| Consulta | Plano | Tempo |
|---|---|---|
| receita bruta, 30 dias | `idx_booking_history_status_time` (V38) | 10 ms |
| receita bruta, 400 dias | o mesmo índice | 16 ms |
| reservas por status criadas em 30 dias | `idx_booking_created` (V35) | 6 ms |
| conversão (criadas e pagas) | os dois índices | 11 ms |
| expiração, 400 dias | `idx_booking_history_status_time` | 21 ms |
| receita por dia, 400 dias | os mesmos, mais `idx_refund_completed` (V38) para os reembolsos | 25 ms |
| rotas mais vendidas, 400 dias | o mesmo índice e *hash join* com voo e aeroportos | 38 ms |
| ocupação dos voos dos próximos 30 dias | `idx_flight_departure` (V37) | 5 ms |

Todas leem o histórico pelo índice (nenhuma varre a tabela de histórico). O *hash join* com a tabela `booking` varre as reservas inteiras, o que o planejador escolhe de propósito por serem poucas; com muitos milhões de reservas o plano passa a buscar pela chave primária, e este arquivo deve ser medido de novo. O teto de **400 dias** existe para limitar o pior caso.

## Fora do escopo (de propósito)

Receita por companhia ou por cliente (o M30 já mostra o total por cliente); metas e comparação com o período anterior (é conta da tela); exportação do dashboard; invalidar o cache quando algo muda (60 segundos é o prazo, por decisão).

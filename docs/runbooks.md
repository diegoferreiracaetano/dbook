# Runbooks dos alertas

Cada alerta de `observability/alerts.yml` aponta para a seção daqui com o mesmo nome. Para cada um: **o que significa**, **onde olhar** e **como mitigar**. Os alertas vão do Prometheus ao Alertmanager (`http://localhost:9093`), que os entrega ao receptor de `observability/alertmanager.yml`. Os SLOs e o orçamento de erro estão no painel **"DBook — SLOs e orçamento de erro"** do Grafana (http://localhost:3000/d/dbook-slo). Como investigar com logs, traces e métricas: [Observabilidade](observabilidade.md#5-roteiros-como-investigar-um-problema).

`critical` é o que exige alguém agora; `warning` pode esperar o horário comercial. Com a aplicação fora do ar, `DbookDown` **inibe** os demais (nada do que depende dela é notícia).

## SLOs

| SLO | Meta | Como se mede |
|---|---|---|
| Disponibilidade | **99,5 %** em 30 dias | 1 − (respostas 5xx ÷ todas as respostas) |
| Latência da busca de voos | **p95 < 500 ms** | `http_server_requests_seconds_bucket{uri="/v1/flights/search"}` |

Os 0,5 % que sobram são o **orçamento de erro**: o painel mostra quanto dele resta. Orçamento acabando é motivo para parar de lançar coisa nova e cuidar da estabilidade.

## DbookDown
**Crítico.** O Prometheus não consegue ler a aplicação há 1 minuto.
- **Onde olhar:** `http://localhost:9090/targets` (o alvo `dbook`); `curl localhost:8081/actuator/health` (a porta de operação, 8081); o log do processo ou da tarefa ECS.
- **Mitigar:** se o processo caiu, subir de novo e ler o motivo no log antes (falta de memória, banco ou Redis fora, migration que falhou). Se só o alvo não responde (rede, porta), é problema de coleta e os usuários podem estar bem: confirmar com um acesso real à porta 8080.

## HighServerErrorRate
**Crítico.** Mais de 5 % das requisições terminam em 5xx por 5 minutos (com pelo menos 6 requisições por minuto, para uma falha à noite não acordar ninguém).
- **Onde olhar:** o painel de visão geral (erros por rota); no Loki, `{app="dbook"} | json | level="ERROR"`; a rota com mais 5xx em `sum by (uri) (rate(http_server_requests_seconds_count{status=~"5.."}[5m]))`; o trace de uma requisição falha (o `traceId` está no log).
- **Mitigar:** a causa mais comum é uma dependência fora do ar (`/actuator/health/readiness` mostra banco e Redis). Se começou com um deploy, reverter. Se é uma rota só, desligar a causa (feature) ou avisar.

## SearchLatencyHigh
**Aviso.** O p95 da busca de voos passou de 500 ms por 10 minutos (o SLO).
- **Onde olhar:** o painel "p95 da busca"; "Latência p95 por caso de uso" (a busca é `SearchFlightsUseCase`); `hikaricp_connections_active` (conexões esgotadas); os planos das consultas da busca (`EXPLAIN`) e o crescimento da tabela `flight`.
- **Mitigar:** pool de conexões saturado = olhar quem segura conexão (uma consulta lenta de outra rota, o painel de CRM ou o dashboard sem cache); índice faltando = criar numa migration; carga atípica = escalar a aplicação.

## DeadLetterQueueNotEmpty
**Crítico.** Há mensagem numa fila de mensagens mortas (o rótulo `queue` diz qual): uma mensagem falhou 3 vezes. Na `booking-expiration`, **o assento dessa reserva pode seguir preso**; na `notifications`, **um cliente pode não ter sido avisado** (ou o evento é de um tipo que ninguém conhece).
- **Onde olhar:** o gauge `dbook_sqs_dlq_depth{queue}`; o log do consumidor (`BookingExpirationConsumer` ou `NotificationConsumer`: "Failed to process message ..."); a mensagem em si (`aws sqs receive-message` na fila `dbook-booking-expiration-dlq` ou `dbook-notifications-dlq`, `awslocal` no LocalStack); `dbook_booking_expiration_total{outcome="failed"}` ou `dbook_notification_messages_total{outcome="failed"}`.
- **Mitigar (expiração):** achar por que falha (o `bookingId` está na mensagem). Se a reserva já foi paga ou cancelada, a mensagem é inofensiva: descartar. Se é um assento realmente preso, cancelar a reserva pelo portal (`POST /v1/bookings/{id}/cancel` com a permissão de staff) e esvaziar a mensagem. Depois de corrigir a causa, devolver as mensagens à fila principal (*redrive* do SQS).

## PendingBookingsPiling
**Aviso.** Mais de 200 reservas `PENDING` e o número só cresce há 15 minutos. Cada reserva deveria expirar em 15 minutos: se não expira, os assentos ficam presos.
- **Onde olhar:** o gauge `dbook_booking_pending`; `dbook_booking_expiration_total` (o consumidor está processando?); a fila principal (`ApproximateNumberOfMessages`); se `booking-expiration.consumer.enabled` está ligado nesta instância; o log do consumidor.
- **Mitigar:** consumidor parado ou sem acesso à fila = religar; fila com atraso = esperar ou escalar o consumidor. Uma campanha forte de venda também sobe o número sem ser defeito: compare com `dbook_payment_total{outcome="created"}`.

## OutboxOverdue
**Crítico.** Há eventos do outbox que deveriam ter saído há mais de 5 minutos e ainda não saíram (`dbook_outbox_overdue_seconds`). O principal é a expiração das reservas: **os assentos podem estar presos**.
- **Onde olhar:** `dbook_outbox_pending` (quantos esperam; os agendados para depois contam, é normal ter até as reservas dos últimos 15 minutos) e `dbook_outbox_failures_total{type}` (o relé está tentando e falhando?); a tabela: `select type, attempts, last_error, available_at from outbox_event where published_at is null and available_at < now() order by available_at limit 20` (o `last_error` diz por quê); `outbox.relay.enabled` ligado nesta instância; a fila (`dbook-booking-expiration`) existe e responde.
- **Mitigar:** o relé é automático e tenta de novo com espera crescente (5 s, 10 s... até 15 minutos): se a causa era a fila fora do ar, ele se recupera sozinho quando ela volta. Fila inexistente ou URL errada (`outbox.queues.*`) = corrigir a configuração (um tipo sem rota falha sempre). Para acelerar depois de consertar: `update outbox_event set next_attempt_at = now() where published_at is null`. Ver [Mensageria](mensageria.md).

## RefundsFailing
**Aviso.** Um reembolso falhou no gateway de pagamento (o dinheiro **não** saiu; a reserva continua `CONFIRMED`).
- **Onde olhar:** `GET /v1/admin/refunds?status=FAILED`; o `failureReason` de cada um; a auditoria (`REFUND_FAILED`); o log.
- **Mitigar:** `POST /v1/admin/refunds/{id}/retry` quando o gateway se recuperar (é seguro repetir: o gateway recebe sempre a mesma chave, `refund-<id>`, e nunca move o dinheiro duas vezes). Ver [Reembolso](reembolso.md#a-mini-saga).

## LoginFailureSpike
**Aviso.** Mais de 2 logins por segundo falhando (senha errada ou conta travada) por 5 minutos.
- **Onde olhar:** `dbook_auth_login_total` por `outcome` e `audience`; o log de acesso (`ip` das tentativas); `rate_limited` crescendo diz que o limitador (e-mail e IP) está segurando.
- **Mitigar:** o limitador já trava por e-mail e por IP. Se vem de poucos IPs, bloquear no balanceador ou no WAF; se mira uma conta de staff, bloquear a conta. Se o aviso é um defeito do app (um cliente travado em *loop* de login), o `ip` e o `User-Agent` mostram.

## AdminDeniedSpike
**Aviso.** Muitas tentativas **negadas** no portal administrativo (`dbook_admin_action_total{outcome="DENIED"}`).
- **Onde olhar:** a trilha de auditoria: `GET /v1/admin/audit?action=ACCESS_DENIED&outcome=DENIED` (quem, onde, quando, de que IP).
- **Mitigar:** um membro da equipe no papel errado é só ajuste de permissão; uma conta sondando endpoints que não deveria acessar é um incidente: bloquear a conta (`POST /v1/admin/staff/{id}/block`, que também encerra as sessões) e revisar o que ela conseguiu ler.

## CacheUnavailable
**Aviso.** O cache falha há 10 minutos (`dbook_cache_total{outcome="error"}`). As respostas continuam saindo, só mais lentas (o dashboard recalcula).
- **Onde olhar:** a saúde do Redis (`/actuator/health/readiness` mostra `redis`); o log ("The dashboard cache is unavailable").
- **Mitigar:** devolver o Redis. Enquanto isso, o limitador de login também deixa passar (fail-open), então vale acompanhar o `LoginFailureSpike`.

## DatabasePoolSaturated
**Aviso.** Há requisições esperando uma conexão com o banco há 2 minutos (`hikaricp_connections_pending > 0`). Quem espera mais de 3 s (`connection-timeout`) leva um erro 500.
- **Onde olhar:** `hikaricp_connections_active` contra `hikaricp_connections_max`, e `hikaricp_connections_usage_seconds` (quanto tempo cada conexão fica emprestada); no log, o aviso "Connection leak detection triggered" aponta quem segura a conexão; consultas lentas no banco (`pg_stat_activity`).
- **Mitigar:** primeiro achar quem segura a conexão (uma consulta lenta, uma transação aberta esperando uma chamada externa) em vez de subir o pool; se for só carga, subir `DB_POOL_SIZE` **sem passar** do que o banco aguenta (instâncias × pool ≤ `max_connections` menos a folga), ou subir mais uma instância.
- **Prevenir:** nenhuma chamada de rede (gateway, SQS, Bedrock) dentro de uma transação; o teste de N+1 reprova listas que crescem em consultas.

## AiCircuitOpen
**Aviso.** O circuito do modelo de IA está aberto há 5 minutos: as sugestões respondem `503` na hora (a busca comum não é afetada).
- **Onde olhar:** `dbook_ai_bedrock_calls_total{outcome}` (`failure` subindo antes de `circuit_open`), o log ("Bedrock call failed"), o painel de serviço da AWS e as cotas do Bedrock na região.
- **Mitigar:** nada a fazer na aplicação: depois de 30 s o circuito deixa passar duas chamadas de teste e fecha sozinho quando o modelo volta. Se o modelo foi aposentado, trocar `ai.bedrock.model-id`.


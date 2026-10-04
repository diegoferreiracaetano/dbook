# Auditoria

[← Voltar ao README](../README.md)

Toda ação administrativa deixa um **registro imutável**, gravado na mesma transação da mudança, e consultável por quem tem a permissão `AUDIT_READ`. É o que responde "quem fez o quê, quando, e a partir de qual requisição" — requisito de qualquer CRM e de prestação de contas (LGPD).

## O que é auditado

| Ação (`AuditAction`) | Quando | Alvo | Antes / depois |
|---|---|---|---|
| `FLIGHT_CREATED` | um membro da equipe cadastra um voo | `FLIGHT` (id do voo) | depois: o voo |
| `BOOKING_CANCELLED_BY_STAFF` | alguém **com** `BOOKING_CANCEL_ANY` cancela a reserva **de outra pessoa** | `BOOKING` (id da reserva) | antes e depois: a reserva (o status muda) |
| `ACCESS_DENIED` | alguém autenticado tenta algo em `/v1/admin/**` sem a permissão (403) | `ENDPOINT` (`POST /v1/admin/flights`) | — (resultado `DENIED`) |

O cliente cancelando a **própria** reserva não é ação administrativa e não entra. Cada ação nova entra no enum `AuditAction` (que já declara o tipo do alvo) e no caso de uso que a executa.

## Por que no caso de uso, e não em AOP

O registro é feito pelo próprio caso de uso, **dentro da `@Transactional`**: se a mudança falha, não sobra registro; se o registro falha, a mudança desfaz. AOP esconderia o que é auditado e não enxerga o "antes". O `AuditLogRepositoryAdapter` entra na transação de quem o chama (um teste prova o *rollback* conjunto).

## O que cada registro guarda

`audit_log` (migration `V28`): quando (`occurred_at`, pelo `Clock`), quem (`actor_id` e `actor_role`), o quê (`action`, `outcome` = `SUCCESS` ou `DENIED`), sobre o quê (`target_type`, `target_id`), o estado `state_before` e `state_after` (JSON), um `reason` opcional e o **contexto da requisição**: `request_id`, `trace_id`, `ip` e `user_agent`.

- **Do registro ao log e ao trace:** o `request_id` e o `trace_id` são os mesmos do log JSON e do Jaeger. Dado um registro suspeito, filtre o Loki por `requestId` (ou abra o trace) e veja tudo o que aquela requisição fez — ver [Observabilidade](observabilidade.md).
- **Quem monta o contexto:** não é o caso de uso (que não conhece HTTP), e sim o adaptador, lendo o MDC e a requisição corrente (`RequestAuditContext`).
- **Sem `actor_email` e sem chave estrangeira para `app_user`:** o registro nunca deve ser bloqueado nem sofrer cascata por uma mudança em usuário.

### O "antes/depois" é uma lista permitida

Só entra o que cada `toAuditSnapshot()` lista (`FlightAuditSnapshot`, `BookingAuditSnapshot`), nunca o objeto inteiro: um campo novo só chega à trilha se alguém o incluir de propósito, e senha, hash, token e dado de cartão nunca vazam. Datas viram texto para o JSON não depender de formato.

## Imutável no banco

Um gatilho recusa `UPDATE`, `DELETE` e `TRUNCATE` em `audit_log`. A garantia vale para qualquer pessoa com acesso ao banco, não só para o código da aplicação (teste: `TheTrailCannotBeUpdatedDeletedNorTruncatedTest`).

## Como consultar

`GET /v1/admin/audit` (permissão `AUDIT_READ`, só o `SUPER_ADMIN` hoje), do mais novo para o mais antigo, **paginado por cursor** — a trilha só cresce, e deslocamento (`OFFSET`) ficaria errado com registros novos entrando. O cursor é o par (`occurred_at`, `id`), opaco para o cliente.

```bash
# as últimas 20 ações de um membro da equipe
curl "localhost:8080/v1/admin/audit?actorId=3&size=20" -H "Authorization: Bearer <accessToken>"

# só as tentativas negadas, na última hora
curl "localhost:8080/v1/admin/audit?action=ACCESS_DENIED&from=2026-10-04T11:00:00Z" -H "Authorization: Bearer <accessToken>"

# a página seguinte: devolva o nextCursor recebido
curl "localhost:8080/v1/admin/audit?size=20&cursor=<nextCursor>" -H "Authorization: Bearer <accessToken>"
```

Filtros (todos opcionais): `actorId`, `action`, `targetType`, `targetId`, `outcome`, `from` (inclusive) e `to` (exclusivo), em ISO-8601. `size` vai de 1 a 100 (padrão 50). Valor inválido (ação desconhecida, data, cursor, tamanho) é `400` com `code=VALIDATION_FAILED`.

## Observabilidade

- Métrica `dbook_admin_action_total{action,outcome}`: quantas vezes cada ação foi feita ou negada. Um pico de `ACCESS_DENIED` é um sinal a olhar.
- Cada registro gera uma linha de log `audit <ação> <resultado> on <alvo>:<id>` (campos `audit_action`, `audit_outcome`, `actor_id`), sem dado pessoal.

## Limites e o que ficou para depois

- **Retenção:** a tabela só cresce. Particionar por mês e arquivar o antigo está descrito, **não implementado**.
- **Leitura de dado pessoal** (abrir a visão 360º de um cliente) será auditada no M30, junto com o CRM.
- **IP atrás de balanceador:** o IP é o `remoteAddr` da conexão; atrás de um ALB ele será o do balanceador até se configurar o cabeçalho `X-Forwarded-For` de forma confiável (o mesmo limite do limite de tentativas de login).

# Autenticação (JWT)

[← Voltar ao README](../README.md)

Registro, login, refresh de token rotativo, papéis e rotas públicas/protegidas.

JWT stateless (sem sessão), com refresh token rotativo persistido (hash, nunca em texto puro) — reutilizar um refresh token já trocado é rejeitado.

```bash
# registrar (sempre cria role CLIENT)
curl -X POST localhost:8080/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email": "diego@example.com", "password": "s3cret-password", "name": "Diego Ferreira"}'

# login -> { accessToken, refreshToken }
curl -X POST localhost:8080/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email": "diego@example.com", "password": "s3cret-password"}'

# usar o accessToken nas rotas protegidas
curl -X POST localhost:8080/v1/bookings \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <accessToken>" \
  -d '{"bookableId": 1}'

# trocar o refreshToken por um par novo (o antigo é revogado no ato)
curl -X POST localhost:8080/v1/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{"refreshToken": "<refreshToken>"}'

# perfil do usuário autenticado
curl localhost:8080/v1/users/me -H "Authorization: Bearer <accessToken>"

# atualizar o próprio nome
curl -X PATCH localhost:8080/v1/users/me \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <accessToken>" \
  -d '{"name": "Novo Nome"}'
```

Rotas públicas: `/health`, `/auth/register|login|refresh`, `/admin/auth/login|refresh|logout`, `/flights/search`, `/flights/lowest-price`, `/destinations`, `/bookables/*/seats`, Swagger. Todo o resto exige `Authorization: Bearer <token>` — inclusive `/users/me` (GET e PATCH).

## Papéis e permissões

Quem pode **fazer** o quê é uma **permissão**, nunca um nome de papel: cada endpoint administrativo declara `@PreAuthorize("hasAuthority('FLIGHT_WRITE')")`, e a tabela abaixo (só no domínio, `Role.permissions`) diz quais papéis a têm. O token carrega apenas o nome do papel, então mudar a tabela não exige reemitir token.

| Papel | Permissões |
|---|---|
| `CLIENT` | nenhuma (usa só a API pública e a própria conta) |
| `SUPPORT` | `ADMIN_PORTAL_ACCESS`, `CUSTOMER_READ`, `CUSTOMER_NOTE`, `CUSTOMER_BLOCK`, `BOOKING_READ_ANY`, `BOOKING_CANCEL_ANY`, `PAYMENT_REFUND`, `REVIEW_MODERATE`, `DASHBOARD_READ` |
| `CATALOG_MANAGER` | `ADMIN_PORTAL_ACCESS`, `FLIGHT_READ`, `FLIGHT_WRITE`, `CATALOG_WRITE`, `PROMO_WRITE`, `DASHBOARD_READ` |
| `SUPER_ADMIN` | todas (inclui `CUSTOMER_EXPORT`, `CUSTOMER_ERASE`, `AUDIT_READ`, `ADMIN_MANAGE`) |

`ADMIN_PORTAL_ACCESS` é o que faz um papel ser "staff": o `SecurityConfig` a exige em tudo sob `/v1/admin/**`, e cada endpoint pede a sua permissão específica por cima. Uma regra do ArchUnit (`EveryAdminEndpointDeclaresItsPermissionTest`) reprova o build se um endpoint `/admin` esquecer o `@PreAuthorize`.

**Papel e status em `/v1/admin/**` vêm do banco**, a cada requisição, e não do token: quem é bloqueado ou rebaixado perde o acesso na chamada seguinte. Fora do admin, o token de um cliente vale até vencer (15 minutos) — trade-off aceito para não consultar o banco em toda requisição.

Não existe endpoint para promover alguém a staff (permitir isso por API seria uma falha de segurança; o convite entra no M28). Para testar localmente, promova direto no banco:

```sql
UPDATE app_user SET role = 'SUPER_ADMIN' WHERE email = 'diego@example.com';
```

## Conta bloqueada

`app_user.status` é `ACTIVE` ou `BLOCKED` (com motivo e momento obrigatórios, garantido por `CHECK` no banco). Conta bloqueada **não faz login nem renova a sessão**: `403` com `code=ACCOUNT_BLOCKED`. Isso só é dito depois de a senha estar certa, para quem chuta senhas não descobrir quais contas existem. Bloquear pelo `BlockUserUseCase` também revoga todos os refresh tokens do usuário, na mesma transação.

O `User` tem `@Version` (lock otimista) e o último acesso é gravado por um `UPDATE` direto, justamente para uma cópia velha do usuário nunca desfazer um bloqueio.

## Limite de tentativas de login

Contador de **falhas** no Redis (compartilhado entre instâncias, expira sozinho em 15 minutos), por **e-mail** (5) e por **IP** (20). Estourou: `429` com `Retry-After` e `code=TOO_MANY_ATTEMPTS`, mesmo com a senha certa. Acertar a senha zera o contador do e-mail. Se o Redis cair, o limitador deixa passar (e registra um aviso) em vez de derrubar o login. Um e-mail inexistente executa uma verificação de senha do mesmo custo, para o tempo de resposta não revelar quais e-mails existem. Valores em `login-attempts.*` (`application.yml`).

Métrica: `dbook.auth.login{outcome=success|invalid_credentials|blocked|rate_limited, audience=client|staff}`.

## Política de senha

Mínimo de **8** caracteres para cliente (**12** para a equipe, a partir do M28), no máximo 72 **bytes** (o BCrypt ignora o que passa disso), diferente do e-mail e fora de uma lista de senhas comuns. Sem regra de composição (NIST 800-63B). Viola: `400` com `code=VALIDATION_FAILED`.

## Portal administrativo: sessão

O portal usa três rotas abertas, porque são elas que criam a sessão. O token de acesso volta no corpo (o portal o guarda só em memória); o de renovação vai **só num cookie** `httpOnly; Secure; SameSite=Strict; Path=/v1/admin/auth`, que scripts não leem. `refresh` e `logout` só aceitam o `Origin` do portal (lista `cors.allowed-origins`).

```bash
# login: só staff entra; credencial de cliente recebe o mesmo 401 de uma senha errada
curl -i -X POST localhost:8080/v1/admin/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email": "diego@example.com", "password": "s3cret-password"}'
# -> 200 {"accessToken": "..."} + Set-Cookie: dbook_admin_refresh=...

# renovar (gira o cookie; precisa do Origin do portal)
curl -i -X POST localhost:8080/v1/admin/auth/refresh \
  -H "Origin: http://localhost:3000" --cookie "dbook_admin_refresh=<valor>"

# quem sou eu e o que posso fazer (o portal monta o menu a partir de `permissions`)
curl localhost:8080/v1/admin/auth/me -H "Authorization: Bearer <accessToken>"

# sair: revoga o token e apaga o cookie (204)
curl -i -X POST localhost:8080/v1/admin/auth/logout \
  -H "Origin: http://localhost:3000" --cookie "dbook_admin_refresh=<valor>"
```

`Secure` faz o navegador só enviar o cookie por HTTPS. O Chrome trata `http://localhost` como seguro; o Safari não — rodando local no Safari, use `ADMIN_PORTAL_COOKIE_SECURE=false`.

## Erros: o campo `code`

Todo erro sai como `{"error": "<mensagem>", "code": "<CODIGO>"}`. App e portal decidem o que mostrar pelo `code`, nunca pela mensagem. Lista em [Endpoints](endpoints.md#formato-de-erro).

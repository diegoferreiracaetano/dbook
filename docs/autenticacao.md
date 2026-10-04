# Autenticação (JWT)

[← Voltar ao README](../README.md)

Registro, login, refresh de token rotativo, papéis e rotas públicas/protegidas.

JWT stateless (sem sessão), com refresh token rotativo persistido (hash, nunca em texto puro) — reutilizar um refresh token já trocado é rejeitado.

```bash
# registrar (sempre cria role CLIENT)
curl -X POST localhost:8080/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email": "diego@example.com", "password": "s3cret-password", "name": "Diego Ferreira"}'

# login -> { accessToken, refreshToken }
curl -X POST localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email": "diego@example.com", "password": "s3cret-password"}'

# usar o accessToken nas rotas protegidas
curl -X POST localhost:8080/bookings \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <accessToken>" \
  -d '{"bookableId": 1}'

# trocar o refreshToken por um par novo (o antigo é revogado no ato)
curl -X POST localhost:8080/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{"refreshToken": "<refreshToken>"}'

# perfil do usuário autenticado
curl localhost:8080/users/me -H "Authorization: Bearer <accessToken>"

# atualizar o próprio nome
curl -X PATCH localhost:8080/users/me \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <accessToken>" \
  -d '{"name": "Novo Nome"}'
```

Rotas públicas: `/health`, `/auth/**`, `/flights/search`, `/flights/lowest-price`, `/destinations`, Swagger. Todo o resto exige `Authorization: Bearer <token>` — inclusive `/users/me` (GET e PATCH). `POST /admin/flights` exige role `ADMIN`.

Não existe endpoint para promover um usuário a `ADMIN` (não foi pedido, e permitir isso via API seria uma falha de segurança). Pra testar rotas de admin localmente, promova direto no banco:

```sql
UPDATE app_user SET role = 'ADMIN' WHERE email = 'diego@example.com';
```

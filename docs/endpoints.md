# Endpoints

[← Voltar ao README](../README.md)

Todos os endpoints da API, com exemplos de `curl` e os códigos de status de cada um.

> A API de negócio é **versionada pelo caminho**: tudo abaixo vive sob `/v1`. Só `/health`, o `/ws` (WebSocket), o Swagger e o actuator ficam sem versão. Detalhes em [Versionamento da API](versionamento.md).

## `GET /health`
Confirma que a aplicação está no ar.

## Formato de erro
Todo erro tem o corpo `{"error": "<mensagem para humanos>", "code": "<CODIGO>"}`. O app e o portal decidem o que mostrar pelo `code`; a mensagem pode mudar. Acrescentar um código é compatível; renomear ou remover um não é (ver [Versionamento](versionamento.md)).

| `code` | Status | Quando |
|---|---|---|
| `VALIDATION_FAILED` | 400 | regra de domínio ou dado inválido (inclui senha fora da política) |
| `MALFORMED_REQUEST` | 400 | corpo ilegível |
| `MISSING_HEADER` | 400 | cabeçalho obrigatório ausente (ex.: `Idempotency-Key`) |
| `UNAUTHORIZED` | 401 | sem token válido |
| `INVALID_CREDENTIALS` | 401 | e-mail/senha errados (também para um cliente no portal) |
| `INVALID_TOKEN` | 401 | refresh token inexistente, vencido, usado ou de outra porta de entrada |
| `FORBIDDEN` | 403 | sem permissão, não é o dono, ou `Origin` não permitido |
| `ACCOUNT_BLOCKED` | 403 | conta bloqueada (só depois de a senha estar certa) |
| `NOT_FOUND` | 404 | recurso inexistente |
| `CONFLICT` | 409 | conflito com o estado atual |
| `STALE_VERSION` | 409 | outra pessoa alterou o mesmo registro antes (lock otimista) |
| `IDEMPOTENCY_KEY_REUSED` | 422 | mesma `Idempotency-Key` para outro pedido |
| `TOO_MANY_ATTEMPTS` | 429 | falhas de login demais (`Retry-After` em segundos) |
| `RATE_LIMITED` | 429 | limite de uso da IA |
| `AI_RESPONSE_INVALID` / `AI_UNAVAILABLE` | 502 / 503 | modelo de IA |

## `GET /v1/admin/audit` (requer a permissão `AUDIT_READ`)
A trilha de ações administrativas, do mais novo para o mais antigo, **por cursor**. Detalhes em [Auditoria](auditoria.md).

```bash
curl "localhost:8080/v1/admin/audit?action=FLIGHT_CREATED&size=20" \
  -H "Authorization: Bearer <accessToken-de-um-SUPER_ADMIN>"
```

Filtros opcionais: `actorId`, `action` (`FLIGHT_CREATED`, `BOOKING_CANCELLED_BY_STAFF`, `ACCESS_DENIED`), `targetType`, `targetId`, `outcome` (`SUCCESS`/`DENIED`), `from`/`to` (ISO-8601), `cursor` e `size` (1–100, padrão 50). Retorna `200` com `{"items": [...], "nextCursor": "..."}` (`nextCursor` é `null` na última página; cada item traz ator, ação, alvo, `before`/`after` e o `requestId`/`traceId`/`ip` da requisição), `401` sem token, `403` sem `AUDIT_READ` e `400` (`VALIDATION_FAILED`) para filtro, cursor ou tamanho inválidos.

## Portal administrativo: sessão (`/v1/admin/auth/*`)
Detalhes e exemplos em [Autenticação](autenticacao.md#portal-administrativo-sessão).

- `POST /v1/admin/auth/login` (aberta): `{email, password}` → `200 {accessToken}` + cookie de renovação. `401` para senha errada **ou** para quem não é staff, `403 ACCOUNT_BLOCKED`, `429 TOO_MANY_ATTEMPTS`.
- `POST /v1/admin/auth/refresh` (aberta, exige `Origin` do portal e o cookie): `200 {accessToken}` + cookie novo; `401` sem cookie ou com cookie usado/inválido, `403` com `Origin` ausente ou de outro site.
- `POST /v1/admin/auth/logout` (aberta, mesmo `Origin`): `204`, revoga o token e apaga o cookie.
- `GET /v1/admin/auth/me` (staff): `{id, name, email, role, permissions[]}`; `403` para cliente.

## `POST /v1/admin/flights` (requer a permissão `FLIGHT_WRITE`)
Cadastra um voo, resolvendo companhia/origem/destino por código IATA, e gera automaticamente seu mapa de assentos a partir de `totalCapacity` e `aircraftType` — a quantidade de assentos por fileira (e onde ficam os corredores) vem de `SeatLayout.kt`, a única fonte dessa regra no sistema: `"Embraer E195"` → 2+2 (4/fileira), `"Airbus A320"` → 3+3 (6/fileira, o padrão), `"Boeing 777"` → 3+4+3 (10/fileira, widebody com 2 corredores). Qualquer outro valor cai no 3+3 padrão.

```bash
curl -X POST localhost:8080/v1/admin/flights \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <accessToken-de-um-staff-com-FLIGHT_WRITE>" \
  -d '{
    "flightNumber": "DB1234",
    "airlineIataCode": "LA",
    "originIataCode": "GRU",
    "destinationIataCode": "GIG",
    "departureTime": "2026-10-01T08:00:00",
    "arrivalTime": "2026-10-01T09:10:00",
    "seatClass": "ECONOMY",
    "price": 450.00,
    "totalCapacity": 180,
    "aircraftType": "Airbus A320"
  }'
```

`GET /v1/flights/search` e `GET /v1/flights/lowest-price` (e qualquer resposta com `FlightResponse`) devolvem `aircraftType` e `seatLayout` (ex.: `[3, 3]`) já resolvidos — o cliente nunca precisa saber qual avião mapeia pra qual layout, só agrupar os assentos pelo array que chega.

Retorna `201` com o voo criado (`availableCapacity` já refletindo os assentos recém-gerados, todos `AVAILABLE`), `401` sem token, `403` se o token não tiver a permissão `FLIGHT_WRITE`, ou `404` se o código IATA de companhia/origem/destino não existir.

## `GET /v1/flights/search?origin=&destination=&date=`
Busca voos por rota e data.

```bash
curl "localhost:8080/v1/flights/search?origin=GRU&destination=GIG&date=2026-10-01"
```

## `GET /v1/flights/lowest-price?destination=`
Menor preço real entre os voos ativos pra esse destino nos próximos 60 dias (público, mesmo espírito de `/flights/search`) — uma consulta agregada (`MIN(price)`), não uma varredura de voos. Alimenta o "from $X" da grade de destinos em destaque no mobile sem esse cliente precisar tentar várias datas uma por uma.

```bash
curl "localhost:8080/v1/flights/lowest-price?destination=GIG"
```

Retorna `200` com `{"destination": "GIG", "lowestPrice": 450.00}`, ou `404` se não houver nenhum voo ativo pra esse destino na janela.

## `GET /v1/destinations`
Todo aeroporto conhecido numa resposta só — código IATA, cidade, país, foto real, região, se é um destino em destaque (`isPopular`) e menor preço real (reaproveita a mesma agregação de `/flights/lowest-price`, sem duplicar a consulta). Público, mesmo espírito de `/flights/search`. Existe pra o cliente mobile não precisar de nenhuma lista de aeroportos fixa no app — a Home, a aba Explore e o seletor de origem/destino da busca renderizam exatamente essa lista, sem dado de negócio hardcoded no front. `region` e `isPopular` seguem o mesmo raciocínio de `photoUrl`: são atributos do aeroporto, não uma estrutura paralela — o cliente agrupa/filtra a mesma lista já carregada em vez de buscar "regiões" ou "destinos populares" como conceitos à parte.

```bash
curl localhost:8080/v1/destinations
```

Retorna `200` com `[{"iataCode": "GIG", "city": "Rio de Janeiro", "country": "Brasil", "photoUrl": "https://...", "region": "América do Sul", "isPopular": true, "lowestPrice": 305.00, "averageRating": 4.5}, ...]` — `lowestPrice` vem `null` quando não há voo ativo pra esse destino na janela de 60 dias, e `averageRating` (média de 1 a 5 das avaliações de reservas de voos pra esse destino, ver `POST /v1/reviews`) vem `null` enquanto ninguém avaliou nenhuma viagem até lá.

## `GET /v1/bookables/{id}/seats`
Retorna o mapa de assentos de um `Bookable` (público, mesmo espírito de `/flights/search`) — cada assento com seu `label` (ex.: `"12A"`) e `status` (`AVAILABLE`/`RESERVED`).

```bash
curl localhost:8080/v1/bookables/1/seats
```

Retorna `200` com a lista de assentos, ou `404` se o `bookableId` não existir.

## `GET /v1/bookings` (autenticado)
Lista todas as reservas do usuário autenticado ("minhas viagens") — cada item já vem com o `Seat` e o `Flight` completos, sem precisar de chamadas extras do lado do cliente, e também com a `review` (se a reserva já tiver sido avaliada) — o cliente nunca precisa guardar isso localmente pra saber se já avaliou. Sempre filtra por `authentication.currentUserId()`: nunca devolve reserva de outro usuário.

```bash
curl localhost:8080/v1/bookings \
  -H "Authorization: Bearer <accessToken>"
```

Retorna `200` com a lista (vazia se o usuário não tiver reservas), ou `401` sem token. Cada item traz o `price` **da reserva**, o valor de quando ela foi feita; o `flight.price` é o preço **atual** do voo e pode ser outro se ele foi reajustado depois.

## `POST /v1/bookings` (autenticado)
Reserva um assento específico (`seatId`) de um `Bookable` (hoje só `Flight`; qualquer especialização futura funciona sem mudar este endpoint) em nome do usuário autenticado, travando o assento sob lock otimista. `availableCapacity` do `Bookable` é derivado da contagem de assentos `AVAILABLE` — não é mais um contador em paralelo. Cria a reserva como `PENDING`.

```bash
curl -X POST localhost:8080/v1/bookings \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <accessToken>" \
  -d '{"bookableId": 1, "seatId": 1}'
```

Retorna `201` com a reserva criada (com o `price` que ficou congelado nela), `401` sem token, `400` se o `seatId` não pertencer ao `bookableId` informado, `404` se o `bookableId` ou `seatId` não existirem, ou `409` se o assento não estiver `AVAILABLE` (ou em caso de conflito de concorrência — duas reservas simultâneas disputando o mesmo assento).

## `POST /v1/bookings/{id}/cancel` (autenticado, dono ou `BOOKING_CANCEL_ANY`)
Cancela uma reserva `PENDING`, liberando o assento de volta a `AVAILABLE`. Só quem criou a reserva (ou quem tem a permissão `BOOKING_CANCEL_ANY`) pode cancelá-la.

```bash
curl -X POST localhost:8080/v1/bookings/1/cancel \
  -H "Authorization: Bearer <accessToken>"
```

Retorna `200` com a reserva `CANCELLED`, `401` sem token, `403` se não for o dono nem tiver `BOOKING_CANCEL_ANY`, `404` se não existir, ou `409` se a reserva não estiver `PENDING` (já confirmada ou já cancelada).

## `POST /v1/payments` (autenticado)
Paga uma ou mais reservas `PENDING` do usuário autenticado de uma vez só (ex.: ida + volta de uma Round Trip, num único pagamento) e as confirma (`CONFIRMED`). Não existe gateway de pagamento real por trás — só os 4 últimos dígitos do cartão e o nome do titular são recebidos e guardados; número completo e CVV nunca chegam ao backend.

```bash
curl -X POST localhost:8080/v1/payments \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <accessToken>" \
  -H "Idempotency-Key: 3f2b8c1e-6a4d-4e7a-9d1b-5c8e2a7f0b94" \
  -d '{"bookingIds": [1, 2], "cardLast4": "4242", "cardholderName": "Jane Doe"}'
```

O header **`Idempotency-Key` é obrigatório**: um UUID gerado pelo cliente, **um por tentativa de pagamento** (de 1 a 64 caracteres). Retorna `201` com o pagamento criado (`amount` já somando todas as reservas), `400` sem o header ou com uma chave inválida, `401` sem token, `403` se alguma reserva não pertencer a quem está pagando, `404` se algum `bookingId` não existir, `409` se alguma reserva não estiver `PENDING` ou se outra requisição com a mesma chave ainda está em andamento, ou `422` se a chave já foi usada para um pedido diferente.

**Idempotência.** Se a resposta se perde (timeout, rede móvel ruim) e o app tenta de novo, a retentativa **com a mesma chave devolve o pagamento original** (mesmo `201`, mesmo `id`), sem cobrar de novo. Sem a chave, essa segunda chamada seria um `409`, porque as reservas já estão `CONFIRMED`, mesmo com o dinheiro já cobrado.

- **Mesma chave e mesmo pedido** (as reservas, o cartão e o nome; a ordem das reservas não importa): devolve o pagamento original, sem tocar nas reservas.
- **Mesma chave e pedido diferente:** `422`, porque uma chave só pode significar um pedido.
- **Duas requisições iguais ao mesmo tempo:** um índice único `(customer_id, idempotency_key)` decide; quem perde recebe `409` e, se tentar de novo, cai no caso do replay.
- **A chave vale por usuário:** outro usuário com a mesma chave não enxerga o pagamento do primeiro.
- A chave e uma impressão digital do pedido (SHA-256) ficam na própria tabela `payment` (migration `V25`); pagamentos anteriores a ela ficam sem chave.

## `POST /v1/reviews` (autenticado)
Avalia uma reserva `CONFIRMED` do usuário autenticado — nota de 1 a 5 e comentário (obrigatório). Só quem fez a reserva pode avaliá-la, só depois de `CONFIRMED` (não dá pra avaliar antes de pagar), e só uma vez por reserva.

```bash
curl -X POST localhost:8080/v1/reviews \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <accessToken>" \
  -d '{"bookingId": 1, "rating": 5, "comment": "Great flight!"}'
```

Retorna `201` com a review criada, `400` se a nota estiver fora de 1-5, `401` sem token, `403` se a reserva não pertencer a quem está avaliando, `404` se o `bookingId` não existir, ou `409` se a reserva não estiver `CONFIRMED` ou já tiver sido avaliada.

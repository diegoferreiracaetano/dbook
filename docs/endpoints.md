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
| `INVALID_INVITATION` | 400 | convite de equipe desconhecido, vencido, já usado ou revogado (o mesmo erro para todos, de propósito) |
| `EMAIL_NOT_VERIFIED` | 403 | reservar ou pagar antes de confirmar o e-mail (com `account.require-verified-email` ligada) |
| `INVALID_ACCOUNT_TOKEN` | 400 | link de confirmação ou de nova senha inválido, vencido ou já usado |
| `TWO_FACTOR_REQUIRED` | 403 | conta da equipe com segundo fator (ou de papel que o exige) usou o login do cliente |
| `INVALID_TWO_FACTOR_CODE` | 400 | código do autenticador ou de recuperação errado, ou já usado |
| `FORBIDDEN` | 403 | sem permissão, não é o dono, ou `Origin` não permitido |
| `ACCOUNT_BLOCKED` | 403 | conta bloqueada (só depois de a senha estar certa) |
| `NOT_FOUND` | 404 | recurso inexistente |
| `CONFLICT` | 409 | conflito com o estado atual |
| `STALE_VERSION` | 409 | outra pessoa alterou o mesmo registro antes (lock otimista) |
| `IDEMPOTENCY_KEY_REUSED` | 422 | mesma `Idempotency-Key` para outro pedido |
| `TOO_MANY_ATTEMPTS` | 429 | falhas de login demais (`Retry-After` em segundos) |
| `RATE_LIMITED` | 429 | limite de uso: da IA, ou o geral por IP / por usuário (`Retry-After` em segundos) |
| `PAYLOAD_TOO_LARGE` | 413 | corpo acima do limite (256 KB; 5 MB na importação CSV de voos) |
| `AI_RESPONSE_INVALID` / `AI_UNAVAILABLE` | 502 / 503 | modelo de IA |

## `GET /v1/admin/audit` (requer a permissão `AUDIT_READ`)
A trilha de ações administrativas, do mais novo para o mais antigo, **por cursor**. Detalhes em [Auditoria](auditoria.md).

```bash
curl "localhost:8080/v1/admin/audit?action=FLIGHT_CREATED&size=20" \
  -H "Authorization: Bearer <accessToken-de-um-SUPER_ADMIN>"
```

Filtros opcionais: `actorId`, `action` (`FLIGHT_CREATED`, `BOOKING_CANCELLED_BY_STAFF`, `ACCESS_DENIED`), `targetType`, `targetId`, `outcome` (`SUCCESS`/`DENIED`), `from`/`to` (ISO-8601), `cursor` e `size` (1–100, padrão 50). Retorna `200` com `{"items": [...], "nextCursor": "..."}` (`nextCursor` é `null` na última página; cada item traz ator, ação, alvo, `before`/`after` e o `requestId`/`traceId`/`ip` da requisição), `401` sem token, `403` sem `AUDIT_READ` e `400` (`VALIDATION_FAILED`) para filtro, cursor ou tamanho inválidos.

## Recuperação de conta (`/v1/auth/*`)
Detalhes em [Autenticação](autenticacao.md#recuperação-de-conta-e-mail-confirmado-e-senha-esquecida).

- `POST /v1/auth/verify-email` (aberta): `{token}` → `204`; `400 INVALID_ACCOUNT_TOKEN`.
- `POST /v1/auth/resend-verification` (cliente logado): `204` com um link novo; `409` se já confirmou; `429`.
- `POST /v1/auth/forgot-password` (aberta): `{email}` → **sempre `202`** (exista a conta ou não); `429` se o mesmo endereço ou IP pedir demais.
- `POST /v1/auth/reset-password` (aberta): `{token, newPassword}` → `204` e todas as sessões da conta terminam; `400 INVALID_ACCOUNT_TOKEN` ou `400 VALIDATION_FAILED` (senha fraca: o link continua valendo).
- `POST /v1/auth/register` e `GET /v1/users/me` passam a trazer `emailVerified`.

## Portal administrativo: sessão (`/v1/admin/auth/*`)
Detalhes e exemplos em [Autenticação](autenticacao.md#portal-administrativo-sessão).

- `POST /v1/admin/auth/login` (aberta): `{email, password}` → `200 {accessToken}` + cookie de renovação, ou **`202 {challengeToken, enrollmentRequired}`** quando a conta precisa do segundo fator (sem sessão e sem cookie). `401` para senha errada **ou** para quem não é staff, `403 ACCOUNT_BLOCKED`, `429 TOO_MANY_ATTEMPTS`.
- `POST /v1/admin/auth/2fa/verify` (aberta): `{challengeToken, code}` (6 dígitos ou código de recuperação) → `200 {accessToken}` + cookie; `400 INVALID_TWO_FACTOR_CODE`, `401` com desafio inválido ou vencido, `429`.
- `POST /v1/admin/auth/2fa/enroll` (aberta): `{challengeToken}` → `{otpauthUri, manualEntryKey}`; `POST /v1/admin/auth/2fa/confirm` (aberta): `{challengeToken, code}` → `200 {accessToken, recoveryCodes[]}` + cookie (os códigos aparecem **só aqui**).
- `POST /v1/admin/2fa/enroll` (staff) → `{otpauthUri, manualEntryKey}`; `POST /v1/admin/2fa/confirm` `{code}` → `{recoveryCodes[]}`; `POST /v1/admin/2fa/disable` `{password, code}` → `204` (`409` se o papel exige; `401` com a senha errada; `400 INVALID_TWO_FACTOR_CODE`). `409` se já está ligado / não há cadastro pendente.
- `POST /v1/admin/auth/refresh` (aberta, exige `Origin` do portal e o cookie): `200 {accessToken}` + cookie novo; `401` sem cookie ou com cookie usado/inválido, `403` com `Origin` ausente ou de outro site.
- `POST /v1/admin/auth/logout` (aberta, mesmo `Origin`): `204`, revoga o token e apaga o cookie.
- `GET /v1/admin/auth/me` (staff): `{id, name, email, role, permissions[], twoFactorEnabled, twoFactorRequired}`; `403` para cliente.
- `POST /v1/admin/auth/change-password` (staff): `{currentPassword, newPassword}` → `204`, encerra todas as sessões. `401 INVALID_CREDENTIALS` com a senha atual errada, `400 VALIDATION_FAILED` com a nova fraca, `429 TOO_MANY_ATTEMPTS`.

## Equipe: convites e gestão (requer a permissão `ADMIN_MANAGE`)
Fluxo e modelo de ameaças em [Autenticação](autenticacao.md#equipe-convite-e-gestão). Nenhuma resposta traz o token do convite nem o seu hash.

- `POST /v1/admin/invitations` `{email, role}` (`role` ∈ `SUPPORT`, `CATALOG_MANAGER`, `SUPER_ADMIN`): `201 {id, email, role, status, invitedBy, createdAt, expiresAt}`. `409` se o e-mail já tem conta (qualquer caixa); convidar de novo um endereço com convite aberto revoga o anterior.
- `GET /v1/admin/invitations`: os 100 convites mais recentes, com `status` (`PENDING`, `ACCEPTED`, `EXPIRED`, `REVOKED`).
- `POST /v1/admin/invitations/{id}/resend`: `200`, manda um **link novo** (o anterior morre). `409` se o convite já foi aceito ou revogado, `404` se não existe.
- `DELETE /v1/admin/invitations/{id}`: `204`, o link para de funcionar.
- `POST /v1/admin/invitations/accept` (**aberta**, o token é a prova) `{token, name, password}`: `201 {id, name, email, role, permissions[]}`. `400 INVALID_INVITATION` para token desconhecido, expirado, usado ou revogado (mesma resposta), `400 VALIDATION_FAILED` para senha fraca (o convite continua valendo).
- `GET /v1/admin/staff`: a equipe, bloqueados inclusive, `{id, name, email, role, status, blockedReason, lastLoginAt}`.
- `PATCH /v1/admin/staff/{id}/role` `{role}`: `200`, encerra as sessões da pessoa. `409` no próprio id ou no último `SUPER_ADMIN` ativo; `400` ao tentar `CLIENT` (para tirar o acesso, bloqueie); `404` para id inexistente **ou de cliente**.
- `POST /v1/admin/staff/{id}/block` `{reason}` e `POST /v1/admin/staff/{id}/unblock`: `200` com o membro; mesmas regras de `409`/`404`.

## CRM: clientes (`/v1/admin/customers`, requer a permissão `CUSTOMER_READ`)
Só enxerga **clientes**: o id de um membro da equipe ou inexistente é `404 NOT_FOUND`. Listas por página: `page` (a partir de 0) e `size` (1–100, padrão 20), com `{items, page, size, totalElements, totalPages}`; `400 VALIDATION_FAILED` para valor fora disso.

- `GET /v1/admin/customers?query=&status=&createdFrom=&createdTo=&hasBookings=&sort=&direction=&page=&size=`: busca **sem caixa e sem acento** em nome e e-mail (`%` e `_` valem como caracteres), `status` ∈ `ACTIVE`/`BLOCKED`, datas em ISO-8601, `sort` ∈ `NAME`/`EMAIL`/`CREATED_AT`/`LAST_LOGIN_AT` (padrão `CREATED_AT`), `direction` ∈ `ASC`/`DESC` (padrão `DESC`); desempate por id. Cada item: `{id, name, email, status, createdAt, lastLoginAt, bookingCount}`.
- `GET /v1/admin/customers/{id}`: a visão 360º, `{id, name, email, status, blockedReason, blockedAt, createdAt, lastLoginAt, bookings{total, pending, confirmed, cancelled}, payments{count, totalPaid}, reviews{count, averageRating}}`. `averageRating` é `null` sem avaliações.
- `GET /v1/admin/customers/{id}/bookings`: `{id, status, price, title, seatLabel, flightNumber, origin, destination, departureTime, paymentId}`; `price` é o **congelado** na reserva; os campos de voo são `null` para o que não é voo.
- `GET /v1/admin/customers/{id}/payments`: `{id, amount, cardLast4, createdAt, bookingIds[]}` (sem o nome do titular).
- `GET /v1/admin/customers/{id}/reviews`: `{id, bookingId, rating, comment, createdAt}`.
- `GET /v1/admin/customers/{id}` também grava `CUSTOMER_VIEWED` (uma vez por ator e cliente a cada 5 minutos) e traz `anonymizedAt`.
- `GET /v1/admin/customers/{id}/notes` (`CUSTOMER_READ`): página de `{id, authorId, body, pinned, createdAt, editedAt}`, fixadas primeiro. `POST` (`CUSTOMER_NOTE`) `{body, pinned}` → `201`. `PATCH /{noteId}` `{body?, pinned?}` → `200`, só o autor (`403` para outro; `400` se nenhum campo vai). `DELETE /{noteId}` → `204`, o autor ou `SUPER_ADMIN`. `404` para nota apagada ou de outro cliente. O texto nunca aparece na auditoria.
- `POST /v1/admin/customers/{id}/block` `{reason}` (`CUSTOMER_BLOCK`, motivo de 10+ caracteres) e `POST .../unblock` → `200 {id, status, blockedReason, blockedAt}`. `409` ao bloquear quem já está bloqueado e ao desbloquear uma conta anonimizada.
- `GET /v1/admin/customers/export?query=&status=&createdFrom=&createdTo=&hasBookings=&sort=&direction=` (`CUSTOMER_EXPORT`, só `SUPER_ADMIN`): `text/csv` (`id,name,email,status,createdAt,lastLoginAt,bookingCount`), UTF-8 com BOM, até 50 000 linhas, `Content-Disposition: attachment; filename="customers.csv"`. Células que começariam uma fórmula ganham `'`. Gravado na auditoria antes de enviar o primeiro byte.
- `POST /v1/admin/customers/{id}/anonymize` `{reason, confirmation}` (`CUSTOMER_ERASE`, só `SUPER_ADMIN`): `confirmation` é exatamente `ANONYMIZE <id>`. `200 {id, status, blockedReason, blockedAt}`; `400` para frase ou motivo errados; `409` se já foi anonimizada; `404` para staff ou id inexistente. Irreversível; ver [CRM](crm.md#anonimizar-direito-ao-esquecimento).
- `GET /v1/users/me/export` (autenticado): `{profile{id, name, email, lastLoginAt}, bookings[], payments[], reviews[]}` com tudo o que o sistema guarda sobre quem pede.
- `DELETE /v1/users/me` `{password}` (autenticado, cliente): `204`, anonimiza a própria conta. `401` com a senha errada (conta no limite do login), `409` para conta de equipe.

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

## Dashboard (`/v1/admin/dashboard`)
O significado de cada número, o cache e as medições em [Dashboard](dashboard.md). Requer `DASHBOARD_READ` (`SUPPORT`, `CATALOG_MANAGER`, `SUPER_ADMIN`). `from` e `to` são datas (`2026-09-01`), as duas incluídas, no fuso de São Paulo; sem elas, os últimos 30 dias; no máximo 400 dias (`400` se passar, se `to` for antes de `from` ou se a data for inválida). As respostas ficam em cache por 60 segundos.

- `GET /v1/admin/dashboard/summary?from=&to=`: `{from, to, bookingsByStatus{STATUS: n}, grossRevenue, refunded, netRevenue, newCustomers, conversionRate, expirationRate, averageOccupancy}`; as taxas vão de 0 a 1 (quatro casas) ou `null` sem o que dividir.
- `GET /v1/admin/dashboard/timeseries?metric=&granularity=&from=&to=`: `metric` ∈ `REVENUE` (líquida), `BOOKINGS`, `NEW_CUSTOMERS`; `granularity` ∈ `DAY` (padrão), `WEEK` (a data é a segunda-feira). `{metric, granularity, points[{date, value}]}`, sem buracos (os vazios valem 0).
- `GET /v1/admin/dashboard/top-routes?limit=&from=&to=`: `{limit, routes[{origin, destination, bookings, revenue}]}`, `limit` de 1 a 50 (padrão 10).

## Catálogo administrativo (`/v1/admin/flights`, `airlines`, `airports`, `aircraft-models`)
Regras, limites e a importação em [Catálogo administrativo](catalogo-admin.md).

- `GET /v1/admin/flights?origin=&destination=&airline=&departureFrom=&departureTo=&status=&page=&size=` (`FLIGHT_READ`): por partida, mais cedo primeiro, `{items, page, size, totalElements, totalPages}`. Cada voo: `{id, flightNumber, airlineIataCode, airlineName, origin, destination, departureTime, arrivalTime, seatClass, price, totalCapacity, availableSeats, reservedSeats, aircraftType, seatLayout, status, version}`; `status` ∈ `SCHEDULED`, `CANCELLED`; os horários não têm fuso (ex.: `2026-10-01T08:00:00`).
- `GET /v1/admin/flights/{id}` (`FLIGHT_READ`): `{flight, activeBookings}`. `404` se não existe.
- `PUT /v1/admin/flights/{id}` (`FLIGHT_WRITE`): o corpo do cadastro mais **`version`** (a que se leu). `200 {flight, activeBookings}`. `409 STALE_VERSION` se mudou desde a leitura; `409` se a capacidade ficaria abaixo dos assentos reservados, se tiraria um assento com reserva ou histórico, se o layout do avião muda depois de uma reserva, ou se o voo está cancelado; `400` para chegada antes da partida ou o mesmo aeroporto; `404` para voo, companhia ou aeroporto desconhecido.
- `POST /v1/admin/flights/{id}/cancel` (`FLIGHT_WRITE`): tira o voo da venda. `409` com a quantidade se tem reservas ativas, ou se já está cancelado.
- `POST /v1/admin/flights/import?dryRun=true` (`FLIGHT_WRITE`), corpo `text/csv`: `{dryRun, totalRows, toCreate, alreadyExisting, created, errors[{line, message}]}`. `422` com `dryRun=false` e erros (nada é gravado); `400` para arquivo vazio, coluna faltando, aspas abertas ou mais de 5000 linhas.
- `GET /v1/admin/airlines` e `GET /v1/admin/airports` (`FLIGHT_READ`); `POST`, `PUT /{id}` e `DELETE /{id}` (`CATALOG_WRITE`). Companhia `{iataCode, name}` (2 letras ou dígitos); aeroporto `{iataCode, name, city, country, photoUrl, region, isPopular}` (3 letras). `409` para código repetido ou para remover o que um voo usa; `400` para código de formato inválido; `404` para id desconhecido.
- `GET /v1/admin/aircraft-models` (`FLIGHT_READ`): `[{name, seatLayout, seatsPerRow}]`.
- A busca pública (`GET /v1/flights/search`) **ignora voos cancelados**, e `POST /v1/bookings` recusa um voo que não está à venda (`409`).

## Reservas (admin) e reembolso (`/v1/admin/bookings` e `/v1/admin/refunds`)
Máquina de estados, política e a saga em [Reembolso](reembolso.md).

- `GET /v1/admin/bookings?status=&bookableId=&customerId=&createdFrom=&createdTo=&paid=&page=&size=` (`BOOKING_READ_ANY`): mais recentes primeiro, com `{items, page, size, totalElements, totalPages}`. Cada item: `{id, status, price, createdAt, customerId, customerName, bookableId, title, seatLabel, flightNumber, origin, destination, departureTime, paymentId}`; `price` é o **congelado** na reserva e os campos de voo são `null` para o que não é voo. `400` para período invertido ou página inválida.
- `GET /v1/admin/bookings/{id}` (`BOOKING_READ_ANY`): `{booking, payment{id, amount, cardLast4, createdAt}, refund{id, status, amount, reason}, timeline[{from, to, actorId, occurredAt}]}`. `actorId` é `null` quando foi o sistema (a expiração) e `from` é `null` na criação. `404` se não existe.
- `POST /v1/admin/bookings/{id}/refund` (`PAYMENT_REFUND`) com o header **`Idempotency-Key`** (1 a 64 caracteres) e `{reason, note?, override?}`, `reason` ∈ `CUSTOMER_REQUEST`, `FLIGHT_CANCELLED`, `DUPLICATE`, `OTHER`. Reembolsa a reserva `CONFIRMED` **por inteiro**. `201 {id, bookingId, paymentId, amount, reason, status, failureReason, requestedBy, createdAt, completedAt}`, em que `status` é `COMPLETED` ou `FAILED` (o dinheiro não saiu: tente `/retry`). `400` sem o header, sem nota com `override` ou com valor inválido; `403` se não tem `PAYMENT_REFUND` ou usa `override` sem ser `SUPER_ADMIN`; `404` reserva inexistente; `409` se a reserva não é `CONFIRMED`, já tem reembolso, ou `409 REFUND_WINDOW_CLOSED` dentro das 24 horas antes da partida; `422` se a chave já foi usada para outro pedido.
- `GET /v1/admin/refunds?status=&page=&size=` e `GET /v1/admin/refunds/{id}` (`PAYMENT_REFUND`): para achar os `FAILED`.
- `POST /v1/admin/refunds/{id}/retry` (`PAYMENT_REFUND`): tenta de novo um reembolso `FAILED` (ou um que uma queda deixou `REQUESTED`); `409` se já foi concluído.
- O status novo **`REFUNDED`** também aparece em `GET /v1/bookings` ("Minhas Viagens") e em `POST /v1/bookings/{id}/cancel`.

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

## Notificações (`/v1/notifications`, autenticado)
A caixa de entrada, as preferências e os aparelhos do **próprio** usuário. Detalhes (eventos, canais, idempotência) em [notificacoes.md](notificacoes.md).

- `GET /v1/notifications?cursor=&size=20&unreadOnly=false` → `{items: [{id, type, title, body, data, read, createdAt}], nextCursor}`; `400` se `size` estiver fora de 1 a 100.
- `GET /v1/notifications/unread-count` → `{count}`.
- `POST /v1/notifications/{id}/read` → `204`; `404` se não for sua.
- `POST /v1/notifications/read-all` → `{updated}`.
- `GET /v1/notifications/preferences` → todos os pares tipo × canal (`BOOKING_CONFIRMED`, `BOOKING_EXPIRED`, `BOOKING_CANCELLED_BY_STAFF`, `REFUND_COMPLETED`, `FLIGHT_CHANGED` × `IN_APP`, `EMAIL`, `PUSH`), ligados por padrão.
- `PUT /v1/notifications/preferences` com `[{"type": "BOOKING_CONFIRMED", "channel": "EMAIL", "enabled": false}]` → a lista em vigor; `400` para tipo ou canal desconhecido.
- `POST /v1/notifications/devices` com `{"token": "...", "platform": "ANDROID"}` → `204`.
- `DELETE /v1/notifications/devices/{token}` → `204`.

## Ciclo de vida da API
Detalhes em [versionamento.md](versionamento.md).

- `GET /v1/app-config` (**público**) → `{android: {minSupportedVersion, latestVersion, storeUrl}, ios: {...}}`.
- `GET /v2/destinations` (**público**) → destinos com `price: {lowest}` e `rating: {average}` (o v1, `GET /v1/destinations`, está **obsoleto**: responde com `Deprecation`, `Sunset` e `Link`).
- Todo cliente deve mandar `X-App-Version` e `X-App-Platform`.
- WebSocket em `/v1/ws` (o `/ws` antigo segue respondendo).

## Hotéis (`/v1/accommodations`)
Detalhes em [hoteis.md](hoteis.md).

- `GET /v1/accommodations/search?destination=GIG&checkIn=&checkOut=&guests=2&page&size` (**público**) → hotéis com um quarto livre em todas as noites, cada um com os quartos livres e o preço da estadia; `400` para datas ruins, no passado, mais de 30 noites ou hóspedes fora de 1 a 10.
- `GET /v1/accommodations/{id}` e `GET /v1/accommodations/{id}/reviews` (**públicos**); `404` se o hotel não existe ou está fora de venda.
- `POST /v1/accommodations/{id}/bookings` (autenticado) com `{roomTypeId, checkIn, checkOut, guests}` → `201` com a reserva (`stay`, `price = noites × diária`); `409 ROOM_UNAVAILABLE`, `404`, `400`.
- `GET /v1/bookings` traz `stay` e `accommodation` nas estadias (e `seat` e `flight` nulos); as reservas ganharam `discount` e `paidAmount`.
- Admin (`CATALOG_WRITE`): `POST/GET /v1/admin/accommodations`, `GET/PUT /{id}`, `POST /{id}/deactivate` e `/activate`, `POST /{id}/room-types`, `PUT /{id}/room-types/{roomTypeId}`.

## Cancelamento e reembolso pelo cliente
Detalhes em [reembolso.md](reembolso.md).

- `GET /v1/bookings/{id}/cancellation-policy` (autenticado, dono) → `{bookingId, action: CANCEL|REFUND_REQUEST|NONE, refundAmount, refundableUntil, blockedBy}`; `403` se não for sua, `404`.
- `POST /v1/bookings/{id}/refund-request` (autenticado, dono, header `Idempotency-Key` obrigatório) → `201` com o reembolso (`COMPLETED` ou `FAILED`); `409 REFUND_WINDOW_CLOSED` dentro das últimas 24 h; `409` se a reserva não está paga ou já tem reembolso; `403`; `400` sem a chave; `422` chave reaproveitada para outro pedido.

## Histórico e alertas de preço
Detalhes em [precos.md](precos.md).

- `GET /v1/flights/{id}/price-history` (**público**) → `{flightId, current, lowest, highest, points: [{price, changedAt}]}`; `404` se o voo não existe.
- `POST /v1/price-alerts` com `{origin, destination, date, targetPrice}` → `201`; `404` aeroporto inexistente, `400` entrada inválida, `409` alerta repetido ou `PRICE_ALERTS_LIMIT` (20 ativos).
- `GET /v1/price-alerts`, `PATCH /v1/price-alerts/{id}` (`{targetPrice?, active?}`), `DELETE /v1/price-alerts/{id}` (autenticados, só os do usuário).

## Códigos promocionais
Detalhes em [promocoes.md](promocoes.md).

- `POST /v1/promo-codes/validate` (autenticado) com `{code, bookingIds}` → `{code, type, subtotal, discount, total}`, sem usar o código; `404` se o código não existe, `422` com `code=PROMO_REJECTED` se não pode ser usado.
- `POST /v1/payments` aceita `promoCode` (opcional): a resposta traz `subtotal`, `discount` e `promoCode`; `422 PROMO_REJECTED` desfaz tudo e as reservas seguem `PENDING`.
- As reservas (`GET /v1/bookings` e as do admin) ganham `discount` e `paidAmount`.
- `POST/GET /v1/admin/promo-codes`, `GET /{id}`, `GET /{id}/redemptions`, `PUT /{id}`, `POST /{id}/deactivate` e `/activate` (`PROMO_WRITE`).

## Favoritos (`/v1/favorites`, autenticado)
Detalhes em [favoritos.md](favoritos.md).

- `PUT /v1/favorites/{type}/{id}` com `type` = `DESTINATION` (IATA em maiúsculas) ou `FLIGHT` (id) → `204`, idempotente; `404` se o alvo não existe; `400` se o id ou o tipo são inválidos; `409` com `code=FAVORITES_LIMIT` no 201º.
- `DELETE /v1/favorites/{type}/{id}` → `204`, idempotente.
- `GET /v1/favorites?type=&page=&size=` → `{items: [{type, id, createdAt, destination?, flight?}], page, size, totalElements, totalPages}`, do mais novo ao mais antigo.

## Avaliações públicas e moderação
Detalhes em [avaliacoes.md](avaliacoes.md).

- `GET /v1/destinations/{iata}/reviews?sort=RECENT|RATING&page&size` (**público**) → `{summary: {average, total, distribution}, reviews: {items: [{id, rating, comment, author, createdAt, edited}], page, size, totalElements, totalPages}}`; `400` se o código não tem 3 letras.
- `PATCH /v1/reviews/{id}` (autor) com `{rating?, comment?}` → a avaliação; `400` sem nenhum campo, `403` se não for sua, `404`.
- `DELETE /v1/reviews/{id}` (autor) → `204`; `403`, `404`.
- `POST /v1/reviews/{id}/report` com `{reason}` → `204`; `409` se já denunciou ou é a sua; `404` se inexistente ou oculta.
- `GET /v1/admin/reviews?status=REPORTED|HIDDEN|VISIBLE&page&size` (`REVIEW_MODERATE`).
- `POST /v1/admin/reviews/{id}/hide` `{reason}`, `/restore` e `/dismiss-reports` (`REVIEW_MODERATE`, auditados).

## `POST /v1/reviews` (autenticado)
Avalia uma reserva `CONFIRMED` do usuário autenticado — nota de 1 a 5 e comentário (obrigatório). Só quem fez a reserva pode avaliá-la, só depois de `CONFIRMED` (não dá pra avaliar antes de pagar), e só uma vez por reserva.

```bash
curl -X POST localhost:8080/v1/reviews \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <accessToken>" \
  -d '{"bookingId": 1, "rating": 5, "comment": "Great flight!"}'
```

Retorna `201` com a review criada, `400` se a nota estiver fora de 1-5, `401` sem token, `403` se a reserva não pertencer a quem está avaliando, `404` se o `bookingId` não existir, ou `409` se a reserva não estiver `CONFIRMED` ou já tiver sido avaliada.

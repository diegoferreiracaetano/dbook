# Endpoints

[← Voltar ao README](../README.md)

Todos os endpoints da API, com exemplos de `curl` e os códigos de status de cada um.

## `GET /health`
Confirma que a aplicação está no ar.

## `POST /admin/flights` (requer role `ADMIN`)
Cadastra um voo, resolvendo companhia/origem/destino por código IATA, e gera automaticamente seu mapa de assentos a partir de `totalCapacity` e `aircraftType` — a quantidade de assentos por fileira (e onde ficam os corredores) vem de `SeatLayout.kt`, a única fonte dessa regra no sistema: `"Embraer E195"` → 2+2 (4/fileira), `"Airbus A320"` → 3+3 (6/fileira, o padrão), `"Boeing 777"` → 3+4+3 (10/fileira, widebody com 2 corredores). Qualquer outro valor cai no 3+3 padrão.

```bash
curl -X POST localhost:8080/admin/flights \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <accessToken-de-um-ADMIN>" \
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

`GET /flights/search` e `GET /flights/lowest-price` (e qualquer resposta com `FlightResponse`) devolvem `aircraftType` e `seatLayout` (ex.: `[3, 3]`) já resolvidos — o cliente nunca precisa saber qual avião mapeia pra qual layout, só agrupar os assentos pelo array que chega.

Retorna `201` com o voo criado (`availableCapacity` já refletindo os assentos recém-gerados, todos `AVAILABLE`), `401` sem token, `403` se o token não for de um `ADMIN`, ou `404` se o código IATA de companhia/origem/destino não existir.

## `GET /flights/search?origin=&destination=&date=`
Busca voos por rota e data.

```bash
curl "localhost:8080/flights/search?origin=GRU&destination=GIG&date=2026-10-01"
```

## `GET /flights/lowest-price?destination=`
Menor preço real entre os voos ativos pra esse destino nos próximos 60 dias (público, mesmo espírito de `/flights/search`) — uma consulta agregada (`MIN(price)`), não uma varredura de voos. Alimenta o "from $X" da grade de destinos em destaque no mobile sem esse cliente precisar tentar várias datas uma por uma.

```bash
curl "localhost:8080/flights/lowest-price?destination=GIG"
```

Retorna `200` com `{"destination": "GIG", "lowestPrice": 450.00}`, ou `404` se não houver nenhum voo ativo pra esse destino na janela.

## `GET /destinations`
Todo aeroporto conhecido numa resposta só — código IATA, cidade, país, foto real, região, se é um destino em destaque (`isPopular`) e menor preço real (reaproveita a mesma agregação de `/flights/lowest-price`, sem duplicar a consulta). Público, mesmo espírito de `/flights/search`. Existe pra o cliente mobile não precisar de nenhuma lista de aeroportos fixa no app — a Home, a aba Explore e o seletor de origem/destino da busca renderizam exatamente essa lista, sem dado de negócio hardcoded no front. `region` e `isPopular` seguem o mesmo raciocínio de `photoUrl`: são atributos do aeroporto, não uma estrutura paralela — o cliente agrupa/filtra a mesma lista já carregada em vez de buscar "regiões" ou "destinos populares" como conceitos à parte.

```bash
curl localhost:8080/destinations
```

Retorna `200` com `[{"iataCode": "GIG", "city": "Rio de Janeiro", "country": "Brasil", "photoUrl": "https://...", "region": "América do Sul", "isPopular": true, "lowestPrice": 305.00, "averageRating": 4.5}, ...]` — `lowestPrice` vem `null` quando não há voo ativo pra esse destino na janela de 60 dias, e `averageRating` (média de 1 a 5 das avaliações de reservas de voos pra esse destino, ver `POST /reviews`) vem `null` enquanto ninguém avaliou nenhuma viagem até lá.

## `GET /bookables/{id}/seats`
Retorna o mapa de assentos de um `Bookable` (público, mesmo espírito de `/flights/search`) — cada assento com seu `label` (ex.: `"12A"`) e `status` (`AVAILABLE`/`RESERVED`).

```bash
curl localhost:8080/bookables/1/seats
```

Retorna `200` com a lista de assentos, ou `404` se o `bookableId` não existir.

## `GET /bookings` (autenticado)
Lista todas as reservas do usuário autenticado ("minhas viagens") — cada item já vem com o `Seat` e o `Flight` completos, sem precisar de chamadas extras do lado do cliente, e também com a `review` (se a reserva já tiver sido avaliada) — o cliente nunca precisa guardar isso localmente pra saber se já avaliou. Sempre filtra por `authentication.currentUserId()`: nunca devolve reserva de outro usuário.

```bash
curl localhost:8080/bookings \
  -H "Authorization: Bearer <accessToken>"
```

Retorna `200` com a lista (vazia se o usuário não tiver reservas), ou `401` sem token.

## `POST /bookings` (autenticado)
Reserva um assento específico (`seatId`) de um `Bookable` (hoje só `Flight`; qualquer especialização futura funciona sem mudar este endpoint) em nome do usuário autenticado, travando o assento sob lock otimista. `availableCapacity` do `Bookable` é derivado da contagem de assentos `AVAILABLE` — não é mais um contador em paralelo. Cria a reserva como `PENDING`.

```bash
curl -X POST localhost:8080/bookings \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <accessToken>" \
  -d '{"bookableId": 1, "seatId": 1}'
```

Retorna `201` com a reserva criada, `401` sem token, `400` se o `seatId` não pertencer ao `bookableId` informado, `404` se o `bookableId` ou `seatId` não existirem, ou `409` se o assento não estiver `AVAILABLE` (ou em caso de conflito de concorrência — duas reservas simultâneas disputando o mesmo assento).

## `POST /bookings/{id}/cancel` (autenticado, dono ou ADMIN)
Cancela uma reserva `PENDING`, liberando o assento de volta a `AVAILABLE`. Só quem criou a reserva (ou um `ADMIN`) pode cancelá-la.

```bash
curl -X POST localhost:8080/bookings/1/cancel \
  -H "Authorization: Bearer <accessToken>"
```

Retorna `200` com a reserva `CANCELLED`, `401` sem token, `403` se não for o dono nem `ADMIN`, `404` se não existir, ou `409` se a reserva não estiver `PENDING` (já confirmada ou já cancelada).

## `POST /payments` (autenticado)
Paga uma ou mais reservas `PENDING` do usuário autenticado de uma vez só (ex.: ida + volta de uma Round Trip, num único pagamento) e as confirma (`CONFIRMED`). Não existe gateway de pagamento real por trás — só os 4 últimos dígitos do cartão e o nome do titular são recebidos e guardados; número completo e CVV nunca chegam ao backend.

```bash
curl -X POST localhost:8080/payments \
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

## `POST /reviews` (autenticado)
Avalia uma reserva `CONFIRMED` do usuário autenticado — nota de 1 a 5 e comentário (obrigatório). Só quem fez a reserva pode avaliá-la, só depois de `CONFIRMED` (não dá pra avaliar antes de pagar), e só uma vez por reserva.

```bash
curl -X POST localhost:8080/reviews \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <accessToken>" \
  -d '{"bookingId": 1, "rating": 5, "comment": "Great flight!"}'
```

Retorna `201` com a review criada, `400` se a nota estiver fora de 1-5, `401` sem token, `403` se a reserva não pertencer a quem está avaliando, `404` se o `bookingId` não existir, ou `409` se a reserva não estiver `CONFIRMED` ou já tiver sido avaliada.

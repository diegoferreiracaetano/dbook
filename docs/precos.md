# Histórico e alerta de preço

O cliente vê **como o preço de um voo se moveu** e pode pedir **um aviso quando algum voo de uma rota e data chegar a um valor**. Os dois nascem de um fato só: o voo ganhou um preço.

## O fato: o voo ganhou um preço

Toda vez que um voo ganha um preço (ao ser criado, inclusive pela importação CSV, ou ao ser editado), o `FlightPriceRecorder` faz duas coisas **na mesma transação**:
1. grava o preço no histórico (`flight_price_history`; a primeira linha é o preço de abertura);
2. escreve no outbox o evento `flight.price-changed` (`flightId`, rota, data, `price`, `previousPrice` ou nulo na criação).

O mesmo preço de novo **não** é uma mudança: nada é gravado (mesmo que o número venha com outra quantidade de casas). O dinheiro nos eventos sempre tem os centavos (`80.00`).

## O histórico

`GET /v1/flights/{id}/price-history` (**público**, como a busca que mostra o preço) devolve `{flightId, current, lowest, highest, points: [{price, changedAt}]}`, do mais antigo ao mais novo (no máximo os 500 mais recentes). Voo inexistente: `404`.

## Os alertas (`/v1/price-alerts`, autenticado, sempre os do próprio usuário)

Um alerta é **uma rota e uma data** (`origin`, `destination`, `date`) e um **alvo** (`targetPrice`): "avise quando um voo GRU→GIG em 15/01/2027 custar até R$ 300".

| Endpoint | O que faz |
|---|---|
| `POST /v1/price-alerts` | cria (`201`); `404` se um aeroporto não existe; `400` para data passada, origem igual ao destino, código fora do padrão ou alvo que não é positivo; `409` para um segundo alerta da mesma rota e data (mude o alvo no que já existe); `409` com `code=PRICE_ALERTS_LIMIT` no 21º ativo |
| `GET /v1/price-alerts?page&size` | os do cliente, mais novos primeiro |
| `PATCH /v1/price-alerts/{id}` | muda o alvo, liga ou desliga (`404` se não for seu, como se não existisse) |
| `DELETE /v1/price-alerts/{id}` | apaga (`204`; `404` se não for seu) |

O limite (20 ativos para datas de hoje em diante) é decidido de forma atômica, travando a linha do cliente, como nos favoritos. Um alerta de data já passada deixa de contar e nunca dispara (a data precisa ser igual).

## O disparo

O consumidor da fila de notificações recebe o `flight.price-changed` e, em vez de avisar alguém, o entrega ao `EvaluatePriceAlertsUseCase`, que **numa transação**:
1. **reivindica**, num único comando, os alertas ativos daquela rota e data cujo alvo o preço atende (`target_price >= price`, o alvo **inclui** o valor) e que **não foram avisados nas últimas 24 horas**, marcando-os como avisados agora (`UPDATE ... RETURNING`);
2. escreve, para cada um, o evento `price-alert.triggered` no outbox.

O resto é o pipeline do M36: o evento vira o tipo `PRICE_ALERT`, a notificação in-app, o e-mail e o push, respeitando as preferências do cliente e a idempotência por evento e canal. A mensagem: "O voo GRU-GIG em 15/01 está por R$ 250,00, dentro do seu alvo de R$ 300,00."

- **Janela de 24 horas:** um preço que sobe e desce em volta do alvo não manda uma mensagem a cada oscilação; passada a janela, o próximo preço que atende o alvo avisa de novo.
- **Idempotência da entrega:** como a reivindicação é **um comando**, o mesmo evento entregue duas vezes, ou dez entregas ao mesmo tempo, avisam **uma vez só** por janela (testado com 10 *threads*).
- **Mudar o alvo zera a janela:** um alvo novo é uma promessa nova; o próximo preço que o atende avisa, mesmo que o alerta tenha avisado há pouco.
- **Alerta desligado** não dispara.
- **Em aberto (de propósito):** criar um alerta quando já existe um voo abaixo do alvo **não** avisa na hora, só o próximo preço; o app mostra o menor preço atual na busca.

## Desenho

- Migration `V44`: `flight_price_history` (índice `(flight_id, changed_at, id)`) e `price_alert` (único por `(user_id, origin, destination, travel_date)`, índice parcial das ativas por rota e data, que é o que o avaliador lê).
- Conceito novo `pricing` (15 no total); `notification` passa a depender dele (o tipo `PRICE_ALERT` leva o nome do evento).
- A anonimização de um cliente apaga os alertas dele.
- O `outbox.queues.price-alert` roteia o `price-alert.triggered` para a fila de notificações.

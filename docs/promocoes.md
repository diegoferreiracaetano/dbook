# Códigos promocionais

Um código (`WELCOME10`) tira dinheiro de um pagamento. O cliente o digita na hora de pagar, o app pode **prever** o desconto antes, e a equipe cria, muda e desliga os códigos. O ponto delicado é a contagem de usos: dois clientes disputando o último uso nunca podem ganhar os dois.

## Como um código funciona

| Campo | O que é |
|---|---|
| `code` | de 3 a 32 letras, dígitos, `-` ou `_`; **guardado em maiúsculas**, então `welcome10` e `WELCOME10` são o mesmo código (índice único) |
| `type` e `value` | `PERCENT` (abaixo de 100) ou `FIXED` (um valor em dinheiro); **nunca mudam** depois de criado: outro desconto é outro código |
| `minAmount` | o total precisa ser pelo menos isso |
| `validFrom` / `validUntil` | a janela: o início **vale**, o fim **não** (usa-se até o último instante antes dele) |
| `maxRedemptions` | quantas vezes ao todo (vazio = sem limite) |
| `maxPerUser` | quantas vezes cada cliente (padrão 1) |
| `active` | ligado ou desligado; **desligar em vez de apagar**, porque os pagamentos que o usaram continuam apontando para ele |

**O cálculo** (`PromoCode.discountFor`, no domínio): `PERCENT` é o total × percentual, **arredondado para o centavo, metade para cima** (10 % de 100,05 é 10,01); `FIXED` é o valor. Em qualquer caso **nunca passa do total menos um centavo por reserva**: um pagamento nunca é grátis, e nenhuma reserva fica sem nada para reembolsar.

## Pagar com um código

`POST /v1/payments` aceita `promoCode` (opcional, em qualquer caixa). A resposta ganha `subtotal`, `discount` e `promoCode`; `amount` continua sendo **o que foi cobrado** (`subtotal − discount`).

- **Tudo ou nada:** o pagamento, o uso do código e a confirmação das reservas acontecem na **mesma transação**. Se o código não puder ser usado (expirado, esgotado, acima do mínimo, fora do limite do cliente), o pagamento inteiro é desfeito e as reservas continuam `PENDING`: `422` com `code = PROMO_REJECTED` e o motivo na mensagem. Código inexistente é `404`.
- **A parte de cada reserva:** o desconto é dividido entre as reservas do pagamento **em proporção ao preço, ao centavo** (`allocateDiscount`: as partes somam exatamente o desconto, e os centavos perdidos no arredondamento vão, um a um, para quem ainda tem folga). A parte fica na reserva (`booking.discount`); `paidAmount = price − discount` é o que ela de fato custou. `price` continua sendo o preço congelado na criação (não muda).
- **Idempotência:** o código faz parte do pedido (a impressão digital o inclui, normalizado). A **repetição** (mesma chave, mesmo código) devolve o mesmo pagamento com o mesmo desconto **sem usar o código de novo**; a mesma chave com **outro código, ou sem código, é `422`**. Sem código a impressão digital é a de sempre, então os pagamentos anteriores continuam repetíveis.

## Prever sem gastar

`POST /v1/promo-codes/validate` com `{code, bookingIds}` (reservas **do próprio cliente**, `PENDING`) devolve `{code, type, subtotal, discount, total}` aplicando as mesmas regras do pagamento, **sem usar o código**. Não promete que o código estará lá no pagamento (alguém pode pegar o último uso no meio): isso se decide, de forma atômica, ao pagar.

## A corrida pelo último uso

Ler "quantos usos já foram" e depois gravar deixaria dois pagamentos simultâneos passarem os dois. O consumo é **um único comando condicional**:

```sql
UPDATE promo_code SET redeemed = redeemed + 1
WHERE id = :id AND (max_redemptions IS NULL OR redeemed < max_redemptions)
RETURNING max_per_user
```

Se nenhuma linha muda, o código esgotou. Esse `UPDATE` também **trava a linha do código** até o fim da transação, então todos os outros resgates do mesmo código esperam: a contagem por cliente que vem a seguir (`promo_redemption`) não pode ser furada por duas requisições do mesmo cliente. O consumo **exige uma transação** (`MANDATORY`) e só devolve o resultado: quem chama **lança** se não foi `REDEEMED`, e é essa exceção que desfaz o pagamento e o uso juntos. Testado com **20 clientes** disputando 1 uso (exatamente 1 vence), **10 resgates do mesmo cliente** (exatamente 1) e, por HTTP, **2 clientes pagando ao mesmo tempo** (um `201`, um `422`, a reserva do perdedor segue `PENDING`).

## Reembolso (M31)

O reembolso devolve **o valor efetivamente pago** pela reserva (`paidAmount`), não o preço dela: 100 com um código de 10 % vira 90. **O uso do código não é devolvido** (regra registrada): o código foi consumido e continua contando.

## A API da equipe (`PROMO_WRITE`: CATALOG_MANAGER e SUPER_ADMIN)

| Endpoint | O que faz |
|---|---|
| `POST /v1/admin/promo-codes` | cria (`201`); o código existente, em qualquer caixa, é `409`; dados inválidos são `400` |
| `GET /v1/admin/promo-codes?active=&page&size` | lista, mais novos primeiro, com `redeemed` (quantos usos) |
| `GET /v1/admin/promo-codes/{id}` e `/{id}/redemptions` | um código; quem o usou, quando e em qual pagamento |
| `PUT /v1/admin/promo-codes/{id}` | muda a janela, o mínimo e os limites (nunca o código, o tipo nem o valor); `maxRedemptions` abaixo do que já foi usado é `400` |
| `POST /v1/admin/promo-codes/{id}/deactivate` e `/activate` | liga e desliga (`409` se já está assim) |

Tudo é **auditado** (`PROMO_CREATED`, `PROMO_UPDATED`, `PROMO_ACTIVATED`, `PROMO_DEACTIVATED`) com o estado antes e depois.

## Desenho

- Migration `V43`: `promo_code`, `promo_redemption` (`payment_id` **único**), e as colunas novas: `payment.subtotal/discount/promo_code_id/promo_code` (o que já existia ganha `subtotal = amount`) e `booking.discount`.
- Conceito novo `promo` (14 no total), sem dependência de outro no domínio. A persistência é por SQL (`NamedParameterJdbcTemplate`), porque o ponto é a atomicidade de comandos escritos à mão.
- **Fora do que o plano pedia:** nada. **Em aberto:** tentar adivinhar códigos pelo `validate` não tem limite de tentativas (entra no M46, endurecimento de segurança).

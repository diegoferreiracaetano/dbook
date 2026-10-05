# Avaliações: públicas, editáveis e moderadas

Um cliente avalia (nota de 1 a 5 e comentário) uma reserva `CONFIRMED` sua, uma vez por reserva (M12, `POST /v1/reviews`). O M37 fecha o ciclo: qualquer pessoa **lê** as avaliações de um destino, o autor **edita ou apaga**, qualquer cliente **denuncia** e a equipe **modera**.

## Ler (público, sem login)

`GET /v1/destinations/{iata}/reviews?sort=RECENT|RATING&page=0&size=20`

```json
{
  "summary": {"average": 4.67, "total": 3, "distribution": {"1": 0, "2": 0, "3": 0, "4": 1, "5": 2}},
  "reviews": {"items": [{"id": 12, "rating": 5, "comment": "Loved it", "author": "Maria S.",
                         "createdAt": "2026-10-05T10:00:00", "edited": false}],
              "page": 0, "size": 20, "totalElements": 3, "totalPages": 1}
}
```

- **Privacidade do autor:** primeiro nome e a inicial do último sobrenome (`Maria Silva` → `Maria S.`; um nome só fica como está). A resposta **nunca** traz o id do cliente, o e-mail nem o id da reserva; um cliente anonimizado aparece como `Cliente anônimo`.
- **Só avaliações visíveis** entram na lista, na média e na distribuição.
- **Ordem estável:** `RECENT` ordena por data e `RATING` por nota e data, e os dois terminam no `id`, então duas avaliações no mesmo instante não trocam de lugar entre páginas.
- Código que não tem três letras é `400`; destino sem avaliação devolve média `null` e lista vazia.

**A média é sempre derivada**, calculada na hora a partir das avaliações visíveis, e **nunca guardada** numa coluna: editar, apagar ou ocultar uma avaliação não tem como deixá-la dessincronizada. A mesma regra vale para a média que aparece em `GET /v1/destinations` (a consulta ignora as ocultas).

## Editar e apagar (o autor)

- `PATCH /v1/reviews/{id}` com `{"rating": 4, "comment": "..."}`: só o que for enviado muda (pelo menos um dos dois, senão `400`); marca `updatedAt`, e a lista pública mostra `edited: true`. Quem não é o autor recebe `403`; avaliação inexistente, `404`. Editar uma avaliação **oculta** não a torna visível: só a equipe faz isso.
- `DELETE /v1/reviews/{id}`: apaga de verdade (as denúncias vão junto). A reserva pode ser avaliada de novo.

## Denunciar (qualquer cliente)

`POST /v1/reviews/{id}/report` com `{"reason": "Offensive language"}` (3 a 500 caracteres) → `204`. Uma denúncia por cliente e avaliação (a segunda é `409`, decidida pelo índice único, mesmo se duas chegarem juntas); não dá para denunciar a própria (`409`); uma avaliação oculta é `404` para quem denuncia, como se não existisse.

## Moderar (permissão `REVIEW_MODERATE`: SUPPORT e SUPER_ADMIN)

| Endpoint | O que faz |
|---|---|
| `GET /v1/admin/reviews?status=REPORTED\|HIDDEN\|VISIBLE&page&size` | `REPORTED` é a **fila**: avaliações visíveis com denúncia aberta, a mais antiga primeiro; mostra autor, destino, nota, comentário, quantas denúncias abertas e o último motivo |
| `POST /v1/admin/reviews/{id}/hide` `{"reason": "..."}` | oculta (motivo de 10 a 500 caracteres), guarda quem e quando e **fecha as denúncias**; `409` se já está oculta |
| `POST /v1/admin/reviews/{id}/restore` | devolve uma oculta (`409` se está visível) |
| `POST /v1/admin/reviews/{id}/dismiss-reports` | a equipe olhou e a avaliação fica: fecha as denúncias abertas e a tira da fila (`409` se não há nenhuma aberta); sem isso uma avaliação denunciada à toa ficaria na fila para sempre |

Cada ação é **auditada** (`REVIEW_HIDDEN`, `REVIEW_RESTORED`, `REVIEW_REPORTS_DISMISSED`), com o estado antes e depois e o motivo; a auditoria guarda a **nota e o estado, nunca o texto** (um comentário pode ter dado pessoal).

## Desenho

- Migration `V41`: `review` ganha `status` (`VISIBLE`/`HIDDEN`), `updated_at`, `hidden_reason/by/at`; a tabela `review_report` (`UNIQUE (review_id, reporter_id)`, `resolved_at` e `ON DELETE CASCADE`) e o índice `flight(destination_airport_id)`.
- **Sem coluna `destination_iata` na avaliação** (o plano previa uma): o destino de um voo pode ser editado (M32), e a cópia ficaria errada. A leitura junta `review → booking → flight → airport`, com o índice que faltava no último salto.
- A leitura pública e a da moderação são *read models* por SQL, como o CRM e o dashboard; a escrita passa pelo domínio (`Review.edit/hide/restore`).
- O texto continua na avaliação mesmo depois da anonimização do cliente (ver [crm.md](crm.md)): o que muda é o nome exibido.

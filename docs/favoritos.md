# Favoritos no servidor

Os favoritos (destinos e voos) viviam só no aparelho: sumiam ao trocar de celular, e não havia uma fonte única da verdade. Agora o servidor guarda, por cliente, e o app só mostra.

## A API (autenticada; sempre os favoritos do próprio usuário)

| Endpoint | O que faz |
|---|---|
| `PUT /v1/favorites/{type}/{id}` | salva; `type` é `DESTINATION` (o `id` é o código IATA em maiúsculas, `GRU`) ou `FLIGHT` (o `id` é o número do voo); `204`, **idempotente** (salvar de novo não muda nada e também é `204`) |
| `DELETE /v1/favorites/{type}/{id}` | remove; `204`, idempotente (remover o que não existe também é `204`) |
| `GET /v1/favorites?type=&page=&size=` | os favoritos, **do mais novo para o mais antigo**, paginados; cada um traz o que o app precisa para desenhá-lo (`destination` com nome, cidade, país e foto; ou `flight` com número, origem, destino, partida, preço e `onSale`) |

- **Alvo inexistente:** `404` (o destino é validado no catálogo de aeroportos; o voo, no de voos). **Id mal formado** (`gru` em minúsculas, `GRUU`, `abc` para um voo) ou **tipo desconhecido**: `400`.
- **Voo tirado de venda** continua na lista, com `onSale: false`: o app decide como mostrá-lo.
- **Limite de 200 por cliente:** o 201º é `409` com `code = FAVORITES_LIMIT`. Salvar de novo um que já existe continua sendo `204`, mesmo no limite; remover um libera espaço.

## Por que o limite não vaza

Contar e inserir são dois comandos: duas requisições do mesmo cliente podiam ver "199" ao mesmo tempo e inserir as duas. O adaptador trava **a linha do cliente** (`SELECT ... FOR UPDATE` em `app_user`) antes de contar, na mesma transação: as requisições do mesmo cliente entram em fila, as de outros clientes não são atrasadas. O teste usa 20 *threads* com limite 5 e exige exatamente 5 salvos (sem a trava, ele reprova).

## Desenho

- Tabela `favorite` (migration `V42`): `(user_id, target_type, target_id)` **único** (é isso que torna o `PUT` idempotente mesmo com duas chamadas juntas) e um índice `(user_id, created_at DESC, id DESC)` para a lista. O `target_id` é texto (o IATA ou o id do voo) e **não é chave estrangeira**: um destino é um aeroporto, e a tabela serve igual a um tipo novo de alvo (o hotel do M42).
- A leitura é um *read model* por SQL: corta a página primeiro, nas colunas do próprio favorito, e só então junta o que cada linha aponta (um `CASE` evita converter o código de um destino em número).
- Conceito novo `favorite` (13 no total): não depende de nenhum outro no domínio; quem consulta o catálogo para validar é o caso de uso.

## Privacidade

A exportação dos dados do próprio usuário (`GET /v1/users/me/export`) inclui os favoritos, e a anonimização do cliente os apaga.

## No app

A migração dos favoritos que hoje só existem no aparelho (enviá-los com `PUT` no primeiro acesso depois da atualização) é o item do app, no marco dele.

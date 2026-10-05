# Catálogo administrativo

Até aqui só se **criava** voo. O portal precisa listar, editar, cancelar, manter companhias e aeroportos e carregar voos em lote, sem quebrar o que já foi reservado. Os endpoints estão em [Endpoints](endpoints.md#catálogo-administrativo-v1adminflights-airlines-airports-aircraft-models).

## Voos

**Lista e detalhe** (`FLIGHT_READ`): origem, destino, companhia, período de partida e status, mais cedo primeiro, com página e total. Cada voo traz os assentos livres e reservados, o layout do avião e a **`version`**. O detalhe traz também as reservas ativas. A contagem de assentos é uma subconsulta por linha, então o `LIMIT` fica dentro da consulta (a mesma lição do [CRM](crm.md#medido-com-50-000-clientes)).

**Editar** (`PUT /v1/admin/flights/{id}`, `FLIGHT_WRITE`) com a `version` que o editor leu. Se o voo mudou desde então, `409 STALE_VERSION`: o portal mostra "outro administrador editou". Duas edições ao mesmo tempo com a mesma versão: uma vence e a outra recebe `409` (testado com duas threads). As regras, todas no domínio:

| Regra | Onde |
|---|---|
| chegada depois da partida; origem diferente do destino | `Flight` (vale também para o cadastro e a importação) |
| o voo cancelado não se edita | `Flight.edit` |
| a capacidade não pode ficar abaixo dos assentos já reservados | `planSeatChange` |
| mais capacidade: assentos novos no fim do mapa | `planSeatChange` |
| menos capacidade: tira assentos do **fim**, e só os livres e **sem histórico de reserva** | `planSeatChange` |
| o layout do avião (assentos por fileira) só muda se **nenhum assento foi reservado, nem uma vez**; aí o mapa é refeito | `planSeatChange` |
| o preço muda a qualquer hora: as reservas já feitas guardam o preço de quando foram feitas | `Booking.price` (M27) |

"Fim do mapa" é a **posição** do assento no avião, não a ordem do texto (`10A` vem depois de `9F`, e ordenar pelo rótulo tiraria o assento errado). Um assento com reserva cancelada ou reembolsada **também** conta como histórico: a reserva o referencia, e apagá-lo quebraria a integridade. O voo e os assentos mudam **na mesma transação**: uma mudança de assentos recusada deixa o voo intacto (inclusive o preço que veio junto).

**Cancelar** (`POST /v1/admin/flights/{id}/cancel`, `FLIGHT_WRITE`), **fase 1**: o voo sai da busca pública, não pode mais ser reservado nem editado. Se ainda tem reservas **ativas** (pendentes ou pagas), é `409` dizendo quantas: a equipe as reembolsa ou cancela antes (o [reembolso](reembolso.md) já existe). A **fase 2** (cancelar, reembolsar e avisar cada reserva sozinho) depende do outbox e das notificações (M35 e M36). Limite conhecido: uma reserva criada no mesmo instante em que o cancelamento confere as reservas pode escapar da contagem; ela vence pelo prazo de 15 minutos ou pelo reembolso.

O status (`SCHEDULED` ou `CANCELLED`) é **derivado** de `Bookable.active`: um voo cancelado é um voo inativo. O plano previa uma coluna nova; ela seria um segundo jeito de dizer a mesma coisa. Aproveitando o `active`, a busca pública passou a ignorá-lo também (antes só a busca de menor preço e a lista da IA o faziam), e **o cadastro de uma reserva recusa um voo que não está à venda**. Isso era uma lacuna: nada impedia reservar um voo desativado.

## Companhias e aeroportos

`GET` com `FLIGHT_READ`, escrita com `CATALOG_WRITE`. O código IATA é validado no domínio (companhia: 2 letras ou dígitos, como `G3`; aeroporto: 3 letras) e é único (`409` se repetido). Só se remove o que **nenhum voo usa** (`409` com quantos voos o usam): apagar uma companhia ou um aeroporto em uso quebraria o histórico dos voos. `GET /v1/admin/aircraft-models` (somente leitura) lista os modelos com layout conhecido, para o formulário do voo.

## Importação em lote

`POST /v1/admin/flights/import` com um CSV no corpo (`text/csv`), em `FLIGHT_WRITE`. Colunas: `flightNumber`, `airlineIataCode`, `originIataCode`, `destinationIataCode`, `departureTime`, `arrivalTime`, `seatClass`, `price`, `totalCapacity`, `aircraftType`. No máximo 5000 linhas.

- **`dryRun=true` por padrão:** só valida e conta. **Todos** os erros voltam com a sua linha (não só o primeiro), para o arquivo ser corrigido de uma vez: código desconhecido, data inválida, chegada antes da partida, mesmo aeroporto, capacidade zero, e a mesma partida repetida no arquivo.
- **Tudo ou nada:** com `dryRun=false`, só grava se **não há erro nenhum** (`422` com a lista, e nada é criado, nem as linhas boas). Quando grava, é uma transação só, com os assentos de cada voo.
- **Repetível:** uma linha cujo número de voo e partida já existem é contada (`alreadyExisting`) e pulada; enviar o mesmo arquivo duas vezes não cria nada na segunda.
- Um arquivo vazio, sem uma coluna obrigatória, com aspas abertas ou com mais de 5000 linhas é `400`.

`scripts/seed-flights.sh` passou a montar um CSV e usar este endpoint (antes fazia uma chamada por voo): verificado ao vivo, 120 voos e os seus assentos numa chamada.

## Auditoria

`FLIGHT_UPDATED` e `FLIGHT_CANCELLED` com o estado antes e depois (o `status` entra no snapshot), `AIRLINE_*`, `AIRPORT_*` e `FLIGHTS_IMPORTED` (quantos criou e quantos já existiam; cada voo criado também grava o seu `FLIGHT_CREATED`).

## Fora do escopo (de propósito)

Cancelar um voo com reservas de ponta a ponta (fase 2); o histórico de preço (M40); invalidar cache (não há cache; o M47 decide); importar companhias e aeroportos em lote.

# Hotéis

O `Bookable` do M1 era uma promessa: tudo que se reserva herda dele, e o primeiro (e único) filho era o voo. O M42 cumpre a promessa com o segundo, o **hotel** (`Accommodation`), e é o que prova que o desenho aguenta uma coisa que **não é um assento**: uma estadia, por **noites**.

## A decisão (o que o spike respondeu)

**Como a `Booking`, presa a um `Seat`, representa uma estadia por datas?** Três caminhos foram pesados:

- **(A) Generalizar o `Seat` em "unidade de inventário":** um assento de voo e "um quarto numa noite" passam a ser a mesma coisa. Descartado: uma estadia ocupa **várias** unidades de uma vez (uma por noite), e o `Seat` tem uma identidade e um rótulo ("12A") que o quarto-noite não tem.
- **(B) `Booking` com um item polimórfico:** uma hierarquia nova só para dizer "assento ou estadia". Descartado: cada caso de uso que toca a reserva teria de entender os dois sem ganhar nada com isso.
- **(C) A reserva guarda *o que* reservou, com as colunas da estadia nela mesma, e o estoque é contado por noite:** **escolhido.** `Booking.seatId` passou a ser opcional e a reserva ganhou um `Stay` (o tipo de quarto, `checkIn`, `checkOut`, hóspedes e **a diária congelada**); a regra é que **ou há um assento, ou há uma estadia, nunca os dois e nunca nenhum** (invariante do domínio e `CHECK` no banco). O resto da vida da reserva (`PENDING` por 15 minutos, pagamento, expiração, reembolso, avaliação) **não mudou**.

**O estoque por noite** (`room_night`): para cada tipo de quarto e cada noite, quantos quartos estão ocupados (uma noite sem linha tem zero). Reservar uma estadia sobe a contagem de **cada uma** das suas noites, **cada uma com um único comando condicional**:

```sql
INSERT INTO room_night (room_type_id, night, booked) VALUES (:type, :night, 1)
ON CONFLICT (room_type_id, night) DO UPDATE SET booked = room_night.booked + 1
WHERE room_night.booked < (SELECT quantity FROM room_type WHERE id = :type)
```

Se nenhuma linha muda, a noite está cheia: a estadia inteira é recusada (`409 ROOM_UNAVAILABLE`) e a exceção desfaz, junto, as noites que já tinham entrado. A trava de linha que o comando leva faz a segunda estadia da mesma noite esperar e então encontrar a contagem já subida. As noites entram **em ordem**, então duas estadias que se sobrepõem nunca esperam uma pela outra em círculo (sem *deadlock*). Isso resolve a **sobreposição parcial** noite a noite, sem comparar intervalos.

**Alternativa registrada:** uma restrição `EXCLUDE USING gist` com `tstzrange` por quarto físico. Serve quando cada **quarto** é uma entidade (número 101, 102...) e se quer atribuí-lo na reserva; aqui o hotel vende **tipos** de quarto em quantidade, e a contagem por noite é mais simples e dá a disponibilidade da busca de graça.

## O modelo

- `Accommodation : Bookable` (`accommodation`, junto da tabela `bookable`): nome (o `title`), **destino** (um aeroporto: é assim que "hotéis por destino" e as avaliações do destino funcionam igual para voos e hotéis), endereço, estrelas, descrição, foto e comodidades. O `price` do `Bookable` é a **menor diária** ("a partir de") e a capacidade é o número de quartos; **quantos estão livres é uma pergunta da noite**, respondida pelo estoque, não um número vivo no hotel.
- `RoomType` (`room_type`): nome, quantos hóspedes cabem, diária, quantidade de quartos e se está à venda. O nome é único dentro do hotel (repetir é `409`).
- `Stay` (domínio da reserva): `checkIn` é a primeira noite e `checkOut` é o dia em que os hóspedes saem, **que não é uma noite**: quem sai no dia 18 e quem entra no dia 18 cabem no mesmo quarto. No máximo 30 noites. `total = noites × diária`.
- **O preço da reserva é congelado** (`price = noites × diária`, a diária fica na `Stay`), como o do voo no M27: mudar a diária depois não mexe em quem já reservou.

## A API

**Pública** (como a busca de voos):
- `GET /v1/accommodations/search?destination=GIG&checkIn=2027-01-15&checkOut=2027-01-18&guests=2&page&size` devolve os hotéis do destino que têm **um tipo de quarto livre em todas as noites** e que acomoda os hóspedes, com cada quarto livre e o **preço da estadia inteira** (`totalPrice`), o `fromPrice` (a estadia mais barata) e a nota média e o número de avaliações. Mais barato primeiro. Validações: o destino é um IATA de três letras, `checkOut` depois de `checkIn`, `checkIn` não no passado, no máximo 30 noites, de 1 a 10 hóspedes; tudo isso é `400`.
- `GET /v1/accommodations/{id}` (o hotel com seus tipos de quarto à venda; `404` se não existe ou está fora de venda) e `GET /v1/accommodations/{id}/reviews` (como as avaliações de um destino: média, distribuição e itens).

**Reservar** (autenticado): `POST /v1/accommodations/{id}/bookings` com `{roomTypeId, checkIn, checkOut, guests}` devolve a `Booking` (`201`, `PENDING` por 15 minutos, **o mesmo evento de expiração do assento**). `409 ROOM_UNAVAILABLE` se alguma noite está cheia; `404` para um tipo de quarto desconhecido ou fora de venda; `409` se o hotel saiu de venda; `400` para datas ruins, no passado ou hóspedes demais. Paga-se com o mesmo `POST /v1/payments` (e o código promocional, e o reembolso, funcionam como no voo).

`GET /v1/bookings` (minhas viagens) traz, numa estadia, `stay` e `accommodation` e **`seat` e `flight` nulos** (campos que já existiam e ficaram opcionais; os de uma reserva de voo continuam iguais). `BookingResponse` ganhou `stay` e `seatId` opcional. A lista do admin mostra `checkIn` e `checkOut` e o `seatLabel` nulo.

**Equipe** (`/v1/admin/accommodations`, permissão `CATALOG_WRITE`, tudo **auditado** com antes e depois): criar o hotel com seus tipos de quarto, listar e ver (inclusive os fora de venda), mudar o que o descreve, **tirar de venda e pôr de volta** (as reservas já feitas ficam), adicionar e mudar um tipo de quarto (diária, capacidade, nome, se está à venda e a **quantidade, que nunca desce abaixo do máximo já reservado em alguma noite de hoje em diante**: `409`).

## Quando a reserva acaba

Cancelar (pendente), **expirar** (15 minutos sem pagar) e **reembolsar** devolvem as noites, no mesmo caso de uso que antes devolvia o assento (`BookingInventoryReleaser`, que decide pelo que a reserva guardava). Devolver duas vezes não deixa a contagem abaixo de zero.

## Avaliações e destinos

A avaliação é de uma **reserva**, então já valia para qualquer reservável; o que mudou é a leitura por destino: o destino de uma avaliação é onde o voo chega **ou** onde o hotel está (`COALESCE` das duas), então `GET /v1/destinations/{iata}/reviews` e a média de `GET /v1/destinations` passam a contar os dois. Um hóspede só avalia uma estadia **paga**, como no voo.

## Decisões registradas

- **Tempo real (42g): não fazer agora.** O canal STOMP anuncia quantos **assentos** livres um voo tem, um número único e vivo. A disponibilidade de um hotel é **por noite e por tipo de quarto**: um número só não a descreve, e anunciar a grade inteira a cada reserva seria barulho. A tela de reserva consulta a busca (`409` na hora de reservar é a garantia). Se um dia for preciso, o ponto é um evento "noites de X mudaram" no outbox.
- **Importação em lote de hotéis (42e): não fez parte desta entrega.** A importação CSV tudo-ou-nada do M32 é de voos; os hotéis se cadastram um a um pela API. A estrutura (criar com os tipos de quarto numa chamada) deixa a importação como uma camada fina por cima.
- A reserva de um quarto **não tem mapa nem escolha de quarto**: o cliente reserva um **tipo**.
- Sem tarifa por noite diferente (alta temporada), sem política de cancelamento própria do hotel e sem pagamento no hotel: a diária é uma só por tipo e as regras de reembolso são as do M31/M41.

## Desenho em camadas

`domain/accommodation` (Accommodation, RoomType, `RoomInventory`, a busca e o leitor do admin; depende só de `catalog` e `booking`), `application/accommodation`, `presentation/accommodation`, `infrastructure/persistence/accommodation` (JDBC para os tipos de quarto, o estoque e a busca; a entidade JPA do hotel fica com a do voo, em `persistence/catalog`, porque faz parte da hierarquia `Bookable`). Migration `V45`.

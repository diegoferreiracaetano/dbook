# ADR 0001 — Dividir o backend em módulos Gradle por responsabilidade

- **Status:** proposta, **revisão 5** (48.5 feito: o vocabulário de segurança e de auditoria e os handlers por módulo já estão onde o ADR diz). Antes: **revisão 4** (2026-10-05: o ensaio do 48.1 mediu o grafo de verdade e corrigiu a lista de ciclos; nada foi movido ainda)
- **Marco:** M48 (`CHECKLIST.md`)
- **Substitui:** a decisão de 2026-10-04 de *não* dividir "agora, reavaliar depois do M45" (o M45 fechou)

## Contexto

Hoje há um módulo Gradle só, com 17 conceitos em pastas, um deploy e um banco. As regras do ArchUnit impõem as camadas, os ciclos e as dependências permitidas, mas **só no `domain/`**. O dono quer módulos pequenos por responsabilidade, com um núcleo comum, e pensando no que vem depois: **hospedagem não pode depender de voo**, e um terceiro tipo de reserva (carro, passeio) deve entrar sem mexer nos outros.

Medimos o que cada conceito importa dos outros **em todas as camadas**:

| Conceito | Depende de (todas as camadas) |
|---|---|
| `ai`, `favorite`, `seating` | só do catálogo (voos) |
| `pricing` | catálogo, `messaging` |
| `promo` | `booking`, `audit`, `identity` |
| `audit` ↔ `identity` | um no outro (`Actor` e `AuditLog`) |
| `crm`, `dashboard` | `identity`, `audit`, `favorite` (leitura por SQL) |
| `notification` | `booking`, catálogo, `payment`, `pricing`, `identity` |
| **catálogo, `booking`, `accommodation`, `payment`, `review`** | **uns nos outros, em ciclo** |

### O que está misturado dentro do "catálogo"

O pacote `catalog` junta três coisas que **não são da mesma responsabilidade**:

1. **O genérico:** `Bookable` ("qualquer coisa que se reserva": id, título, preço, capacidade, ativo) e o `Airport` (o lugar: o hotel também aponta para um aeroporto como destino).
2. **O que é só de voo:** `Flight`, `Airline`, o modelo de aeronave, a busca de voos, a importação, o preço mais baixo.
3. **O que é só de voo, e mora em `seating`:** o assento (`Seat`), o mapa por aeronave, a disponibilidade de assentos.

Hospedagem não tem assento (tem tipo de quarto e estoque por noite). Por isso **assento não pode ir para o catálogo genérico**, e a sugestão do dono procede: voo vira um módulo (`:flight`) com os seus assentos, e hospedagem é o seu espelho.

## Opções avaliadas

| | A. Catálogo gordo | **B. Genérico fino + um módulo por tipo (recomendada)** | C. Tipos como *plugins* com estoque opaco |
|---|---|---|---|
| Ideia | `catalog` = genérico + voo + assentos; `accommodation` à parte | `:catalog` fino (`Bookable`, `Airport`); `:flight` (com assentos) e `:accommodation` **simétricos**; `:booking` genérico, sem saber de nenhum dos dois | `booking` guarda uma "referência de estoque" opaca; cada tipo implementa reservar/liberar |
| Hospedagem depende de voo? | sim (via catálogo) | **não** | não |
| Um terceiro tipo (carro) | mexe no catálogo | **um módulo novo** que usa as mesmas duas portas | um módulo novo |
| Esforço agora | o menor | médio: ~11 ciclos, 3 mudam código de verdade | o maior: muda o esquema (migration) e o modelo da reserva |
| Risco | baixo | médio | alto, sem necessidade hoje |

**Recomendação: B**, e deixar C como evolução natural (B já deixa o caminho aberto: o que `booking` guarda de cada tipo é só um `Long`, `seatId`, e um valor, `Stay`).

## Decisão (proposta)

Módulos Gradle por responsabilidade, com as camadas como pacotes dentro de cada um (`domain`, `application`, `presentation`, `infrastructure`) e um núcleo comum. **Não** dividir cada feature em sub-módulos `api`/`domain`/`data`: o ArchUnit já impõe as camadas, e seriam umas 50 pastas de build para um deploy só.

Do mais baixo para o mais alto, **sem ciclos** (cada módulo só enxerga os que estão nas colunas de dependência):

| Módulo | Responsabilidade | Depende de |
|---|---|---|
| `:core` | **só portas e conceitos minúsculos compartilhados, nunca regra de negócio** (o nome é `core`, não `common`, para não virar gaveta): paginação, `ApiPaths`, `ErrorCode`, `TransactionSupport`, **o vocabulário de segurança** (`Actor`, `Role`, `Permission`), **o vocabulário e a porta da auditoria** (`AuditAction`, `AuditEvent`, `AuditLog`), `OutboxWriter` (com os seus eventos), a base de web e segurança | — |
| `:audit` | **a implementação** da trilha: gravação, consulta, `GET /v1/admin/audit` | `core` |
| `:identity` | usuários, tokens, papéis, equipe, 2FA | `core` |
| `:catalog` | o genérico: `Bookable` (e a entidade JPA base), `Airport`, o registro de mapeadores por tipo | `core` |
| `:booking` | a reserva **genérica**: ciclo de vida, preço congelado, expiração, histórico, a porta "liberar estoque" | `catalog`, `core` |
| `:flight` | voo, companhia, **assentos**, modelos de aeronave, busca, importação, a reserva de assento (`POST /v1/bookings`), a disponibilidade em tempo real | `catalog`, `booking`, `core` |
| `:accommodation` | hotéis, quartos, estoque por noite, a reserva de estadia | `catalog`, `booking`, `core` |
| `:pricing` | histórico de preço e alertas | `flight`, `catalog` |
| `:favorite` | favoritos | `flight`, `catalog` |
| `:ai` | sugestões por IA (Bedrock, disjuntor) | `flight` |
| `:payment` | pagamento, reembolso, política de cancelamento **e códigos promocionais** (pacote `promo` dentro) | `booking`, `flight`, `catalog` |
| `:review` | avaliações e moderação (e as avaliações do hotel) | `booking`, `accommodation`, `flight` |
| `:trips` | "minhas viagens": junta reserva, assento ou estadia, pagamento e avaliação | `booking`, `flight`, `accommodation`, `review`, `catalog` |
| `:notification` | avisos (in-app, e-mail, push) e preferências | `booking`, `flight`, `payment`, `pricing`, `identity` |
| `:admin` | CRM e dashboard (leitura por SQL) | `identity`, `audit`, `favorite`, `core` |
| **`:app`** | **só a montagem**: `main`, configuração, `SecurityConfig`, migrations do Flyway, Dockerfile; **sem regra de negócio** (decisão do dono) | todos |

`appconfig` fica em `:app` (decisão do dono).

## Os ciclos a desfazer (11 arestas, nenhuma é regra de negócio)

| # | Aresta | O que é | Como se desfaz |
|---|---|---|---|
| 1 | `booking → flight` | `RegisterBookingUseCase` (reserva de assento) e `BookingEvents` conhecem `Flight` e `Seat` | a reserva de assento **vai para `:flight`**, como a de estadia já está em `accommodation`; os eventos usam o `Bookable` genérico |
| 2 | `booking → flight/accommodation` | `BookingInventoryReleaser` libera assento ou quarto | porta `InventoryReleaser` no `booking`, **implementada por `flight` e por `accommodation`** |
| 3 | `booking → flight` (JPA) | `BookingJpaEntity` tem `@ManyToOne` para `SeatJpaEntity` | a relação vira a coluna `seat_id` |
| 4 | `booking → payment` (JPA) | `@ManyToOne` para `PaymentJpaEntity` | a relação vira a coluna `payment_id` |
| 5 | `catalog → flight/accommodation` | `CatalogMappers` conhece cada tipo (herança JPA `JOINED`) | um registro de mapeadores em `catalog`; cada módulo registra o do seu tipo |
| 6 | `booking → review` | "minhas viagens" mostra a avaliação | a composição vai para `:trips` |
| 7 | `flight → pricing` | `RegisterFlight`/`UpdateFlight` chamam `FlightPriceRecorder` | `flight` publica o fato (evento ou porta), `pricing` o ouve |
| 8 | `flight → review` | destinos em destaque pedem a nota média | porta `DestinationRatings` em `flight`, implementada por `review` |
| 9 | `accommodation → review` | `AccommodationController` serve as avaliações do hotel | a rota (`/accommodations/{id}/reviews`) passa para `review` |
| 10 | `identity ↔ audit` e todos → ambos | `Actor`, `Role`, `Permission` e `AuditAction`/`AuditEvent`/`AuditLog` | vão para `core` (ver "O que o ensaio mediu"); a implementação da trilha fica em `:audit` |
| 11 | `payment → flight` | a janela de reembolso usa a partida do voo | **aceita por ora** (`payment → flight` não cria ciclo); quando o hotel tiver política própria, entra uma porta `CancellationPolicy` por tipo |

## O que o ensaio (passo 48.1) mediu

A regra `TheModulesOnlyDependOnTheirTargetModulesTest` confere o grafo alvo no **bytecode**, em todas as camadas. A primeira rodada achou **318** pares de classes fora do grafo; o motivo de quase dois terços era o mesmo, e já foi tratado no mapa:

- **O vocabulário compartilhado de segurança e auditoria** (`Actor`, `Role`, `Permission`, e `AuditAction`, `AuditEvent`, `AuditOutcome`, `AuditLog`) mora em `identity` e `audit`, mas **todo módulo o usa** (186 pares). No alvo ele vai para o **`core`**; a regra já o conta como `core`, e o passo 48.5 o move de verdade. Decisão: o enum `AuditAction` fica **inteiro no `core`** (adicionar uma ação toca o `core`); trocá-lo por um vocabulário por módulo é possível depois, e só valeria a pena com *deploys* separados.
- **Três dependências legítimas que o ADR não tinha**: `favorite` e `pricing` usam o `Airport` do `catalog`; `admin` (CRM) usa `identity` (bloquear e anonimizar usuário), `audit` (a trilha na visão 360º) e `favorite` (a exportação dos dados do titular). Entram na tabela.

**Depois disso restam 70 pares, em 17 arestas.** São as 11 do ADR, mais os efeitos de o voo ainda morar dentro de `catalog` (`ai`, `payment`, `catalog → flight`: somem na separação do 48.3) e **uma que o ADR não previa**:

| # | Aresta | O que é | Como se desfaz |
|---|---|---|---|
| 13 | `catalog → flight` (cache) | mudar um aeroporto invalida a busca de voos (`AirportRepositoryAdapter` usa `FlightSearchCache`) | porta `AirportChangeListener` no `catalog`, implementada por `flight` (que invalida o seu cache) |
| 14 | `catalog → flight` (assentos) | `BookableRepositoryAdapter` calcula a capacidade livre pelos assentos (`SeatAvailability`) | cada tipo informa a sua própria capacidade no registro de mapeadores do ciclo 5 |
| 15 | `accommodation → flight` | `CatalogLookup` (que mora em `flight` desde o 48.3, por também resolver a companhia) é usado pelo cadastro de hotéis | dividir: `catalog` resolve o aeroporto, `flight` a companhia |
| 12 | `core → identity, payment, catalog, booking, flight` | o `ApiExceptionHandler` (em `presentation/common`) traduz as exceções de domínio de **todos** os módulos para HTTP | cada módulo ganha o seu handler (já é o padrão de `IdentityExceptionHandler`); o `core` fica só com os erros genéricos |

## Como chegar lá sem quebrar nada (o método do M45, em escala)

1. **Ensaiar antes de mover.** Estender o ArchUnit para impor o **grafo alvo em todas as camadas**. Cada violação é um item da lista de trabalho, e nenhum arquivo muda de lugar.
2. **Desfazer os ciclos**, um por vez, cada um com porta e teste.
3. **Mover por módulo**, de baixo para cima, um commit e um `check` verde por módulo, **sem mudança de comportamento**. As migrations ficam em `:app`.
4. **Build:** um *convention plugin* (`build-logic`) com Kotlin, Spring, ktlint, detekt e JaCoCo uma vez só; cobertura agregada; CI rodando só os módulos afetados (e todos no `main`).

## Consequências

**Ganhos:** quebrar uma fronteira vira erro de compilação; `:favorite` e `:ai` ficam do tamanho que merecem; **um terceiro tipo de reserva é um módulo novo** (implementa o `InventoryReleaser` e registra o seu mapeador), sem mexer em `booking`, `flight` nem `accommodation`; testar só o módulo que mudou; `:admin` pode virar um segundo deploy.

**Custos e riscos:** as fixtures de teste compartilhadas precisam de `java-test-fixtures`, e os testes de integração ficam em `:app`, que enxerga tudo; o Spring precisa achar beans e entidades JPA de todos os módulos; **os ciclos 1, 3, 4 e 5 mudam código de verdade** (a reserva de assento muda de módulo, duas relações JPA viram colunas, o mapeamento polimórfico): são os de maior risco, e por isso vêm primeiro e sozinhos.

## Decisões do dono

| | Decisão |
|---|---|
| `:app` | só montagem e configuração, sem negócio (**decidido**) |
| `appconfig` | fica em `:app` (**decidido**) |
| assentos | **não** vão para o catálogo: ficam em `:flight` (**decidido**) |
| `:promo` | **dentro de `:payment`**, como pacote (revisão 3): um código promocional só existe para abater um pagamento (é resgatado no `RegisterPaymentUseCase`, não tem outro cliente), e `payment → promo → booking` virava uma cadeia de três módulos para uma coisa só. O pacote `promo` continua separado por dentro e o ArchUnit o mantém sem se enredar: se um dia a campanha ganhar vida própria (cupom por e-mail, outro canal), sai com a pasta |
| `:trips` | **módulo próprio** (revisão 3): "minhas viagens" junta reserva, assento ou estadia, pagamento e avaliação, e nenhum módulo de baixo pode conhecer todos eles; no `:app` seria negócio, que o dono não quer lá. É pequeno, e também recebe o que mais for "visão do cliente sobre as suas reservas" (recibo, histórico) |
| `Airport` | **em `:catalog`** (revisão 3): voo e hotel o usam; em `:flight`, a hospedagem dependeria de voo |
| `audit` | **a porta (`AuditLog`, `AuditEvent`) em `:core` e a implementação em `:audit`** (revisão 3): todo módulo grava auditoria, e só a implementação precisa de banco e de rota; assim ninguém depende de `:audit`, e o ciclo com `identity` some |
| quantidade | **15 módulos + `:app`**. Regra para um módulo existir: tem responsabilidade própria **e** um perfil de dependência próprio (ou é candidato a *deploy* separado). `appconfig` (3 arquivos) e `messaging` (2) não passam e ficam em `:app`/`core` |

## Alternativas descartadas

- **Um módulo só:** funciona, mas a fronteira depende de teste e passa de 17 conceitos.
- **Sub-módulos `api`/`domain`/`data` por feature:** granularidade demais para um deploy e um banco.
- **Microsserviços:** um banco, transações entre reserva e pagamento e uma pessoa só; o custo operacional não se paga.

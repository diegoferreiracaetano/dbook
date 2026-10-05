# Plano de execução do M48 — modularização do backend

O **porquê** e o **desenho final** estão no [ADR 0001](adr/0001-modulos-gradle-por-responsabilidade.md). Este documento é o **como**: a ordem, o que muda em cada passo, como provar que nada quebrou e o que fazer se algo quebrar.

## Regras do jogo

1. **Nenhum passo muda comportamento.** O contrato da API é guardado pelo `TheOpenApiBaselineMatchesTheCodeTest` (qualquer rota ou campo diferente reprova), o banco por "nenhuma migration nova", e o resto pelos 961 testes (a contagem só pode subir).
2. **Um passo = uma mudança de um tipo só:** ou *refatorar* (desfazer um ciclo, no mesmo módulo Gradle) ou *mover* (de pasta, de pacote ou de módulo), **nunca os dois juntos**. Assim, quando algo quebra, a causa é uma só.
3. **Cada passo termina com `./gradlew check` exit 0** e um commit pequeno (só quando você pedir, como sempre). Um passo que falha se desfaz com `git revert`, sem deixar a árvore pela metade.
4. **Os nomes de pacote não mudam** (`com.dbook.domain.booking`, `com.dbook.application.payment`...): módulo é um conjunto de pastas de conceito, e não há renomeação em massa. As exceções são só onde um conceito se divide (`catalog` → `catalog` + `flight`) e onde nasce um (`trips`).
5. **Rede de segurança extra** depois das fases C e D: subir a aplicação e rodar o k6 (`loadtest/booking-race.js` e `checkout.js`), que provam o fluxo de reserva e pagamento de ponta a ponta.

**Medidas de partida** (anotar antes do primeiro passo): 961 testes, `check` ≈ 10 min, cobertura JaCoCo, `docs/openapi/openapi-v1.json` e `v2` (o hash deles).

## Fase A — Ensaiar (nada muda de lugar)

**48.1 A regra do grafo alvo em todas as camadas.** Hoje o ArchUnit só confere as dependências entre conceitos no `domain/`. Passa a existir uma regra que mapeia cada pacote de conceito ao seu **módulo alvo** (a tabela do ADR) e proíbe, em **todas as camadas** (`domain`, `application`, `presentation` e `infrastructure.persistence`), uma dependência que o módulo alvo não declara. As violações **de hoje** ficam congeladas com `FreezingArchRule` (o ArchUnit guarda a lista em `src/test/resources/archunit_store/`): o teste só reprova violação **nova**, e cada ciclo desfeito **encolhe** a lista. Entrega: o mapa de violações por aresta (a lista de trabalho real, para conferir contra as 11 arestas do ADR, porque podem aparecer mais). Nenhum arquivo de produção muda.

**48.2 O `build-logic`** (um *convention plugin* via `includeBuild`) com o que hoje está no `build.gradle.kts` raiz: Kotlin e os plugins `spring`/`jpa`/`allopen`, ktlint, detekt (a mesma `config/detekt/detekt.yml`), JaCoCo, a configuração dos testes (versão da API do Docker para o Testcontainers) e a toolchain 21. Ainda com **um módulo só** (o raiz passa a usar o plugin): prova que o plugin reproduz o build de hoje.

## Fase B — Separar os pacotes (ainda um módulo Gradle)

**48.3 `flight` nasce como conceito.** Movem-se de `catalog` para `flight`, nas quatro camadas: `Flight`, `Airline`, `FlightRepository`, os modelos de aeronave e a busca, a importação, o cadastro e o `FlightAdminController`. Ficam em `catalog`: `Bookable`, `Airport`, `CatalogLookup` e o que for genérico. `seating` **continua com o nome**, e o mapa o marca como parte do módulo `flight`. É um script (muda `package` e `import`), mais os testes que acompanham. A regra da 48.1 passa a ter `flight` e `catalog` como nós distintos.

**48.4 `trips` nasce como conceito.** `ListMyBookingsUseCase`, `BookingWithDetails`, `MyBookingResponse` e o `GET /v1/bookings` saem de `booking` (o `BookingController` se divide em dois: reservar e cancelar ficam, listar vai para `trips`). A rota continua `GET /v1/bookings`.

## Fase C — Desfazer os ciclos (um por commit, no módulo único)

Da menor para a maior chance de dar problema; **cada um** vira uma porta ou uma coluna, com teste, e **encolhe a lista congelada**:

| Passo | Ciclo (nº do ADR) | O que muda |
|---|---|---|
| 48.5 | 10 | `Actor` e as portas `AuditLog`/`AuditEvent` vão para `core`; `audit` e `identity` deixam de se conhecer |
| 48.6 | 7 | `flight` publica o fato do preço (evento do outbox ou porta) e `pricing` o ouve; `FlightPriceRecorder` sai de `RegisterFlight`/`UpdateFlight` |
| 48.7 | 8 e 9 | porta `DestinationRatings` em `flight` (implementada por `review`); a rota `/accommodations/{id}/reviews` passa de `AccommodationController` para o `review` (**mesma URL**) |
| 48.8 | 6 | já feito pelo 48.4 (`trips`): aqui só se confirma que `booking` não importa mais `review` |
| 48.9 | 5 | o **registro de mapeadores de `Bookable`** em `catalog`; `flight` e `accommodation` registram o seu; `BookableRepositoryAdapter` e `BookingMappers` passam a usá-lo |
| 48.10 | 3 e 4 | `BookingJpaEntity`: o `@ManyToOne` de `SeatJpaEntity` e de `PaymentJpaEntity` vira `seatId`/`paymentId` (as colunas **já existem**: sem migration); os mapeadores de `booking` deixam de montar `Seat` e `Payment` |
| 48.11 | 2 | a porta `InventoryReleaser` em `booking`, com a implementação de `flight` (libera assento) e a de `accommodation` (libera noites); some o `BookingInventoryReleaser` que conhecia os dois |
| 48.12 | 1 | `RegisterBookingUseCase` (a reserva de assento) e o `POST /v1/bookings` passam para `flight`; `BookingEvents` deixa de importar `Flight` (usa o título do `Bookable`) |
| 48.13 | — | **a lista congelada está vazia**: a regra do 48.1 vira `.check` sem `Freezing`. O k6 de reserva e de pagamento roda contra a aplicação de verdade |

**Os de maior risco** são o 48.9, o 48.10, o 48.11 e o 48.12 (mudam código de verdade). Cada um tem o seu próprio teste de integração que **já existe** (corrida de reserva, expiração, reembolso, "minhas viagens") e é o que prova que o comportamento é o mesmo.

## Fase D — Os módulos Gradle (de baixo para cima)

Cada módulo é uma pasta na raiz do repositório (`core/`, `audit/`, `identity/`...), com o seu `build.gradle.kts` e os pacotes de conceito do mapa. Um commit por módulo, na ordem do grafo:

| Passo | Módulo | Observação |
|---|---|---|
| 48.14 | `:core` | `domain/common`, `application/common`, `presentation/common`, `messaging` (o outbox), a base de web e segurança; **`testFixtures`** com `MutableClock`, `FakeAuditLog` e o que muitos testes de unidade usam |
| 48.15 | `:audit`, `:identity` | |
| 48.16 | `:catalog`, `:booking` | |
| 48.17 | `:flight`, `:accommodation` | os dois lados da simetria |
| 48.18 | `:pricing`, `:favorite`, `:ai` | folhas simples; o módulo `:ai` leva o `BedrockGuard` |
| 48.19 | `:payment` (com `promo`), `:review`, `:trips` | |
| 48.20 | `:notification`, `:admin` | `crm` e `dashboard` |
| 48.21 | `:app` | `main`, `config/`, `SecurityConfig` e os filtros, as migrations do Flyway, o Dockerfile, o `application.yml`, os testes de integração (que precisam do contexto inteiro) e as regras do ArchUnit |

Em cada passo da fase D:
- **Testes de unidade** acompanham o módulo dono (os `Fake*` e os `Fixture` de unidade viajam juntos); os **de integração** (`AbstractIntegrationTest` e tudo que sobe o Spring) **ficam em `:app`**, que enxerga todos.
- Cada módulo declara **só** as dependências do ADR (`api(project(":core"))`...): a compilação passa a recusar o que o ArchUnit já recusava.
- O Spring continua achando tudo: o `@SpringBootApplication` em `:app` está em `com.dbook` e todo módulo usa esse prefixo, então o escaneamento de componentes, de entidades JPA e de propriedades não muda.
- **O que precisa ser ajustado fora do código**: o `Dockerfile` e o `cd.yml` (o `bootJar` agora é de `:app`, em `app/build/libs`), o relatório de cobertura e a baseline do OpenAPI no `ci.yml`, `scripts/` e `.claude/CLAUDE.md`.

**48.22 Cobertura agregada** (`jacoco-report-aggregation` em `:app`, mínimo 75 % sobre o total, como hoje), **cache de build** ligado (`--build-cache`: rodar só o que mudou vem de graça, módulo por módulo) e o CI conferido.

**48.23 Fechamento.** `CLAUDE.md` e `README` com o mapa de módulos; o ADR passa a "aceita"; `check` exit 0; k6; memória do projeto.

## O que pode dar errado

| Risco | Como se pega | O que fazer |
|---|---|---|
| a lista congelada do 48.1 mostra **mais** arestas do que as 11 do ADR | já no 48.1 | o ADR é corrigido antes de qualquer refatoração |
| uma relação JPA entre módulos (herança `JOINED` de `Bookable`) quebra o `ddl-auto: validate` | o `check` (todo teste de integração valida o esquema) | o passo se desfaz com `git revert`; o 48.9 é refeito com o mapeamento mais simples |
| um `@MockBean` ou fixture de unidade passa a precisar de um módulo que não pode enxergar | falha de compilação do teste | a fixture sobe para `testFixtures` do `:core` ou o teste vira de integração em `:app` |
| o build fica **mais lento** (configurar 16 projetos) | tempo do `check` | cache de build e `org.gradle.parallel`; se ainda assim piorar, juntar módulos folha |
| a cobertura agregada cai abaixo de 75 % | `jacocoTestCoverageVerification` | investigar a classe que ficou sem teste de unidade ao mudar de módulo; **não** baixar o mínimo |

## Perguntas antes de começar

1. **Commit por passo:** posso commitar a cada passo verde (são uns 23), ou você prefere revisar e pedir o commit?
2. **Branch:** fazer o trabalho numa branch `m48-modulos` (e mesclar por fase) ou direto na `main`, como até aqui?
3. **Ordem em relação ao resto:** as lacunas funcionais (recuperação de senha e confirmação de e-mail) vêm **antes** do 48.1, ou o M48 vai primeiro?

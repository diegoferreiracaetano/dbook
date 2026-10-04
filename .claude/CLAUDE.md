# DBook (backend)

API de reservas em Kotlin + Spring Boot. Hoje só voos (`Flight`), mas o domínio foi modelado pra receber hotéis: tudo que é reservável estende `Bookable` (`domain/catalog/Bookable.kt`) e uma reserva (`Booking`) aponta pra um `Bookable` + um `Seat`. O cliente é o app Flutter em `../dbook-mobile`.

Contexto do dono: especialista em Android/KMP que está aprendendo Kotlin + Spring **fazendo**. Ao introduzir um conceito de Spring/JPA novo, explique em uma ou duas frases por que ele existe (analogia com Android/KMP quando ajudar) e siga o ritmo do `CHECKLIST.md`: um item por vez, explicar antes, confirmar depois de testar.

Idioma: docs (`README.md`, `docs/*.md`, `CHECKLIST.md`, este arquivo) e commits em **português**; identificadores, KDoc e comentários de código em **inglês** (é o que o código existente faz — não misture).

> Regra de ouro: **não invente padrões**. Antes de escrever código, ache o irmão mais próximo (outro use case, controller, adapter, teste) e copie a forma dele. As skills em `.claude/skills/` têm exemplos reais.

## Stack e versões (fonte: `build.gradle.kts`, `gradle/wrapper`, `docker-compose.yml`)

| Item | Versão / escolha |
|---|---|
| Kotlin | 2.0.21 (`-Xjsr305=strict`; plugins `spring`, `jpa`, `allopen` p/ `@Entity`) |
| JDK | toolchain 21 (Temurin) |
| Gradle | 8.8 (wrapper) — **não roda em JDK > 22**, por isso o `JAVA_HOME` explícito nos comandos |
| Spring Boot | 3.3.4 (web, data-jpa, security, websocket, data-redis) |
| Persistência | PostgreSQL 16 + Flyway (`src/main/resources/db/migration`, hoje V1–V27), `ddl-auto: validate`, `open-in-view: false` |
| Tempo real | STOMP/WebSocket + Redis Pub/Sub (Redis 7) |
| Observabilidade | Spring Boot Actuator + Micrometer (registro Prometheus). Endpoints em `/actuator/*` na **porta de gestão 8081** (nunca na 8080 pública): `health/liveness`, `health/readiness` (db + redis), `metrics`, `prometheus` |
| Logs | Logback + `logstash-logback-encoder`. Perfil `json` = um JSON por linha (produção/ECS); padrão = texto legível. `requestId`/`userId` no MDC (`RequestLoggingFilter`, `JwtAuthenticationFilter`). **Nunca logar** token, `Authorization`, corpo de requisição, query string nem dado de cartão |
| Fila | AWS SQS via SDK direto (`sqs` 2.28.29, mesma versão do Bedrock; sem Spring Cloud AWS) — expiração de reservas `PENDING` (M20); LocalStack local |
| Auth | JWT (jjwt 0.12.6) stateless, refresh token rotativo, BCrypt. Papéis com **permissões** (`Role.permissions`, só no domínio), conta bloqueável, limite de tentativas de login em Redis, política de senha, sessão do portal admin em cookie `httpOnly` (ver `docs/autenticacao.md`) |
| IA | AWS Bedrock (`bedrockruntime` 2.28.29) + rate limit com bucket4j 8.10.1 |
| Docs da API | springdoc-openapi 2.6.0 (Swagger UI) |
| Lint / estática | ktlint (plugin 12.1.1), detekt 1.23.8 (`config/detekt/detekt.yml`, `maxIssues: 0`) |
| Cobertura | JaCoCo 0.8.12, mínimo **75%** de linha |
| Testes | JUnit 5 + `kotlin-test-junit5`, Spring Boot Test (MockMvc, Mockito via `@MockBean`), Testcontainers 1.20.4 (fixado de propósito) |
| Corrotinas | **não usadas** — MVC bloqueante. Não adicione `kotlinx-coroutines` |

## Arquitetura (camadas em `com.dbook`, com subpacotes por conceito)

```
presentation  → application → domain ← infrastructure
   (HTTP)        (use cases)   (regras + portas)   (adapters)
```

Regras de dependência, **verificadas a cada build por testes ArchUnit** (`src/test/kotlin/com/dbook/architecture/` — uma regra por classe; quebrar uma reprova o `check`):
- `domain/` não importa Spring nem JPA. Contém entidades/invariantes, **portas** (`interface XxxRepository`, `PasswordHasher`, `TokenService`, `AvailabilityBroadcaster`, `AiSuggestionService`) e exceções de domínio.
- `application/` = um `@Service` por caso de uso (`XxxUseCase.execute(...)`), com o `XxxCommand` (data class) no mesmo arquivo. Só conhece `domain` (e Spring p/ `@Service`/`@Transactional`).
- `presentation/` = `@RestController`s + DTOs `XxxRequest`/`XxxResponse` (`companion fun from(...)`) + `ApiExceptionHandler`. Não importa `infrastructure`.
- `infrastructure/` = adapters das portas: `persistence` (`XxxRepositoryAdapter` + `XxxJpaEntity` + `XxxJpaRepository` + `XxxMappers.kt`, por conceito), `security` (JWT), `messaging` (Redis/STOMP), `ai` (Bedrock), `web` (interceptor de rate limit). Não importa `application`.
- Consumidor de fila (`BookingExpirationConsumer`) é adapter de **entrada**, como um controller: mora em `presentation/` e chama use case; `infrastructure/` só tem o lado que *publica* (`SqsBookingExpirationScheduler`).
- `config/` = configuração transversal (hoje só `OpenApiConfig`).
- **Cada camada se divide em subpacotes por conceito** (decisão de 2026-10-03, quando `domain/` chegou a 47 arquivos soltos): `domain/<conceito>/`, `application/<conceito>/`, `presentation/<conceito>/`, `infrastructure/persistence/<conceito>/`. Os 7 conceitos: `catalog` (Flight, Airport, Airline, Bookable — onde entraria Hotel), `seating` (Seat, SeatLayout), `booking` (Booking, expiração, disponibilidade em tempo real), `payment`, `review`, `identity` (User, token, refresh, hash de senha) e `ai`. O que atravessa conceitos (`ApiExceptionHandler`, `HealthController`, `SecurityExtensions`, `TransactionSupport`) vai em `common/`. A camada continua sendo o 1º nível — **não** crie `<conceito>/domain`, `<conceito>/application`... (opção avaliada e descartada: os conceitos compartilham o mesmo banco e têm relações JPA entre si, então módulos completos ainda não se pagam; se um dia um conceito for extraído, esta divisão já o deixa separado). Conceito novo = pasta nova nas camadas em que ele tiver arquivos; arquivo novo vai na pasta do conceito dono.
- Dependências entre conceitos (hoje, no `domain/`): só `booking → catalog` e `ai → catalog`; `seating`, `payment`, `review`, `identity` e `catalog` não dependem de nenhum outro e se referenciam por id (`bookableId`, `bookingId`). Mantenha **sem ciclos** (teste `DomainConceptsAreFreeOfCyclesTest`). Na persistência existe um acoplamento conhecido e aceito: `catalog` → `seating` (o `availableCapacity` do voo é derivado da contagem de assentos livres, então os adapters do catálogo consultam o `SeatJpaRepository`); está declarado como exceção em `PersistenceConceptsAreFreeOfCyclesTest` — qualquer outro ciclo novo reprova o build.

## Convenções de nome

- `XxxUseCase` / `XxxCommand` · porta `XxxRepository` (domain) → `XxxRepositoryAdapter` (infra) · `XxxJpaEntity` · `XxxJpaRepository` · `XxxController` · `XxxRequest` / `XxxResponse` · `XxxNotFoundException` (domain).
- Migration: `V<N>__snake_case.sql` (próxima livre: V28). PK `BIGSERIAL`. Nunca edite migration já aplicada — crie a próxima.
- Entidades JPA **não** são `data class` (equals/hashCode em associações lazy é armadilha) — por isso `LongParameterList.constructorThreshold` é 15 no detekt.
- Mappers domínio↔JPA ficam em `XxxMappers.kt` na pasta do conceito (`persistence/booking/BookingMappers.kt`, ...), um arquivo por conceito — o `Mappers.kt` único estourou `TooManyFunctions` e foi dividido em vez de subir o threshold. Um mapper que precisa converter entidade de outro conceito importa a função dele (ex.: `BookingMappers` importa `catalog.toDomain`).

## Regras para criar/alterar uma feature

1. Ache o caso de uso mais parecido e siga a mesma forma (ex.: pagar reservas → `RegisterPaymentUseCase`, espelho de `CancelBookingUseCase`).
2. Ordem: regra no **domain** (`init { require(...) }` / `check(...)`) → porta → use case → adapter/entidade/migration → controller + DTOs → testes → docs.
3. **Todo controller de negócio fica sob uma versão da API**: `@RequestMapping("${ApiPaths.V1}/recurso")` (`presentation/common/ApiPaths.kt`) — só os operacionais (`/health`, `/ws`, Swagger, actuator) ficam sem versão. A versão vive **só na `presentation`** (controllers e DTOs): use case e domínio não sabem que ela existe. Rota pública nova entra na lista de `SecurityConfig` como `/v1/...`. O teste `EveryApiControllerIsVersionedTest` reprova controller sem versão. Mudança que **quebra** cliente (remover/renomear campo, tipo, tornar algo obrigatório, mudar status) pede `/v2`; acrescentar campo opcional não. Ver `docs/versionamento.md`.
4. Endpoint autenticado por padrão: só o que está em `SecurityConfig.permitAll()` é público. Recurso do usuário logado usa `authentication.currentUserId()` (`presentation/SecurityExtensions.kt`) — **nunca** aceite `userId`/`customerId` no corpo. Endpoint administrativo: `@PreAuthorize("hasAuthority('FLIGHT_WRITE')")` — **sempre uma permissão, nunca um nome de papel** (`Role.permissions` é a tabela de quem tem o quê); `SecurityConfig` já exige `ADMIN_PORTAL_ACCESS` em `/v1/admin/**` e o teste `EveryAdminEndpointDeclaresItsPermissionTest` reprova endpoint `/admin` sem `@PreAuthorize`.
5. Todo controller novo: `@Tag`, `@Operation`, `@SecurityRequirement(name = "bearerAuth")` (se autenticado) e `@Schema(example = ...)` nos Requests.
6. Fechar um marco = atualizar `docs/endpoints.md` (endpoints novos), o documento do assunto em `docs/` e o `README.md` (só o mapa e o roadmap) **e** `CHECKLIST.md` (seção `## Mn — ...` com itens numerados + checklist de fechamento). É a memória do projeto.

## Tratamento de erros

- Invariante de domínio quebrada → `require` (→ `IllegalArgumentException` → **400**) ou `check` (→ `IllegalStateException` → **409**). Não crie exceção nova pra isso.
- Recurso ausente → `XxxNotFoundException : RuntimeException` em `domain/`, **e registre-a** em `ApiExceptionHandler.handleNotFound` (senão vira 500).
- Mapeamento atual (`ApiExceptionHandler`): NotFound→404 · `IllegalArgumentException`→400 · `IllegalStateException`/`OptimisticLockingFailureException`/`UserAlreadyExists`→409 · `InvalidCredentials`/`InvalidToken`→401 · `NotBookingOwner`→403 · `AiResponseParsing`→502 · `AiServiceUnavailable`→503. Corpo sempre `{"error": "<mensagem>"}`.
- Regras do detekt que pegam erro de tratamento: `TooGenericExceptionCaught/Thrown`, `SwallowedException`, `ThrowsCount ≤ 2`, `ReturnCount ≤ 2`. Suppress só pontual e com motivo (precedentes: `JwtTokenService`, `SeatLayout`).
- Ver a armadilha do Jackson com data class de **um único parâmetro** em `RefreshRequest`/`UpdateUserNameRequest` (`@JsonCreator`/`@JsonProperty`) — request de 1 campo precisa disso.

## Concorrência e transação

- Sem corrotinas. Concorrência é resolvida no banco: `@Version` (lock otimista) em `SeatJpaEntity`/`BookableJpaEntity`; conflito vira 409.
- `@Transactional` no use case que escreve. Efeito colateral externo (WebSocket/Redis) **só depois do commit**: `afterCommit { ... }` de `application/TransactionSupport.kt` (precedente: `RegisterBookingUseCase`, `CancelBookingUseCase`).
- Reservar assento acontece na criação da `Booking` (PENDING); pagamento só **confirma** (`Booking.confirm(paymentId)`), pra ninguém perder o assento durante o pagamento.

## Estratégia de testes (detalhes na skill `backend-testing`)

- **Um cenário por classe**, nome `XxxTest`, método `` `given ... when ... then ...` ``. Setup compartilhado numa classe `abstract XxxFixture` do mesmo pacote.
- Use case → unit test com **fakes escritos à mão** (`FakeBookingRepository`...) dentro do `Fixture`; sem Mockito nem Spring.
- Controller → `@WebMvcTest` + `@MockBean` do use case + `@MockBean TokenService`, `addFilters = false`.
- Segurança/fluxo real → `SecurityIntegrationFixture` (`@SpringBootTest` + Testcontainers Postgres/Redis compartilhados em `AbstractIntegrationTest`).
- Sem MockK, sem mockito-kotlin (não estão no projeto).

## Comandos oficiais

Sempre com o JDK 21 explícito (`export` uma vez ou prefixe):

```bash
export JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home

docker compose up -d                 # Postgres + Redis (+ LocalStack) p/ rodar localmente
./gradlew bootRun                    # sobe em :8080 (Flyway migra sozinho)
./scripts/seed-flights.sh 300        # popula voos via POST /v1/admin/flights

./gradlew ktlintCheck detekt         # estilo + análise estática (rápido, rode sempre)
./gradlew ktlintFormat               # corrige formatação
./gradlew test                       # todos os testes
./gradlew test --tests 'com.dbook.application.booking.cancelbookingusecase.*'   # um pacote
./gradlew check                      # o que o CI roda: test + ktlint + detekt(Main/Test) + JaCoCo ≥ 75%
./gradlew jacocoTestReport           # build/reports/jacoco/test/html/index.html
```

**Testcontainers local:** precisa do Docker Desktop aberto. O `build.gradle.kts` já fixa `api.version=1.41` (sem isso o Testcontainers 1.20.4 falha com `Could not find a valid Docker environment` no Docker Desktop recente). Se esses testes falharem localmente, **é falha real** — o mesmo que o CI vê; não os descarte como limitação de ambiente.

## Regras de qualidade (sempre valem)

- `maxIssues: 0` no detekt; linha ≤ 120 (`.editorconfig` e detekt precisam bater); 4 espaços; trailing comma; imports em ordem lexicográfica com `java`, `javax`, `kotlin` **no fim** (o ktlint reprova senão).
- `WildcardImport` proibido (exceto `java.util.*`); `MagicNumber` proibido em `main` (constante nomeada); `TODO:`/`FIXME:` proibidos em comentário; `UnusedPrivateMember` ativo.
- Limites: `LongMethod` 60, `CyclomaticComplexMethod` 15, `NestedBlockDepth` 4, `LongParameterList` função 6 / construtor 15, `TooManyFunctions` arquivo 16 / classe 11. Estourou → **extraia**; só suba threshold se for crescimento legítimo de atributos e documente o motivo no `detekt.yml` (é o que o arquivo já faz).
- Não subir o mínimo do JaCoCo pra "passar" nem excluir classe da cobertura sem motivo (as exclusões atuais — `*JpaEntity*`, `*Request*`, `*Response*`, `DbookApplicationKt` — são dados sem ramificação).
- Não commitar segredo. (`jwt.secret` em `application.yml` é só de dev local; produção vem do Secrets Manager.)
- Commits: português, imperativo, um por marco (`Adiciona Payment real e POST /payments (M18)`). **Sem** trailer `Co-Authored-By` (preferência explícita do dono). Não commitar/dar push sem pedido.

## Definition of Done

Antes de dizer "pronto", confirme cada item (ou diga qual não se aplica e por quê):

- [ ] Segue a arquitetura acima e imita o irmão mais próximo (nenhuma camada importando o que não deve).
- [ ] Nenhuma duplicação nova que um helper/porta existente resolveria; nenhuma abstração sem 2º uso real.
- [ ] Invariantes no `domain`; erro novo mapeado no `ApiExceptionHandler` com o status certo.
- [ ] Endpoint novo: autenticação correta (`currentUserId()`, sem id no corpo), `@Tag`/`@Operation`/`@Schema`.
- [ ] Migration nova (se mexeu no schema) sem editar as antigas; entidade/mapper/adapter alinhados.
- [ ] Testes criados/atualizados no padrão um-cenário-por-classe (caminho feliz **e** as falhas: 400/403/404/409).
- [ ] `./gradlew ktlintCheck detekt` limpo e `./gradlew test` rodado — sem nenhuma falha (inclusive os de integração, que exigem Docker aberto).
- [ ] `docs/endpoints.md` (ou o `docs/` do assunto), o roadmap do `README.md` e o `CHECKLIST.md` atualizados quando o comportamento público mudou.
- [ ] Relatório final lista **arquivos alterados** e **validações executadas** (com o resultado real, não o esperado).

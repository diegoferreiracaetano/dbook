---
name: backend-testing
description: Convenções de teste do backend Kotlin/Spring do DBook — um cenário por classe, nome given/when/then, fakes escritos à mão para use cases, @WebMvcTest com @MockBean para controllers, Testcontainers para segurança/integração, cobertura JaCoCo e comandos oficiais. Use SEMPRE que for escrever, revisar, consertar ou rodar testes deste repositório, ou quando uma feature nova precisar de cobertura, mesmo que o pedido diga só "testa isso".
---

# Testes no backend DBook

Estratégia real do projeto (extraída de `src/test/kotlin`, ~100 testes): **muitos testes unitários rápidos com fakes**, um punhado de slices `@WebMvcTest`, e integração de verdade só onde segurança/banco é o ponto. Não há MockK nem mockito-kotlin.

## Qual tipo de teste usar

| Sujeito | Tipo | Base | Ferramentas |
|---|---|---|---|
| Entidade/regra de domínio | unit puro, sem Spring | classe simples | `kotlin.test` |
| Use case (`application`) | unit com fakes | `abstract XxxFixture` | `kotlin.test`, `Fake*Repository` à mão |
| Controller (contrato HTTP) | slice | `@WebMvcTest` | `MockMvc` DSL, `@MockBean`, `BDDMockito` |
| Segurança (401/403), fluxo ponta a ponta, concorrência | integração | `SecurityIntegrationFixture` → `AbstractIntegrationTest` | `@SpringBootTest`, Testcontainers Postgres+Redis |
| Adapter externo (Bedrock, rate limit, STOMP) | unit/integração no pacote `infrastructure/...` | conforme o caso | ver testes vizinhos |

Regra prática: **suba o menor contexto que prova a regra.** Regra de negócio não precisa de Spring; contrato HTTP não precisa de banco; só segurança real e concorrência precisam do container.

## Nomenclatura (obrigatória)

- **Um `@Test` por classe.** Classe descreve o cenário: `ThrowsWhenANonOwnerCancelsTest`, `PaysBothLegsOfARoundTripTogetherTest`, `RejectsABlankEmailTest`.
- Método em backticks: `` `given <contexto> when <ação> then <resultado>` ``. O nome **é** a documentação — sem comentário no corpo explicando o óbvio.
- Pacote de teste = pacote do sujeito em minúsculas: `com.dbook.application.booking.cancelbookingusecase`, `com.dbook.presentation.catalog.flightadmincontroller`, `com.dbook.domain.payment` — o teste fica na pasta do **conceito** do sujeito, igual à produção.
- Setup compartilhado → `abstract class XxxFixture` no mesmo pacote (o detekt exclui `UnnecessaryAbstractClass` em teste, de propósito).

## Exemplos reais

**Use case com fixture + fakes** (`application/cancelbookingusecase/`):
```kotlin
class FakeBookingRepository(initial: List<Booking>) : BookingRepository {
    val store = initial.associateBy { requireNotNull(it.id) }.toMutableMap()
    override fun findById(id: Long): Booking? = store[id]
    override fun save(booking: Booking): Booking { store[requireNotNull(booking.id)] = booking; return booking }
    // ...
}

abstract class CancelBookingUseCaseFixture {
    protected val bookingId = 100L
    protected val ownerId = 1L
    protected val bookingRepository = FakeBookingRepository(listOf(Booking(bookingId, flight, seatId, ownerId)))
    protected val useCase = CancelBookingUseCase(bookingRepository, seatRepository, availabilityBroadcaster)
}

class ThrowsWhenANonOwnerCancelsTest : CancelBookingUseCaseFixture() {
    @Test
    fun `given another user's booking when a non-owner CLIENT cancels it then it throws NotBookingOwnerException`() {
        assertFailsWith<NotBookingOwnerException> {
            useCase.execute(bookingId, requestingUserId = 999, requestingUserRole = Role.CLIENT)
        }
    }
}
```
Use case que chama `afterCommit { }` precisa de sincronização de transação simulada — o fixture tem o helper `withTransactionSynchronization { ... }` (`TransactionSynchronizationManager.initSynchronization()` / `clearSynchronization()` no `finally`). Copie-o em vez de reinventar.

**Domínio** (`domain/payment/RejectsAnInvalidCardLast4Test.kt`):
```kotlin
class RejectsAnInvalidCardLast4Test {
    @Test
    fun `given a cardLast4 that isn't 4 digits when a Payment is built then it throws IllegalArgumentException`() {
        assertFailsWith<IllegalArgumentException> {
            Payment(customerId = 1, amount = BigDecimal("100.00"), cardLast4 = "42", cardholderName = "Jane Doe")
        }
    }
}
```

**Controller** (`presentation/flightadmincontroller/`):
```kotlin
@WebMvcTest(FlightAdminController::class)
@AutoConfigureMockMvc(addFilters = false)   // segurança é testada na integração, não aqui
abstract class FlightAdminControllerFixture {
    @Autowired lateinit var mockMvc: MockMvc
    @MockBean lateinit var registerFlightUseCase: RegisterFlightUseCase
    @MockBean lateinit var tokenService: TokenService   // o filtro JWT ainda é carregado no slice
}

class ReturnsNotFoundWhenAirportDoesNotExistTest : FlightAdminControllerFixture() {
    @Test   // org.junit.jupiter.api.Test nos slices
    fun `given a nonexistent airport when posting to admin flights then it returns 404`() {
        given(registerFlightUseCase.execute(expectedCommand)).willThrow(AirportNotFoundException("GRU"))
        mockMvc.post("/admin/flights") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect { status { isNotFound() } }
    }
}
```

**Integração** (`presentation/securityintegration/`): estenda `SecurityIntegrationFixture` e use `registerAndLogin(uniqueEmail())`, `registerFlightWithOneSeat()`, header `Authorization: Bearer $token`. Sempre teste os dois lados: sem token → 401; token de outro usuário → 403/lista sem vazar dado alheio.

## O que cobrir por feature nova

Caminho feliz + **cada** ramo de falha do use case (`?: throw`, dono errado, estado inválido) + contrato HTTP de cada status que o controller pode devolver + (se endpoint novo) teste de que exige autenticação. Domínio: um cenário por `require`/`check`.

## Assincronismo e determinismo

- Não há corrotinas nem `Thread.sleep` nos testes — e não deve haver. Efeito pós-commit é testado exercitando `afterCommit` sob `withTransactionSynchronization`; concorrência real vive em `BookingConcurrencyTest` (integração).
- Datas fixas (`LocalDateTime.of(2026, 10, 1, 8, 0)`), ids explícitos, e-mail único por execução em integração (`uniqueEmail()`). Nada de `now()` na asserção.
- Cada cenário independe dos outros; nada de ordem implícita.

## Proibido

- `Thread.sleep`, ordem de execução implícita, estado estático compartilhado, asserção enfraquecida ou teste apagado só pra ficar verde.
- Mockar o que dá pra faker à mão em use case; adicionar MockK/mockito-kotlin/AssertJ sem pedido.
- `@SpringBootTest` para testar regra que cabe num unit test.
- Teste de getter/DTO trivial (o JaCoCo já exclui `*JpaEntity*`, `*Request*`, `*Response*`).

## Lint nos testes

ktlint + detekt rodam em `src/test` também. Imports: lexicográficos com `java`, `javax`, `kotlin` **no fim**. Linha ≤ 120 (nome `given/when/then` longo estoura fácil — encurte o texto, não desligue a regra).

## Cobertura e comandos

```bash
export JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home
./gradlew test --tests 'com.dbook.application.booking.cancelbookingusecase.*'   # estreito primeiro
./gradlew test                                                          # tudo
./gradlew ktlintCheck detekt
./gradlew jacocoTestReport                       # build/reports/jacoco/test/html/index.html
./gradlew check                                  # CI: inclui JaCoCo ≥ 75%
```

**Docker precisa estar aberto.** As classes que estendem `AbstractIntegrationTest` usam Testcontainers (Postgres, Redis; o LocalStack nos testes de fila). O `build.gradle.kts` já fixa `api.version=1.41` para o Docker Desktop recente, então **falha nelas é falha real**, não limitação de ambiente (foi assim que dois testes quebrados ficaram escondidos por meses). Sem Docker rodando, abra-o e rode de novo; nunca descarte essas falhas.

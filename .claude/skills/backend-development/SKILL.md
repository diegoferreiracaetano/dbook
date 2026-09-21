---
name: backend-development
description: Como desenvolver no backend Kotlin/Spring do DBook — use cases, controllers, DTOs, adapters JPA, migrations Flyway, segurança JWT, tratamento de erros e transações, com exemplos reais do código. Use SEMPRE que for criar ou alterar endpoint, caso de uso, entidade, repositório, migration ou regra de negócio neste repositório, mesmo que o pedido seja pequeno ("adiciona um campo", "corrige esse 500", "novo GET").
---

# Desenvolvimento no backend DBook

Objetivo: qualquer mudança nova parecer escrita pela mesma pessoa que escreveu o resto. Por isso o método é **achar o irmão mais próximo e imitar**, não decidir do zero. Padrões abaixo foram extraídos do código; se o código real divergir, o código vence — atualize esta skill.

## Receita de uma feature (ordem importa)

1. **Domain** — invariante e porta. Entidade em Kotlin puro, validando no `init`:
   ```kotlin
   // domain/Payment.kt
   class Payment(val id: Long? = null, val customerId: Long, val amount: BigDecimal,
                 val cardLast4: String, val cardholderName: String) {
       init {
           require(amount > BigDecimal.ZERO) { "amount must be positive" }
           require(CARD_LAST4_PATTERN.matches(cardLast4)) { "cardLast4 must be exactly 4 digits" }
           require(cardholderName.isNotBlank()) { "cardholderName must not be blank" }
       }
   }
   // domain/PaymentRepository.kt — porta (interface), sem Spring
   interface PaymentRepository { fun save(payment: Payment): Payment }
   ```
   Transições de estado são métodos da entidade que devolvem **nova instância** e usam `check`: `Booking.confirm(paymentId)` / `cancel()` → `transitionTo(...)` com `check(status == PENDING)`.
2. **Application** — um `@Service` com `execute`, `Command` como `data class` no mesmo arquivo, KDoc dizendo o *porquê*:
   ```kotlin
   data class RegisterPaymentCommand(val bookingIds: List<Long>, val cardLast4: String,
                                     val cardholderName: String, val requestingUserId: Long)

   @Service
   class RegisterPaymentUseCase(
       private val bookingRepository: BookingRepository,
       private val paymentRepository: PaymentRepository,
   ) {
       @Transactional
       fun execute(command: RegisterPaymentCommand): Payment { ... }
   }
   ```
   Injeção **por construtor** com portas do `domain`. Nada de `@Autowired` em código de produção.
3. **Infrastructure** — `XxxJpaEntity` (classe comum, `var`, defaults; PK `IDENTITY`), `XxxJpaRepository : JpaRepository`, `XxxRepositoryAdapter : XxxRepository` (`@Repository`), mappers `toDomain()`/`toJpaEntity()` em `Mappers.kt` (ou arquivo próprio se estourar 16 funções). Relacionamento por referência: `bookableJpaRepository.getReferenceById(id)` no `save` (ver `BookingRepositoryAdapter`).
4. **Migration** — `src/main/resources/db/migration/V<N>__snake_case.sql`, `BIGSERIAL PRIMARY KEY`. Coluna nova `NOT NULL` em tabela com dados: adicione nullable → `UPDATE` (backfill) → `SET NOT NULL` (ver `V19__add_aircraft_type_to_flight.sql`).
5. **Presentation** — controller fino + DTOs:
   ```kotlin
   @RestController @RequestMapping("/payments")
   @Tag(name = "Payments", description = "...") @SecurityRequirement(name = "bearerAuth")
   class PaymentController(private val registerPaymentUseCase: RegisterPaymentUseCase) {
       @Operation(summary = "Pays for the given bookings, confirming each of them")
       @PostMapping
       fun register(@RequestBody request: RegisterPaymentRequest, authentication: Authentication)
           : ResponseEntity<PaymentResponse> { ... requestingUserId = authentication.currentUserId() ... }
   }
   ```
   `Response` tem `companion object { fun from(domain) }`; `Request` usa `@get:Schema(example = ...)`. O controller só traduz HTTP ↔ `Command`; regra de negócio **não** entra aqui.
6. **Docs** — `README.md` (seção Endpoints, com `curl` e lista de status) e `CHECKLIST.md` (`## Mn`). Fecha o marco.

## Onde cada coisa vai

| Preciso de… | Vai em… |
|---|---|
| Validar dado / regra que sempre vale | `domain` (`init`/`check` na entidade) |
| Regra que orquestra vários repositórios | `application/XxxUseCase` |
| Acesso a banco, Redis, JWT, Bedrock | `infrastructure` (adapter de uma porta do `domain`) |
| Formato JSON, status HTTP, anotação Swagger | `presentation` |
| Ler o usuário logado | `authentication.currentUserId()` / `currentRole()` |

## Erros — como o projeto faz

- Regra de domínio: `require` → 400, `check` → 409. Não crie exceção só pra isso.
- Não encontrado: `class BookingNotFoundException(id: Long) : RuntimeException("Booking not found: $id")` em `domain/`, `?: throw` no use case (`bookingRepository.findById(id) ?: throw BookingNotFoundException(id)`), e **adicione a classe na lista de `ApiExceptionHandler.handleNotFound`**. Esquecer isso vira HTTP 500.
- Dono do recurso: `if (booking.customerId != requestingUserId) throw NotBookingOwnerException(id)` (→ 403). `CancelBookingUseCase` deixa ADMIN passar; `RegisterPaymentUseCase` não.
- Resposta de erro é sempre `{"error": "..."}` (`Map<String, String>`).

## Transação e efeitos colaterais

```kotlin
@Transactional
fun execute(...): Booking {
    ...
    val saved = bookingRepository.save(booking)
    afterCommit {   // application/TransactionSupport.kt — nunca no rollback
        availabilityBroadcaster.broadcast(bookableId, seatRepository.countAvailable(bookableId))
    }
    return saved
}
```
Concorrência = lock otimista (`@Version` em `SeatJpaEntity`); dois clientes no mesmo assento → um recebe 409. Não introduza `synchronized`, locks manuais ou corrotinas.

## Segurança

- Tudo é autenticado, exceto o que está em `SecurityConfig.permitAll()` (health, auth, busca de voos, destinos, mapa de assentos, swagger, `/ws/**`). Endpoint público novo = mudança **deliberada** nessa lista + teste de que continua público.
- ADMIN: `@PreAuthorize("hasRole('ADMIN')")` no método do controller (ver `FlightAdminController`).
- Nunca receba `customerId`/`userId` do cliente; derive do token.
- Request de **um único campo** precisa de `@JsonCreator`/`@JsonProperty` explícitos (armadilha do Jackson já registrada em `RefreshRequest`/`UpdateUserNameRequest`).
- Segredo nunca no repositório; o `jwt.secret` de `application.yml` é só de dev local.

## Estilo Kotlin do projeto

- 4 espaços, ≤ 120 colunas, trailing comma, argumentos nomeados em construções longas, `val x =` com o valor na linha de baixo quando quebra.
- KDoc em inglês explicando **por que** (não o quê). Sem `TODO:`/`FIXME:` (detekt reprova).
- Sem `!!` em produção (`UnsafeCallOnNullableType`); use `requireNotNull(x) { "msg" }`/`checkNotNull`.
- Sem número mágico em `main`; `@Suppress("MagicNumber")` só quando o número **é** o dado de negócio (`SeatLayout.kt`), com comentário.
- Função com >6 parâmetros ou >60 linhas → extraia (o detekt falha). Padrão do projeto: extrair helper privado (`resolveAirline`/`resolveAirport` em `RegisterFlightUseCase`).

## O que evitar (já foi decidido contra, ou o código prova)

- Lógica de negócio em controller ou em adapter.
- `domain` importando Spring/JPA; `presentation` importando `infrastructure`; `application` importando `infrastructure`.
- `data class` como entidade JPA; `@Autowired` em produção; `open-in-view`.
- Aceitar id do usuário no corpo; devolver entidade JPA no JSON (sempre `Response.from(domain)`).
- Editar migration aplicada; usar `ddl-auto` diferente de `validate`.
- Subir `detekt.yml`/JaCoCo para passar; `@Suppress` sem motivo.
- Adicionar dependência (MockK, MapStruct, coroutines, Lombok-like) sem pedido explícito.
- "Melhorar" código fora do escopo da tarefa — reporte no fim.

## Checklist antes de entregar

`ktlintCheck detekt` limpos → testes do pacote → `./gradlew test` → README/CHECKLIST → relatório com arquivos alterados e validações reais. Detalhes de teste: skill `backend-testing`.

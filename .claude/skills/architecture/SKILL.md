---
name: architecture
description: Decisões arquiteturais do backend Kotlin/Spring do DBook — responsabilidade de cada camada, onde colocar lógica nova, quando criar (ou não) uma abstração, porta, entidade de domínio ou migration, com exemplos reais. Use SEMPRE que a dúvida for "onde isso vai?", "preciso de uma interface/classe nova?", "como modelar hotéis/um novo tipo de reserva", "esse código tá na camada certa?" ou ao planejar qualquer feature que cruze camadas, antes de escrever código.
---

# Arquitetura — backend DBook

Camadas planas em `com.dbook`, dependência sempre para dentro:

```
presentation ─▶ application ─▶ domain ◀─ infrastructure
```

O que torna isso verdade **hoje** (checado por grep, 0 violações): `domain` não importa Spring/JPA; `application` e `presentation` não importam `infrastructure`; `infrastructure` não importa `application`. É o que permite testar use case com fakes sem subir Spring — proteja isso.

## Responsabilidade de cada camada

| Camada | É | Não é |
|---|---|---|
| `domain` | Entidades com invariantes (`Booking`, `Payment`, `Bookable`/`Flight`), **portas** (interfaces), exceções de domínio, funções puras de negócio (`seatLayoutFor`) | Lugar de Spring, JPA, JSON, HTTP |
| `application` | Orquestração de um caso de uso: busca, valida dono, chama regras do domínio, salva, dispara efeito pós-commit. Uma classe por caso de uso, `execute(command)` | Lugar de regra que sempre vale (isso é domínio) nem de detalhe de HTTP/SQL |
| `presentation` | Traduz HTTP ↔ `Command`/`Response`, status codes, autenticação/autorização de rota, Swagger | Lugar de regra de negócio, transação ou acesso a banco |
| `infrastructure` | Implementa portas: JPA (`XxxRepositoryAdapter`), JWT, Redis/STOMP, Bedrock, rate limit. Converte domínio ↔ entidade JPA | Lugar de decisão de negócio |
| `config` | Beans/config transversais (`OpenApiConfig`) | — |

## Onde uma lógica nova deve entrar (decida nesta ordem)

1. **É uma regra que vale sempre, independente de quem chama?** → `domain`, no `init` da entidade (`require`) ou num método dela (`check`). Ex.: cartão tem 4 dígitos (`Payment`); só reserva PENDING transiciona (`Booking.transitionTo`).
2. **Envolve buscar/salvar mais de uma coisa, ou checar permissão do usuário?** → `application`, num `XxxUseCase`. Ex.: `RegisterPaymentUseCase` valida dono de cada reserva, soma preços, salva `Payment`, confirma reservas.
3. **Precisa de tecnologia externa (banco, cache, token, IA)?** → declare a **porta** no `domain` (interface) e implemente o **adapter** em `infrastructure`. Ex.: `AvailabilityBroadcaster` (porta) ← `RedisAvailabilityBroadcaster`; `PasswordHasher` ← `BCryptPasswordHasher`.
4. **É formato/verbo/status HTTP?** → `presentation`.

Se a resposta parece "um pouco de cada", provavelmente é um use case chamando o domínio — não espalhe.

## Quando criar uma abstração (e quando NÃO)

Crie **porta** (interface no domain) quando a `application` precisa de algo que só a infra sabe fazer. Já é o padrão para 100% dos repositórios.

**Não** crie: interface com uma implementação e sem porta externa (o `XxxUseCase` é classe concreta — não existe `IXxxUseCase`); camada de "service" extra entre controller e use case; mapper genérico/lib de mapeamento (o projeto tem `Mappers.kt` explícito); base class "para reaproveitar" com uma só subclasse; pacote por feature; `Manager`/`Helper`/`Util` cujo nome não diz responsabilidade.

Regra do 2º uso: extraia helper/abstração quando o **segundo** caso real aparecer, no menor escopo possível (precedentes: `resolveAirline`/`resolveAirport` privados em `RegisterFlightUseCase`; `afterCommit` é função de `application/TransactionSupport.kt` compartilhada por `RegisterBookingUseCase` e `CancelBookingUseCase`, não copiada em cada um).

Quando `detekt` reclama de tamanho (`TooManyFunctions`, `LongMethod`): **extraia** (arquivo próprio, helper privado). Só suba threshold quando for crescimento legítimo de dados — e registre o motivo no `detekt.yml`. Precedente: `PaymentMappers.kt` em vez de subir o limite de `Mappers.kt`.

## Decisões estruturais já tomadas (não reverta sem o dono)

- **`Bookable` abstrato + `Flight`** (herança JPA `JOINED`): hotéis entrarão como novo subtipo; `Booking` e `RegisterBookingUseCase` não sabem de voo. Não acople código genérico a `Flight`.
- **Reserva cria PENDING e já reserva o assento; pagamento só confirma** (`Booking.confirm(paymentId)`): evita perder o assento durante o pagamento. `Payment` cobre N reservas de uma vez (Round Trip) e guarda só `cardLast4` — número/CVV nunca chegam ao backend.
- **Concorrência no banco** (`@Version` em `Seat`/`Bookable`), sem corrotinas nem locks em memória.
- **Efeito externo só após commit** (`afterCommit`), nunca dentro da transação.
- **`SeatLayout` é a única fonte** do mapeamento aeronave → colunas por fileira; cliente recebe `seatLayout` pronto. Regra de negócio do cliente vem do backend, não é duplicada no app.
- **Segurança por padrão-fechado** (`anyRequest().authenticated()`); identidade sempre do JWT.
- **Erro = exceção de domínio + `ApiExceptionHandler`** com corpo `{"error": ...}`.

## Exemplos reais de "onde foi parar"

- *Pagar reservas (M18)*: invariantes de cartão em `domain/Payment`; `Booking.confirm(paymentId)` no domínio; orquestração em `application/RegisterPaymentUseCase` (espelho de `CancelBookingUseCase`); `PaymentRepository` porta → `PaymentRepositoryAdapter`; `PaymentJpaEntity` + migration `V20`; `PaymentController` + DTOs em `presentation`. Nada de regra no controller.
- *Minhas viagens (M17)*: `Booking.bookable` já é o `Flight` completo → o use case só compõe; não criou segunda consulta nem estrutura paralela.
- *Perfil (`/users/me`)*: leitura simples direto no controller via porta existente; escrita (`PATCH`) ganhou `UpdateUserNameUseCase`. Não crie use case para `findById` de uma linha.

## Perguntas para se fazer antes de mexer em estrutura

1. Existe um caso irmão que já resolve isso? (Se sim, imite.)
2. Essa mudança faz `domain`/`application` importar algo que não devia?
3. Estou criando abstração para um único uso? Vai ter 2º uso em breve **de verdade**?
4. Isso muda o contrato JSON? O app `../dbook-mobile` espelha essa resposta — o campo é aditivo?
5. Como eu testaria isso? Se exige Spring inteiro para testar uma regra, a regra está na camada errada.

## Documentar

Mudança estrutural relevante vira item no `CHECKLIST.md` (seção do marco) com o **porquê** — é assim que o projeto guarda decisões.

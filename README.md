# DBook

[![CI](https://github.com/diegoferreiracaetano/dbook/actions/workflows/ci.yml/badge.svg)](https://github.com/diegoferreiracaetano/dbook/actions/workflows/ci.yml)

Backend de reservas em Kotlin + Spring Boot, começando por passagens aéreas e com hotéis planejados como segunda especialização do mesmo domínio (M9). Projeto pessoal de estudo de backend Kotlin, construído em marcos incrementais.

## Stack

- Kotlin 2.0.21 + Spring Boot 3.3.4 (JDK 21 LTS)
- PostgreSQL 16 + Flyway (migrations versionadas)
- Spring Security + JWT ([jjwt](https://github.com/jwtk/jjwt)) com refresh rotativo
- Gradle Kotlin DSL (wrapper incluso)
- Docker Compose (Postgres local)
- ktlint + detekt (`./gradlew check`)

Planejado para os próximos marcos: Redis (WebSocket Pub/Sub), AWS Bedrock (IA), Terraform + AWS ECS Fargate, GitHub Actions.

## Arquitetura

Clean Architecture, em camadas por pacote:

```
com.dbook
├── domain            # Entidades e regras de negócio puras. Zero dependência de Spring/JPA.
│                      # Define as "portas" (interfaces) que a infraestrutura implementa.
├── application       # Casos de uso — orquestram domínio + portas.
├── infrastructure
│   └── persistence   # Entidades JPA, repositórios Spring Data, adapters e mappers
│                      # domínio <-> JPA.
└── presentation      # Controllers REST, DTOs de request/response, exception handler.
```

`Bookable` é a abstração central do domínio: `Flight` (e futuramente `Accommodation`, para hotéis) especializa `Bookable`. A persistência usa herança JPA `JOINED` (tabela própria por especialização) para evitar colunas nulas quando o segundo tipo reservável for adicionado.

## Autenticação

JWT stateless (sem sessão), com refresh token rotativo persistido (hash, nunca em texto puro) — reutilizar um refresh token já trocado é rejeitado.

```bash
# registrar (sempre cria role CLIENT)
curl -X POST localhost:8080/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email": "diego@example.com", "password": "s3cret-password"}'

# login -> { accessToken, refreshToken }
curl -X POST localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email": "diego@example.com", "password": "s3cret-password"}'

# usar o accessToken nas rotas protegidas
curl -X POST localhost:8080/bookings \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <accessToken>" \
  -d '{"bookableId": 1}'

# trocar o refreshToken por um par novo (o antigo é revogado no ato)
curl -X POST localhost:8080/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{"refreshToken": "<refreshToken>"}'
```

Rotas públicas: `/health`, `/auth/**`, `/flights/search`, Swagger. Todo o resto exige `Authorization: Bearer <token>`. `POST /admin/flights` exige role `ADMIN`.

Não existe endpoint para promover um usuário a `ADMIN` (não foi pedido, e permitir isso via API seria uma falha de segurança). Pra testar rotas de admin localmente, promova direto no banco:

```sql
UPDATE app_user SET role = 'ADMIN' WHERE email = 'diego@example.com';
```

## Rodando localmente

Pré-requisito: Docker Desktop instalado e aberto.

```bash
docker compose up -d
JAVA_HOME="/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home" ./gradlew bootRun
```

> O `JAVA_HOME` explícito é necessário porque o Gradle 8.8 (versão do wrapper) ainda não roda em JDK mais recentes que o 21/22 — ver decisão registrada no histórico do projeto.

A aplicação sobe em `http://localhost:8080`. O Flyway aplica as migrations automaticamente (schema + seed de 3 aeroportos: `GRU`, `GIG`, `JFK`).

## Documentação da API (Swagger)

Com a aplicação no ar:

- Swagger UI: http://localhost:8080/swagger-ui/index.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs

## Endpoints

### `GET /health`
Confirma que a aplicação está no ar.

### `POST /admin/flights` (requer role `ADMIN`)
Cadastra um voo, resolvendo origem/destino por código IATA.

```bash
curl -X POST localhost:8080/admin/flights \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <accessToken-de-um-ADMIN>" \
  -d '{
    "flightNumber": "DB1234",
    "originIataCode": "GRU",
    "destinationIataCode": "GIG",
    "departureTime": "2026-10-01T08:00:00",
    "arrivalTime": "2026-10-01T09:10:00",
    "seatClass": "ECONOMY",
    "price": 450.00,
    "totalCapacity": 180
  }'
```

Retorna `201` com o voo criado, `401` sem token, `403` se o token não for de um `ADMIN`, ou `404` se o código IATA de origem/destino não existir.

### `GET /flights/search?origin=&destination=&date=`
Busca voos por rota e data.

```bash
curl "localhost:8080/flights/search?origin=GRU&destination=GIG&date=2026-10-01"
```

### `POST /bookings` (autenticado)
Reserva um `Bookable` (hoje só `Flight`; qualquer especialização futura funciona sem mudar este endpoint) em nome do usuário autenticado, decrementando a disponibilidade. Cria a reserva como `PENDING`.

```bash
curl -X POST localhost:8080/bookings \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <accessToken>" \
  -d '{"bookableId": 1}'
```

Retorna `201` com a reserva criada, `401` sem token, `404` se o `bookableId` não existir, ou `409` se não houver disponibilidade (ou em caso de conflito de concorrência — duas reservas simultâneas disputando o último lugar).

### `POST /bookings/{id}/cancel` (autenticado, dono ou ADMIN)
Cancela uma reserva `PENDING`, devolvendo a disponibilidade ao `Bookable`. Só quem criou a reserva (ou um `ADMIN`) pode cancelá-la.

```bash
curl -X POST localhost:8080/bookings/1/cancel \
  -H "Authorization: Bearer <accessToken>"
```

Retorna `200` com a reserva `CANCELLED`, `401` sem token, `403` se não for o dono nem `ADMIN`, `404` se não existir, ou `409` se a reserva não estiver `PENDING` (já confirmada ou já cancelada).

## Qualidade de código

```bash
JAVA_HOME="/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home" ./gradlew check
```

Roda testes + [ktlint](https://github.com/pinterest/ktlint) (estilo/formatação) + [detekt](https://detekt.dev/) (análise estática) juntos. `./gradlew ktlintFormat` corrige formatação automaticamente. Configuração em `.editorconfig` (4 espaços, `max_line_length=120`, precisa bater entre os dois) e `config/detekt/detekt.yml`.

## Testes

```bash
JAVA_HOME="/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home" ./gradlew test
```

- **Domínio** (`FlightTest`, `BookingTest`, `UserTest`): invariantes de `Bookable`/`User` e a máquina de estados de `Booking` (PENDING → CONFIRMED/CANCELLED).
- **Aplicação** (`RegisterFlightUseCaseTest`, `RegisterUserUseCaseTest`, `LoginUseCaseTest`): regras dos casos de uso com repositórios/hasher/token service fake.
- **Apresentação** (`FlightAdminControllerTest`, `FlightSearchControllerTest`): contrato HTTP (status code, shape do JSON, mapeamento de exceção) com o caso de uso mockado via `@WebMvcTest` — segurança desligada nesses slices de propósito (ver `SecurityIntegrationTest`).
- **Concorrência** (`BookingConcurrencyTest`): duas threads disputando o último assento contra o Postgres real.
- **Segurança** (`SecurityIntegrationTest`): `@SpringBootTest` completo — rota admin sem token (401) e com role errada (403), reserva exige autenticação, dono vs. não-dono de reserva vs. ADMIN, rotação de refresh token (reuso rejeitado), busca pública sem token.

Esses dois últimos usam [Testcontainers](https://testcontainers.com/) (`AbstractIntegrationTest`) — sobem um Postgres descartável sozinhos, não precisam mais de `docker compose up -d` manual. Só exigem Docker instalado e rodando.

> **Nota:** em algumas instalações do Docker Desktop muito recentes, o Testcontainers pode falhar ao detectar o daemon (`Could not find a valid Docker environment`) por incompatibilidade do cliente HTTP interno com a API do Docker. Se isso acontecer localmente, o pipeline de CI (GitHub Actions, Docker padrão do runner) continua funcionando normalmente — é uma limitação do ambiente local, não do código.

## Status do roadmap

- ✅ **M1 — Domínio e persistência** (voos, herança JPA `JOINED`, endpoints de cadastro/busca, testes)
- ✅ **M2 — Reserva com concorrência real** (lock otimista, endpoint de reserva/cancelamento, teste de concorrência real, Swagger)
- ✅ **M3 — Segurança** (User + BCrypt, JWT com refresh rotativo, filtro de autenticação, rotas públicas/protegidas, roles + `@PreAuthorize`, testes de segurança)
- ✅ **M4 — CI** (GitHub Actions rodando `./gradlew check`, Testcontainers pros testes de integração, badge no README)
- ⬜ M5 — Tempo real (WebSocket + Redis)
- ⬜ M6 — Nuvem (Terraform + AWS)
- ⬜ M7 — IA (Bedrock)
- ⬜ M8 — CI/CD completo
- ⬜ M9 — Hotéis + microsserviços + Kubernetes

Checklist item a item (o que exatamente foi feito em cada marco, e o que falta): [CHECKLIST.md](CHECKLIST.md).

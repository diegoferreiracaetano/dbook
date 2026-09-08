# DBook

Backend de reservas em Kotlin + Spring Boot, começando por passagens aéreas e com hotéis planejados como segunda especialização do mesmo domínio (M9). Projeto pessoal de estudo de backend Kotlin, construído em marcos incrementais.

## Stack

- Kotlin 2.0.20 + Spring Boot 3.3.4 (JDK 21 LTS)
- PostgreSQL 16 + Flyway (migrations versionadas)
- Gradle Kotlin DSL (wrapper incluso)
- Docker Compose (Postgres local)

Planejado para os próximos marcos: Redis (WebSocket Pub/Sub), Spring Security + JWT, AWS Bedrock (IA), Terraform + AWS ECS Fargate, GitHub Actions.

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

## Rodando localmente

Pré-requisito: Docker Desktop instalado e aberto.

```bash
docker compose up -d
JAVA_HOME="/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home" ./gradlew bootRun
```

> O `JAVA_HOME` explícito é necessário porque o Gradle 8.8 (versão do wrapper) ainda não roda em JDK mais recentes que o 21/22 — ver decisão registrada no histórico do projeto.

A aplicação sobe em `http://localhost:8080`. O Flyway aplica as migrations automaticamente (schema + seed de 3 aeroportos: `GRU`, `GIG`, `JFK`).

## Endpoints (M1)

### `GET /health`
Confirma que a aplicação está no ar.

### `POST /admin/flights`
Cadastra um voo, resolvendo origem/destino por código IATA.

```bash
curl -X POST localhost:8080/admin/flights \
  -H "Content-Type: application/json" \
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

Retorna `201` com o voo criado, ou `404` se o código IATA de origem/destino não existir.

### `GET /flights/search?origin=&destination=&date=`
Busca voos por rota e data.

```bash
curl "localhost:8080/flights/search?origin=GRU&destination=GIG&date=2026-10-01"
```

## Testes

```bash
JAVA_HOME="/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home" ./gradlew test
```

- **Domínio** (`FlightTest`): invariantes de `Bookable` (preço não-negativo, capacidade disponível entre 0 e o total).
- **Aplicação** (`RegisterFlightUseCaseTest`): regras do caso de uso com repositórios fake — resolução de aeroporto e erro quando o código IATA não existe.
- **Apresentação** (`FlightAdminControllerTest`, `FlightSearchControllerTest`): contrato HTTP (status code, shape do JSON, mapeamento de exceção) com o caso de uso mockado via `@WebMvcTest`.

## Status do roadmap

- ✅ **M1 — Domínio e persistência** (voos, herança JPA `JOINED`, endpoints de cadastro/busca, testes)
- ⬜ M2 — Reserva com concorrência real (lock otimista)
- ⬜ M3 — Segurança (JWT)
- ⬜ M4 — CI
- ⬜ M5 — Tempo real (WebSocket + Redis)
- ⬜ M6 — Nuvem (Terraform + AWS)
- ⬜ M7 — IA (Bedrock)
- ⬜ M8 — CI/CD completo
- ⬜ M9 — Hotéis + microsserviços + Kubernetes

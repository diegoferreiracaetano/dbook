# DBook

[![CI](https://github.com/diegoferreiracaetano/dbook/actions/workflows/ci.yml/badge.svg)](https://github.com/diegoferreiracaetano/dbook/actions/workflows/ci.yml)

Backend de reservas em Kotlin + Spring Boot, começando por passagens aéreas — o domínio (`Bookable`) já foi desenhado pra suportar hotéis como segunda especialização no futuro, se fizer sentido. Projeto pessoal de estudo de backend Kotlin, construído em marcos incrementais.

## Stack

- Kotlin 2.0.21 + Spring Boot 3.3.4 (JDK 21 LTS)
- PostgreSQL 16 + Flyway (migrations versionadas)
- Redis (Pub/Sub pra disponibilidade em tempo real)
- AWS SQS (expiração de reservas pendentes; LocalStack localmente)
- Spring Security + JWT ([jjwt](https://github.com/jwtk/jjwt)) com refresh rotativo
- WebSocket/STOMP (disponibilidade em tempo real)
- Gradle Kotlin DSL (wrapper incluso)
- Docker Compose (Postgres/Redis/LocalStack local)
- ktlint + detekt (`./gradlew check`)
- Terraform (VPC, ECR, RDS, ElastiCache, Secrets Manager, ECS Fargate) + GitHub Actions
- AWS Bedrock (sugestões de voo por IA) + Bucket4j (rate limiting)
- Observabilidade: Actuator + Micrometer (Prometheus), logs JSON (Logback), OpenTelemetry (Jaeger), Grafana + Loki — ver [docs/observabilidade.md](docs/observabilidade.md)

M1-M8 e M10 completos. Ideias registradas pra depois: script de seed de dados, integração com API real de voos — ver "Ideias futuras" no [CHECKLIST.md](CHECKLIST.md).

## Arquitetura

Clean Architecture, em camadas por pacote, cada camada dividida em **subpacotes por conceito**:

```
com.dbook
├── domain            # Entidades e regras de negócio puras. Zero dependência de Spring/JPA.
│   ├── catalog/      #   Flight, Airport, Airline, Bookable  (onde entraria Hotel)
│   ├── seating/      #   Seat, SeatLayout
│   ├── booking/      #   Booking, expiração, disponibilidade em tempo real
│   ├── payment/      #   Payment, idempotência
│   ├── review/       #   Review
│   ├── identity/     #   User, token, refresh, hash de senha
│   └── ai/           #   sugestões de voo
├── application       # Casos de uso — orquestram domínio + portas. Mesmos conceitos
│                      # (+ common/ com o helper de afterCommit).
├── infrastructure
│   ├── persistence/  # Entidades JPA, repositórios Spring Data, adapters e mappers
│   │                  # domínio <-> JPA — por conceito.
│   ├── messaging/    # availability/ (Redis + STOMP) e expiration/ (SQS)
│   └── security/ ai/ web/
└── presentation      # Controllers REST, DTOs de request/response — por conceito
                       # (+ common/ com o exception handler).
```

A camada continua sendo o primeiro nível (a regra de dependência é por camada); o conceito é o segundo. Dentro do `domain/` só existem duas dependências entre conceitos (`booking → catalog` e `ai → catalog`), sem ciclos.

`Bookable` é a abstração central do domínio: `Flight` (e futuramente `Accommodation`, para hotéis) especializa `Bookable`. A persistência usa herança JPA `JOINED` (tabela própria por especialização) para evitar colunas nulas quando o segundo tipo reservável for adicionado.

## Rodando localmente

Pré-requisito: Docker Desktop instalado e aberto.

```bash
docker compose up -d
JAVA_HOME="/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home" ./gradlew bootRun
```

> O `JAVA_HOME` explícito é necessário porque o Gradle 8.8 (versão do wrapper) ainda não roda em JDK mais recentes que o 21/22 — ver decisão registrada no histórico do projeto.

A aplicação sobe em `http://localhost:8080`. O Flyway aplica as migrations automaticamente (schema + seed de 9 aeroportos, cada um com foto real: `GRU`, `GIG`, `JFK`, `LHR`, `CDG`, `LIS`, `MIA`, `EZE`, `NRT` — e 6 companhias aéreas: `LA`, `AD`, `G3`, `AA`, `DL`, `UA`, mesmo padrão de dado de referência).

Pra popular o banco com voos de teste (útil pra demo e pra dar contexto real à sugestão por IA do M7):

```bash
./scripts/seed-flights.sh 1000   # cria 1000 voos; padrão é 1000 se omitido
```

Cria um usuário admin (`seed-admin@example.com`), promove via SQL direto (só funciona local — não existe endpoint de auto-promoção, por decisão de segurança) e cadastra voos com companhia/rotas/preços/datas variados entre as 6 companhias e os 9 aeroportos seedados via `POST /v1/admin/flights` — os mesmos endpoints já cobertos pelos testes, não é INSERT direto no banco.

## Documentação da API (Swagger)

Com a aplicação no ar:

- Swagger UI: http://localhost:8080/swagger-ui/index.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs

## Documentação detalhada

O README é a porta de entrada. Cada assunto tem seu documento em [`docs/`](docs/):

| Documento | O que tem |
|---|---|
| [Endpoints](docs/endpoints.md) | todos os endpoints, com `curl` de exemplo e os códigos de status |
| [Versionamento da API](docs/versionamento.md) | por que e como a API é versionada (`/v1`), o que é mudança que quebra, como lançar uma `v2` e aposentar a antiga |
| [Autenticação](docs/autenticacao.md) | registro, login, refresh de token rotativo, papéis e permissões, conta bloqueada, limite de tentativas e a sessão do portal admin |
| [Migrações do banco](docs/migracoes.md) | as regras do Flyway e a técnica *expand / contract* para adicionar uma coluna obrigatória |
| [Auditoria](docs/auditoria.md) | o registro imutável das ações administrativas: o que é gravado, como consultar (`GET /v1/admin/audit`) e como se liga ao log e ao trace |
| [Observabilidade](docs/observabilidade.md) | **o conceito, o que foi usado, como foi implementado, onde acessar** e roteiros para investigar problemas (métricas, logs, tracing) |
| [Expiração de reservas (SQS)](docs/expiracao-de-reservas.md) | como uma reserva pendente é cancelada sozinha, com diagrama |
| [Tempo real](docs/tempo-real.md) | disponibilidade de assentos por WebSocket + Redis |
| [IA](docs/ia.md) | sugestões de voo com AWS Bedrock |
| [Nuvem e CI/CD](docs/nuvem-e-cicd.md) | Terraform + AWS e os pipelines |
| [Testes e qualidade](docs/testes-e-qualidade.md) | estratégia de testes e verificações automáticas |
| [CHECKLIST](CHECKLIST.md) | o que exatamente foi feito em cada marco, e o que falta |

## Status do roadmap

- ✅ **M1 — Domínio e persistência** (voos, herança JPA `JOINED`, endpoints de cadastro/busca, testes)
- ✅ **M2 — Reserva com concorrência real** (lock otimista, endpoint de reserva/cancelamento, teste de concorrência real, Swagger)
- ✅ **M3 — Segurança** (User + BCrypt, JWT com refresh rotativo, filtro de autenticação, rotas públicas/protegidas, roles + `@PreAuthorize`, testes de segurança)
- ✅ **M4 — CI** (GitHub Actions rodando `./gradlew check`, Testcontainers pros testes de integração, badge no README)
- ✅ **M5 — Tempo real** (WebSocket/STOMP, autenticação no `CONNECT`, broadcast pós-commit, fan-out entre instâncias via Redis Pub/Sub)
- ✅ **M6 — Nuvem** (Terraform: VPC/ECR/RDS/ElastiCache/Secrets Manager/ECS Fargate, LocalStack por padrão, validado contra AWS real)
- ✅ **M7 — IA** (sugestões via Bedrock, sempre auditadas, rate limit dedicado — validação real do model-id pendente de sessão AWS ativa)
- ✅ **M8 — CI/CD completo** (build/push automático via OIDC, deploy auto em dev, gate de aprovação pra prod — pipeline nunca rodou de ponta a ponta, precisa de conta AWS persistente)
- 💡 M9 — Hotéis + microsserviços + Kubernetes (rebaixado a ideia futura, não é o próximo passo — ver CHECKLIST.md)
- ✅ **M10 — Marcação de assentos** (`Seat` com lock otimista próprio, geração automática do mapa ao cadastrar o voo, `availableCapacity` derivado da contagem de assentos `AVAILABLE`, `GET /v1/bookables/{id}/seats`, `seatId` obrigatório em `POST /v1/bookings`)
- ✅ **M19 — Avaliação de reserva** (`Review`, `POST /v1/reviews` autenticado — nota 1-5 + comentário obrigatório de uma reserva `CONFIRMED`, só o dono, só uma vez; `GET /v1/bookings` devolve a review de cada reserva; addendum corrigiu um 401 falso sistêmico em qualquer corpo JSON malformado, não só no Review)
- ✅ **M20 — Expiração de reservas pendentes** (fila SQS com atraso de 15 min no LocalStack, consumidor idempotente com DLQ, lock otimista na `Booking` contra a corrida pagar × expirar — Terraform da SQS e outbox ficaram como evolução)
- ✅ **M21 — Idempotência no pagamento** (`Idempotency-Key` obrigatório em `POST /v1/payments`: a retentativa devolve o pagamento original, chave reutilizada com outro pedido é 422, corrida entre requisições iguais resolvida pelo índice único — o teste de corrida achou um 500 real causado pela tradução de exceções do Spring em `@Repository`)
- ✅ **M22 — Pacotes por conceito** (domain/application/presentation/persistence divididos em `catalog`, `seating`, `booking`, `payment`, `review`, `identity`, `ai`; regras de arquitetura verificadas por testes ArchUnit)
- ✅ **M23 — Observabilidade** (health liveness/readiness e métricas Prometheus numa porta de gestão separada; `@Observed` em todos os casos de uso; métricas de negócio e o gauge de reservas pendentes; logs em JSON com `requestId`/`userId`/`traceId`; tracing com OpenTelemetry que atravessa a fila SQS; Prometheus + Loki + Jaeger + Grafana locais com dashboard — ver [docs/observabilidade.md](docs/observabilidade.md))
- ✅ **M25 — Fundação administrativa** (papéis com permissões, conta bloqueável, login com limite de tentativas em Redis, política de senha, sessão do portal admin em cookie `httpOnly`, `code` nos erros — ver [docs/autenticacao.md](docs/autenticacao.md))
- ✅ **M26 — Auditoria** (registro imutável das ações administrativas na mesma transação, consulta por cursor em `GET /v1/admin/audit`, ligada ao log e ao trace — ver [docs/auditoria.md](docs/auditoria.md))
- ✅ **M27 — Preço congelado na reserva** (`Booking.price` gravado na criação e nunca alterado; o pagamento e "Minhas Viagens" leem dele, então reajustar o preço de um voo não muda o que já foi reservado — ver [docs/migracoes.md](docs/migracoes.md))

- ✅ **M28 — Equipe e convites** (convite por e-mail com token de 256 bits guardado só como hash, aceite com senha escolhida pelo convidado, troca de papel/bloqueio que nunca deixa o sistema sem `SUPER_ADMIN`, bootstrap do primeiro administrador por variável de ambiente, troca da própria senha — ver [docs/autenticacao.md](docs/autenticacao.md#equipe-convite-e-gestão))

Checklist item a item (o que exatamente foi feito em cada marco, e o que falta): [CHECKLIST.md](CHECKLIST.md).

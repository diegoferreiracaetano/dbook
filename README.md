# DBook

[![CI](https://github.com/diegoferreiracaetano/dbook/actions/workflows/ci.yml/badge.svg)](https://github.com/diegoferreiracaetano/dbook/actions/workflows/ci.yml)

Backend de reservas em Kotlin + Spring Boot, começando por passagens aéreas — o domínio (`Bookable`) já foi desenhado pra suportar hotéis como segunda especialização no futuro, se fizer sentido. Projeto pessoal de estudo de backend Kotlin, construído em marcos incrementais.

## Stack

- Kotlin 2.0.21 + Spring Boot 3.3.4 (JDK 21 LTS)
- PostgreSQL 16 + Flyway (migrations versionadas)
- Redis (Pub/Sub pra disponibilidade em tempo real)
- Spring Security + JWT ([jjwt](https://github.com/jwtk/jjwt)) com refresh rotativo
- WebSocket/STOMP (disponibilidade em tempo real)
- Gradle Kotlin DSL (wrapper incluso)
- Docker Compose (Postgres/Redis/LocalStack local)
- ktlint + detekt (`./gradlew check`)
- Terraform (VPC, ECR, RDS, ElastiCache, Secrets Manager, ECS Fargate) + GitHub Actions
- AWS Bedrock (sugestões de voo por IA) + Bucket4j (rate limiting)

M1-M8 completos. Ideias registradas pra depois: script de seed de dados, integração com API real de voos — ver "Ideias futuras" no [CHECKLIST.md](CHECKLIST.md).

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

Pra popular o banco com voos de teste (útil pra demo e pra dar contexto real à sugestão por IA do M7):

```bash
./scripts/seed-flights.sh 30   # cria 30 voos; padrão é 30 se omitido
```

Cria um usuário admin (`seed-admin@example.com`), promove via SQL direto (só funciona local — não existe endpoint de auto-promoção, por decisão de segurança) e cadastra voos com rotas/preços/datas variados entre os 3 aeroportos seedados via `POST /admin/flights` — os mesmos endpoints já cobertos pelos testes, não é INSERT direto no banco.

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

## Tempo real (WebSocket + Redis)

Quando uma reserva é criada ou cancelada, a disponibilidade atualizada do `Bookable` é publicada em tempo real via WebSocket/STOMP, para clientes que estejam olhando aquela rota/voo no momento.

- Endpoint STOMP: `ws://localhost:8080/ws` (sem SockJS — o cliente é um app KMP/Compose, não uma página de navegador precisando de fallback HTTP).
- Tópico por `Bookable`: `/topic/bookables/{bookableId}/availability`, payload `{"bookableId": 1, "availableCapacity": 179}`.
- Autenticação acontece no frame STOMP `CONNECT` (header `Authorization: Bearer <accessToken>`), não no handshake HTTP — um WebSocket nativo de navegador não permite setar headers HTTP arbitrários no handshake, então `/ws` é público no `SecurityConfig` e a validação real do JWT é feita pelo `StompAuthChannelInterceptor`. Um `CONNECT` sem token válido é rejeitado com um frame `ERROR` e a conexão é fechada.
- O broadcast só acontece depois que a transação commita (`afterCommit`, ver `TransactionSupport.kt`) — uma reserva que sofre rollback nunca deveria ter avisado ninguém sobre uma mudança que não aconteceu.
- Fan-out entre instâncias é feito via Redis Pub/Sub (canal `dbook:availability`): cada instância publica no Redis ao invés de empurrar direto pras próprias sessões STOMP; toda instância também assina esse canal e reencaminha pras suas sessões locais. Com 1 instância isso parece redundante, mas é exatamente o que passa a ser necessário a partir do M6 (múltiplos containers ECS Fargate) — decisão de aprender o padrão agora, achando com 1 instância, antes de precisar dele de verdade.

Pré-requisito local: Redis também sobe pelo `docker compose up -d` (serviço `redis`, porta `6379`).

## Nuvem (Terraform + AWS)

Infraestrutura como código em `terraform/`, organizada em módulos reutilizáveis (`terraform/modules/{vpc,ecr,rds,redis,secrets,ecs}`) amarrados pelo módulo raiz (`terraform/`). `terraform/bootstrap` cria o bucket S3 + tabela DynamoDB usados como backend de state remoto — aplicado uma vez, separado do resto (não dá pra guardar o state de quem cria o lugar de guardar o state).

**Decisão central: LocalStack por padrão, AWS real só sob demanda.** Toda a stack roda de graça contra um [LocalStack](https://www.localstack.cloud/) local (sobe junto no `docker compose up -d`, serviço `localstack`) — é o alvo padrão (`use_localstack = true`). Apontar pra uma conta AWS real exige a flag explícita:

```bash
cd terraform
terraform init -reconfigure -backend-config=backend-aws.hcl
terraform plan -var="use_localstack=false"
terraform apply -var="use_localstack=false"
```

> **Nota:** a LocalStack **community** (gratuita) só emula de verdade S3, DynamoDB, EC2 (VPC/security groups), IAM e Secrets Manager. ECR, ECS, RDS, CloudWatch Logs e ElastiCache são recursos **Pro-only** — contra a community, esses módulos só validam via `terraform plan` (sintaxe e grafo de dependências), não `apply`. A validação de comportamento real desses módulos foi feita uma vez contra um AWS Academy Learner Lab (ver `CHECKLIST.md` pra detalhes e descobertas do processo).

**O que a infraestrutura provisiona:**
- VPC com 2 subnets públicas + 2 privadas (2 AZs), 1 NAT Gateway
- ECR (repositório de imagem, tags imutáveis)
- RDS Postgres + ElastiCache Redis, ambos em subnet privada, só alcançáveis pelo security group da aplicação
- Secrets Manager (senha do banco + segredo JWT — nunca hardcoded, gerados via `random_password`)
- ECS Fargate (cluster + task definition + service), sem Application Load Balancer neste marco (task recebe IP público direto)

```bash
docker build -t dbook:latest .
# depois de `terraform apply` criar o repositório ECR:
aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin <account-id>.dkr.ecr.us-east-1.amazonaws.com
docker tag dbook:latest <account-id>.dkr.ecr.us-east-1.amazonaws.com/dbook:latest
docker push <account-id>.dkr.ecr.us-east-1.amazonaws.com/dbook:latest
```

**Não esquecer:** `terraform destroy -var="use_localstack=false"` ao terminar de usar a AWS real — NAT Gateway e RDS cobram por hora rodando, mesmo em contas de estudo.

## IA (sugestões de voo via AWS Bedrock)

`POST /ai/suggestions` (autenticado) recebe um pedido em linguagem natural e devolve voos sugeridos, sempre a partir dos voos realmente ativos no banco — a IA **nunca** cria, altera ou confirma uma reserva sozinha, só sugere.

```bash
curl -X POST localhost:8080/ai/suggestions \
  -H "Content-Type: application/json" -H "Authorization: Bearer <accessToken>" \
  -d '{"query": "voos baratos pra o Rio mês que vem"}'
```

- O prompt inclui até 50 voos ativos (`FlightRepository.findActive()`, mais próximos primeiro) — o modelo é instruído a nunca inventar um `flightId` fora dessa lista.
- Cada chamada é auditada em `ai_suggestion_log` (sucesso **ou** falha — decisão fechada do M7 é "sempre auditada").
- Rate limit de 5 requisições/minuto por usuário (`AiRateLimitInterceptor`, Bucket4j), só em `/ai/**` — protege contra custo descontrolado de chamadas a um modelo pago, não é rate limit geral da API.
- `ai.bedrock.model-id` em `application.yml` **precisa ser verificado** antes de usar contra uma conta real — modelos do Bedrock são descontinuados com o tempo; confirme o catálogo atual com `aws bedrock list-foundation-models` ou o console AWS.

## CI/CD

`.github/workflows/ci.yml` (teste+lint+detekt, toda PR/push) e `.github/workflows/cd.yml` (build+push+deploy, só depois que o CI passa) são pipelines separados — o CD só começa via `workflow_run` quando o CI termina com sucesso no `main`.

Fluxo do CD:
1. **build-and-push**: builda a imagem Docker, autentica no ECR via OIDC (sem chave de longa duração guardada como secret) e publica com a tag sendo o SHA do commit — necessário porque o repositório ECR é `IMMUTABLE` (M6/6.3), não dá pra reusar uma tag como `latest` em pushes repetidos.
2. **deploy-dev**: automático após o build, roda `terraform apply -var="image_tag=<sha>"` — reaproveita a variável `image_tag` que a Task Definition do ECS já usa desde o M6.
3. **deploy-prod**: mesma infraestrutura que o dev neste projeto (um setup de produção de verdade teria state/ambiente separado) — o que importa aqui é o **gate**: só roda depois que um revisor aprova, via as regras de proteção do GitHub Environment `production`.

**Configuração manual única, necessária antes do pipeline funcionar de verdade** (nada disso é automatizável nem foi feito por mim — precisa de uma conta AWS persistente, diferente de um AWS Academy Lab, cuja sessão expira em horas):
1. `terraform apply` do módulo `terraform/modules/github_oidc` contra a conta AWS real (já incluso no `terraform apply` normal do módulo raiz).
2. Pegar o output `github_oidc.role_arn` e criar o secret `AWS_DEPLOY_ROLE_ARN` no repositório (Settings → Secrets and variables → Actions).
3. Criar os GitHub Environments `dev` e `production` (Settings → Environments) — `production` precisa de "Required reviewers" configurado pra virar um gate de aprovação de verdade.

**Nota honesta:** o pipeline nunca rodou de ponta a ponta nesta sessão — a única conta AWS disponível foi um Academy Lab, incompatível com credenciais persistentes de CI. O YAML e o Terraform estão corretos e prontos, mas a validação de um `workflow_run` verde fica pendente de uma conta AWS real.

## Qualidade de código

```bash
JAVA_HOME="/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home" ./gradlew check
```

Roda testes + [ktlint](https://github.com/pinterest/ktlint) (estilo/formatação) + [detekt](https://detekt.dev/) (análise estática) juntos. `./gradlew ktlintFormat` corrige formatação automaticamente. Configuração em `.editorconfig` (4 espaços, `max_line_length=120`, precisa bater entre os dois) e `config/detekt/detekt.yml`.

## Testes

```bash
JAVA_HOME="/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home" ./gradlew test
```

**Convenção (2026-09-09): um cenário por classe, nomeado `given/when/then`.** Cada `@Test` fica sozinho numa classe cujo nome descreve o cenário (ex.: `AdminEndpointRejectsAClientTokenTest`), e o próprio nome do método (`` `given a CLIENT token when posting to admin flights then it returns 403` ``) documenta o cenário — sem comentário explicando o óbvio dentro do corpo do teste. Setup compartilhado entre cenários do mesmo caso de uso/controller vira uma classe fixture abstrata (ex.: `SecurityIntegrationFixture`) que cada cenário estende.

- **Domínio** (`domain/booking`, `domain/flight`, `domain/user`): invariantes de `Bookable`/`User` e a máquina de estados de `Booking` (PENDING → CONFIRMED/CANCELLED).
- **Aplicação** (`application/loginusecase`, `application/registeruserusecase`, `application/registerflightusecase`, `application/suggestflightsusecase`): regras dos casos de uso com repositórios/hasher/token service fake.
- **Apresentação** (`presentation/flightadmincontroller`, `presentation/flightsearchcontroller`): contrato HTTP (status code, shape do JSON, mapeamento de exceção) com o caso de uso mockado via `@WebMvcTest` — segurança desligada nesses slices de propósito (ver `securityintegration`).
- **Concorrência** (`BookingConcurrencyTest`): duas threads disputando o último assento contra o Postgres real.
- **Segurança** (`presentation/securityintegration`): `@SpringBootTest` completo — rota admin sem token (401) e com role errada (403), reserva exige autenticação, dono vs. não-dono de reserva vs. ADMIN, rotação de refresh token (reuso rejeitado), busca pública sem token.
- **Tempo real** (`infrastructure/messaging/availabilitybroadcast`): cliente STOMP real (não mock) conecta autenticado, assina o tópico de disponibilidade, dispara uma reserva e recebe o evento publicado via Redis Pub/Sub; e um `CONNECT` sem token válido é rejeitado.
- **IA** (`application/suggestflightsusecase`, `infrastructure/ai/bedrockaisuggestionservice`, `infrastructure/web/airatelimitinterceptor`): caso de uso com fakes (inclui o log sendo salvo tanto no sucesso quanto na falha), construção do prompt/parse da resposta do Bedrock isolados de qualquer chamada de rede, e o rate limiter (5/min, escopo por usuário) exercitado diretamente — nada disso depende de credencial AWS real pra rodar.

Esses últimos usam [Testcontainers](https://testcontainers.com/) (`AbstractIntegrationTest`) — sobem Postgres e Redis descartáveis sozinhos, não precisam mais de `docker compose up -d` manual. Só exigem Docker instalado e rodando.

**Cobertura (JaCoCo, 2026-09-09):** `./gradlew check` roda `jacocoTestCoverageVerification` com um mínimo de 80% de cobertura de linha, excluindo entidades JPA/DTOs (dados puros, sem lógica própria) e o `main()`. Relatório em `build/reports/jacoco/test/html/index.html` após `./gradlew jacocoTestReport`. **Nota:** localmente a cobertura medida fica bem abaixo de 80% por causa do mesmo bloqueio Testcontainers/Docker Desktop documentado acima — os pacotes que só têm teste de integração (`persistence`, `security`, `messaging`) aparecem como 0% cobertos porque esses testes simplesmente não rodam aqui, não porque não existem. O número real só é confiável rodando no CI.

> **Nota:** em algumas instalações do Docker Desktop muito recentes, o Testcontainers pode falhar ao detectar o daemon (`Could not find a valid Docker environment`) por incompatibilidade do cliente HTTP interno com a API do Docker. Se isso acontecer localmente, o pipeline de CI (GitHub Actions, Docker padrão do runner) continua funcionando normalmente — é uma limitação do ambiente local, não do código.

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

Checklist item a item (o que exatamente foi feito em cada marco, e o que falta): [CHECKLIST.md](CHECKLIST.md).

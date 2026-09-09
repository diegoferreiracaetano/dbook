# Checklist do projeto

Cada marco tem uma decisão de arquitetura que o justifica. Itens marcados `[x]` estão implementados, testados localmente e commitados.

## M1 — Domínio e persistência ✅

Decisão: `Bookable` como abstração central desde o início, pra `Flight` e `Accommodation` (hotéis) herdarem dela depois sem retrabalho.

- [x] 1.1 Setup do projeto (Spring Initializr, Kotlin, Gradle)
- [x] 1.2 `HealthController` rodando local + Git inicial
- [x] 1.3 Modelar `Bookable` (abstrato) — campos comuns entre voo e hotel
- [x] 1.4 Modelar `Flight`, `Airport`, `SeatClass` como especialização de `Bookable`
- [x] 1.5 Decidir/aplicar estratégia de herança JPA (`JOINED`)
- [x] 1.6 Postgres via Docker Compose + Flyway (primeira migration)
- [x] 1.7 Entidades JPA + repositórios
- [x] 1.8 Endpoint admin: cadastrar voo
- [x] 1.9 Endpoint público: buscar voos (origem/destino/data)
- [x] 1.10 Testes unitários das regras de domínio

## M2 — Reserva com concorrência real ✅

Decisão: lock otimista, porque conflito é raro frente a leituras — lock pessimista penalizaria performance à toa.

- [x] 2.1 Modelar `Booking` vinculado a `Bookable` (não a `Flight` direto)
- [x] 2.2 Campo `@Version` pra lock otimista
- [x] 2.3 Endpoint de reserva (`POST /bookings`)
- [x] 2.4 Teste de concorrência: duas reservas simultâneas no mesmo assento
- [x] 2.5 Tratamento de conflito (`409` no `OptimisticLockException`)
- [x] 2.6 Endpoint de cancelamento liberando o assento
- [x] 2.7 Máquina de estados: `PENDENTE → CONFIRMADA/CANCELADA`

## M3 — Segurança ✅

Decisão: entra só agora, porque proteger endpoints inexistentes é teatro. JWT com refresh rotativo, não sessão.

- [x] 3.1 `User` + senha hasheada (BCrypt)
- [x] 3.2 Endpoint de registro
- [x] 3.3 Endpoint de login (access + refresh token)
- [x] 3.4 Filtro JWT validando requisições
- [x] 3.5 `SecurityFilterChain`: rotas públicas vs. protegidas
- [x] 3.6 Roles + `@PreAuthorize` nos endpoints certos
- [x] 3.7 Refresh com rotação
- [x] 3.8 Testes de segurança (token ausente, role errada)

## M4 — CI ✅

Decisão: entra assim que existe algo testável de verdade — não deixa pro final. ktlint/detekt já existem no build desde o M2/M3; faltava só conectar no pipeline.

- [x] 4.1 Workflow GitHub Actions (build)
- [x] 4.2 Rodar testes no pipeline (incluindo Testcontainers)
- [x] 4.3 `./gradlew check` (ktlint + detekt) no pipeline
- [x] 4.4 Badge de status no README

## M5 — Tempo real ⬜

Decisão: WebSocket + Redis Pub/Sub mesmo com 1 instância — é o jeito barato de aprender o padrão antes de precisar dele em produção.

- [ ] 5.1 Config STOMP (`/ws`)
- [ ] 5.2 Canal de disponibilidade de assento
- [ ] 5.3 Publicar evento ao confirmar/cancelar
- [ ] 5.4 Cliente de teste escutando o canal
- [ ] 5.5 Redis local via Docker Compose
- [ ] 5.6 Trocar broadcast local por Redis Pub/Sub
- [ ] 5.7 Autenticar handshake STOMP com JWT

## M6 — Nuvem (Terraform + AWS) ⬜

Decisão: ECS Fargate, não EKS ainda — Kubernetes só compensa com múltiplos serviços de verdade (isso é M9).

- [ ] 6.1 AWS CLI + bucket S3/DynamoDB pro state remoto
- [ ] 6.2 Módulo Terraform: VPC
- [ ] 6.3 Módulo Terraform: repositório ECR (isolado, provisiona antes do resto)
- [ ] 6.4 Dockerfile multi-stage da aplicação (build + runtime enxuto)
- [ ] 6.5 Build local da imagem + push manual pro ECR
- [ ] 6.6 Módulo Terraform: RDS
- [ ] 6.7 Módulo Terraform: Secrets Manager
- [ ] 6.8 Módulo Terraform: ECS Fargate (com `container_image` real)
- [ ] 6.9 `terraform plan` revisado + `apply`
- [ ] 6.10 Validar deploy no console AWS

## M7 — IA ⬜

Decisão: fora do caminho síncrono da reserva — sugere, nunca decide sozinha, sempre auditada.

- [ ] 7.1 Acesso ao Bedrock configurado
- [ ] 7.2 Endpoint de sugestão por linguagem natural
- [ ] 7.3 Prompt com contexto real (voos do banco)
- [ ] 7.4 Parse da resposta estruturada (JSON)
- [ ] 7.5 `AiSuggestionLog` (auditoria)
- [ ] 7.6 Rate limiting no endpoint de IA (Bucket4j)

## M8 — CI/CD completo ⬜

Decisão: só automatiza deploy depois que a infra já rodou manualmente validada (M6).

- [ ] 8.1 Build da imagem Docker no pipeline
- [ ] 8.2 Push automático para o ECR
- [ ] 8.3 Deploy automático em dev após merge
- [ ] 8.4 Gate manual de aprovação para produção

## M9 — Evolução: hotéis + microsserviços + Kubernetes ⬜

Decisão: aqui o `Bookable` paga a dívida de design do M1 — `Accommodation` entra sem tocar em `Booking`.

- [ ] 9.1 `Accommodation` implementando `Bookable`
- [ ] 9.2 Adaptar busca para suportar os dois tipos
- [ ] 9.3 Separar serviços (`auth`, `flight`, `ai`, `realtime`)
- [ ] 9.4 Terraform de cluster EKS simples
- [ ] 9.5 Migrar `ai-service` para o EKS + documentar comparação com Fargate

## Checklist de fechamento de módulo

Rodado ao final de todo marco, nesta ordem, antes de considerar o marco fechado:

1. Revisar o checklist do marco — conferir que todo item foi implementado e testado.
2. Revisão de Clean Code (nomes, funções pequenas, duplicação, comentários só onde o "porquê" não é óbvio).
3. Revisão de SOLID (responsabilidade única, inversão de dependência — portas em `domain`, implementação em `infrastructure`).
4. `./gradlew check` (ktlint + detekt) sem violações.
5. Testes das camadas ainda não cobertas por teste dedicado.
6. Documentação (`README.md`) atualizada.
7. Swagger em dia — `@Tag`/`@Operation`/`@Schema`/exemplos reais em todos os controllers, incluindo botão Authorize se houver rota autenticada nova.

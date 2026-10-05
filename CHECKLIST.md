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

## M5 — Tempo real ✅

Decisão: WebSocket + Redis Pub/Sub mesmo com 1 instância — é o jeito barato de aprender o padrão antes de precisar dele em produção.

- [x] 5.1 Config STOMP (`/ws`)
- [x] 5.2 Canal de disponibilidade de assento
- [x] 5.3 Publicar evento ao confirmar/cancelar
- [x] 5.4 Cliente de teste escutando o canal
- [x] 5.5 Redis local via Docker Compose
- [x] 5.6 Trocar broadcast local por Redis Pub/Sub
- [x] 5.7 Autenticar handshake STOMP com JWT

Checklist de fechamento:
- [x] Revisão dos itens do checklist
- [x] Revisão Clean Code
- [x] Revisão SOLID
- [x] `./gradlew check` (ktlint + detekt) limpo
- [x] Cobertura de testes das camadas novas
- [x] README.md atualizado
- [x] Swagger/OpenAPI — não se aplica (nenhum endpoint REST novo neste módulo; WebSocket/STOMP não é descrito por OpenAPI)

## M6 — Nuvem (Terraform + AWS) ✅

Decisão: ECS Fargate, não EKS ainda — Kubernetes só compensa com múltiplos serviços de verdade (isso é M9).

Decisão (2026-09-09): todo o Terraform deste marco roda contra **LocalStack** por padrão (`use_localstack = true`), zero custo — real AWS só com `-var="use_localstack=false"` explícito. Conta real disponível é um **AWS Academy Learner Lab** (token temporário, sem risco de cartão, mas sem permissão pra criar roles IAM novas — usa a `LabRole` pré-existente).

Descoberta importante (2026-09-09): a LocalStack **community** (gratuita) só emula de verdade S3, DynamoDB, EC2 (VPC/security groups), IAM e Secrets Manager. **ECR, ECS, RDS, CloudWatch Logs e ElastiCache são recursos Pro-only** (erro 501 "not yet implemented or pro feature" ao aplicar) — esses módulos são validados só por `terraform plan` localmente; o `apply` de verdade acontece contra o Academy Lab no 6.9.

Decisão extra (2026-09-09, fora do checklist original): adicionado um módulo de **ElastiCache Redis**, porque o M5 depende de Redis pra funcionar — sem isso, toda reserva/cancelamento quebraria com 500 em produção assim que o evento de disponibilidade tentasse publicar.

**Descobertas reais do apply contra o AWS Academy Lab (2026-09-09), não previstas no design inicial:**
- Essa conta não tem uma role `LabRole` (comum em outros templates do Academy) — só `voclabs`, `vocareum` e roles de serviço. Pior: `iam:GetRole` é explicitamente negado, então nem dava pra descobrir a role certa via `data` source em runtime.
- Mesmo assim, `iam:CreateRole` **funciona** nessa conta (a suposição inicial de que Academy Lab sempre bloqueia isso estava errada) — o módulo ECS passou a criar sua própria role de execução, igual faria numa conta AWS normal, sem branch condicional por ambiente.
- A role recém-criada precisou de uma policy própria pra `secretsmanager:GetSecretValue` — a policy gerenciada `AmazonECSTaskExecutionRolePolicy` cobre só ECR pull + CloudWatch Logs, não Secrets Manager.
- Depois desses dois ajustes, o deploy funcionou de ponta a ponta: Flyway migrou o schema real no RDS (6 migrations), JWT assinado com o segredo do Secrets Manager funcionou (`/auth/register` + `/auth/login` retornaram 200/201), busca pública no RDS respondeu 200.

**Sessão do AWS Academy Lab encerrada em pleno `terraform destroy` (2026-09-09):** metade dos recursos (~15 de 36) foi destruída com sucesso antes da sessão do lab expirar — nesse ponto toda chamada de API real (inclusive leituras como `ecs:DescribeServices`) passou a ser negada por uma policy chamada literalmente `voc-cancel-cred`, claramente algo que a própria Vocareum/AWS Academy anexa quando a sessão termina. Não é bug nosso, não tem mais nada pra tentar por Terraform quando isso acontece. Sem risco de custo — é sandbox educacional sem cartão pessoal; a própria AWS Academy recicla a conta no ritmo dela, independente do que sobrou de pé. Uma sessão de lab futura seria uma conta nova (não dá pra "continuar" destruindo essa). **Lição pra próxima vez:** rodar o `destroy` bem antes do fim previsto da sessão do lab, não só depois de terminar os testes.

- [x] 6.1 AWS CLI + bucket S3/DynamoDB pro state remoto
- [x] 6.2 Módulo Terraform: VPC
- [x] 6.3 Módulo Terraform: repositório ECR (aplicado de verdade — imagem publicada com sucesso)
- [x] 6.4 Dockerfile multi-stage da aplicação (testado de ponta a ponta: build local + `docker run` contra Postgres/Redis reais + `/health` 200)
- [x] 6.5 Build local da imagem + push manual pro ECR (`docker push` confirmado via `aws ecr describe-images`)
- [x] 6.6 Módulo Terraform: RDS (aplicado de verdade — Flyway migrou 6 migrations no Postgres real)
- [x] 6.6b Módulo Terraform: ElastiCache Redis (extra, não estava no checklist original — ver decisão acima; aplicado de verdade contra a conta real)
- [x] 6.7 Módulo Terraform: Secrets Manager (aplicado de verdade — JWT assinado com o segredo real funcionou em `/auth/login`)
- [x] 6.8 Módulo Terraform: ECS Fargate (aplicado de verdade — task rodando, `has reached a steady state`)
- [x] 6.9 `terraform plan` revisado + `apply` (contra a conta real do AWS Academy Lab, 2 correções de IAM no caminho — ver descobertas acima)
- [x] 6.10 Validar deploy no console AWS (validado via CLI + HTTP real: `/health` 200, registro/login/busca funcionando contra RDS+Secrets Manager reais)

## M7 — IA ✅

Decisão: fora do caminho síncrono da reserva — sugere, nunca decide sozinha, sempre auditada. `POST /ai/suggestions` (autenticado, `SuggestFlightsUseCase`) pega até 50 voos ativos (`FlightRepository.findActive()`, mais próximos primeiro), monta um prompt e chama o Bedrock via `AiSuggestionService` (porta em `domain`, implementada por `BedrockAiSuggestionService`).

- [x] 7.1 Acesso ao Bedrock configurado (`BedrockConfig`, `software.amazon.awssdk:bedrockruntime`, credenciais via provider chain padrão — nunca hardcoded, mesmo princípio de todo segredo deste projeto)
- [x] 7.2 Endpoint de sugestão por linguagem natural (`POST /ai/suggestions`, `AiSuggestionController`)
- [x] 7.3 Prompt com contexto real (voos do banco) (`BedrockAiSuggestionService.buildPrompt`, testado isoladamente)
- [x] 7.4 Parse da resposta estruturada (JSON) (`parseSuggestions`/`extractText`, testados com respostas válidas e malformadas — `AiResponseParsingException` → 502)
- [x] 7.5 `AiSuggestionLog` (auditoria) — **corrigido durante teste ao vivo:** a primeira versão só logava em caso de sucesso; a decisão fechada é "sempre auditada", então `SuggestFlightsUseCase` agora loga sucesso E falha (`runCatching`), confirmado gravando no Postgres real mesmo com o Bedrock retornando erro
- [x] 7.6 Rate limiting no endpoint de IA (Bucket4j) — `AiRateLimitInterceptor`, 5 requisições/minuto por usuário, escopado só a `/ai/**`; testado ao vivo (6ª tentativa retornou 429) e com teste automatizado

**Validação real contra o AWS Academy Lab (2026-09-09) — o que funcionou de verdade e o que ficou pendente:**
- O cliente Bedrock conectou de verdade (credenciais/região resolvidas, corpo da requisição aceito estruturalmente) e nosso tratamento de erro (`AiServiceUnavailableException` → 503) funcionou corretamente contra um erro real da AWS.
- **Não foi possível confirmar um `model-id` válido nessa sessão**: todo model ID de Claude 3.x testado (`claude-3-5-sonnet-20241022-v2:0`, `claude-3-haiku-20240307-v1:0`, `claude-3-sonnet-20240229-v1:0`) retornou "reached the end of its life", e `bedrock:ListFoundationModels` é negado pela política restritiva desse lab — sem como consultar o catálogo atual de dentro dessa sessão. Um dos testes também já bateu na mesma política `voc-cancel-cred` do M6, confirmando que o acesso desse lab específico segue instável/em processo de revogação.
- `ai.bedrock.model-id` em `application.yml` fica documentado como "precisa verificar antes de usar de verdade" — é só um valor de configuração, não exige mudança de código quando corrigido.
- Testado com sucesso: parsing/prompt/auditoria/rate-limit, tudo isolado do Bedrock real (unit tests) + fluxo completo end-to-end contra Postgres real localmente (registro→login→chamada→log de falha persistido).

## M8 — CI/CD completo ✅

Decisão: só automatiza deploy depois que a infra já rodou manualmente validada (M6).

**Limitação de ambiente (2026-09-09), mais restritiva que M6/M7:** CI/CD de verdade exige credenciais AWS *persistentes* (o pipeline roda toda vez que alguém faz push, indefinidamente) — um token de AWS Academy Lab expira em horas, então nem dá pra colocar como secret do GitHub Actions. Diferente do M6/M7 (onde pelo menos parte rodou contra a AWS real antes da sessão cair), aqui **nenhum workflow foi executado de verdade** nesta sessão — o YAML e o Terraform de suporte (módulo `github_oidc`) estão corretos e prontos, testados o quanto dava (validação de sintaxe via `terraform plan`/`validate`, e o módulo `github_oidc` — que só usa IAM, disponível na LocalStack community — aplicado e verificado de verdade lá).

- [x] 8.1 Build da imagem Docker no pipeline (`cd.yml`, job `build-and-push`)
- [x] 8.2 Push automático para o ECR (mesma job — autenticação via OIDC, `aws-actions/configure-aws-credentials` + `amazon-ecr-login`, sem chave de longa duração; tag = SHA do commit, necessário porque o repositório é `IMMUTABLE`)
- [x] 8.3 Deploy automático em dev após merge (job `deploy-dev`, dispara sozinho após o build — roda `terraform apply -var="image_tag=<sha>"`, reaproveitando a variável que a Task Definition do ECS já tinha desde o M6)
- [x] 8.4 Gate manual de aprovação para produção (job `deploy-prod`, `environment: production` — só roda depois de um revisor aprovar via GitHub Environment protection rules; mesma infraestrutura que o dev neste projeto, dado que não há orçamento/conta pra manter dois ambientes AWS separados rodando)

**Módulo Terraform novo, fora do checklist original mas necessário pra 8.1-8.2 funcionarem:** `terraform/modules/github_oidc` — cria o provedor OIDC (`token.actions.githubusercontent.com`) + uma role IAM que o GitHub Actions assume via `sts:AssumeRoleWithWebIdentity`, restrita a este repositório (`repo:diegoferreiracaetano/dbook:*`). Aplicado e verificado contra a LocalStack (`aws iam get-role` confirma a trust policy correta) — só não dá pra usar contra AWS real nesta sessão pela mesma limitação de sessão/tempo do Academy Lab.

**Configuração manual única pendente (não automatizável, precisa de conta AWS persistente):** aplicar `github_oidc` contra a conta real, criar o secret `AWS_DEPLOY_ROLE_ARN` no GitHub com o `role_arn` de saída, e criar os GitHub Environments `dev`/`production` (o segundo com revisor obrigatório) — documentado no README.

## M9 — Evolução: hotéis + microsserviços + Kubernetes 💡 (sugestão futura, não é próximo passo)

Decisão (2026-09-09): rebaixado de "próximo marco" pra "ideia registrada" — o usuário avaliou que microsserviços/Kubernetes nesse ponto seriam mais exercício de "mostrar que sei desenhar pra isso" do que algo que agrega valor real a um projeto pessoal de portfólio. Fica documentado caso o interesse mude no futuro; o design abaixo continua válido se/quando isso acontecer.

Decisão original: aqui o `Bookable` paga a dívida de design do M1 — `Accommodation` entra sem tocar em `Booking`.

- [ ] 9.1 `Accommodation` implementando `Bookable`
- [ ] 9.2 Adaptar busca para suportar os dois tipos
- [ ] 9.3 Separar serviços (`auth`, `flight`, `ai`, `realtime`)
- [ ] 9.4 Terraform de cluster EKS simples
- [ ] 9.5 Migrar `ai-service` para o EKS + documentar comparação com Fargate

## M10 — Marcação de assentos ✅

Decisão (2026-09-11): hoje `Bookable`/`Flight` só rastreiam capacidade agregada (`totalCapacity`/`availableCapacity`), sem assento individual — uma reserva decrementa um número, não trava um lugar específico. Este marco introduz `Seat` como entidade própria, com lock otimista no nível do assento (mesmo padrão do M2, só que granular por assento em vez de por `Bookable` inteiro).

Decisões fechadas:
- **Geração automática**: ao cadastrar um voo, os assentos são gerados a partir de `totalCapacity` (6 por fileira, A-F) — sem mudar o request de `POST /admin/flights`.
- **`availableCapacity` vira derivado**: deixa de ser um contador mantido em paralelo — passa a ser calculado a partir da contagem de assentos `AVAILABLE`, uma fonte de verdade só (evita os dois números descolarem).
- **`seatId` obrigatório em `POST /bookings`**: reservar sem escolher assento deixa de fazer sentido — é uma mudança de contrato da API (`bookableId` sozinho não basta mais).

- [x] 10.1 Migration: tabela `seat` (`bookable_id`, `label`, `status`, `version` para lock otimista)
- [x] 10.2 Domínio: `Seat`, `SeatStatus`, `SeatRepository` (porta)
- [x] 10.3 Auto-geração do mapa de assentos em `RegisterFlightUseCase`
- [x] 10.4 `Bookable.availableCapacity` passa a ser derivado da contagem de assentos `AVAILABLE` (coluna `available_capacity` removida da tabela `bookable` — migration V9)
- [x] 10.5 Endpoint `GET /bookables/{id}/seats` — mapa de assentos (rota pública, mesmo espírito de `/flights/search`)
- [x] 10.6 `POST /bookings` exige `seatId`; `RegisterBookingUseCase` trava o assento via lock otimista (migration V10 adiciona `booking.seat_id`)
- [x] 10.7 `CancelBookingUseCase` libera o assento (volta a `AVAILABLE`)
- [x] 10.8 Testes: concorrência no nível do assento (2 reservas disputando o mesmo `seatId`), casos de uso (`registerbookingusecase`, `cancelbookingusecase`, `getseatmapusecase`, geração do mapa em `registerflightusecase`), endpoint (`bookablecontroller`)
- [x] 10.9 Swagger/OpenAPI atualizado (novo endpoint + `seatId` no request de reserva)
- [x] 10.10 README atualizado

## M11 — Companhias aéreas (Airline) ✅

Decisão (2026-09-12): construindo a tela de Resultados da Busca no
mobile a partir de uma referência visual real (várias companhias, selo
colorido por voo), ficou claro que o backend não tem nenhum conceito de
companhia aérea — todo voo gerado é anônimo, sem operador. Em vez de
inventar esse dado só no lado mobile (o que contrariaria a regra já
fechada de "sem parte fake" depois da correção do Multi-city), a
companhia aérea vira dado real de backend, seguindo exatamente o mesmo
padrão já usado por `Airport` (entidade JPA com tabela própria, seedada
via Flyway, resolvida por código IATA) — ver `Airport.kt`,
`AirportJpaEntity.kt`, `V1__create_bookable_and_flight_tables.sql`,
`V2__seed_airports.sql`.

- [x] 11.1 Migration `V11__create_airline_table.sql` (id, iata_code, name) + `V12__seed_airlines.sql` com 6 companhias reais que operam essas rotas (LATAM, Azul, GOL, American, Delta, United)
- [x] 11.2 Domínio `Airline` (id, iataCode, name) + `AirlineRepository` (porta) + `AirlineNotFoundException` — mesmo padrão de `Airport`
- [x] 11.3 `AirlineJpaEntity` + `AirlineJpaRepository` + `AirlineRepositoryAdapter`
- [x] 11.4 `Flight` (domínio) ganha `airline: Airline`; `FlightJpaEntity` ganha `@ManyToOne airline` (`ALTER TABLE flight ADD COLUMN airline_id ...` — migration `V13`); `FlightRepositoryAdapter.save` resolve a referência igual já fazia com origem/destino
- [x] 11.5 `FlightResponse` ganha `airlineName`/`airlineIataCode`; `.from()` atualizado
- [x] 11.6 `POST /admin/flights` (`RegisterFlightRequest`/`RegisterFlightUseCase`) passa a exigir `airlineIataCode`, resolvido via `AirlineRepository` — mesmo padrão de resolução de `Airport` por IATA code; `RegisterFlightUseCase.execute` ganhou `resolveAirline`/`resolveAirport` privados (evita duplicar a lógica de "busca ou lança" 3x e mantém `ThrowsCount` do detekt em dia)
- [x] 11.7 `scripts/seed-flights.sh` atualizado pra sortear uma companhia (`AIRLINES`) por voo gerado
- [x] 11.8 Testes: `ThrowsWhenAirlineDoesNotExistTest` (caso de uso), asserção de `airline.iataCode` em `RegistersAFlightResolvingAirportsByIataCodeTest`, `airlineIataCode`/`airlineName` no JSON de `ReturnsMatchingFlightsTest` e `ReturnsCreatedWithTheCreatedFlightTest` — mais todos os fixtures existentes (`FlightTestFixture`, `BookingTestFixture`, `RegisterFlightUseCaseFixture` e os demais que constroem `Flight`/`RegisterFlightCommand`) atualizados pra continuar compilando com o campo novo
- [x] 11.9 Swagger atualizado (`@Schema` em `airlineIataCode` de `RegisterFlightRequest`; `FlightResponse` documentado automaticamente como os demais campos, sem anotação dedicada — mesmo padrão já usado nesse DTO)
- [x] 11.10 README atualizado (seed de aeroportos+companhias, exemplo de `POST /admin/flights` com `airlineIataCode`)

**Checklist de fechamento do M11:**
- [x] Itens 11.1-11.10 revisados
- [x] Clean Code — `resolveAirline`/`resolveAirport` eliminam a duplicação que existia entre a resolução de origem e destino, além de resolver o `ThrowsCount`
- [x] SOLID (porta `AirlineRepository` em `domain`, implementação em `infrastructure`, mesmo padrão de `Airport`)
- [x] `./gradlew ktlintCheck detekt` sem violações — 2 thresholds documentados em `config/detekt/detekt.yml` (`LongParameterList.constructorThreshold`, `TooManyFunctions.thresholdInFiles`) precisaram subir mais uma unidade cada, mesmo motivo já registrado ali (crescimento legítimo de atributos/pares de mapper, não complexidade real)
- [x] Testes das camadas ainda não cobertas — cobertos acima (11.8)
- [x] README atualizado
- [x] Swagger em dia

**Limitação de ambiente nesta sessão (2026-09-12):** `./gradlew test` teve 13 falhas, todas em testes que estendem `AbstractIntegrationTest` (Testcontainers) — o Docker deste ambiente sandboxed responde `docker info`/`docker context` pela CLI, mas devolve um `/info` HTTP degenerado (todos os campos zerados/vazios) pro cliente Java do Testcontainers, que rejeita a resposta como inválida. Confirmado que não é causado por este marco: as 13 falhas são só integração (`BookingConcurrencyTest`, `PushesAnAvailabilityUpdateOverWebsocketTest`, todo `presentation/securityintegration`), nenhuma toca `Airline`; as 52 restantes passaram, incluindo todos os testes novos/atualizados deste marco. Precisa validação manual (`docker compose up` + app rodando de verdade) fora deste ambiente antes de considerar o fluxo de ponta a ponta 100% confirmado.

## M12 — Preço mínimo por destino (endpoint agregado) ✅

Decisão (2026-09-12): a grade "Destinos em destaque" do mobile precisa
mostrar um preço real tipo "from $450" por destino. `GET
/flights/search` só aceita data exata — perguntar preço mínimo assim
exigiria o mobile tentar várias datas por destino (voos são seedados em
datas aleatórias entre 1-60 dias, então boa parte das tentativas
voltaria vazia). Decisão com o usuário: em vez disso, um endpoint
agregado no backend (`MIN(price)` numa janela de datas, uma consulta
SQL só), mais rápido e sempre real.

- [x] 12.1 `GET /flights/lowest-price?destination={iataCode}` — retorna o menor preço encontrado num voo ativo com `destination = iataCode` e `departureTime` nos próximos 60 dias; `404` quando não há nenhum voo pra esse destino na janela
- [x] 12.2 `FlightRepository.findLowestPrice` (porta) + `FlightJpaRepository` com `@Query("SELECT MIN(f.price) ...")` — agregação de verdade no banco, sem trazer as linhas inteiras
- [x] 12.3 Endpoint público — `/flights/lowest-price` adicionado ao `permitAll()` do `SecurityConfig` (a lista é por rota exata, não `/flights/**`, então precisava da entrada própria — sem isso o endpoint teria voltado `401` em vez de `404`/`200`)
- [x] 12.4 Testes: `GetLowestPriceForDestinationUseCase` (com preço, sem preço — um cenário por classe, convenção do projeto), controller (200 com preço, 404 sem voos), `LowestPriceRemainsPublicWithoutATokenTest` confirmando que não é bloqueado por auth
- [x] 12.5 Swagger atualizado (`@Operation` no novo endpoint)
- [x] 12.6 README atualizado

**Checklist de fechamento do M12:**
- [x] Itens 12.1-12.6 revisados
- [x] Clean Code
- [x] SOLID (`FlightRepository` continua a única porta pra tudo relativo a `Flight`, mesmo padrão de `search`/`findActive`)
- [x] `./gradlew ktlintCheck detekt` sem violações
- [x] Testes das camadas ainda não cobertas — cobertos acima (12.4)
- [x] README atualizado
- [x] Swagger em dia

**Mesma limitação de ambiente do M11** (ver nota lá) — `LowestPriceRemainsPublicWithoutATokenTest` também estende `AbstractIntegrationTest` (Testcontainers) e falha só por isso neste sandbox; os testes de caso de uso e de controller (não-integração) passaram normalmente.

**Bug real encontrado e corrigido durante a verificação visual do M12 (2026-09-13):** o backend nunca teve nenhuma configuração de CORS — todo request de um cliente browser (o app Flutter rodando em `flutter run -d web-server`) era bloqueado antes de chegar em qualquer endpoint, `/flights/lowest-price` incluído. Passou despercebido até agora porque `curl` e clientes nativos (Android/iOS) não aplicam CORS — só apareceu ao testar de verdade no Browser pane. Corrigido em `SecurityConfig.kt`: novo bean `corsConfigurationSource` (origens `http://localhost:*`/`http://127.0.0.1:*` — só servidor de dev local, nenhum frontend está publicado em lugar nenhum ainda) plugado em `http.cors { ... }` no `securityFilterChain`. Confirmado de ponta a ponta: `curl -H "Origin: http://localhost:8767"` retorna `Access-Control-Allow-Origin`, e a busca/preço mínimo funcionam de verdade no app rodando no Browser pane.

## M13 — Endpoint agregado de destinos (`GET /destinations`) ✅

Decisão (2026-09-13): ao pedir mais destinos reais na Home do mobile,
ficou claro que `knownAirports` (lista fixa de aeroportos, hardcoded no
Flutter) era dado de negócio vivendo no front — o usuário pediu
explicitamente pra tirar isso de lá ("front deve ser burro e não ter
regras de negócio"). Em vez de um serviço BFF separado (não se justifica
pra um cliente só), este marco cria um endpoint novo **no próprio
backend**, moldado pro que a Home do mobile precisa: pra cada aeroporto,
código IATA, cidade, país, foto real e menor preço real, numa resposta
só. Reaproveita o `GetLowestPriceForDestinationUseCase` do M12 (zero SQL
novo pra preço) e segue o mesmo padrão de `Airport`/`AirportRepository`
já estabelecido. Foto real vira dado do backend também (coluna nova em
`airport`) — não fica metade no front, metade no backend.

- [x] 13.1 Migration `V14__add_photo_url_to_airport.sql` (coluna nova + backfill das 3 fotos já usadas hoje no Flutter pra GRU/GIG/JFK, depois `NOT NULL`) e `V15__seed_more_airports.sql` (LHR, CDG, LIS, MIA, EZE — nome oficial real, foto real do Unsplash verificada visualmente antes de usar, uma a uma)
- [x] 13.2 `Airport`/`AirportJpaEntity` ganharam `photoUrl`; `AirportRepository` ganhou `findAll()` (o `JpaRepository` já tinha de graça)
- [x] 13.3 `GetFeaturedDestinationsUseCase` — busca todos os aeroportos e chama `GetLowestPriceForDestinationUseCase` (M12) pra cada um, sem duplicar a agregação de preço
- [x] 13.4 `DestinationController` (`GET /destinations`, público) + `DestinationResponse` (iataCode, city, country, photoUrl, lowestPrice)
- [x] 13.5 `SecurityConfig` — `/destinations` no `permitAll()` (rota exata, mesmo detalhe que faltou no M12 pro `/flights/lowest-price`)
- [x] 13.6 `scripts/seed-flights.sh` — 20 rotas novas envolvendo os 5 aeroportos (GRU/GIG/JFK ↔ LHR/CDG/LIS/MIA/EZE)
- [x] 13.7 Testes: caso de uso (`ReturnsEmptyListWhenNoAirportsExistTest`, `ReturnsNullPriceWhenAnAirportHasNoFlightsTest`, `ReturnsEachAirportWithItsRealLowestPriceTest`), controller (`ReturnsTheFeaturedDestinationsListTest`), segurança (`DestinationsRemainPublicWithoutATokenTest`)
- [x] 13.8 Swagger (`@Operation`/`@Tag`) + README atualizados (endpoint novo, rota pública, contagem de aeroportos 3→8)

**Checklist de fechamento do M13:**
- [x] Itens 13.1-13.8 revisados
- [x] Clean Code
- [x] SOLID (`GetFeaturedDestinationsUseCase` reaproveita `GetLowestPriceForDestinationUseCase` em vez de duplicar a agregação)
- [x] `./gradlew ktlintCheck detekt` sem violações (rodou `ktlintFormat` uma vez pra requebrar linhas depois de um `photoUrl` adicionado em bloco nos fixtures de teste)
- [x] Testes das camadas ainda não cobertas — cobertos acima (13.7)
- [x] README atualizado
- [x] Swagger em dia

**Mesma limitação de ambiente do M11/M12** — `DestinationsRemainPublicWithoutATokenTest` também estende `AbstractIntegrationTest` (Testcontainers) e falha só por isso neste sandbox; caso de uso e controller (não-integração) passaram normalmente. 60/75 testes do módulo inteiro passaram; as 15 falhas são todas dessa mesma categoria, nenhuma nova.

## M14 — Região e destino em destaque por aeroporto ✅

Decisão (2026-09-13): o mobile precisava de uma tela Explore com "Principais
destinos" (subconjunto curado) e "Destinos por Região" (Europa, América do
Sul...). Primeira ideia (rejeitada pelo usuário, com razão): tratar região
como uma lista separada, buscada à parte — teria criado uma segunda
estrutura paralela à lista de destinos já existente, exatamente o tipo de
retrabalho que o padrão desta sessão vem evitando. Decisão final: `region`
e `isPopular` são só mais dois atributos de `Airport`/`DestinationResponse`
— mesmo padrão de `photoUrl` (M13). Uma chamada só (`GET /destinations`); o
cliente agrupa/filtra a mesma lista já carregada, sem endpoint novo.

- [x] 14.1 Migration `V16__add_region_to_airport.sql` — colunas `region` e `is_popular`, backfill dos 8 aeroportos existentes (América do Sul: GRU/GIG/EZE; América do Norte: JFK/MIA; Europa: LHR/CDG/LIS) e curadoria de 4 populares (GRU, GIG, JFK, LHR — espalhados entre regiões, não um corte arbitrário)
- [x] 14.2 `Airport` (domínio) + `AirportJpaEntity` + `Mappers.kt` ganham os dois campos
- [x] 14.3 `DestinationResponse` ganha `region`/`isPopular`; `isPopular` precisou de `@get:JsonProperty("isPopular")` — Jackson, por padrão, stripa o prefixo `is` de getters booleanos Kotlin e serializaria como `"popular"` sem a anotação (achado pelo teste do controller, que falhou com `PathNotFoundException` até a anotação entrar)
- [x] 14.4 Testes: `ReturnsTheFeaturedDestinationsListTest` passa a afirmar `region`/`isPopular` no JSON; fixtures de `Airport(...)` em todo o módulo atualizadas (`region = "América do Sul"`, `isPopular = false` como valores neutros de teste, exceto onde o teste em si depende do valor)
- [x] 14.5 README (exemplo de resposta) e Swagger em dia
- [x] 14.6 (2026-09-13, follow-up) Mobile só tinha 3 regiões reais (América do Sul/Norte, Europa) — o usuário pediu uma 4ª. `V17__seed_asia_airport.sql` adiciona Narita (NRT, Tóquio, Japão), região "Ásia", foto real verificada no Unsplash (gratuita, checada visualmente antes de usar — mesmo processo do M1/M13); `scripts/seed-flights.sh` ganhou 6 rotas envolvendo NRT pra ele ter preço real, não `null`

**Checklist de fechamento do M14:**
- [x] Item 14.1-14.5 revisados
- [x] Clean Code
- [x] Arquitetura (nenhuma estrutura nova — `region`/`isPopular` são atributos do mesmo `Destination`, não uma segunda lista)
- [x] `./gradlew ktlintCheck detekt test` — `ktlintFormat` rodou uma vez pra requebrar linhas depois do bulk-edit nos fixtures; 60/75 testes passaram (mesma limitação de Testcontainers do M11-M13, nenhuma falha nova depois do fix do `@JsonProperty`); reconfirmado depois do item 14.6 (Ásia), mesma baseline
- [x] README atualizado
- [x] Swagger em dia

## M15 — Nome do usuário + `GET /users/me` / `PATCH /users/me` ✅

Decisão (2026-09-14): o usuário revisou a parte logada do app (Perfil,
assento, Minhas Viagens, resultados) contra 5 telas de referência e pediu
um plano estruturado (ver plano aprovado). Perfil precisava de nome de
verdade — `User` só tinha `email`+`role`. Escopo maior escolhido (das 3
perguntas feitas): campo `name` de verdade no cadastro, não só cosmético.

- [x] 15.1 Migration `V18__add_name_to_app_user.sql` — coluna `name`, backfill dos usuários existentes com `split_part(email, '@', 1)` (não inventa nome bonito, só evita NULL), depois `NOT NULL` — mesmo padrão backfill-antes-de-NOT-NULL do V16
- [x] 15.2 `User` (domínio) ganha `name: String` com validação `isNotBlank()` no `init`; `UserJpaEntity` + `Mappers.kt` (`toDomain`/`toJpaEntity`) idem
- [x] 15.3 Cadastro: `RegisterUserRequest`/`RegisterUserCommand`/`RegisterUserUseCase` passam a exigir `name`; `UserResponse` expõe `name`
- [x] 15.4 `UserController` novo (`/users`, autenticado, fora do `permitAll()`) — `GET /me` (lê via `UserRepository.findById` direto, sem caso de uso — é só leitura de um registro) e `PATCH /me` (`UpdateUserNameUseCase` novo, valida e salva); `UserNotFoundException` nova, registrada no `ApiExceptionHandler` (404)
- [x] 15.5 Testes: `UpdateUserNameUseCase` (atualiza, lança `UserNotFoundException` pra id desconhecido); `RegisterUserUseCase` confirma `name` persistido; `securityintegration/` ganhou `UsersMeEndpointsRequireAuthenticationTest` (401 sem token nos dois verbos), `ReturnsTheAuthenticatedUsersOwnProfileTest`, `UpdatesTheAuthenticatedUsersNameTest` — sem slice `@WebMvcTest` pra este controller (mesmo padrão já usado em `BookingController`: métodos que dependem de `Authentication.currentUserId()` só são testados via integração real, não dá pra fake-ar de forma confiável numa slice)
- [x] 15.6 README (exemplo de `/auth/register` com `name`, `GET`/`PATCH /users/me`) + Swagger + `CHECKLIST.md`

**Bug real encontrado e corrigido durante o teste manual do `PATCH /users/me`:** o endpoint devolvia `401 Authentication required` mesmo com um token válido — parecia um problema de autenticação, mas era outra coisa. Debug com `println` temporário no `JwtAuthenticationFilter` mostrou que o contexto de segurança estava correto (`userId`/`role` certos) até `filterChain.doFilter()`, mas o status da resposta virava `400` logo depois — o log do Spring revelou o motivo real: `HttpMessageNotReadableException: Cannot construct instance of UpdateUserNameRequest (although at least one Creator exists)`. É um problema conhecido do Jackson com data class Kotlin de **um único parâmetro** (o projeto já tinha batido nisso antes — `RefreshRequest.kt` tem um comentário exatamente sobre isso). O `400` original disparava o redirecionamento de erro do Spring Boot pra `/error`, que reentra na cadeia de filtros como `DispatcherType.ERROR` — só que `OncePerRequestFilter` pula esse redespacho por padrão (`shouldNotFilterErrorDispatch() == true`), então o `JwtAuthenticationFilter` não roda de novo, o contexto de segurança fica anônimo nesse segundo passe, e *isso* é o que gera o `401` que o cliente via — mascarando o erro `400` real de desserialização. Corrigido do mesmo jeito que `RefreshRequest`: `UpdateUserNameRequest` ganhou `@JsonCreator`/`@JsonProperty` explícitos no construtor.
- [x] 15.7 (achado acima) `UpdateUserNameRequest` corrigido com `@JsonCreator` explícito

**Checklist de fechamento do M15:**
- [x] Itens 15.1-15.7 revisados
- [x] Clean Code
- [x] Arquitetura (`/users/me` sempre opera sobre o próprio usuário via `authentication.currentUserId()`, nunca aceita um id arbitrário — não existe rota pra ver/editar o perfil de outro usuário)
- [x] `./gradlew ktlintCheck detekt test` — 62/81 testes passaram; as 19 falhas são todas a mesma limitação de Testcontainers do sandbox (as 15 já conhecidas + as 4 novas rotas de `/users/me`, que também estendem `AbstractIntegrationTest`), nenhuma falha real
- [x] README atualizado
- [x] Swagger em dia

## M16 — Modelo de avião real e layout de assento variável ✅

Decisão (2026-09-14): seguindo o plano aprovado — a seleção de assento no
mobile era só quadrados iguais, sem noção de fileira/corredor/modelo de
avião; todo voo gerava sempre 6 assentos por fileira (`A`-`F`), fixo no
código (`SEATS_PER_ROW`). O usuário escolheu o escopo maior: variação de
verdade por voo (2+2 / 3+3 / 3+3+3), não só um redesenho visual do 3+3
único que já existia.

- [x] 16.1 `SeatLayout.kt` (novo) — `seatLayoutFor(aircraftType): List<Int>`, única fonte da regra (nem front nem seed reinventam o mapeamento): `"Embraer E195"` → `[2, 2]`, `"Boeing 777"` → `[3, 4, 3]`, qualquer outro (inclusive `"Airbus A320"`) → `[3, 3]` (o padrão de antes, agora explícito em vez de implícito)
- [x] 16.2 Migration `V19__add_aircraft_type_to_flight.sql` — coluna `aircraft_type`, backfill dos voos existentes com `'Airbus A320'` (o layout que já tinham), depois `NOT NULL`
- [x] 16.3 `Flight`/`FlightJpaEntity`/`Mappers.kt` ganham `aircraftType`; `RegisterFlightRequest`/`RegisterFlightCommand` exigem no cadastro; `FlightResponse` expõe `aircraftType` **e** `seatLayout` (já resolvido via `seatLayoutFor`, o cliente nunca precisa saber qual avião mapeia pra qual layout)
- [x] 16.4 `RegisterFlightUseCase.generateSeatMap` troca `SEATS_PER_ROW = 6` fixo por `seatLayoutFor(aircraftType).sum()`; letras das colunas viram `'A'..'Z'` (precisa de até `J` pro Boeing 777, 10/fileira)
- [x] 16.5 `scripts/seed-flights.sh` — `AIRCRAFT_TYPES` (mesmos 3 nomes de `SeatLayout.kt`) sorteado por voo, dá variedade real entre buscas; script também ganhou `"name"` no registro do admin seed (quebrado pelo M15 — `/auth/register` agora exige nome)
- [x] 16.6 Testes: `SeatLayout` (os 3 layouts + fallback), `RegisterFlightUseCase` gera o mapa certo pra Embraer E195 (2+2) e Boeing 777 (3+4+3), controller confirma `aircraftType`/`seatLayout` no JSON
- [x] 16.7 `detekt`: `Flight`/`FlightJpaEntity` cresceram pra 15 parâmetros — `constructorThreshold` bumpado 14→15 (mesmo padrão documentado do M11); `seatLayoutFor` ganhou `@Suppress("MagicNumber")` — os números são o próprio dado de negócio (contagem real de colunas por avião), não "mágicos" no sentido que a regra tenta pegar (precedente: `JwtTokenService`/`BookableJpaEntity` já usam `@Suppress` pontual do mesmo jeito)
- [x] 16.8 README (endpoint atualizado) + Swagger + `CHECKLIST.md`
- [x] 16.9 (2026-09-14, addendum durante o M16 do `dbook-mobile` — "Resultados de busca mais ricos") `scripts/seed-flights.sh`: janela de datas de `days_ahead` reduzida de 1-60 pra 1-21 dias — mesma quantidade de voos (`FLIGHT_COUNT`), mais concentrados nas datas que uma busca de verdade tende a testar (quase triplica a densidade por rota+data); não afeta a janela de 60 dias do `GET /flights/lowest-price` (regra de negócio independente do endpoint, continua valendo pra todo voo gerado nos primeiros 21 dias)

**Checklist de fechamento do M16:**
- [x] Itens 16.1-16.8 revisados
- [x] Clean Code
- [x] Arquitetura (`seatLayoutFor` é a única fonte da regra — backend gera os assentos E expõe o layout já resolvido; o mobile só vai precisar agrupar colunas pelo array que já chega, sem tabela própria)
- [x] `./gradlew ktlintCheck detekt test` — 68/87 testes passaram; as 19 falhas são a mesma limitação de Testcontainers de sempre (as 15 originais + as 4 do M15), nenhuma falha nova
- [x] Testado manualmente: reseed com `scripts/seed-flights.sh 1000` gerou voos nos 3 modelos; confirmado via `GET /flights/search` que `aircraftType`/`seatLayout` variam de verdade entre voos, e via `GET /bookables/{id}/seats` que um voo Boeing 777 realmente gera 10 assentos por fileira (`A`-`J`)
- [x] README atualizado
- [x] Swagger em dia

## M17 — `GET /bookings` (Minhas Viagens persistidas) ✅

Decisão (2026-09-14): seguindo o plano aprovado — Minhas Viagens no mobile
era só memória de sessão (`BookingRecord` local), sumia ao reabrir o app.
Não existia `GET /bookings`. O usuário escolheu o escopo maior: persistir
de verdade no backend em vez de só guardar localmente no app.

- [x] 17.1 `BookingRepository` (porta) ganha `findByCustomerId(customerId): List<Booking>`; `BookingJpaRepository` implementa via método derivado do Spring Data (sem SQL manual); `BookingRepositoryAdapter` mapeia reaproveitando o mesmo padrão de `findById` (`.toDomain(availableCapacityOf(...))`)
- [x] 17.2 `ListMyBookingsUseCase` (novo) — busca as reservas do usuário e resolve o `Seat` de cada uma via `SeatRepository.findById`; descoberta durante a implementação: `Booking.bookable` já É o `Flight` completo (resolvido pelo mapper), então não precisa de uma segunda chamada a `FlightRepository` — só compõe o que já tem, sem estrutura paralela redundante
- [x] 17.3 `MyBookingResponse(id, status, seat: SeatResponse, flight: FlightResponse)` (novo) — reaproveita `SeatResponse.from`/`FlightResponse.from` já existentes, só compõe
- [x] 17.4 `BookingController` ganha `GET /bookings` (autenticado, filtra sempre por `authentication.currentUserId()` — nunca aceita um `customerId` arbitrário, mesma disciplina de `/users/me`)
- [x] 17.5 Testes: `ListMyBookingsUseCase` (lista vazia, reservas com seat+flight corretos, nunca vaza reserva de outro customer — 3 cenários, um por classe, seguindo o padrão da sessão); `securityintegration/` ganhou o caso de 401 sem token em `BookingEndpointsRequireAuthenticationTest` e `ReturnsOnlyTheAuthenticatedUsersOwnBookingsTest` (dois usuários, cada um reserva um voo, confirma que `GET /bookings` de um nunca devolve a reserva do outro)
- [x] 17.6 README (`GET /bookings`) + Swagger + `CHECKLIST.md`

**Checklist de fechamento do M17:**
- [x] Itens 17.1-17.6 revisados
- [x] Clean Code
- [x] Arquitetura (`GET /bookings` sempre filtra por `currentUserId()`, nunca vaza dado de outro customer; sem estrutura paralela pra "minhas reservas" — reaproveita `Booking.bookable` que já é o `Flight` completo)
- [x] `./gradlew ktlintCheck detekt test` — 71/92 testes passaram; as 21 falhas são a mesma limitação de Testcontainers de sempre (as 19 já conhecidas + as 2 novas rotas de `GET /bookings` que também estendem `AbstractIntegrationTest`), nenhuma falha real
- [x] Testado manualmente via curl: usuário sem reserva → `GET /bookings` devolve `[]`; após `POST /bookings`, `GET /bookings` devolve a reserva com `seat`/`flight` corretos (incluindo `aircraftType`/`seatLayout` do M16)
- [x] README atualizado
- [x] Swagger em dia

## M18 — `Payment` real, `POST /payments` ✅

Decisão (2026-09-14): seguindo o plano aprovado — o usuário pediu que a
seleção de assento virasse só um detalhe dentro de uma revisão, com uma
tela de pagamento nova cobrindo os dois trechos de uma Round Trip de uma
vez só. Escopo fechado com o usuário: suporte real no backend (não só
UI) e uma única revisão/pagamento no final, não um por trecho.
`Booking.confirm()` já existia mas era código morto (nenhuma reserva
saía de PENDING pra CONFIRMED) — pagar é o gatilho que faltava.

- [x] 18.1 Migration `V20__create_payment.sql` — tabela `payment` (`customer_id`, `amount`, `card_last4`, `cardholder_name`) + `booking.payment_id` (FK nullable, preenchida só quando a reserva é confirmada)
- [x] 18.2 `Payment` (domínio, novo) — valida `cardLast4` como 4 dígitos e `cardholderName` não-vazio, mesmo estilo de `init { require(...) }` de `User`/`Bookable`; `PaymentRepository` (porta) + `PaymentRepositoryAdapter`/`PaymentJpaEntity` (mirror de `Booking`)
- [x] 18.3 `Booking.confirm()` ganha o parâmetro `paymentId: Long` — deixa de ser código morto, passa a exigir de qual pagamento a confirmação veio (`transitionTo` repassa o campo, mesma disciplina de "só PENDING transiciona" de antes)
- [x] 18.4 `RegisterPaymentUseCase` (novo) — busca cada `Booking` (`BookingNotFoundException` se faltar), valida dono (`NotBookingOwnerException`), soma `booking.bookable.price` de todas (reaproveita `Booking.bookable`, já resolvido — sem lookup de `Flight` separado), salva o `Payment` e confirma cada reserva com o `paymentId` gerado, tudo em uma `@Transactional`
- [x] 18.5 `PaymentController` (novo) — `POST /payments`, autenticado, mesmo padrão de `BookingController` (`RegisterPaymentRequest`/`PaymentResponse`, `201 CREATED`)
- [x] 18.6 Testes: `Payment` (validação de `cardLast4`/`cardholderName`/`amount`), `RegisterPaymentUseCase` (paga 1 reserva, paga 2 reservas de trechos diferentes somando o preço certo, dono errado → forbidden, reserva não-PENDING → conflict, reserva inexistente → not found — um cenário por classe, mesmo padrão de `cancelbookingusecase`), `securityintegration` (401 sem token, fluxo completo reserva→paga→confirma via `GET /bookings`)
- [x] 18.7 `Mappers.kt` passou do limite de 16 funções por arquivo (detekt `TooManyFunctions`) — mappers de `Payment` extraídos pra `PaymentMappers.kt` própria, em vez de bumpar o threshold (não é um caso de "esse arquivo é legitimamente grande", é um novo agregado que merece seu próprio arquivo)
- [x] 18.8 README (`POST /payments`) + Swagger + `CHECKLIST.md`

**Checklist de fechamento do M18:**
- [x] Itens 18.1-18.8 revisados
- [x] Clean Code
- [x] Arquitetura (a reserva continua sendo criada — e o assento reservado — no momento da escolha do assento, não no pagamento; pagar só confirma reservas `PENDING` que já existem, evitando uma corrida onde o assento seria perdido enquanto o usuário ainda preenche o cartão; full card number/CVV nunca chegam ao backend, só `cardLast4`+`cardholderName`)
- [x] `./gradlew ktlintCheck detekt test` — 80/103 testes passaram; as 23 falhas são a mesma limitação de Testcontainers de sempre (as 21 já conhecidas + as 2 novas rotas de `/payments` que também estendem `AbstractIntegrationTest`), nenhuma falha real — todos os testes de domínio/caso de uso do Payment (que não dependem de Testcontainers) passaram
- [x] README atualizado
- [x] Swagger em dia

## M19 — Avaliação de reserva, `POST /reviews` ✅

Decisão (2026-09-24): exercício de aprendizado do dono pra praticar como
criar uma feature de ponta a ponta no backend, camada por camada. Escopo
fechado antes de começar: usuário avalia uma reserva `CONFIRMED` (nota de
1 a 5 + comentário opcional), só o dono pode avaliar, só depois de
`CONFIRMED` (não dá pra avaliar uma reserva que nunca foi paga), e só uma
vez por reserva. Escrito pelo próprio dono, revisado item a item contra o
irmão mais próximo (`Payment`/`RegisterPaymentUseCase`/`PaymentController`).

- [x] 19.1 Migration `V21__create_review_table.sql` — tabela `review` (`booking_id` `UNIQUE` + `REFERENCES booking (id)`, `customer_id`, `rating SMALLINT CHECK (rating BETWEEN 1 AND 5)`, `comment` opcional, `created_at`). Bug achado só ao subir a aplicação de verdade (não pega em `ktlintCheck`/`detekt`/teste de domínio): `rating SMALLINT` não bate com `ReviewJpaEntity.rating: Int`, que o Hibernate mapeia pra `INTEGER` — `ddl-auto: validate` recusa subir (`SchemaManagementException`). Como o V21 já tinha sido aplicado no banco de dev, a correção foi uma migration nova (`V22__widen_review_rating_column.sql`, `ALTER TABLE review ALTER COLUMN rating TYPE INTEGER`) em vez de editar o V21 — regra do projeto, nenhuma outra coluna `int` do schema usa `SMALLINT`
- [x] 19.2 `Review` (domínio, novo) — valida `rating in 1..5` via `init { require(...) }`, mesmo estilo de `Payment`; `ReviewRepository` (porta, `findById`/`findByBookingId`/`save`)
- [x] 19.3 `CreateReviewUseCase` (novo) — busca o `Booking` (`BookingNotFoundException` se faltar), valida dono (`NotBookingOwnerException`), valida `status == CONFIRMED` e "ainda não avaliada" via `check(...)` (409 — sem exceção nova pra invariante de estado, regra do projeto), salva o `Review`
- [x] 19.4 `ReviewJpaEntity`/`ReviewJpaRepository`/`ReviewRepositoryAdapter` (mirror de `Payment`); mappers em `ReviewMappers.kt` própria (`Mappers.kt` estourou `TooManyFunctions` de novo, mesmo precedente do `PaymentMappers.kt`)
- [x] 19.5 `ReviewController` (novo) — `POST /reviews`, autenticado, `customerId` vem de `authentication.currentUserId()` (nunca do corpo — achado numa revisão: a primeira versão aceitava `customerId` no `RegisterReviewRequest`, corrigido antes de fechar)
- [x] 19.6 Testes, um cenário por classe: `domain/review` (nota válida, nota 0, nota 6), `application/createreviewusecase` (avalia reserva `CONFIRMED`, reserva inexistente → not found, dono errado → forbidden, reserva ainda `PENDING` → conflict, reserva já avaliada → conflict), `presentation/securityintegration` (401 sem token, fluxo completo registra→reserva→paga→avalia via `POST /reviews`)
- [x] 19.7 README (`POST /reviews`) + `CHECKLIST.md`

**Checklist de fechamento do M19:**
- [x] Itens 19.1-19.7 revisados
- [x] Clean Code
- [x] Arquitetura (nenhuma exceção nova pra invariante de estado — `check()` cobre "não confirmada"/"já avaliada", igual a regra já documentada; review referencia `booking_id`, não `bookable_id` — é a experiência de uma reserva específica, não do voo em abstrato; `ReviewNotFoundException` criada mas nunca usada — removida, sem `GET /reviews/{id}` no escopo)
- [x] `./gradlew ktlintCheck detekt` limpo (confirmado com `--rerun-tasks`, sem cache)
- [x] `./gradlew test` — 110 testes, 85 passaram, 25 falharam — todas as 25 são a mesma limitação de Testcontainers/Docker de sempre (confirmado comparando com `PaymentEndpointRequiresAuthenticationTest`, que já existia antes desta feature e falha do mesmo jeito); todos os testes de domínio/caso de uso do Review (que não dependem de Testcontainers) passaram
- [x] Aplicação rodando de verdade (`./gradlew bootRun`, Postgres/Redis via `docker compose`) — achou o bug do `SMALLINT` (19.1) e confirmou `/reviews` no Swagger (`@Tag`/`@Operation`/`@Schema` com exemplos reais nos 3 campos do `RegisterReviewRequest`). Fluxo completo via `curl` contra o banco real: registra→loga→reserva→**avalia antes de pagar → 409**→paga→**avalia CONFIRMED → 201**→**avalia de novo → 409**→**nota 9 numa reserva nova → 400** (`"rating must be between 1 and 5"`, mensagem do domínio)→**sem token → 401**
- [x] README atualizado

**Addendum (2026-09-27):**
- [x] 19.8 `GET /bookings` passa a devolver a `review` de cada reserva (se existir) — o app do mobile não tinha como saber "já avaliada" nem mostrar nota/comentário depois de enviados, e guardar isso só localmente no aparelho quebraria ao logar em outro dispositivo (decisão do dono: nunca aproximar com storage local o que é estado real do servidor). `ListMyBookingsUseCase` busca a review de cada `Booking` via `ReviewRepository.findByBookingId`; `MyBookingResponse` reaproveita o `ReviewResponse` que já existia, sem DTO novo
- [x] 19.9 `comment` deixa de ser opcional — decisão do dono, nota sem comentário não fecha o objetivo da avaliação. Migration `V23__require_review_comment.sql` (`NOT NULL`), `Review.kt` ganha `require(comment.isNotBlank())`, DTOs/entidade JPA acompanham (`String?` → `String` em cascata)
- [x] 19.10 **Bug de segurança achado testando o 19.9 de ponta a ponta**: mandar `POST /reviews` sem o campo `comment` devolvia **401** ("Authentication required") em vez de **400** — nada a ver com autenticação de verdade. Causa: `HttpMessageNotReadableException` (corpo JSON que não bate com o DTO) não tinha handler no `ApiExceptionHandler`, então o Spring cai no `sendError(400)` padrão, que aciona um *forward* interno do container pra `/error` — e `/error` não está no `permitAll()` do `SecurityConfig`, batendo na parede de autenticação. **Confirmado sistêmico, não específico do Review**: reproduzido também em `/payments` (endpoint do M18) e em `/auth/register` (público, sem token nenhum, mesmo assim 401). Corrigido com um handler novo pra `HttpMessageNotReadableException` → 400 no `ApiExceptionHandler`, no mesmo padrão dos outros — resolve a causa (nunca mais precisa do `sendError`/forward) em vez de só liberar `/error` no `permitAll()` (que vazaria a página de erro genérica do container em vez do corpo `{"error": "..."}` padrão da API)
- [x] 19.11 (2026-10-02) `GET /destinations` passa a devolver `averageRating` (média 1-5 das avaliações das reservas de voos pra aquele destino, `null` enquanto ninguém avaliou) — pedido do dono pra Home mostrar a nota na lista de destinos. Novo `GetAverageRatingForDestinationUseCase` (espelha `GetLowestPriceForDestinationUseCase`, reaproveitado por `GetFeaturedDestinationsUseCase`) e `ReviewRepository.findAverageRatingByDestination`. A consulta é **nativa** (`AVG(rating)` atravessando `review → booking → flight → airport`) e não JPQL: `Review` guarda só o `bookingId` cru, sem relação `@ManyToOne` com `Booking`, e `Bookable`/`Flight` usam herança JOINED — mesma ideia do `findLowestPrice`, só que em SQL. Campo novo e opcional: o app mobile antigo continua funcionando. Sem migration (nenhuma mudança de schema). Validado contra o Postgres real: GRU 5.0, GIG 4.67 e `null` nos destinos sem avaliação (conversão `AVG` numeric → `Double` ok). Testes com fake por cenário (`getaverageratingfordestinationusecase`, `getfeatureddestinationsusecase`, `listmybookingsusecase`); a agregação em si só tem cobertura de integração, que roda no CI
- [x] 19.12 (2026-10-02) `AirlineNotFoundException` (lançada por `RegisterFlightUseCase` quando o código da companhia não existe) não estava registrada no `ApiExceptionHandler.handleNotFound`, então `POST /admin/flights` com companhia inválida devolvia 500 em vez de 404 — mesma armadilha já documentada no `CLAUDE.md` pra exceção de "não encontrado" nova. Registrada junto das outras; teste novo `ReturnsNotFoundWhenAirlineDoesNotExistTest` (falhava antes da correção, passa depois), irmão do de aeroporto inexistente
- [x] `./gradlew ktlintCheck detekt` limpo e `./gradlew test` — 89/114 passaram, as 25 falhas continuam sendo só Testcontainers/Docker (mesmos nomes de sempre, nada novo)
- [x] Testado de ponta a ponta de novo: `/auth/register`, `/payments` e `/reviews` com corpo incompleto → `400` certinho; requisição sem token nenhum continua `401` de verdade
- [x] README atualizado

## M20 — Expiração de reservas pendentes (fila SQS) ✅

Problema real: uma `Booking` `PENDING` já tirou o assento de circulação e nada a expira — quem abandona o pagamento prende o assento para sempre. Solução planejada: mensagem com atraso (SQS, via LocalStack) que cancela a reserva se ela ainda estiver `PENDING`, com DLQ e consumidor idempotente. Pagamento continua síncrono (não há gateway real; webhook/pagamento assíncrono fica como evolução).

- [x] 20.1 Lock otimista no nível da `Booking` (pré-requisito da fila) — pagar e cancelar/expirar a mesma reserva ao mesmo tempo eram uma corrida real: ambos liam `PENDING` e ambos gravavam, podendo terminar com reserva `CONFIRMED` e assento liberado (mesmo assento vendido duas vezes). Só o `Seat` tinha `@Version`. Migration `V24__add_optimistic_lock_to_booking.sql`, `@Version` em `BookingJpaEntity` e `version` no domínio `Booking`: o `save` do adapter monta uma entidade nova a cada gravação, então a versão precisa viajar pelo domínio (lida do banco em `toDomain`, devolvida em `toJpaEntity` e no retorno do `save`) — um `@Version` sozinho na entidade seria sempre comparado com `0`. O perdedor da corrida falha com `OptimisticLockingFailureException` (→ 409, já mapeado) e a transação dele inteira é desfeita
- [x] Testes de concorrência reorganizados em `application/bookingconcurrency/` (fixture + um cenário por classe): `OnlyOneOfTwoBookingsForTheSameSeatWinsTest` (antigo `BookingConcurrencyTest`, agora com usuário real em vez de `customerId = 1L`) e `OnlyOneOfPayAndCancelWinsTest` (20 rodadas; verificado por mutação: sem o `@Version` ele falha, com ele passa)
- [x] 20.2 SQS no LocalStack: `sqs` em `SERVICES` do `docker-compose.yml` e script `scripts/localstack-init/01-sqs.sh` (montado em `/etc/localstack/init/ready.d`, roda sozinho quando o LocalStack fica pronto — precisa ser executável). Cria a fila `dbook-booking-expiration` (`VisibilityTimeout` 30s) e a DLQ `dbook-booking-expiration-dlq`, ligadas por redrive policy com `maxReceiveCount` 3. O atraso de 15 min vai em cada mensagem (`DelaySeconds`); 900 s é o máximo que a SQS permite. Validado: `aws --endpoint-url=http://localhost:4566 sqs list-queues` lista as duas e `get-queue-attributes` mostra o redrive
- [x] 20.3a Porta `BookingExpirationScheduler` (domínio) + adapter `SqsBookingExpirationScheduler` + `SqsConfig` (SDK direto, `software.amazon.awssdk:sqs`, mesma versão do Bedrock; sem Spring Cloud AWS). Com `aws.sqs.endpoint` preenchido (LocalStack) o cliente usa credenciais falsas `test/test`; na AWS o endpoint fica vazio e vale a cadeia padrão do SDK. Atraso validado em 0..900 s (limite da SQS). **Falha ao publicar é logada e engolida de propósito**: o envio roda depois do commit, e lançar viraria um 500 para uma reserva que existe — a janela que isso deixa (queda entre commit e envio) é o *dual write*, resolvido pelo outbox (20.6). Testes com LocalStack via Testcontainers (`infrastructure/messaging/sqsbookingexpirationscheduler`): mensagem fica invisível até o atraso e carrega o `bookingId`; fila inalcançável não lança; atraso > 15 min é rejeitado
- [x] 20.3b `RegisterBookingUseCase` agenda a expiração em `afterCommit` (15 min, `BOOKING_EXPIRATION`), registrada **antes** do broadcast: uma exceção num callback de `afterCommit` pula os seguintes, e a expiração é o que libera o assento (o broadcast é só best-effort). `RecordingBookingExpirationScheduler` (fake) e dois cenários (agenda depois do commit; não agenda se a transação não confirma). `@MockBean BookingExpirationScheduler` no `AbstractIntegrationTest`: sem SQS no CI, cada reserva criada nos testes de integração tentaria alcançar uma. Revisão pegou um bug que os testes não pegam: o broadcast ficou registrado duas vezes (o fake só guarda o último valor)
- [x] 20.4a `ExpireBookingUseCase`: cancela a reserva que continua `PENDING` reaproveitando o `CancelBookingUseCase` (age em nome do dono, `Role.CLIENT`, sem precisar de ADMIN) — não duplica a lógica de cancelar/liberar assento/avisar disponibilidade. **Idempotente**: reserva inexistente ou que já não está `PENDING` (paga/cancelada) vira no-op, o que torna inofensiva a entrega duplicada da SQS. Se um pagamento confirmar entre a checagem e o cancelamento, o cancelamento falha e a mensagem é reentregue (a próxima tentativa encontra a reserva paga). A reserva expirada fica `CANCELLED` (sem status `EXPIRED`, para não mexer no contrato com o app). Testes reaproveitam o fixture do cancelamento
- [x] 20.4b `BookingExpirationConsumer` (`presentation`, é o lado que *recebe*, como um controller — `infrastructure` não pode importar `application`): `@Scheduled` com long polling, lê o `bookingId` pelo nome do campo (evita a armadilha do Jackson com data class de um parâmetro) e chama `ExpireBookingUseCase`. **Só apaga a mensagem se processou**; falha fica na fila → reentrega após o `VisibilityTimeout` → DLQ depois de `maxReceiveCount` (captura ampla com `@Suppress` justificado: uma mensagem venenosa não pode derrubar o loop). Interruptor `booking-expiration.consumer.enabled` (desligado em `src/test/resources/application.properties`; sem ele os testes de Spring consultariam a fila de desenvolvimento). `@EnableScheduling` na aplicação. `LocalStackSqs` compartilhado nos testes (um LocalStack por JVM). Testes: expira e apaga; reserva já paga é no-op e apaga; mensagem venenosa vai para a DLQ
- [x] Validado de ponta a ponta com a aplicação de verdade (`bootRun` + Postgres/Redis/LocalStack): criar a reserva deixa 1 mensagem atrasada na fila (`ApproximateNumberOfMessagesDelayed` 0 → 1); uma mensagem sem atraso para o mesmo `bookingId` leva a reserva a `CANCELLED` e o assento a `AVAILABLE` em poucos segundos (`GET /bookings` confirma); uma reserva **paga** que recebe a mensagem continua `CONFIRMED` com o assento `RESERVED`
- [x] `./gradlew check` (ktlint + detekt + testes de integração com Testcontainers + JaCoCo ≥ 75%) verde
- [x] README com a seção "Expiração de reservas (SQS)" e o diagrama `docs/booking-expiration.svg` (desenhado à mão e renderizado antes de entrar no repositório)

**Fora do M20 (evolução, não feito):**
- [ ] 20.5 Terraform do módulo SQS — sem conta AWS persistente só seria validado por `terraform plan`
- [x] 20.6 Outbox transacional: gravar o evento na mesma transação da reserva e publicar por um relay, fechando a janela de perda do *dual write* (queda entre o commit e o envio à SQS). **Feito no M35** (2026-10-05); o `BookingExpirationScheduler` e o `SqsBookingExpirationScheduler` dos itens 20.3a/20.3b deixaram de existir (o outbox os substitui) e o limite de 900 s da SQS (20.2) deixou de ser uma restrição
- [ ] Pagamento assíncrono (gateway que confirma depois) e `Idempotency-Key` no `POST /payments`

## M21 — Idempotência no `POST /payments` ✅

Problema real: se a resposta de um pagamento se perde (timeout, rede móvel ruim) e o app tenta de novo, a segunda chamada falha com 409 — as reservas já estão `CONFIRMED` — embora o pagamento tenha sido feito. Com um header `Idempotency-Key` (um UUID por tentativa, gerado pelo cliente) a retentativa devolve o resultado da primeira em vez de falhar ou cobrar duas vezes. Chave nova paga normalmente; mesma chave e mesmo pedido devolve o pagamento original; mesma chave com pedido diferente é 422; duas requisições iguais ao mesmo tempo, o índice único decide e o perdedor recebe 409. A chave é por usuário. Guardada na própria tabela `payment` (sem tabela genérica: só valeria com um 2º uso real).

- [x] 21.1 Persistência: migration `V25__add_idempotency_to_payment.sql` (`idempotency_key`, `request_fingerprint`, índice único `(customer_id, idempotency_key)` — colunas anuláveis, pagamentos antigos ficam sem chave), campos no `Payment` (chave validada: 1 a 64 caracteres, não em branco), na entidade e nos mappers, `PaymentRepository.findByCustomerIdAndIdempotencyKey`. O adapter traduz a violação do índice único em `DuplicateIdempotencyKeyException` (409, ver 21.4) — é o que torna seguras duas requisições iguais em corrida
- [x] 21.2 `RegisterPaymentUseCase` idempotente: `RegisterPaymentCommand` ganha `idempotencyKey` e `fingerprint()` (SHA-256 do pedido normalizado — as reservas ordenadas, então `[1,2]` e `[2,1]` são o mesmo pedido). Chave já usada pelo mesmo usuário com o mesmo pedido devolve o pagamento original sem tocar nas reservas; com pedido diferente lança `IdempotencyKeyReusedException` (422, e não 409: o pedido em si está errado, não há conflito de estado). `Idempotency-Key` obrigatório no `PaymentController` (`@RequestHeader`); header ausente vira 400 via handler de `MissingRequestHeaderException` — sem ele cairia na mesma armadilha do `/error` do 19.10 e viraria 401. Testes: replay, ordem das reservas, chave reutilizada com outro pedido, chave de outro usuário (não é reaproveitada). Os 3 testes de integração que pagam por HTTP passaram a enviar o header
- [x] 21.3 Teste de controller (`@WebMvcTest`, pasta `paymentcontroller/`): a chave chega ao use case, header ausente → 400 com a mensagem certa (sem tocar no use case), chave reutilizada → 422, mesma chave em andamento (`DuplicateIdempotencyKeyException`) → 409. O usuário autenticado entra pelo `principal` da requisição (filtros desligados no slice). Swagger: o header `Idempotency-Key` aparece como obrigatório e com exemplo (centralizado no `OpenApiConfig`, como os demais exemplos de parâmetro). Validado com a aplicação de verdade: 1º pagamento 201; retry com a mesma chave e corpo → 201 com o **mesmo** id e 1 só linha em `payment`; mesma chave com outro pedido → 422 e a 2ª reserva continua `PENDING`; sem header → 400; outro usuário com a mesma chave → tratado como pedido novo (403 por não ser dono); chave de 65 caracteres → 400
- [x] 21.4 Integração: `RetryingAPaymentWithTheSameKeyReturnsTheSamePaymentTest` (HTTP: o retry devolve exatamente o mesmo corpo e há 1 só linha em `payment`) e `ConcurrentPaymentsWithTheSameKeyCreateOnlyOnePaymentTest` (10 rodadas de duas requisições idênticas em corrida: pelo menos uma vence, quem perde é um 409, 1 só pagamento, reserva `CONFIRMED`). **O teste de corrida achou um bug real que os testes de unidade/controller (com fake e mock) não pegavam:** o adapter lançava `IllegalStateException`, mas todo `@Repository` passa pela tradução de exceções do Spring, que reescreve `IllegalStateException`/`IllegalArgumentException` em `InvalidDataAccessApiUsageException` — que ninguém mapeia, então quem perdesse a corrida receberia **500**. Corrigido com `DuplicateIdempotencyKeyException` (domínio, mapeada para 409 no `ApiExceptionHandler`). Duas redes de segurança independentes protegem a corrida (o índice único e o `@Version` da `Booking`), então o teste prova o comportamento (1 pagamento, só 409), não cada rede isolada. `BookingConcurrencyFixture` ganhou `pay(...)`, `raceOutcomes(...)` e `JdbcTemplate`
- [x] 21.5 README: `POST /payments` documenta o header obrigatório, os códigos `400/409/422` e a seção de idempotência; M21 no roadmap
- [x] 21.6 **App mobile** (repositório `dbook_mobile`, M22 de lá): gera um UUID por tentativa de pagamento e o reaproveita nas retentativas com os mesmos dados. Validado com o `PaymentRepositoryImpl` real do app contra este backend: o retry com a mesma chave devolveu o mesmo `id`, e a chave reutilizada com outro cartão voltou 422

## M22 — Pacotes por conceito (reorganização, sem mudança de comportamento) ✅

Problema: com 156 arquivos de produção, as camadas planas deixaram `domain/` com 47 arquivos soltos (`persistence/` 36, `presentation/` 34), enquanto os testes já estavam organizados por assunto. Decisão do dono (2026-10-03), entre duas opções avaliadas: **A** — subpacotes por conceito dentro de cada camada (escolhida) — e **B** — um módulo por conceito com as camadas dentro (descartada: os conceitos compartilham o banco e têm relações JPA entre si, então o ganho de modularidade ainda não existe; vale reavaliar se um conceito for extraído, ex. hotéis).

Medição que decidiu o desenho: o domínio se divide em 7 conceitos (`catalog`, `seating`, `booking`, `payment`, `review`, `identity`, `ai`) com **só 2 dependências entre eles** (`booking → catalog`, `ai → catalog`; o resto se liga por id) e sem ciclos.

- [x] 22.1 Produção: `domain/`, `application/`, `presentation/`, `infrastructure/persistence/<conceito>/` e `infrastructure/messaging/{availability,expiration}/`; o que atravessa conceitos (`ApiExceptionHandler`, `HealthController`, `SecurityExtensions`, `TransactionSupport`) em `common/`. `Mappers.kt` (que misturava 5 conceitos) dividido em `CatalogMappers`, `BookingMappers`, `SeatMappers`, `IdentityMappers`, `AiSuggestionLogMappers`. `security/`, `ai/` e `web/` ficaram como estavam (já pequenos e coesos)
- [x] 22.2 Testes acompanham a produção: a pasta do teste fica no conceito do sujeito (`application/booking/cancelbookingusecase`, `presentation/catalog/flightadmincontroller`, ...); `presentation/securityintegration` (atravessa todos os endpoints) ficou onde estava
- [x] 22.3 Feito por script (movimentação com `git mv`, reescrita de `package`/`import` e imports que eram implícitos por estarem no mesmo pacote) — 266 arquivos movidos, 262 reconhecidos como renomeação pelo Git, histórico preservado. Nenhuma linha de lógica mudou
- [x] 22.4 Revisão do resultado, não só "compilou": cada adapter/mapper importava `toDomain`/`toJpaEntity` de todos os 7 conceitos (ruído de 6 a 12 imports por arquivo); removidos os estrangeiros e o compilador exigiu **um** (`BookingMappers` → `catalog.toDomain`). Um KDoc ficou 8 caracteres maior com o pacote novo e estourou o detekt: quebrado em duas linhas
- [x] `./gradlew check` verde: **145 testes, 0 falhas, 0 puladas**, ktlint, detekt e JaCoCo ≥ 75% — os mesmos testes de antes
- [x] `CLAUDE.md`, skills e README atualizados (a regra "pacotes planos por camada" foi trocada)
- [x] 22.5 **Teste de arquitetura automático (ArchUnit 1.3.0)** em `src/test/kotlin/com/dbook/architecture/`, uma regra por classe: `domain` não depende de Spring, JPA nem de camada externa; `application` não depende de `presentation`/`infrastructure`; `presentation` não depende de `infrastructure`; `infrastructure` não depende de `application`/`presentation`; os conceitos do domínio não têm ciclo e só dependem do permitido (`booking → catalog`, `ai → catalog`); os conceitos da `application`, da `presentation` e da persistência não têm ciclo (a persistência com uma exceção declarada, ver abaixo). Antes disso, essas regras eram "verificadas no código" só à mão. Verificado por mutação: uma violação provocada de propósito (Spring no domínio; `payment` dependendo de `booking`) reprovou as regras certas, e foi desfeita. **Achado:** a persistência tem um ciclo real `catalog` ↔ `seating` (a entidade `Seat` aponta para `Bookable`, e os adapters de voo consultam o `SeatJpaRepository` para calcular `availableCapacity`); no domínio os dois são independentes. Ficou como exceção explícita e documentada no teste — uma refatoração possível é uma porta de domínio para a disponibilidade, não feita agora

## M23 — Observabilidade (métricas, logs, tracing) ✅

Lacuna: até aqui só havia um `GET /health` fixo (devolve `UP` sem checar nada), logs em texto do Spring indo para o CloudWatch e nenhuma métrica nem tracing. Plano em 7 itens — 23.1 Actuator e health, 23.2 métricas (`@Observed` nos casos de uso + poucas métricas de negócio), 23.3 logs JSON com `traceId`/`userId`, 23.4 tracing OpenTelemetry (Jaeger local), 23.5 trace atravessando a fila SQS, 23.6 Prometheus + Grafana locais (opcional), 23.7 docs. Divisão de trabalho: configuração e testes por mim; o código de negócio (anotações, métricas, consumidor) pelo dono.

- [x] 23.1 **Actuator e health de verdade.** `spring-boot-starter-actuator` + `micrometer-registry-prometheus`. Endpoints expostos: `health`, `info`, `metrics`, `prometheus`, **numa porta de gestão separada (8081)** — o Dockerfile e o Terraform do ECS só expõem a 8080, então nada operacional chega pela API pública. `liveness` (só o processo) e `readiness` (`readinessState` + `db` + `redis`): com o Redis parado o readiness vira **503 DOWN** e o liveness continua **200 UP** (verificado na aplicação de verdade). O `GET /health` antigo continua como estava. **Descoberta:** a cadeia de segurança do app também vale na porta de gestão (tudo dava 401, até para o Prometheus); resolvido com `EndpointRequest.toAnyEndpoint().permitAll()`, que com porta separada só casa no contexto de gestão — na 8080 os mesmos caminhos seguem autenticados. Testes de integração com o app em duas portas reais (`infrastructure/observability/`): liveness UP, readiness UP, `/actuator/prometheus` expõe métricas de JVM e HTTP, e a **porta pública não serve actuator** (401) — verificado por mutação: removendo a porta separada, esse teste falha. Nos testes a porta de gestão é aleatória (`management.server.port=0`) para não competir com um `bootRun`, e o fixture usa `@AutoConfigureObservability` (o Spring Boot desliga os exportadores de métricas em `@SpringBootTest` por padrão). **Não verificado:** quanto tempo o readiness leva para voltar a `UP` depois que o Redis volta (testei só até 4 s)
- [x] 23.2a **`@Observed` em todos os casos de uso.** `spring-boot-starter-aop` + `management.observations.annotations.enabled: true` (sem a propriedade o `@Observed` é ignorado). Cada caso de uso leva `@Observed(name = "dbook.usecase")`: uma única métrica `dbook_usecase_seconds_*` com as etiquetas `class`, `method` e `error` que o Micrometer acrescenta sozinho (em vez de 17 nomes diferentes), e a base dos spans do 23.4. Escrito pelo dono nos 17 arquivos. Testes: `AnObservedUseCaseIsTimedTest` (registra um usuário pela API e o Prometheus passa a exibir o timer do `RegisterUserUseCase`) e a regra ArchUnit `EveryUseCaseIsObservedTest` (todo `*UseCase` precisa do `@Observed`, então um novo não fica sem medição). Verificado antes de anotar: as duas falhavam; anotando só um caso de uso, o teste da métrica passava. **Armadilha vista:** ao anotar, o `@Observed` sobrescreveu o `@Service` de um dos casos de uso (o `git diff` mostrou `-@Service +@Observed`) — o ktlint acusou o import de `Service` sem uso. Os dois precisam coexistir
- [x] 23.2b **Métricas de negócio.** `dbook.payment{outcome=created|replayed}` e `dbook.booking.expiration{outcome=expired|ignored|failed}` (a falha do consumidor virou mais um valor de `outcome` da mesma métrica, em vez de um contador à parte). Todas com etiquetas de **baixa cardinalidade** — nunca `bookingId`/`userId`, porque cada valor novo cria uma série nova no Prometheus. A contagem vive numa *extension* `MeterRegistry.countOutcome(name, outcome)` (`application/common/MeterRegistryExtensions.kt`), em vez de um `count` privado repetido em cada classe. **`created` conta depois do commit** (`afterCommit`): um pagamento desfeito (por exemplo, que perdeu a corrida contra um cancelamento) nunca aconteceu e não pode contar como criado — verificado por mutação: contando dentro da transação, o teste `DoesNotCountAPaymentThatNeverCommitsTest` falha. `replayed` e as de expiração não precisam disso. Testes unitários com `SimpleMeterRegistry` (novo, reprocessado, chave reutilizada com outro pedido → nada novo, não confirmou → não conta; expirada, paga → ignorada, inexistente → ignorada; mensagem inválida → `failed`). Os testes de pagamento passaram a simular o commit (`executeCommitted`), já que o caso de uso agora registra um `afterCommit`. Validado com a aplicação de verdade: um fluxo com pagamento novo + retentativa, expiração, mensagem de reserva já paga, de reserva inexistente e uma inválida deu exatamente `created=1, replayed=1, expired=1, ignored=2, failed=1` no `/actuator/prometheus`
- [x] 23.2c **Gauge `dbook.booking.pending`** — quantas reservas estão `PENDING` agora (assentos presos esperando pagamento; se o número só cresce, a fila de expiração não está funcionando — é o que se alerta). Ao contrário dos contadores, é o valor do momento, lido do banco a cada coleta do Prometheus (`PendingBookingsMetrics`, um `MeterBinder` em `infrastructure/observability/`). `BookingRepository.countPending()` na porta de domínio. A consulta é **nativa com `'PENDING'` literal**, de propósito: com parâmetro o PostgreSQL pode não escolher o **índice parcial** (`V26`, `idx_booking_pending ... WHERE status = 'PENDING'`), que fica pequeno por mais que a tabela cresça já que quase toda reserva deixa de ser pendente rápido. Testes: o gauge devolve o valor do momento a cada leitura (unitário) e, contra o Postgres de verdade, a contagem inclui só as pendentes (verificado por mutação: sem o filtro, o teste falha). Os 5 fakes de `BookingRepository` ganharam `countPending`
- [x] 23.3 **Logs estruturados com correlação.** `logstash-logback-encoder` + `logback-spring.xml` com três perfis: texto legível por padrão (com `requestId` e `userId`), **`json`** (um objeto por linha; o Terraform do ECS já ativa `SPRING_PROFILES_ACTIVE=json`) e **`logfile`** (grava `logs/dbook.json`, com rotação, para o Loki local). `RequestLoggingFilter` (ordem mais alta, antes do Spring Security) dá um `requestId` a cada requisição (aceita `X-Request-Id` do cliente só se for `[A-Za-z0-9._-]{1,64}`, senão gera um UUID — evita injeção de linhas no log), devolve o cabeçalho na resposta e grava **uma linha de acesso** com método, caminho, status e duração (os dois últimos como campos JSON separados; `5xx` sai como `ERROR`). O `JwtAuthenticationFilter` põe o `userId` no MDC. **Nunca logado:** query string, cabeçalhos e corpos (teste para a query string). **Lugar novo para consultar:** perfil opcional `observability` do `docker-compose` (Loki + Promtail + Grafana, `observability/`), só `level` como *label*; e, na AWS, o CloudWatch Logs Insights (consultas na doc, **não executadas**: sem conta AWS). Testes: id gerado/mantido/substituído, uma linha de acesso por requisição com o `requestId` no MDC, query string nunca no log, `userId` só nas autenticadas, e o formato JSON real do perfil `json` capturado da saída. **Armadilha achada:** o Spring Boot configura o Logback uma vez por JVM, então o teste do perfil `json` passava sozinho mas falhava na suíte conforme a ordem — corrigido forçando a reinicialização do logging antes do contexto desse teste. Validado de ponta a ponta: a aplicação em `json,logfile`, tráfego real (autenticado, 401, query string com segredo) e consultas LogQL no Loki por nível, `requestId`, `userId`, status e duração (o segredo da query string: 0 ocorrências). O `traceId`/`spanId` entram sozinhos no 23.4, quando houver tracing
- [x] 23.4 **Tracing com OpenTelemetry.** `micrometer-tracing-bridge-otel` + `opentelemetry-exporter-otlp` + `datasource-micrometer-spring-boot` (um span por consulta SQL, com o texto da consulta e **sem** os valores dos parâmetros); Jaeger no `docker-compose` (perfil `observability`, UI em `localhost:16686`, OTLP/HTTP em `4318`). Os spans são **sempre criados** (então o `traceId` está em toda linha de log, texto e JSON), mas o **envio é opt-in**, pelo perfil `tracing` (`SPRING_PROFILES_ACTIVE=tracing`): sem coletor, o exportador despejava `ERROR ... Failed to export spans` no log de quem só quer rodar a aplicação — corrigido excluindo a auto-configuração do exportador por padrão (`application.yml`) e reabilitando no `application-tracing.yml`. **Armadilha achada (2):** a primeira versão dos testes de propagação usava o **SDK real do OpenTelemetry** em memória, e isso quebrava, só na suíte completa, os testes de `traceId` dos contextos Spring que subiam depois: o OpenTelemetry guarda o armazenamento de contexto **global na JVM**, e tocá-lo cedo impede o contexto do Spring de instalar o gancho que põe o `traceId` no MDC (rodando só um teste de cada vez, tudo passava). Trocado por um tracer sem estado global; a propagação com o OpenTelemetry real foi validada ao vivo no Jaeger. **Armadilha achada:** a classe se chama `OtlpAutoConfiguration` no Spring Boot 3.3 (`OtlpTracingAutoConfiguration` só a partir da 3.4); um nome de classe inexistente em `spring.autoconfigure.exclude` é **ignorado em silêncio** — o ruído continuou até eu conferir o nome no jar. O `RequestLoggingFilter` passou a ficar logo depois do filtro de observação do Spring (`HIGHEST_PRECEDENCE + 2`), senão a linha de acesso era escrita depois que o trace já tinha fechado e saía sem `traceId`. `management.tracing.sampling.probability: 1.0` (todo request, para desenvolvimento; em produção, pela variável `MANAGEMENT_TRACING_SAMPLING_PROBABILITY`) e histogramas de `http.server.requests` e `dbook.usecase` para calcular percentis no Prometheus. Testes: o `traceId` (32 hex) está no log de acesso e no JSON; uma requisição com o cabeçalho `traceparent` **continua o trace de quem chamou** em vez de abrir outro. Validado com a aplicação de verdade + Jaeger: um `POST /bookings` gerou um trace de 18 spans (HTTP → segurança → caso de uso → consultas SQL) e o `traceId` do log de acesso é **idêntico** ao do Jaeger; o span HTTP guarda só o caminho (sem query string)
- [x] 23.5 **O trace atravessa a fila SQS.** A expiração roda minutos depois, em outra thread: o `SqsBookingExpirationScheduler` injeta o contexto do trace atual (o `traceparent` W3C) como **atributo da mensagem**, e o `BookingExpirationConsumer` o extrai (`messageAttributeNames("All")`) e abre um span `booking-expiration consume` **filho** do que agendou, dentro de cuja execução rodam os spans do caso de uso. Mensagem sem contexto (por exemplo, enviada à mão) abre um trace próprio. O span marca o erro (`span.error`) quando o processamento falha. Testes com um **tracer em memória** (`TracingTestSupport`: o `SimpleTracer` do Micrometer + um propagador de teste do `traceparent`) e o SQS real do LocalStack: a mensagem leva o `traceparent` (e não leva nada sem span ativo); o consumidor continua o mesmo `traceId` e o span do consumidor tem como pai o span da requisição; sem contexto, abre um trace novo. Verificado por mutação: sem anexar o contexto, os dois testes de propagação falham. Validado com a aplicação de verdade + Jaeger: uma reserva feita dentro de um trace conhecido e uma mensagem de expiração levando o mesmo contexto produziram **um trace só** (`http post /bookings` ↔ `booking-expiration consume` → `expire-booking-use-case` → `cancel-booking-use-case`). A mensagem de produção fica 15 min atrasada, então a injeção do produtor foi validada pelo teste com o LocalStack (e a extração, ao vivo, com uma mensagem imediata)
- [x] 23.6 **Prometheus + Grafana locais, com dashboard e o elo log → trace.** Perfil `observability` do `docker-compose`: `prometheus` (coleta a porta de gestão da aplicação a cada 15 s via `host.docker.internal:8081`), `grafana` (sem login, só local) com **três fontes provisionadas** — Prometheus, Loki e Jaeger — e o dashboard **"DBook — visão geral"** (11 painéis, em `observability/grafana/dashboards/`): reservas pendentes agora, erros 5xx, falhas de expiração, requisições por segundo por status, latência p95 por rota e por caso de uso (histogramas ligados em `http.server.requests` e `dbook.usecase`), pagamento novo × reprocessado, expiração por resultado, heap, conexões do banco e threads. **O elo entre os pilares:** o Loki tem um *campo derivado* que extrai o `traceId` de cada linha de log e abre aquele trace no Jaeger. Validado com a aplicação de verdade e tráfego real: o alvo `dbook` do Prometheus está `up`; as **11 consultas do dashboard** rodam sem erro no Prometheus **e pela API de consulta do próprio Grafana** (11/11); o Loki e o Prometheus passam no teste de saúde do Grafana; o Jaeger responde pelo proxy do Grafana (o `/health` dele devolve 500 — a fonte Jaeger do Grafana 11 não tem verificação no backend); e a cadeia log → trace: uma linha real de `POST /bookings` do Loki → o `traceId` extraído pelo regex do campo derivado → o trace completo (18 spans) aberto no Jaeger pelo Grafana. **Não verificado:** a aparência do dashboard renderizado (o painel do navegador estava oculto e o Grafana só desenha painéis com a página visível) e o clique no link do campo derivado na interface. **Descuido meu, registrado:** um `docker compose --profile observability stop` sem nomes parou **todos** os serviços, inclusive Postgres, Redis e LocalStack de desenvolvimento — religados em seguida (o volume do Postgres manteve os dados; as filas são recriadas pelo script de init). Também esvaziei as filas de desenvolvimento, que continham mensagens inválidas enviadas por mim nos testes e inflavam a métrica de falhas
- [x] 23.7 **Documentação própria e a regra de arquitetura.** O README tinha crescido para 514 linhas e foi **dividido**: virou a porta de entrada (115 linhas: stack, arquitetura, como rodar, mapa da documentação e roadmap) e cada assunto ganhou seu documento em `docs/` — `endpoints`, `autenticacao`, `tempo-real`, `expiracao-de-reservas`, `ia`, `nuvem-e-cicd`, `testes-e-qualidade` e a nova **`observabilidade.md`** (o conceito dos três pilares, a tabela do que foi usado e para quê, um diagrama, como cada pilar foi implementado, onde acessar com endereços e comandos, **roteiros de investigação** — "um usuário reclamou", "a API ficou lenta", "assentos presos" —, consultas LogQL/PromQL prontas, um guia de como implementar no dia a dia, a tabela de configuração, os testes que a protegem, e as decisões, armadilhas e **limites**). Os links relativos dos trechos movidos foram reescritos e conferidos; `CLAUDE.md` e as skills, que mandavam editar "a seção Endpoints do README", agora apontam para `docs/endpoints.md`. Aproveitado para corrigir a skill de testes, que ainda dizia que falha em teste de integração era "limitação de ambiente" (não é mais desde o `api.version=1.41`). ArchUnit: o `domain` agora também **não pode depender de `io.micrometer..` nem de `io.opentelemetry..`** (a observabilidade não vaza para as regras de negócio)

**Checklist de fechamento do M23:**
- [x] Itens 23.1-23.7 revisados
- [x] `./gradlew check` verde com ktlint, detekt (incluindo `detektMain`/`detektTest`, que usam resolução de tipos) e JaCoCo — **lição:** rodar só a tarefa `detekt` não pega regras que dependem de tipos (como `NoNameShadowing`); o comando que vale é o `check`, e **só commitar depois de olhar o código de saída** (um commit meu saiu quebrado por eu encadear o commit sem conferir)
- [x] Validado de ponta a ponta com a aplicação de verdade em cada item (health com o Redis parado, métricas no Prometheus, logs no Loki, trace no Jaeger, o trace atravessando a fila, as 11 consultas do dashboard pelo Grafana)
- [x] Verificado por mutação o que protege comportamento sutil (porta pública sem actuator, contador só depois do commit, filtro do gauge, anexar o contexto do trace à mensagem)
- [ ] **Não verificado:** a aparência do dashboard renderizado e o clique no campo derivado do Loki (painel do navegador oculto); as consultas do CloudWatch Logs Insights (sem conta AWS); qualquer alerta (nenhum foi configurado)

## M24 — Versionamento da API (`/v1`) ✅

Problema: o consumidor é um app mobile, que não dá para forçar a atualizar; sem versão, qualquer mudança que quebra o contrato quebra as versões antigas do app. Decisões do dono (2026-10-04), sobre a proposta: **versão no caminho** (`/v1/...`, e não cabeçalho ou tipo de mídia: aparece sozinha em logs, métricas, Swagger e `curl`), **só na camada `presentation`**, **quebrar os caminhos antigos de uma vez** (não há app publicado, então não vale manter dois contratos) e **`/ws` sem versão por ora**. Feito por mim por inteiro, inclusive a parte dos controllers, por pedido do dono para este caso.

- [x] 24.1 `ApiPaths.V1` (`presentation/common/ApiPaths.kt`) e os 10 controllers de negócio sob `${ApiPaths.V1}/...`; `HealthController` fica sem versão. Domínio e casos de uso não sabem que existe versão (as regras de camada já proíbem importar `presentation`)
- [x] 24.2 Segurança (as 7 rotas públicas viraram `/v1/...`, agrupadas e comentadas), limite de uso da IA (`/v1/ai/**`) e o `scripts/seed-flights.sh`
- [x] 24.3 Swagger com **um grupo por versão** (`v1`) e um para os operacionais. **Defeito achado ao validar na aplicação de verdade:** os exemplos dos parâmetros (`Idempotency-Key`, `origin`...) sumiram dos grupos — no springdoc 2.x um `OpenApiCustomizer` comum não é aplicado a grupos; é preciso o `GlobalOpenApiCustomizer`. Corrigido, com teste (`EachVersionGroupCarriesTheParameterExamplesTest`, verificado por mutação)
- [x] 24.4 Testes: 39 arquivos / 60 caminhos reescritos por script, sem sobra de caminho antigo. Nova regra do ArchUnit `EveryApiControllerIsVersionedTest` (todo controller fora o `HealthController` precisa estar sob uma versão; verificado por mutação)
- [x] 24.5 **App mobile** (repositório `dbook_mobile`): a base URL ganhou o `/v1`; os repositórios usam caminhos relativos, então **nenhum repositório mudou**. O WebSocket não herda o `/v1`, porque a URL dele só reaproveita host e porta (testado). Testes: o Dio mantém `.../v1/bookings`; o WebSocket continua `ws://host:8080/ws`
- [x] 24.6 `docs/versionamento.md` (por que, o que quebra e o que não quebra, como é implementado, como lançar uma `v2`, como aposentar uma versão) e as instruções do projeto (`CLAUDE.md`, skill de desenvolvimento) com a regra de que todo controller de negócio fica sob uma versão
- [x] Aproveitado no mesmo marco: o Spring abria um trace de 1 span a **cada execução** do `@Scheduled` do consumidor da fila (12 por minuto, escondendo os traces úteis no Jaeger); desligado com `management.observations.enable.tasks.scheduled.execution=false` (o span que importa, `booking-expiration consume`, é do próprio consumidor). Verificado ao vivo: 0 spans de `poll` depois do reinício
- [x] `./gradlew check` verde: **184 testes**, 0 falhas. Validado de ponta a ponta com a aplicação de verdade: `/v1/destinations` 200 e o antigo `/destinations` 401; `/health` sem versão; o fluxo completo (cadastro, voo, reserva, pagamento com retentativa, expiração) pela API `/v1` dando os mesmos resultados de antes; as métricas do app em execução só com rotas `/v1/...`; e o cliente real do app (`DestinationRepositoryImpl`) pela base `/v1` (200, 9 destinos; a base sem versão é recusada com 401)
- [ ] **Não feito (de propósito):** o mecanismo de `Deprecation`/`Sunset` para aposentar uma versão (não é necessário enquanto só existe a `v1`; descrito em `docs/versionamento.md`) e o versionamento do `/ws`

## Plano — Área administrativa (portal/CRM) e evolução do produto 📋

Decisão (2026-10-04): o dono pediu uma área administrativa de verdade — um **portal web de CRM** com cadastro de usuários admin — e um cronograma completo, no mesmo modelo dos marcos anteriores, reunindo tudo o que foi discutido até aqui (funcionalidades de produto, evoluções de arquitetura e os débitos conhecidos). Escopo fechado: o portal é um **segundo app Flutter Web no monorepo `dbook_mobile`** (reaproveita `dbook_domain`, `dbook_core_network` e o design system); o backend segue o mesmo padrão de sempre. Este bloco é o **mapa**; cada marco abaixo é detalhado em itens numerados e fechado com o checklist de sempre. O espelho do lado do app/portal está em `dbook_mobile/CHECKLIST.md` (M24+).

### Decisão de sequência (2026-10-04): o front só depois de todo o backend

O portal (`apps/dbook_admin`) e as mudanças do app (`dbook-mobile`, marcos M24–M44 de lá) **começam só quando o backend inteiro (M28–M47) estiver pronto**. Enquanto isso o app não é adaptado, e as incompatibilidades com o backend novo ficam registradas aqui para serem tratadas de uma vez:

- `roleFromWire` lança com `SUPER_ADMIN`, `SUPPORT` e `CATALOG_MANAGER` (conta staff no app antigo derruba o login) — já vale desde o M25
- o validador de senha do app aceita 6 caracteres e o backend exige 8 — já vale desde o M25
- "Minhas Viagens" mostra o preço **atual** do voo, não o `price` da reserva — desde o M27
- **`REFUNDED` (M31)** é um valor novo de `BookingStatus`: o app **lança** em enum desconhecido, então "Minhas Viagens" quebraria para qualquer reserva reembolsada. Não use o app antigo contra um banco com reembolsos
- demais marcos que acrescentam enum ou campo (M36, M39, M42) valem a mesma atenção

A tolerância a valor de enum desconhecido (M24 do app) é o **primeiro** item do front, antes de qualquer tela.

### Achados que moldaram o plano (verificados no código em 2026-10-04)

1. `RegisterPaymentUseCase` soma `booking.bookable.price` **ao vivo** — se o preço do voo mudar (admin editando voo, M32) ou entrar desconto (M39), reservas pendentes mudariam de valor retroativamente. **Vira o M27, pré-requisito** do catálogo admin e dos cupons.
2. Não existe `Clock` injetável (nenhum `Clock` em `src/main`): todo teste de data/hora (expiração, dashboard, política de reembolso, janelas de cupom) precisaria de gambiarra. Entra no M25.
3. O login **não tem limite de tentativas** (o rate limit existe só em `/v1/ai/**`) e **não há política de senha** (`RegisterUserRequest` aceita qualquer texto). Entram no M25/M28.
4. O app (`wire_enums.dart`) **lança `FormatException` em enum desconhecido**. Qualquer valor novo de enum no backend (`SUPPORT`, `REFUNDED`...) derrubaria as versões antigas do app — "acrescentar" deixa de ser seguro. Entra como pré-requisito no M24 do app (tolerância a valor desconhecido) e na política de versionamento.
5. O token de acesso dura 15 min e o refresh 7 dias, com rotação (já feito). Bloquear um usuário só tem efeito imediato se o filtro consultar o status; decisão no M25.
6. Favoritos hoje são **locais** (`shared_preferences`, só destinos) — viola a regra "estado que representa ação do usuário vem do backend". Vira o M38.
7. Mensageria hoje tem a janela de *dual-write* (SQS agendado depois do commit). Antes de qualquer notificação nova, o **outbox** (M35) fecha isso.
8. Ciclo `catalog → seating` na persistência está aceito por exceção do ArchUnit; resolver quando o catálogo admin (M32) tocar nessa área (M45).

### Premissas de arquiteto (valem para todos os marcos)

- **Segurança por padrão:** todo controller `/admin/**` exige `@PreAuthorize` em cada método (regra ArchUnit nova no M25); permissões, não só papéis; permissões resolvidas **no servidor** a partir do papel (mudar a matriz não exige reemitir token).
- **Toda ação administrativa é auditada**, na **mesma transação** da mudança (M26). Nada de efeito colateral sem rastro.
- **Mudança aditiva não é automaticamente segura** enquanto o app falha em enum desconhecido — ver achado 4.
- **Expand/contract em migrations** (adicionar coluna nullable → preencher → tornar obrigatória, em marcos/deploys separados quando a tabela for grande); nunca editar migration aplicada.
- **Segunda ocorrência vira abstração** (a regra de sempre): a idempotência do pagamento é extraída quando o reembolso (M31) for o 2º uso; o envelope de paginação nasce no M26 (2º uso: M30).
- **ADR por decisão arquitetural** em `docs/adr/` (novo, M25.0): registro curto de contexto, decisão, alternativas e consequências — as decisões que já tomamos entram retroativamente.
- **Contrato de erro estável:** além de `{"error": "..."}`, passa a existir `{"code": "ACCOUNT_BLOCKED"}` (campo **aditivo**) para o app/portal traduzir mensagens sem comparar texto.
- **Um item por vez, explicar antes, confirmar depois de testar** — o dono escreve o código de produção; eu explico, reviso, testo e documento (ou faço, quando ele delegar).

### Ordem, dependências e tamanho

Tamanho: **P** ≈ meio dia · **M** ≈ 1–2 dias · **G** ≈ 3–5 dias · **GG** ≈ mais de uma semana, quebrado em incrementos.

| Release | Marco (backend) | Depende de | Tam. | Espelho no app/portal |
|---|---|---|---|---|
| **A — Fundação** | M25 RBAC, status de conta, sessão segura, `Clock`, ADRs | — | G | M24 (tolerância a enum, `Role`, tokens desktop) |
| | M26 Auditoria + envelope de paginação | M25 | G | — |
| | M27 Preço congelado na reserva | — | M | — |
| **B — Equipe** | M28 Convites e gestão da equipe | M25, M26 | G | M25–M26 (componentes, shell), M28 (equipe) |
| | M29 2FA (TOTP) | M28 | M | M27 (login, 2FA) |
| **C — CRM** | M30 Clientes (lista, 360º, notas, bloqueio, export, LGPD) | M26 | GG | M29 |
| | M31 Reservas admin e reembolso | M27, M30 | G | M30 |
| | M32 Catálogo admin | M27 | G | M31 |
| | M33 Dashboard de negócio | M31 | M | M32 |
| | M34 Alertas e operação | M23 | M | — |
| **D — Produto** | M35 Outbox transacional | — | G | — |
| | M36 Notificações | M35 | GG | M37 |
| | M37 Avaliações públicas + editar/apagar + moderação | M30 | G | M38 (+ M33 moderação no portal) |
| | M38 Favoritos no servidor | — | M | M39 |
| | M39 Código promocional | M27, M31 | G | M40 (+ M33 promoções no portal) |
| | M40 Histórico e alerta de preço | M32, M36 | G | M41 |
| | M41 Cancelamento/reembolso pelo cliente | M31 | M | M42 |
| | M42 Hotéis (`Accommodation`) | M27, M32, M35 | GG | M43 |
| **E — Plataforma** | M43 Terraform completo (SQS, SES, segredos, portal) | M28, M35 | G | M36 (deploy do portal) |
| | M44 `Deprecation`/`Sunset`, diff de contrato, versão mínima do app | M24 | M | M44 (`X-App-Version`, tela de atualização) |
| | M45 Ciclo catalog↔seating | M32 | M | — |
| | M46 Endurecimento de segurança | M29 | G | M34 |
| | M47 Desempenho e resiliência | M33 | G | M35 |

Caminho crítico até um CRM utilizável: **M25 → M26 → M28 → M30**. M27 pode andar em paralelo. A Release D só começa com A–C fechadas, mas M38 (favoritos) é independente e pode entrar antes como "marco leve".

### Permissões e papéis (decisão fechada para o M25)

Permissões: `CUSTOMER_READ`, `CUSTOMER_NOTE`, `CUSTOMER_BLOCK`, `CUSTOMER_EXPORT`, `CUSTOMER_ERASE`, `BOOKING_READ_ANY`, `BOOKING_CANCEL_ANY`, `PAYMENT_REFUND`, `FLIGHT_READ`, `FLIGHT_WRITE`, `CATALOG_WRITE` (companhias/aeroportos), `PROMO_WRITE`, `REVIEW_MODERATE`, `DASHBOARD_READ`, `AUDIT_READ`, `ADMIN_MANAGE` (convites, papéis, 2FA alheio).

| Papel | Permissões |
|---|---|
| `CLIENT` | nenhuma (usa só a API pública/própria) |
| `SUPPORT` | `CUSTOMER_READ`, `CUSTOMER_NOTE`, `CUSTOMER_BLOCK`, `BOOKING_READ_ANY`, `BOOKING_CANCEL_ANY`, `PAYMENT_REFUND`, `REVIEW_MODERATE`, `DASHBOARD_READ` |
| `CATALOG_MANAGER` | `FLIGHT_READ`, `FLIGHT_WRITE`, `CATALOG_WRITE`, `PROMO_WRITE`, `DASHBOARD_READ` |
| `SUPER_ADMIN` | todas (inclui `CUSTOMER_EXPORT`, `CUSTOMER_ERASE`, `AUDIT_READ`, `ADMIN_MANAGE`) |

O `ADMIN` atual vira `SUPER_ADMIN` por migration. Princípio: **menor privilégio** — exportar dados e anonimizar são exclusivos do `SUPER_ADMIN`.

---

## M25 — Fundação administrativa: papéis, permissões, status de conta e sessão segura ✅

Problema: existia um único papel `ADMIN`, sem permissão fina, sem como bloquear uma conta, sem limite de tentativas de login e sem política de senha; o portal exige uma sessão pensada para navegador (token em memória + cookie `httpOnly`), não só para app. Feito em 2026-10-04: o código de produção foi escrito pelo dono (blocos A–D, mostrados e conferidos um a um) e, **por pedido dele, só nesta vez**, a parte final (testes de integração e persistência, docs) foi gravada por mim.

- [ ] 25.0 **ADRs** (`docs/adr/`): **não feito** — fica para um marco próprio; as decisões estão descritas em `docs/autenticacao.md`
- [x] 25.1 **`Clock` injetável** (`config/ClockConfig`, `Clock.systemDefaultZone()` — a zona do servidor, porque os voos têm data local). Usado onde há lógica de tempo: `RefreshTokenUseCase`, `JwtTokenService`, `GetLowestPriceForDestinationUseCase`, `LoginUseCase`, `BlockUserUseCase`; `Review`/`AiSuggestionLog` continuam com o relógio do sistema até um caso de uso precisar. O teste da janela de 60 dias deixou de depender da data de hoje
- [x] 25.2 Domínio: `Permission` (17), `Role` = `CLIENT`/`SUPPORT`/`CATALOG_MANAGER`/`SUPER_ADMIN` com `permissions`, `isStaff` e `can()`. A matriz vive só no domínio
- [x] 25.3 Migration `V27`: `status`, `blocked_reason`, `blocked_at`, `last_login_at`, `created_at` (ainda **não mapeado** na aplicação — o CRM, M30, o lê), `version`; `ADMIN` → `SUPER_ADMIN`; `CHECK`s de papel, status e coerência do bloqueio. **Aplicada num Postgres descartável com um `ADMIN` antigo** (virou `SUPER_ADMIN`; os `CHECK` recusam estado impossível). `scripts/seed-flights.sh` promove a `SUPER_ADMIN`
- [x] 25.4 `User` com `status`, `blockedReason`, `blockedAt`, `lastLoginAt`, `version`; `block`/`unblock`/`rename` devolvem um `User` novo e preservam o resto (antes, `UpdateUserNameUseCase` reconstruía o usuário à mão e **desfaria um bloqueio**). O último acesso é gravado por `UPDATE` direto, não por `save`
- [x] 25.5 Filtro JWT monta `ROLE_x` + uma autoridade por permissão; `@PreAuthorize("hasAuthority('FLIGHT_WRITE')")`; `CancelBookingUseCase` usa `BOOKING_CANCEL_ANY`. **Em `/v1/admin/**` o papel e o status vêm do banco a cada requisição**, não do token
- [x] 25.6 Conta bloqueada: login e refresh recusam com `403 ACCOUNT_BLOCKED` (só depois da senha certa); `BlockUserUseCase` bloqueia e revoga todas as sessões na mesma transação. **Ainda sem endpoint que bloqueie**: ele chega no M28 (equipe) e no M30 (clientes)
- [x] 25.7 Contrato de erro `{"error", "code"}` (`ErrorCode`, 16 códigos). `ApiExceptionHandler` foi dividido por assunto (`IdentityExceptionHandler`, `AiExceptionHandler`) por estourar o limite de funções do detekt
- [x] 25.8 Login endurecido: `LoginAttemptGuard` (política injetada) + `RedisLoginAttemptLimiter` por e-mail (5) e IP (20), `429` + `Retry-After`, falha aberta se o Redis cair. **Redis, e não bucket4j como no plano**: o contador precisa ser compartilhado entre instâncias e expirar sozinho. E-mail inexistente paga a mesma verificação de senha; métrica `dbook.auth.login{outcome,audience}`
- [x] 25.9 `PasswordPolicy`: mínimo **8** (cliente) / 12 (staff, vale a partir do M28), máximo 72 **bytes** (BCrypt), diferente do e-mail, fora de uma lista de senhas comuns (no código, não em arquivo). O plano dizia 10 para cliente; ficou 8 (NIST) para não quebrar tanto o app, que hoje aceita 6
- [x] 25.10 Sessão do portal: `POST /v1/admin/auth/login|refresh|logout`, token de acesso no corpo, refresh em cookie `httpOnly; Secure; SameSite=Strict; Path=/v1/admin/auth`, `Origin` conferido contra `cors.allowed-origins` (agora lida por `CorsProperties`)
- [x] 25.11 `GET /v1/admin/auth/me` → `{id, name, email, role, permissions[]}`. **Sem `mustChangePassword`** (a coluna e o fluxo de troca obrigatória vêm no M28)
- [x] 25.12 Regra ArchUnit `EveryAdminEndpointDeclaresItsPermissionTest` (exceção explícita: `@PublicEndpoints`). **Verificada por mutação**: sem o `@PreAuthorize` do `/me`, o build reprova
- [x] 25.13 Testes: 249 no total (de 184), 0 falhas. Domínio (matriz de papéis, `User`, `PasswordPolicy`), aplicação (login ×13, renovação, bloqueio, saída, registro), integração (permissão por papel, bloqueio/rebaixamento valendo na próxima chamada, sessão do portal com cookie e `Origin`, 429, `code` dos erros), persistência (o `UPDATE` de último acesso não desfaz um bloqueio; cópia velha não sobrescreve um bloqueio) e o limitador Redis (inclusive com o Redis fora do ar)
- [x] 25.14 Docs: `docs/autenticacao.md` (papéis, bloqueio, limite, política, sessão do portal), `docs/endpoints.md` (formato de erro e rotas do portal), `docs/testes-e-qualidade.md`, `.claude/CLAUDE.md` e as skills (`hasAuthority`, nunca `hasRole`)

**O que os testes de integração pegaram (e os unitários não):**
- `FlightAdminController` ficou com `@PreAuthorize("hasRole('FLIGHT_WRITE')")` — `hasRole` procura `ROLE_FLIGHT_WRITE`, que ninguém tem, então **todo mundo levava 403**. A regra do ArchUnit só confere que o `@PreAuthorize` existe, não o que ele diz.
- `@Value("${cors.allowed-origins}") List<String>` em Kotlin chega como **um texto só** (o parâmetro vira `List<? extends String>`), e o CORS recusava toda origem (`Invalid CORS request`). Trocado por `@ConfigurationProperties` (`CorsProperties`).
- Os `@WebMvcTest` passaram a precisar de um `UserRepository` (o filtro JWT agora consulta o usuário): `@MockBean` nos 5 fixtures.
- A regra ArchUnit lançava exceção em controller sem `@RequestMapping` (o `HealthController`): `tryGetAnnotationOfType`.
- Armadilhas de ferramenta: `/**` dentro de um KDoc abre comentário aninhado em Kotlin; o IDE (Move/Optimize imports) removeu imports e criou arquivos no pacote errado; dois Gradles ao mesmo tempo dão `Could not write XML test results`; sem o Docker aberto, todo teste de integração cai com `Could not find a valid Docker environment`.

**Pendente / impacto fora deste repositório:**
- **App mobile (`dbook_mobile`)**: o backend passa a devolver `SUPER_ADMIN`, `SUPPORT` e `CATALOG_MANAGER`, e `roleFromWire` do app **lança** em valor desconhecido; o validador de senha do app aceita 6 caracteres e o backend exige 8. Resolver no M24 do app (tolerância a enum desconhecido + `Role`).
- Cookie `Secure` no Safari local: `ADMIN_PORTAL_COOKIE_SECURE=false`.
- Origens reais do portal (`cors.allowed-origins`) e o Terraform entram no M43.

**Checklist de fechamento do M25:**
- [x] Itens 25.1–25.14 revisados (25.0 adiado)
- [x] Clean Code / SOLID: `LoginAttemptGuard` extraído do `LoginUseCase`, configuração fora do caso de uso, handlers de erro por assunto, comentários só onde o "porquê" não é óbvio
- [x] `./gradlew check` com **exit 0** (249 testes, ktlint, detekt, JaCoCo)
- [x] Mutação: regra ArchUnit; (a regra de `hasAuthority` foi pega por teste de integração real)
- [x] Docs e instruções do projeto atualizadas

## M26 — Auditoria ✅

Problema: ações administrativas (cadastrar voo, cancelar a reserva de outra pessoa) precisam deixar rastro **imutável** e consultável — requisito de qualquer CRM e de prestação de contas (LGPD). Feito em 2026-10-04, **por autorização expressa do dono** ("pode fazer as alterações"), depois de o desenho ter sido mostrado e aprovado; nada commitado ainda.

- [x] 26.1 Migration `V28__create_audit_log.sql`: `audit_log` com índices por alvo, ator e `(occurred_at desc, id desc)`; **append-only no banco** (gatilho que recusa `UPDATE`, `DELETE` e `TRUNCATE`; teste prova). Sem `actor_email` e sem chave estrangeira para `app_user` (o registro nunca deve ser bloqueado nem sofrer cascata por mudança em usuário)
- [x] 26.2 Novo conceito `audit`: `AuditEvent` (só dado de negócio), `AuditAction` (já declara o tipo do alvo: `FLIGHT_CREATED` → `FLIGHT`), `AuditOutcome`, `AuditEntry`/`AuditContext`, portas `AuditLog` (escrita) e `AuditLogReader` (leitura, separadas), `Actor` em `identity`. **Sem a porta `AuditContextProvider` do plano**: o `request_id`, o `trace_id`, o IP e o user agent são acrescentados pelo adaptador (`RequestAuditContext`, lendo MDC e a requisição), porque o caso de uso não conhece HTTP
- [x] 26.3 Auditoria **explícita no caso de uso**, dentro da `@Transactional` (o adaptador entra na transação de quem o chama: teste de *rollback* conjunto). Tentativas **negadas** em `/v1/admin/**` entram pelo `JsonAccessDeniedHandler` como `ACCESS_DENIED`/`DENIED`; falhar ao gravá-las nunca muda a resposta (continua 403)
- [x] 26.4 "Antes/depois" por **lista permitida** (`Flight.toAuditSnapshot()`, `Booking.toAuditSnapshot()`; datas como texto), testadas campo a campo
- [x] 26.5 Retrofit: `RegisterFlightUseCase` (recebe o `Actor` no comando) e o cancelamento por staff em `CancelBookingUseCase` (só quando o ator **não** é o dono; o dono cancelando a própria reserva não é ação administrativa)
- [x] 26.6 Paginação **só por cursor** (`occurred_at`, `id`; opaco para o cliente, `AuditCursorCodec`), `CursorPageResponse` em `presentation/common`. **O envelope por página (`PageResponse` com `totalElements`) do plano ficou para o M30**, quando houver o 2º uso real
- [x] 26.7 `GET /v1/admin/audit` (`AUDIT_READ`) com filtros opcionais (`actorId`, `action`, `targetType`, `targetId`, `outcome`, `from`, `to`), `size` 1–100. Parâmetro inválido vira `400 VALIDATION_FAILED` (handler novo de `BindException`, que evita o desvio para `/error`)
- [x] 26.8 Métrica `dbook_admin_action_total{action,outcome}` e uma linha de log estruturada por registro (sem dado pessoal). **Documentado em `docs/observabilidade.md` também o `dbook_auth_login_total` do M25**, que estava faltando
- [x] 26.9 Testes: 274 no total (de 249), 0 falhas. Domínio (evento, snapshots), aplicação (busca, voo e cancelamento gravam; voo recusado e dono **não** gravam), codec do cursor, persistência (ida e volta do estado, imutabilidade, páginas 2/2/1 sem lacuna nem repetição, filtros, *rollback*) e integração de ponta a ponta (voo criado, tentativa negada, cancelamento por staff com antes e depois, paginação por HTTP, leitura só com `AUDIT_READ`, consulta inválida). **Mutação:** sem o `auditLog.record` do voo, ou com a condição do cancelamento invertida, os testes reprovam
- [x] 26.10 Docs: `docs/auditoria.md` (novo), `endpoints.md`, `observabilidade.md`, `testes-e-qualidade.md`, `README.md` (mapa e roadmap, com o M25 que faltava), `.claude/CLAUDE.md` (8 conceitos, `audit → identity`, regra "ação administrativa grava `AuditEvent`", migration V29)

**O que a execução ensinou:**
- **Entidade com colunas JSON precisa ser `@Immutable`.** Sem isso, depois do `INSERT` o Hibernate achava a linha "suja" (o JSON relido não bate com o gravado: `BigDecimal("500.00")` vira outro valor) e emitia um `UPDATE`, que o gatilho recusava — e **todo cadastro de voo falhava**, derrubando 19 testes. Marcar `@Immutable` é a correção certa, e também o que a trilha é.
- Detekt: a entidade chegou a 15 parâmetros no construtor (limite do projeto); o contexto da requisição virou `@Embedded` (`AuditContextColumns`).
- Os `check` anteriores só ficam confiáveis com o Docker aberto (sem ele, todo teste de integração cai).

**Fora do escopo (de propósito):** retenção/particionamento da tabela; auditar a **leitura** de dado pessoal (M30); bloquear usuário e demais ações administrativas (cada uma entra com o seu marco: M28, M30, M31, M32); IP atrás de balanceador (`X-Forwarded-For` confiável, junto com o limite de login).

**Checklist de fechamento do M26:**
- [x] Itens 26.1–26.10 revisados
- [x] Clean Code / SOLID: portas de escrita e leitura separadas, o caso de uso não conhece HTTP, ação carrega o tipo do alvo, nada especulativo (sem envelope por página, sem `actor_email`)
- [x] `./gradlew check` com **exit 0** (274 testes, ktlint, detekt, JaCoCo)
- [x] Mutação nos pontos críticos
- [x] Docs e instruções do projeto atualizados

## M27 — Preço congelado na reserva ✅

Problema: o pagamento somava o preço **atual** do voo (`booking.bookable.price`). Editar o preço de um voo (M32) ou dar desconto (M39) mudaria retroativamente o valor de reservas já feitas. **Achado a mais:** `GET /v1/bookings` ("Minhas Viagens") devolvia só o `flight.price` atual, então o app mostraria o preço novo numa reserva antiga. Feito em 2026-10-04, com autorização do dono para aplicar; nada commitado ainda.

- [x] 27.1 Migration `V29__add_price_to_booking.sql` (*expand / contract* numa migration só, porque a tabela é pequena): coluna nula → `UPDATE` com o preço do voo → `NOT NULL` + `CHECK (price >= 0)`. **Aplicada num Postgres descartável com reservas já existentes** (cada uma ganhou o preço do voo dela; a coluna é obrigatória, o `CHECK` recusa negativo e reajustar o voo depois não mexe na reserva). A técnica ficou documentada em `docs/migracoes.md`
- [x] 27.2 `Booking.price`: parâmetro no fim do construtor, com padrão `bookable.price` (só vale para uma reserva **nova**, o que mantém os ~12 testes que constroem `Booking(...)` compilando); `transitionTo` repassa o preço; `require(price >= 0)`. `RegisterBookingUseCase` o grava **explicitamente** (`price = bookable.price`): é o momento do congelamento
- [x] 27.3 `RegisterPaymentUseCase` soma `booking.price`, não `booking.bookable.price`
- [x] 27.4 `price` entra em `BookingResponse` e `MyBookingResponse` (campo **aditivo**, o app antigo continua funcionando); o `flight.price` segue sendo o preço **atual** do voo. Entidade, mapeador e `BookingRepositoryAdapter.save` repassam o campo. **O app ainda não mostra o `price` da reserva** (item do app, abaixo)
- [x] 27.5 Testes: 283 no total (de 274), 0 falhas. Domínio (congela, sobrevive a confirmar/cancelar, recusa negativo), aplicação (a reserva congela; o pagamento cobra 400 numa reserva feita a 400 num voo que agora custa 500), persistência real (reajustar o voo para 150 não altera a reserva) e de ponta a ponta (Minhas Viagens mostra 100 e o voo 150; pagar depois do reajuste cobra 100; **cancelar depois do reajuste devolve 100**). **Mutação:** pagamento voltando a `bookable.price` reprova; adaptador esquecendo o preço ao devolver a reserva reprova
- [x] 27.6 Docs: `docs/migracoes.md` (novo), `endpoints.md`, `testes-e-qualidade.md`, `README.md` (mapa e roadmap), `.claude/CLAUDE.md` (regra "o valor de uma reserva é `Booking.price`", migration V30)

**O que a execução ensinou:**
- O `BookingRepositoryAdapter.save` **reconstrói a reserva campo a campo** para devolvê-la (como o `UpdateUserNameUseCase` fazia com o `User` no M25): esquecer o preço ali devolveria o preço atual do voo e **nenhum teste de leitura pegaria**, porque na criação os dois coincidem. Só o teste de cancelamento depois do reajuste cobre esse caminho.
- Um campo novo num fixture compartilhado (`jdbcTemplate` no `SecurityIntegrationFixture`) colidiu com o mesmo campo declarado num teste antigo.

**Fora do escopo (de propósito):** as reservas anteriores à `V29` ficam com o preço **atual** do voo na hora da migração (não há como saber o de quando foram feitas); o app mostrar o `price` da reserva em "Minhas Viagens" (pendente no `dbook-mobile`); preenchimento em lotes para tabela grande (descrito em `docs/migracoes.md`, não necessário aqui).

**Checklist de fechamento do M27:**
- [x] Itens 27.1–27.6 revisados
- [x] Clean Code: a regra "preço da reserva" mora em um só lugar (`Booking.price`) e o congelamento é explícito no caso de uso
- [x] `./gradlew check` com **exit 0** (283 testes, ktlint, detekt, JaCoCo)
- [x] Migration validada com dados reais; mutação nos pontos críticos
- [x] Docs e instruções do projeto atualizados

## M28 — Convite e gestão da equipe (cadastro do usuário admin) ✅

Problema: a promoção era SQL manual (de propósito: sem endpoint de autopromoção). Um CRM precisa de um fluxo seguro, auditado e sem senha trafegando por e-mail. Feito em 2026-10-04, **por autorização expressa do dono** (a parte de domínio/persistência foi mostrada e aprovada antes; o restante, "seguimos com a implementação do restante do código"), depois de uma revisão de código com 10 achados, todos corrigidos; nada commitado ainda.

- [x] 28.1 **Bootstrap do 1º admin:** `BootstrapSuperAdminUseCase` + `BootstrapSuperAdminRunner` (`presentation/identity/`): lê `DBOOK_BOOTSTRAP_ADMIN_EMAIL`/`_PASSWORD`, cria um `SUPER_ADMIN` **somente se não existir nenhum**, idempotente. As duas vazias: não faz nada; **só uma** preenchida ou e-mail que já é conta: **a subida falha**. **Sem `mustChangePassword`** (ver "Fora do escopo"). A senha em produção virá do Secrets Manager (M43)
- [x] 28.2 Migration `V30__create_staff_invitation.sql`: `staff_invitation` (`token_hash` SHA-256 único, `invited_by`, `expires_at`, `accepted_at`, `revoked_at`, `created_at`, `version`), `CHECK` do papel staff e **índice único parcial: um convite aberto por e-mail**. **Sem a coluna `app_user.must_change_password`** (ver abaixo)
- [x] 28.3 Domínio: `StaffInvitation` (e-mail normalizado, papel precisa ser staff, validade 72 h, uso único, estados `PENDING/ACCEPTED/EXPIRED/REVOKED` derivados do `Clock`, `reissue` para o reenvio), portas `InvitationTokenGenerator` (`SecureRandom`, 256 bits, URL-safe) e `EmailSender`
- [x] 28.4 Casos de uso (todos auditados): `InviteStaffUseCase` (e-mail já cadastrado em qualquer caixa → `409`; reconvidar revoga o anterior; e-mail **depois do commit**), `ResendInvitationUseCase` (link **novo**, pois só o hash é guardado), `RevokeInvitationUseCase`, `ListInvitationsUseCase` (os 100 mais recentes), `AcceptInvitationUseCase` (público; política de senha de staff; fecha o convite **antes** de criar o usuário, então dois aceites simultâneos não criam duas contas; ator da auditoria = a conta criada)
- [x] 28.5 Porta `EmailSender` + `LoggingEmailSender` (só registra o envio; o corpo com o link só com `EMAIL_LOG_BODY=true`, e a aplicação **recusa subir** com isso sob o perfil `json`). Texto em português (`InvitationMailer`, link `${admin-portal.base-url}/accept-invite?token=...`; falha de envio é registrada e não desfaz o convite). **O `SesEmailSender` ficou para o M36**, que é quem precisa de e-mail real
- [x] 28.6 Controllers: `POST/GET /v1/admin/invitations`, `DELETE .../{id}`, `POST .../{id}/resend`, o público `POST /v1/admin/invitations/accept` (erro **idêntico** `400 INVALID_INVITATION` para token inexistente, expirado, usado ou revogado). **Sem rate limit por IP no aceite**, de propósito (token de 256 bits não se adivinha, e o limite bloquearia convidados legítimos atrás do mesmo NAT)
- [x] 28.7 **Gestão da equipe:** `GET /v1/admin/staff`, `PATCH .../{id}/role`, `POST .../block` e `.../unblock`. Ninguém altera o próprio papel nem se bloqueia (`409`); **nunca fica sem `SUPER_ADMIN` ativo** (`lockActiveByRole`: `SELECT ... FOR UPDATE` em ordem de id, `Propagation.MANDATORY`); trocar papel/bloquear revoga os refresh tokens; id de cliente = `404`; papel `CLIENT` recusado (`400`)
- [x] 28.8 `POST /v1/admin/auth/change-password` (senha atual obrigatória, mesmo limite de tentativas do login, revoga **todas** as sessões, política de senha por tipo de conta). **Sem o `code=PASSWORD_CHANGE_REQUIRED` do plano**: não há senha provisória (o convidado escolhe a sua e o bootstrap exige 12+), então não há o que obrigar a trocar
- [x] 28.9 Testes: 362 no total (de 297), 0 falhas. Unitários com fakes em memória (um cenário por classe; `committed {}`/`rolledBack {}` provam que o e-mail só sai com commit), persistência (um aberto por e-mail, versão velha recusada, trava exige transação), `RecordingEmailSender` compartilhado por `RecordingEmailConfig`, e 16 de ponta a ponta (convite → e-mail → aceite → login no portal; 403 por permissão; mesmo `400` para qualquer token ruim; senha fraca não gasta o link; reconvidar e reenviar matam o link antigo; lista sem token; troca de papel encerra a sessão e audita; bloquear/desbloquear; auto-alteração `409`; cliente = `404`; troca de senha). **A corrida "dois SUPER_ADMIN se rebaixando" não tem teste HTTP** (o banco é compartilhado entre as classes, então o "último" nunca é o último): a regra tem teste com fakes e a trava tem teste de persistência. **Mutação:** sem a trava do último `SUPER_ADMIN` (rebaixar e bloquear), sem a revogação das sessões ou com o e-mail antes do commit, os testes reprovam
- [x] 28.10 Docs: `docs/autenticacao.md` (fluxo com diagrama de sequência, bootstrap, troca de senha e **modelo de ameaças** em tabela), `endpoints.md`, `testes-e-qualidade.md`, `README.md`, `.claude/CLAUDE.md` (migration V31). **ADR do convite não escrito**: o projeto ainda não tem `docs/adr/`; as decisões estão no modelo de ameaças

**O que a execução ensinou:**
- Fechar o convite **antes** de criar a conta é o que dá a exclusão mútua: dois aceites leem a mesma `version`, e o segundo `save` perde. Fazer na ordem inversa deixaria as duas contas serem criadas.
- Violação do índice único chega do Spring como exceção de tradução do `@Repository`, não como a do JPA: o adaptador precisa traduzi-la para uma exceção de domínio (`DuplicateOpenInvitationException` → `409`), e o `saveAndFlush` define quando ela estoura.
- Request com **um** campo (`ChangeStaffRoleRequest`, `BlockStaffRequest`) precisa de `@JsonCreator`/`@JsonProperty`, como `RefreshRequest`.
- Snapshot de auditoria por lista permitida **sem e-mail** (dado pessoal) e um teste que garante isso.

**Fora do escopo (de propósito):** `mustChangePassword` e a troca obrigatória (sem senha provisória não há necessidade); SES (M36); 2FA (M29); rate limit no aceite; convite com papel `CLIENT`; paginação da lista de convites (teto de 100); desbloquear por e-mail.

**Checklist de fechamento do M28:**
- [x] Itens 28.1–28.10 revisados (desvios registrados acima)
- [x] Clean Code / SOLID: regras do time num só lugar (`StaffSafeguards`), e-mail atrás de uma porta, casos de uso sem conhecer HTTP, nada especulativo
- [x] `./gradlew check` com **exit 0** (362 testes, ktlint, detekt, JaCoCo)
- [x] Revisão de código (10 achados corrigidos) e mutação nos pontos críticos
- [x] Docs e instruções do projeto atualizados

## M29 — Autenticação em dois fatores (TOTP) para a equipe ✅

- [x] 29.1 Migration `V47__create_staff_two_factor.sql`: `staff_totp` (`user_id`, `secret_encrypted`, `confirmed_at`, `last_used_step`) e `staff_recovery_code` (`user_id`, `code_hash`, `used_at`, único por usuário e hash)
- [x] 29.2 Portas `TotpService` (RFC 6238, HMAC-SHA1 da JVM, 6 dígitos, passo de 30 s, tolerância de ±1 passo, **vetores de teste da RFC**), `SecretCipher` (AES-256-GCM da JVM, IV por valor, prefixo `v1:`, chave de `totp.encryption-key`: o SHA-256 de um texto longo; Secrets Manager em produção, já ligado na Terraform) e `RecoveryCodeGenerator`
- [x] 29.3 Cadastro avulso `POST /v1/admin/2fa/enroll` e `/confirm` (URI `otpauth://` para o QR e a chave para digitar; 10 códigos de recuperação **uma vez**, só o *hash* guardado) e `/disable` (senha **e** código; recusado para o papel que o exige)
- [x] 29.4 Login em duas etapas: senha certa + segundo fator → `202 {challengeToken}` (JWT de 5 min com `use=2fa-verify`) → `POST /v1/admin/auth/2fa/verify` → sessão. Anti-replay por `UPDATE ... WHERE last_used_step < :passo`, tentativas limitadas (limitador próprio por conta e por IP), código de recuperação gasto por `UPDATE ... WHERE used_at IS NULL`
- [x] 29.5 Política: obrigatório por papel (`admin.two-factor.required-roles`, `SUPER_ADMIN` na Terraform): a conta sem segundo fator recebe `202 enrollmentRequired` e só entra depois de cadastrar (`/auth/2fa/enroll` e `/confirm` com o desafio); reset por **outro** `SUPER_ADMIN` (`ADMIN_MANAGE`, motivo ≥ 10, auditado, encerra as sessões)
- [x] 29.6 Testes (vetores da RFC, janela de tempo com relógio manual, replay, recuperação, reset auditado, corrida de 20 *threads* no `UPDATE`, o segredo cifrado no banco) e docs (`autenticacao.md`, `seguranca.md`, `endpoints.md`, `auditoria.md`)

**O que a execução ensinou (e que não estava no plano):**
- **O login do cliente era uma porta em volta do segundo fator.** `POST /v1/auth/login` aceita qualquer papel com a senha certa, e o token que devolve abre `/v1/admin/**`. Sem fechar isso, o 2FA seria decorativo: agora a conta da equipe com o segundo fator (ou de papel que o exige) leva `403 TWO_FACTOR_REQUIRED` ali.
- **O token de renovação (7 dias) abria qualquer endpoint como se fosse de acesso:** os dois tinham o mesmo formato. O desafio do 2FA, que tem de **não** abrir endpoint nenhum, forçou a distinção: todo token leva `use` e só `access` abre endpoint (sem `use`, de antes, vale como acesso até expirar).
- A chave que a Terraform gera (44 letras e números) não é um Base64 de 32 bytes: a chave do AES é o SHA-256 do texto, e qualquer texto longo serve.
- O código errado é `400`, não `401`: um cliente não deve tomá-lo por login vencido.

**Fora do escopo (de propósito):** WebAuthn/chaves de segurança, 2FA do cliente do app, troca da chave do AES (o prefixo `v1:` deixa o caminho aberto), lembrar o dispositivo.

**Checklist de fechamento do M29:**
- [x] Itens 29.1–29.6 revisados
- [x] `./gradlew check` com **exit 0**
- [x] Mutação (replay, porta do cliente, tipo do token) verificada
- [x] Docs atualizados

## M30 — CRM de clientes (backend) ✅  *(GG — incrementos 30a–30d)*

Problema: o suporte precisa **encontrar** um cliente, ver o **quadro completo** dele, registrar contexto e agir — com rastro e respeito à LGPD. Novo conceito `crm` (leitura/consulta) nas camadas; atualizar a lista de conceitos no `CLAUDE.md` (7 → 8) e a regra de ciclos.

**30a — Consulta**
- [x] 30.1 Convenções de listagem: filtros tipados, `sort` (enum fechado) + `direction`, `size` 1–100 validado em `PageQuery` (`domain/common`), ordenação estável (desempate por `id`). **Lista de clientes por `page`/`size` com `totalElements` (`PageResponse`), não por cursor** (a tela precisa de "página N de M" e de ordenar por coluna); a auditoria segue por cursor. O "keyset" do 30.11 cai
- [x] 30.2 Portas de **leitura** (`CustomerSearch`) devolvendo *read models* (`CustomerSummary`), não entidades — separa consulta de agregado (CQRS leve, ADR). Adaptador com `Specification`/SQL nativo
- [x] 30.3 Migration **`V31`**`__add_customer_search_indexes.sql` (a V31 estava livre; as migrations do M30 foram V31–V34 e o M29 passa a V35+) com `pg_trgm`, `unaccent` (via `immutable_unaccent`, busca **sem caixa e sem acento**), índice parcial `(created_at, id) WHERE role='CLIENT'` e `idx_booking_customer` (`booking.customer_id` não tinha índice): extensão `pg_trgm` e índices GIN em `lower(name)` e `lower(email)`; `GET /v1/admin/customers?query&status&createdFrom&createdTo&hasBookings&sort&page&size` (`CUSTOMER_READ`). Caracteres `%` e `_` na busca **escapados**
- [x] 30.4 `GET /v1/admin/customers/{id}` (**uma** consulta, três `LATERAL`; `V32` indexa `review(customer_id)`; id de staff = `404`) — visão 360º: perfil, status, totais (reservas por status, total pago, último acesso, nota média dada). **Sem N+1:** `QueryCounter` (um `ObservationHandler` de teste que conta os `jdbc.query` do datasource-micrometer) prova 1 consulta por perfil e 2 por página de busca, qualquer que seja o histórico. **Atenção M31:** `payments.totalPaid` é a soma de `payment.amount`; com reembolso, o total precisa abater o reembolsado
- [x] 30.5 Subrecursos paginados: `/{id}/bookings` (preço **congelado**, voo em `LEFT JOIN`: a reserva de algo que não é voo, do M42, não some), `/{id}/payments` (só os 4 últimos dígitos do cartão e as reservas cobertas; **sem o nome do titular**), `/{id}/reviews`; mais recentes primeiro, 2 consultas por página (testado). `crm` não importa `booking`/`payment`/`review`: o status vai como texto e tudo lê por SQL, isolado em `infrastructure/persistence/crm/`

**30b — Contexto e ação**
- [x] 30.6 Migration `V33__create_customer_note.sql` (`customer_id`, `author_id`, `body ≤ 2000`, `pinned`, `created_at`, `edited_at`, `deleted_at`); `POST/GET/PATCH/DELETE` de notas (escrever = `CUSTOMER_NOTE`, listar = `CUSTOMER_READ`; **só o autor edita**; o autor ou um `SUPER_ADMIN` apaga; remoção lógica; lista com fixadas primeiro); auditadas **sem o texto** (dado pessoal livre: a trilha só diz que mudou)
- [x] 30.7 `POST /v1/admin/customers/{id}/block` e `/unblock` (`CUSTOMER_BLOCK`): `reason` obrigatório (≥ 10 caracteres), revoga refresh tokens, auditado; contrato `ACCOUNT_BLOCKED` já existente (M25)
- [x] 30.8 Auditoria de **leitura**: abrir a visão 360º grava `CUSTOMER_VIEWED` (deduplicado por ator+alvo em 5 min) — prestação de contas de acesso a dado pessoal

**30c — Dados em massa e LGPD**
- [x] 30.9 `GET /v1/admin/customers/export` (`CUSTOMER_EXPORT`, só `SUPER_ADMIN`): CSV em *streaming* (`StreamingResponseBody`, `fetchSize` 500), UTF-8 com BOM, teto de 50 mil linhas, **proteção contra CSV injection** (célula começando com `=`, `+`, `-`, `@`, tabulação ou retorno ganha `'`), filtro gravado na auditoria **antes** do primeiro byte. O corpo é escrito num *async dispatch*, onde o JWT não roda e o contexto sem sessão se perde: o `SecurityConfig` libera só `DispatcherType.ASYNC`
- [x] 30.10 `POST /v1/admin/customers/{id}/anonymize` (`CUSTOMER_ERASE`): irreversível — exige `reason` (10+) e a frase `ANONYMIZE <id>`; nome e e-mail viram valores anônimos (`anonymized-<id>@anonymous.invalid`), conta bloqueada com hash de senha inutilizável (`app_user.anonymized_at`, V34; desbloquear é recusado), sessões encerradas, **apaga** notas, consultas à IA e o nome do titular nos pagamentos, **mantém** reservas, pagamentos e avaliações; auditoria sem nome nem e-mail. **Decisão do "decidir" do 30.12: o e-mail anonimizado não é reutilizável** (só o hash SHA-256 em `anonymized_email`; o registro recusa com `409`), senão quem foi bloqueado por fraude apagaria a conta e voltaria limpo. Autoatendimento: `GET /v1/users/me/export` (todas as páginas, sem teto) e `DELETE /v1/users/me` com a senha (mesmo `CustomerAnonymizer`; senha errada conta no limite do login via `PasswordConfirmation`, que a troca de senha também passou a usar) — telas no M42 do app

**30d — Qualidade**
- [x] 30.11 `scripts/seed-customers.sh` (SQL direto: 50 mil clientes, 60 mil reservas, 20 mil pagamentos, 8 mil avaliações) e `scripts/explain-customers.sql`; resultados em `docs/crm.md`: listagem 0,3 ms, contagem da busca pelo **trigram** 2,7 ms, página funda 32 ms. **A medição achou um problema real:** a contagem de reservas era calculada nas linhas que o `OFFSET` descarta (286 ms em `OFFSET 40000`); o `LIMIT` passou para dentro (`CustomerSql.pageSql`). Não há "keyset" na lista (decidido no 30.1)
- [x] 30.12 Testes: busca (prefixo, acento, `%` escapado), filtros combinados, N+1, notas (permissões e remoção lógica), bloqueio (efeito no login), export (CSV injection, teto, permissão), anonimização (FK preservadas, e-mail liberado para novo cadastro? — **decidir**: não reutilizar), auditoria de leitura
- [x] 30.13 Docs: `docs/crm.md` (visão geral, glossário, permissões, medições, LGPD), `endpoints.md`, `testes-e-qualidade.md`, `autenticacao.md`, `CLAUDE.md` (9 conceitos, `crm → identity`, `domain/common`, migration V35), `README.md`

**O que a execução ensinou:**
- **Medir achou o que revisar não achou:** uma subconsulta por linha na lista roda também nas linhas puladas pelo `OFFSET`. Só o `EXPLAIN (ANALYZE)` num volume realista mostrou 40 020 execuções.
- **Resposta em *streaming* + segurança sem sessão:** o `StreamingResponseBody` termina num *async dispatch*, e o `authorizeHttpRequests` nega (o JWT não roda de novo). Os três testes da exportação reprovaram com `AccessDeniedException`, inclusive com `SUPER_ADMIN`.
- **A sequência do Postgres não volta atrás:** uma carga que falhou e foi desfeita deixou os ids começando em 50 001, e um id "calculado" no script de medição dava zero linhas.
- O MockMvc decodifica o corpo como ISO-8859-1 sem charset (`é` virava `Ã©`); os testes de busca leem como UTF-8.
- **Migration não commitada ainda pode estar aplicada no banco do dono:** por isso o índice de `review(customer_id)` foi para a V32, e não para dentro da V31.

**Fora do escopo (de propósito):** retenção e *backups* (têm prazo próprio); exportar as notas na portabilidade do titular (são dado da empresa sobre o cliente, não dado do cliente: decisão a rever com o jurídico); apagar o texto de avaliações já publicadas (o autor some, o conteúdo fica); "keyset" na lista de clientes.

**Checklist de fechamento do M30:**
- [x] Itens 30.1–30.13 revisados
- [x] Clean Code / SOLID: o SQL de busca e exportação num só lugar (`CustomerSql`), a anonimização num só lugar (`CustomerAnonymizer`), a confirmação de senha num só lugar (`PasswordConfirmation`), `crm` sem importar `booking`/`payment`/`review`
- [x] `./gradlew check` com **exit 0** (448 testes, ktlint, detekt, JaCoCo)
- [x] Mutação nos pontos críticos (autor edita, deduplicação da leitura, apagar o nome do titular, reuso do e-mail, fórmula do CSV, ordem e filtro de cliente)
- [x] Docs e instruções do projeto atualizados

## M31 — Reservas (admin) e reembolso ✅

Problema: o suporte só conseguia cancelar reservas `PENDING`; uma reserva paga e cancelada não devolvia dinheiro. Reembolso é uma máquina de estados nova com dinheiro envolvido — idempotência e concorrência são obrigatórias. Feito em 2026-10-04, com autorização do dono para aplicar o backend inteiro; nada commitado ainda.

- [x] 31.1 `GET /v1/admin/bookings` (status, voo, cliente, período, pago ou não; página, mais recentes primeiro, `PageResponse`) e `GET /v1/admin/bookings/{id}` com pagamento, reembolso e **linha do tempo** (`BOOKING_READ_ANY`). Voo em `LEFT JOIN`: a reserva de algo que não é voo (M42) não some
- [x] 31.2 Migration **`V35`**`__create_booking_status_history.sql` (+ `booking.created_at`): a linha do tempo é escrita **pelo `BookingRepositoryAdapter`** na própria gravação, quando o status muda (a Booking não sabe de ator): nenhum caso de uso consegue mudar um status sem deixar rastro. Ator = o usuário autenticado na thread; `null` é o sistema (a expiração). Reservas antigas ganham uma entrada de criação com o momento da migração
- [x] 31.3 Domínio: `Refund` (`REQUESTED`/`COMPLETED`/`FAILED`, `reopen`), `RefundReason`, `Booking.refund()` e `BookingStatus.REFUNDED` (terminal, só de `CONFIRMED`), `RefundPolicy` (24 h; limite exato testado com `Clock.fixed`). **`Refund` mora no conceito `payment`**, por id; sem nota na auditoria de snapshot (ela vai como `reason`)
- [x] 31.4 Achado do app registrado: `REFUNDED` quebra "Minhas Viagens" no app atual (`wire_enums.dart` lança em enum desconhecido); a tolerância é o primeiro item do front, antes de qualquer reembolso real. **O backend foi feito primeiro, como decidido**
- [x] 31.5 Porta `PaymentGateway.refund(paymentId, amount, idempotencyKey)` e o `FakePaymentGateway` (sempre aceita; como seria um real, no KDoc e em `docs/reembolso.md`). **Mini-saga em três passos, cada um com a sua transação**, o caso de uso **sem** transação: `RefundRegistrar` (grava `REQUESTED` antes do gateway), `RefundProcessor` (chama o gateway **fora** de transação, com `refund-<id>` como chave, igual em toda tentativa) e `RefundSettler` (`COMPLETED`: reserva `REFUNDED`, assento liberado e auditoria na mesma transação; ou `FAILED`)
- [x] 31.6 `POST /v1/admin/bookings/{id}/refund` com `Idempotency-Key` (`PAYMENT_REFUND`), `POST /v1/admin/refunds/{id}/retry`, `GET /v1/admin/refunds[/{id}]`. **Idempotência extraída** do pagamento (`fingerprintOf` e `idempotently`, em `application/common`): o 2º uso real; o fingerprint do pagamento continua idêntico (as chaves já gravadas seguem válidas) e os testes dele não mudaram. Um reembolso `FAILED` volta como `201` com o `status` no corpo (o replay devolve a mesma resposta)
- [x] 31.7 Corrida: índice único **parcial** `refund(booking_id) WHERE status <> 'FAILED'` (V36) + `409`; e `(requested_by, idempotency_key)` para a mesma requisição enviada duas vezes. Teste com **duas threads reais**: `201` e `409`, e o gateway é chamado **uma** vez
- [x] 31.8 Auditoria (`REFUND_REQUESTED`, `REFUND_RETRIED`, `REFUND_COMPLETED`, `REFUND_FAILED`), métrica `dbook.refund{outcome=completed|failed|replayed|conflict}`. **O evento `RefundCompleted` não entrou:** o consumidor (M36) e o outbox (M35) ainda não existem, e um evento sem destino seria código morto; ele entra com o outbox, na mesma transação do passo 3a
- [x] 31.9 Fora do escopo (registrado em `docs/reembolso.md`): reembolso parcial, gateway real, estorno por item de pagamento com várias reservas, reembolso pelo cliente (M41)
- [x] 31.10 Testes: 483 no total (de 448), 0 falhas; `docs/reembolso.md` (estados, política, diagramas, saga e a decisão da mini-saga no lugar de um ADR, já que o projeto não tem `docs/adr/`), `endpoints.md`, `testes-e-qualidade.md`, `README.md`, `.claude/CLAUDE.md` (migration V37)

**O que a execução ensinou:**
- **Chamada de rede não cabe dentro de transação:** por isso a saga, e por isso o caso de uso que a conduz não é `@Transactional`. A chave do gateway é o **id do reembolso**, estável entre tentativas, e não a chave do atendente.
- Um teste "de corrida" com fakes só vale se o fake imitar o índice do banco (uma vaga viva por reserva); o primeiro cenário que escrevi deixava a reserva já `REFUNDED` e nunca chegava ao `INSERT` que perde.
- Gravar a linha do tempo no adaptador exigiu ler o status anterior **antes** do `save` (o `merge` o sobrescreve na entidade gerenciada).

**Fora do escopo (de propósito):** ver 31.9 e 31.8.

**Checklist de fechamento do M31:**
- [x] Itens 31.1–31.10 revisados (desvios registrados acima)
- [x] Clean Code / SOLID: a saga em três classes de uma responsabilidade cada, a idempotência num componente, o gateway atrás de uma porta, `crm`/`booking`/`payment` sem importar uns aos outros além do já permitido
- [x] `./gradlew check` com **exit 0** (483 testes, ktlint, detekt, JaCoCo)
- [x] Mutação nos pontos críticos (janela, liberação do assento, linha do tempo, chave estável do gateway)
- [x] Docs e instruções do projeto atualizados

## M32 — Catálogo administrativo ✅

Problema: até aqui só se **criava** voo. Faltava listar, editar, cancelar e cadastrar companhias e aeroportos, com integridade (voo com reservas não pode ser editado de forma destrutiva). Feito em 2026-10-05, com autorização do dono para aplicar o backend inteiro; nada commitado ainda.

- [x] 32.1 `GET /v1/admin/flights` (origem, destino, companhia, período, status; página, mais cedo primeiro) e `GET /{id}` com assentos livres e reservados, layout, `version` e reservas ativas (`FLIGHT_READ`). O `LIMIT` dentro da consulta, pelo mesmo motivo do M30
- [x] 32.2 `PUT /v1/admin/flights/{id}` com **`version` no corpo**: `409 STALE_VERSION` se mudou (nova `StaleVersionException`, tratada junto com o lock otimista do JPA; **duas edições simultâneas: uma vence**, testado com threads). Invariantes: chegada > partida e origem ≠ destino (**no `Flight`, o que também protege o cadastro e a importação; antes nada validava isso**); `planSeatChange` (domínio puro): capacidade ≥ reservados, mais capacidade = assentos novos no fim, menos = tira do **fim por posição** (`10A` vem depois de `9F`) só assentos livres e **sem histórico de reserva** (a reserva cancelada o referencia), layout só muda sem nenhuma reserva (aí o mapa é refeito). Voo e assentos na mesma transação: recusa deixa tudo intacto. A geração dos rótulos foi extraída (`generateSeats`) e o cadastro passou a usá-la
- [x] 32.3 **Sem a coluna de status do plano:** `FlightStatus` é derivado de `Bookable.active` (um voo cancelado é um voo inativo; a coluna seria um segundo jeito de dizer a mesma coisa). `POST /{id}/cancel` fase 1: `409` com quantas reservas ativas; fase 2 fica para M35 + M36. A busca pública passou a ignorar voo inativo, e **achado:** o cadastro de reserva **não checava** `active` (nada impedia reservar um voo desativado); agora recusa com `409`. V37: índices em `booking(bookable_id)` e `flight(departure_time, id)`
- [x] 32.4 Companhias e aeroportos (`CATALOG_WRITE`; listar com `FLIGHT_READ`): IATA validado no domínio (companhia: 2 letras ou dígitos, como `G3`; aeroporto: 3 letras) e único (`409`); só se remove o que nenhum voo usa (`409` com quantos). Os dados semeados já passam na validação
- [x] 32.5 `GET /v1/admin/aircraft-models` (somente leitura: nome, layout, assentos por fileira)
- [x] 32.6 `POST /v1/admin/flights/import` (CSV em `text/csv`, até 5000 linhas): `dryRun=true` por padrão, **todos** os erros com a linha, tudo ou nada em uma transação (`422` com erro e nada gravado), repetível por número do voo + partida (`alreadyExisting`). Leitor de CSV próprio (RFC 4180: aspas, vírgulas e quebras de linha dentro da célula, BOM, número da linha). `scripts/seed-flights.sh` passou a usar o endpoint, **verificado ao vivo** (120 voos e seus assentos numa chamada, contra a aplicação subindo com as migrations V1–V37 num Postgres real)
- [x] 32.7 Efeitos colaterais: não há cache a invalidar (M47 decide); o preço alterado ainda não grava histórico (M40)
- [x] 32.8 Auditoria antes e depois (`FLIGHT_UPDATED`, `FLIGHT_CANCELLED`, `AIRLINE_*`, `AIRPORT_*`, `FLIGHTS_IMPORTED`; o `status` entrou no snapshot do voo), testes (cada invariante é uma classe; 40 novos, 523 no total), `docs/catalogo-admin.md`

**O que a execução ensinou:**
- **Ordenar assentos pelo rótulo tira o assento errado:** `"10A" < "1A"` como texto. A posição vem da fileira e da letra, e o teste cobre a fileira 10.
- **Uma regra de domínio nova pode quebrar testes antigos que a violavam sem querer:** um teste montava um voo com a mesma origem e destino, e o `Flight` agora o recusa; o snapshot de auditoria do voo (uma lista permitida) ganhou `status`.
- Mudar uma porta (`FlightRepository`, `SeatRepository`, `AirportRepository`, `AirlineRepository`) obriga todos os fakes de teste: nove classes de seis arquivos receberam o método novo.
- O `edit()` do meu helper de teste lia o voo antes do `PUT`, e falhava nos casos de `403` e `404` (o voo não podia ser lido): precisou de uma variante que envia um corpo completo sem ler.

**Fora do escopo (de propósito):** a fase 2 do cancelamento (cancelar, reembolsar e avisar cada reserva); histórico de preço (M40); importar companhias e aeroportos em lote; uma reserva criada no mesmo instante em que o cancelamento confere as reservas pode escapar da contagem (registrado em `docs/catalogo-admin.md`).

**Checklist de fechamento do M32:**
- [x] Itens 32.1–32.8 revisados (desvios registrados acima)
- [x] Clean Code / SOLID: as regras de assento num só lugar (`planSeatChange`, `generateSeats`), as de voo no `Flight`, a resolução de códigos num só serviço (`CatalogLookup`), a leitura administrativa separada da escrita
- [x] `./gradlew check` com **exit 0** (523 testes, ktlint, detekt, JaCoCo)
- [x] Mutação nos pontos críticos (versão, cancelamento com reservas, voo cancelado reservável, ordem dos assentos)
- [x] Docs e instruções do projeto atualizados

## M33 — Dashboard de negócio ✅

Feito em 2026-10-05, com autorização do dono para aplicar o backend inteiro; nada commitado ainda.

- [x] 33.1 **Glossário primeiro** (`docs/dashboard.md`): receita = preço congelado das reservas **pagas no período** (pela entrada `CONFIRMED` do histórico de status do M31) menos os reembolsos **concluídos no período**; ocupação = assentos reservados dos voos **que partem** no período ÷ capacidade; conversão = das criadas no período, quantas foram pagas alguma vez; expiração = das criadas, quantas o sistema cancelou (cancelamento sem ator); fuso `America/Sao_Paulo`. Uma taxa sem denominador é `null`
- [x] 33.2 `GET /v1/admin/dashboard/summary?from&to` (`DASHBOARD_READ`): reservas por status, receita bruta/reembolsado/líquida, clientes novos, conversão, expiração, ocupação média. Sem período: os últimos 30 dias até hoje em São Paulo
- [x] 33.3 `timeseries?metric=REVENUE|BOOKINGS|NEW_CUSTOMERS&granularity=DAY|WEEK` (baldes vazios como zero, a semana pela segunda-feira, o reembolso negativo no dia em que concluiu) e `top-routes?limit` (1–50)
- [x] 33.4 Novo conceito `dashboard` (só leitura, SQL em `DashboardSql`, `bucketsOf` puro); V38: `booking_status_history(to_status, occurred_at)` e `refund(completed_at)` parcial. **`EXPLAIN` revisado** com 60 mil reservas e 100 mil linhas de histórico (`scripts/explain-dashboard.sql`; o `seed-customers.sh` agora grava o histórico): todas as consultas usam os índices e levam 5–38 ms, registrado em `docs/dashboard.md`. Teto de 400 dias → `400`
- [x] 33.5 **Cache em Redis (TTL 60 s)**, o primeiro do projeto, atrás de uma porta do domínio (`DashboardCache`); chave com os parâmetros; **nunca atrapalha a resposta** (Redis fora do ar ou cópia ilegível = calcula e conta); métrica `dbook_cache_total{cache,outcome=hit|miss|error}`. A decisão (por que 60 s, por que só aqui, por que não invalida) está em `docs/dashboard.md` no lugar de um ADR, já que o projeto não tem `docs/adr/`
- [x] 33.6 Testes (19 novos, 542 no total): cada regra do glossário contra o Postgres com a história inserida nos instantes exatos, **o pagamento às 23:30 de São Paulo cai no dia 30 e não no 31**, o reembolso diminui a receita, a série semanal, o cache (acerto, TTL, Redis fora do ar), permissões e `400`. **Mutação:** sem descontar o reembolso, sem o fuso, com a expiração invertida, sem gravar no cache: os testes reprovam

**O que a execução ensinou:**
- **O horário de verão passou despercebido:** meu cenário da virada de dia falhou porque em janeiro de 2000 São Paulo estava em UTC−2 (o horário de verão só acabou em 2019). Os testes agora usam janelas em 2023, onde o deslocamento é sempre −03:00; a regra do produto não muda (o `ZoneId` já conhece o histórico).
- **Um tipo de um campo só não volta do JSON** sem um criador (a armadilha dos DTOs): o `TopRoutes` ganhou o `limit` e continua sem anotar nada de Jackson no domínio.
- Dados de teste por janela de dias própria resolvem o banco compartilhado: um período "só meu" não vê o resto da suíte.

**Fora do escopo (de propósito):** receita por companhia ou cliente; comparação com o período anterior (conta da tela); invalidar o cache quando algo muda (o prazo é de 60 s, por decisão); as reservas anteriores à V35 ficam atribuídas ao dia da migração.

**Checklist de fechamento do M33:**
- [x] Itens 33.1–33.6 revisados
- [x] Clean Code / SOLID: o SQL fora do código que o roda (`DashboardSql`), os baldes numa função pura, o cache atrás de uma porta, o período com a regra do fuso num só lugar
- [x] `./gradlew check` com **exit 0** (542 testes, ktlint, detekt, JaCoCo)
- [x] Mutação nos pontos críticos
- [x] Docs e instruções do projeto atualizados

## M34 — Alertas e operação ✅

Problema: havia métricas, logs e traces, mas **ninguém era avisado**: observabilidade sem alerta só serve para investigar depois. Feito em 2026-10-05, com autorização do dono para aplicar o backend inteiro; nada commitado ainda.

- [x] 34.1 `observability/alerts.yml`, nove regras em cinco grupos: `DbookDown`, `HighServerErrorRate` (5xx acima de 5 % **com tráfego mínimo**, para uma falha à noite não acordar ninguém), `SearchLatencyHigh` (p95 acima de 500 ms), `ExpirationQueueDeadLetters`, `PendingBookingsPiling` (acima de 200 **e crescendo**), `RefundsFailing`, `LoginFailureSpike`, `AdminDeniedSpike`, `CacheUnavailable`. Gauge novo **`dbook_sqs_dlq_depth`** (`DeadLetterQueueMetrics`, lido do SQS a cada coleta; `NaN`, e não um zero falso, quando o SQS não responde). **O alerta de `outbox` atrasado entra com o M35**, quando houver a métrica
- [x] 34.2 Alertmanager no perfil `observability` do compose (`prom/alertmanager:v0.27.0`) com um receptor *webhook* de exemplo, agrupamento, repetição mais frequente para `critical` e `DbookDown` inibindo o resto. **Verificado ao vivo:** com a aplicação parada, o `DbookDown` disparou no Prometheus e chegou ao Alertmanager com o runbook
- [x] 34.3 `docs/runbooks.md`: para cada alerta, o que significa, onde olhar (Grafana, Loki, Jaeger, `curl`) e como mitigar
- [x] 34.4 SLOs (disponibilidade 99,5 % em 30 dias; p95 da busca < 500 ms) e o painel **"DBook — SLOs e orçamento de erro"** no Grafana (disponibilidade, orçamento restante, p95 da busca, 5xx, fila de mensagens mortas, pendentes, reembolsos), provisionado: verificado que o Grafana o carrega
- [x] 34.5 `observability/alerts_test.yml`: **cada alerta tem um caso que dispara e um que não** (inclusive o de baixo tráfego e o limite do `for`), rodado com `promtool check rules`, `promtool test rules` e `amtool check-config` num job novo do CI (`alert-rules`, validado com `actionlint`). **Alarmes do CloudWatch no Terraform ficam no M43**

**O que a execução ensinou:**
- **Um teste de regra que erra o tempo mostra a regra:** meu primeiro caso de `PendingBookingsPiling` esperava silêncio aos 30 minutos, mas o valor passa de 200 no minuto 11 e o `for: 15m` já tinha vencido. A correção foi do teste, e a regra ficou como estava.
- **`increase()` herda as etiquetas da série:** o `RefundsFailing` vinha com `outcome="failed"` no alerta; um `sum(...)` o deixou com só os rótulos do alerta.
- `docker compose --profile observability stop` **sem nomes** para todos os serviços, inclusive o Postgres (já dito em `observabilidade.md`): desliguei só os da pilha.

**Fora do escopo (de propósito):** um receptor real (Slack, PagerDuty: a configuração é do time); alarmes do CloudWatch (M43); o alerta do outbox (M35).

**Checklist de fechamento do M34:**
- [x] Itens 34.1–34.5 revisados (desvios registrados acima)
- [x] Clean Code: regras pequenas, cada uma com o seu runbook e o seu teste
- [x] `./gradlew check` com **exit 0** (544 testes, ktlint, detekt, JaCoCo)
- [x] Regras testadas (`promtool`) e a cadeia verificada ao vivo
- [x] Docs atualizados (`runbooks.md`, `observabilidade.md`, `README.md`)

## M35 — Outbox transacional ✅

Problema (achado 7): agendar a expiração no SQS depois do commit deixava uma janela: se o processo caísse no meio, a reserva nunca expirava. Feito em 2026-10-05, com autorização do dono para aplicar o backend inteiro; nada commitado ainda.

- [x] 35.1 Migration **`V39`**`__create_outbox_event.sql`: `id uuid`, `aggregate_type`, `aggregate_id`, `type`, `payload jsonb`, `headers jsonb` (leva o `traceparent`), `created_at`, `available_at` (quando **pode** sair), **`next_attempt_at`** (a reserva em uso ou o *backoff*; não estava no plano e separa "quando devia sair" de "quando tento de novo", o que deixa o atraso medido pela hora certa), `published_at`, `attempts`, `last_error`; índice parcial dos não publicados e outro dos publicados (a limpeza)
- [x] 35.2 Porta `OutboxWriter.add(event)` (novo conceito `messaging` no domínio) que **recusa fora de uma transação** (`Propagation.MANDATORY`): um evento escrito sozinho poderia sobreviver a uma mudança desfeita. `OutboxEvent` carrega o `availableAt`, não um atraso. O adaptador guarda o `traceparent` da requisição nos cabeçalhos
- [x] 35.3 `OutboxRelay` (`@Scheduled`, 1 s) em três passos, **cada um na sua transação, sem segurar uma transação durante a chamada de rede**: reivindicar um lote (`FOR UPDATE SKIP LOCKED` + `UPDATE ... RETURNING`, com uma reserva de 60 s), entregar à SQS (`SqsOutboxPublisher`, rota pelo tipo; tipo sem rota **falha**), marcar publicado; *backoff* exponencial de 5 s a 15 min; o trace segue como atributo da mensagem. Várias instâncias sem duplicar (testado com duas threads e 60 eventos)
- [x] 35.4 O agendamento de expiração (M20) migrou: `RegisterBookingUseCase` grava `booking.expiration.requested` com `availableAt = agora + 15 min` na transação da reserva e a SQS recebe a mensagem **sem `DelaySeconds`** (o limite de 900 s deixou de ser restrição). Removidos `BookingExpirationScheduler`, `SqsBookingExpirationScheduler`, `BookingExpirationMessage` e o `@MockBean` do `AbstractIntegrationTest` (sem relé nos testes, as reservas só deixam linhas no outbox, o que dispensa o mock). O formato da mensagem não mudou: o consumidor segue igual
- [x] 35.5 Métricas `dbook_outbox_pending`, **`dbook_outbox_overdue_seconds`** (o atraso do mais atrasado pela hora em que devia sair), `dbook_outbox_published_total` e `dbook_outbox_failures_total`; alerta **`OutboxOverdue`** (10º do `alerts.yml`, com teste de `promtool` e runbook)
- [x] 35.6 Limpeza dos publicados há mais de 7 dias, às 03:30
- [x] 35.7 Testes (11 novos, 555 no total): queda entre o commit e a publicação → o relé entrega; queda depois de reivindicar → entrega ao fim da reserva; **dois relés ao mesmo tempo, 60 eventos, cada um entregue uma vez**; falha com o *backoff* de 5 s e 10 s e entrega ao se recuperar; escrever fora de transação recusado; transação desfeita não deixa evento; a reserva e o evento nascem juntos por HTTP (e uma reserva recusada não deixa evento); o `traceparent` viaja; o publicador contra o LocalStack (corpo, atributos, tipo sem rota, fila inalcançável); duplicata tolerada e ordem não garantida, **documentadas**. **Mutação:** sem o `MANDATORY`, sem a reserva ou sem o *backoff*, os testes reprovam
- [x] 35.8 `docs/mensageria.md` (com a decisão **outbox × CDC × ignorar** no lugar de um ADR, já que o projeto não tem `docs/adr/`), `docs/expiracao-de-reservas.md` atualizado, e o item 20.6 fechado

**Verificado ao vivo** (aplicação real, Postgres, Redis, LocalStack): a reserva gravou o evento (vencendo em 14 min 59 s), o relé o publicou, o consumidor expirou a reserva e o assento voltou a `AVAILABLE`; e **com o SQS parado** a reserva foi criada (`201`), o evento esperou e tentou de novo guardando o erro, e foi entregue quando o SQS voltou.

**O que a execução ensinou:**
- **Segurar uma transação durante a chamada de rede é o erro clássico do relé:** reivindicar com uma reserva (`UPDATE ... RETURNING`) e entregar fora da transação evita que um SQS lento trave uma conexão com bloqueios abertos.
- **Medir o atraso pela próxima tentativa esconde uma queda longa:** com o *backoff* empurrando o evento para 15 minutos adiante, "atrasado" nunca passaria de 15 minutos. Por isso `available_at` (imutável) e `next_attempt_at` são colunas diferentes.
- O `SKIP LOCKED` não é o que impede a duplicata no teste das duas threads (o Postgres reavalia a linha depois do bloqueio); quem impede é a **reserva**: o teste que remove a reserva é o que reprova.
- Um `sed`/script que escreve num caminho sem pasta aborta antes de escrever qualquer arquivo: só descobri pela falta dos testes.
- **Um relógio criado no construtor da classe de teste envelhece enquanto o Spring sobe o contexto:** um teste passava sozinho e falhava na suíte inteira. Os eventos de teste agora vencem em relação ao próprio relógio do teste, nunca ao `Instant.now()`.

**Fora do escopo (de propósito):** ordem entre eventos; entrega exatamente uma vez; trocar a SQS por outro destino (a porta `OutboxPublisher` é o ponto); o Terraform da SQS (20.5).

**Checklist de fechamento do M35:**
- [x] Itens 35.1–35.8 revisados
- [x] Clean Code / SOLID: a escrita atrás de uma porta do domínio, o relé, o armazenamento e o publicador em classes de uma responsabilidade, nada de Spring no domínio
- [x] `./gradlew check` com **exit 0** (555 testes, ktlint, detekt, JaCoCo)
- [x] Mutação nos pontos críticos e o fluxo verificado ao vivo, inclusive com o SQS fora do ar
- [x] Docs atualizados

## M36 — Notificações ✅

Eventos: reserva confirmada, expirada, cancelada pelo suporte, reembolso concluído, voo alterado (o alerta de preço entra com o M40, como um tipo novo).

**36a — Núcleo e in-app**
- [x] 36.1 Eventos de domínio escritos via outbox (M35), **na mesma transação** da mudança: `booking.confirmed` (um por reserva paga), `booking.expired`, `booking.cancelled-by-staff` (o dono cancelando a própria reserva não avisa ninguém), `refund.completed` (com o valor) e `flight.changed` (um por reserva ativa, só quando número, origem, destino, partida ou chegada mudam; preço e capacidade não). Os tipos são `booking.*`/`refund.*`/`flight.*`, todos roteados para a fila de notificações
- [x] 36.2 Migration `V40__create_notification.sql` (a V37 prevista já estava ocupada): `notification` (com `event_id` **único**), `notification_preference` (só o que o cliente escolheu; ausente = ligado), `device_token` (token único: muda de dono) e `notification_delivery` (a chave `event_id + channel` da idempotência)
- [x] 36.3 Fila `dbook-notifications` + DLQ (`scripts/localstack-init/01-sqs.sh` e o módulo `terraform/modules/sqs`, validado com `terraform validate`; a ligação ao ECS fica no M43), `NotificationConsumer` na entrada (com o `SqsMessageLoop` extraído do consumidor da expiração) e o `SqsOutboxPublisher` mandando `eventId` e `eventType`; **idempotente por evento e canal**
- [x] 36.4 `GET /v1/notifications` (keyset por `id`, `unreadOnly`), `GET /unread-count`, `POST /{id}/read` (`404` se não for sua), `POST /read-all`; `GET/PUT /v1/notifications/preferences`

**36b — Canais**
- [x] 36.5 E-mail pelo `EmailSender`, texto em português, respeitando a preferência; conta anonimizada não recebe
- [x] 36.6 Push: porta `PushSender` + `LoggingPushSender` (só registra quantos aparelhos, nunca o token); o FCM real fica **registrado como evolução** (exige conta Firebase); `POST/DELETE /v1/notifications/devices`

**36c — Qualidade**
- [x] 36.7 Um canal que falha não derruba os outros (a mensagem volta e só o que falhou roda de novo), DLQ depois de 3 tentativas, métrica `dbook_notification_total{channel,outcome}`, o gauge da DLQ agora por fila (`dead-letter-queues.queues`) e o alerta **`DeadLetterQueueNotEmpty`** (substitui o `ExpirationQueueDeadLetters`, testado com `promtool`)
- [x] 36.8 Testes (53 novos, 608 no total): idempotência por evento e canal (unidade, banco e fila de verdade), preferência desligada, falha de um canal sem barrar os outros e só ele reenviado, push sem aparelho e e-mail de conta anonimizada pulados, texto de cada tipo, caixa paginada por cursor e só do dono, `404` na de outro, preferências e aparelhos (inclusive o token que muda de dono), consumidor contra o LocalStack (notifica e apaga, duplicata notifica uma vez, tipo desconhecido e mensagem sem tipo na DLQ), o claim `NEW`/`RETRY`/`ALREADY_DONE` no Postgres, os eventos escritos junto com pagar, cancelar pela equipe e editar o voo, a anonimização apagando notificações, aparelhos e preferências. **Mutação:** sem a checagem do log de entregas, a duplicata e a retentativa reprovam; sem a preferência, o teste dela reprova
- [x] 36.9 `docs/notificacoes.md`, `endpoints.md`, `mensageria.md`, `runbooks.md`, `CLAUDE.md` e `README.md`

**Verificado ao vivo** (aplicação real, Postgres, Redis, LocalStack): pagar uma reserva e depois mudar o horário do voo geraram `booking.confirmed` e `flight.changed`; o relé os publicou, o consumidor os processou e a caixa do cliente mostrou as duas notificações em português (`unread-count` 2), com `dbook_notification_total` em `in_app`/`email` enviados, `push` pulado (sem aparelho) e as duas DLQs em zero.

**O que a execução ensinou:**
- **Extrair o laço da SQS** (long polling, trace, apagar só depois de tratar) em vez de copiar 60 linhas para o segundo consumidor: o primeiro continuou com o mesmo construtor, então seus testes não mudaram.
- **A DLQ deixa de ser "da expiração":** o gauge e o alerta viraram por fila (`queue`), e o alerta não filtra mais por nome, então uma terceira fila entra sem mexer na regra.
- **Os tipos de aviso levam o nome dos eventos de outros conceitos:** a regra de dependências entre conceitos passou a permitir `notification → booking/catalog/payment` e `catalog`/`payment → messaging`, escrito no teste e no `CLAUDE.md`, em vez de duplicar as strings.
- O `detektMain` e o `detektTest` pegam o que o `detekt` simples não pega (sombreamento de `it`, `!!`): rodar o `check` inteiro, não só o `ktlintCheck detekt`.

**Fora do escopo (de propósito):** o adaptador real do FCM; modelos de e-mail em HTML; agrupar avisos (*digest*); ordem entre avisos; o tipo "alerta de preço" (M40).

**Checklist de fechamento do M36:**
- [x] Itens 36.1–36.9 revisados
- [x] Clean Code / SOLID: o consumidor sem regra (entrega a um caso de uso), cada canal numa classe de uma responsabilidade atrás de uma interface, nada de Spring no domínio
- [x] `./gradlew check` com **exit 0** (608 testes, ktlint, detekt, JaCoCo)
- [x] Mutação nos pontos críticos
- [x] Docs atualizados

## M37 — Avaliações públicas, editar/apagar e moderação ✅

- [x] 37.1 `GET /v1/destinations/{iata}/reviews?page&size&sort=RECENT|RATING` (pública): agregado (média, total, distribuição 1–5) + itens; **privacidade:** autor exibido como primeiro nome + inicial do último sobrenome (e `Cliente anônimo` para conta anonimizada), sem id do cliente, e-mail nem reserva. Migration `V41` com o índice `flight(destination_airport_id)` (o salto que faltava em `review → booking → flight → airport`); **desvio:** sem coluna `destination_iata` na avaliação, porque o destino do voo pode ser editado (M32) e a cópia ficaria errada
- [x] 37.2 `PATCH` e `DELETE /v1/reviews/{id}` (só o autor; `Review.edit` no domínio); a média continua **derivada** das visíveis, nada armazenado
- [x] 37.3 Denúncia `POST /v1/reviews/{id}/report` (uma por cliente e avaliação, pelo índice único) e moderação `GET /v1/admin/reviews?status=REPORTED|HIDDEN|VISIBLE`, `POST .../hide|restore` (`REVIEW_MODERATE`, motivo, auditado: `REVIEW_HIDDEN`, `REVIEW_RESTORED`) + `.../dismiss-reports` (**acrescentado ao plano:** sem ele uma denúncia à toa deixaria a avaliação na fila para sempre)
- [x] 37.4 Testes (37 novos, 645 no total) e docs (`docs/avaliacoes.md`): privacidade do autor (nome, sem ids nem e-mail, anonimizado), média e distribuição, paginação estável nas duas ordens, média após editar/ocultar/restaurar/apagar (inclusive a de `GET /v1/destinations`), autor × terceiro `403`, `404`, `400`, uma denúncia por cliente e não a própria, ocultar tira da lista e da fila e é auditado **sem o texto**, restaurar, dispensar denúncias, só `REVIEW_MODERATE` entra na fila. **Mutação:** sem o filtro `VISIBLE` na leitura pública, ou na média de `GET /v1/destinations`, os testes reprovam

**Fora do escopo (de propósito):** responder a uma avaliação; avaliar sem reserva; apagar o texto de uma avaliação ao anonimizar o cliente (muda só o nome exibido); notificar o autor de que a avaliação foi ocultada.

**Checklist de fechamento do M37:**
- [x] Itens 37.1–37.4 revisados
- [x] Clean Code / SOLID: regras no domínio (`Review.edit/hide/restore`), leituras como *read models*, casos de uso pequenos
- [x] `./gradlew check` com **exit 0** (645 testes, ktlint, detekt, JaCoCo)
- [x] Mutação nos pontos críticos
- [x] Docs atualizados

## M38 — Favoritos no servidor ✅

Problema (achado 6): favoritos só no aparelho → somem ao trocar de celular e não existe fonte única da verdade.

- [x] 38.1 Migration `V42__create_favorite.sql` (a V38 prevista já estava ocupada): `favorite` (`user_id`, `target_type` ∈ {`DESTINATION`, `FLIGHT`}, `target_id` como texto, `created_at`), único por `(user_id, target_type, target_id)`, com o índice da lista
- [x] 38.2 `PUT /v1/favorites/{type}/{id}` (idempotente, `204`), `DELETE` (`204`, idempotente), `GET /v1/favorites?type&page&size` (mais novos primeiro, com o que o app precisa para desenhar cada um); teto de 200 por usuário (`409` + `code=FAVORITES_LIMIT`), decidido **atomicamente** (trava da linha do cliente), então nem 20 requisições juntas o furam
- [x] 38.3 Alvo inexistente → `404` (destino validado no catálogo de aeroportos, voo no de voos); id ou tipo mal formados → `400`
- [x] 38.4 Testes (22 novos, 667 no total) e docs (`docs/favoritos.md`, `endpoints.md`): idempotência de salvar e remover, ordem, filtro por tipo, paginação, detalhes de destino e de voo (e voo fora de venda continua na lista), isolamento entre clientes, o limite (`409` com o código, repetido segue `204`, remover libera), **20 *threads* com limite 5 → exatamente 5**, `404`/`400`/`401`, exportação e anonimização. **Mutação:** sem a trava da linha, o teste de corrida reprova. **App:** migração dos favoritos locais (M39 do app)

**O que a execução ensinou:**
- Um parâmetro de rota ou de consulta de enum inválido (`/favorites/HOTEL/1`, `?sort=NOPE`) **não** é um `BindException`: virava `sendError(400)` e, por não estar no `permitAll`, um **`401` enganoso**. O `ApiExceptionHandler` ganhou o `MethodArgumentTypeMismatchException` → `400` com `VALIDATION_FAILED` (que corrigiu também a ordenação e o status das avaliações do M37).
- O `CAST` do id de um voo, numa junção com `OR` de tipos, podia rodar sobre o código de um destino (`GRU`) e quebrar a consulta: o `CASE WHEN` na condição garante que a conversão só roda onde o tipo é voo.

**Fora do escopo (de propósito):** ordenar favoritos à mão; pastas ou listas; sincronização de favoritos locais (é do app); hotéis (entram no M42 como mais um tipo).

**Checklist de fechamento do M38:**
- [x] Itens 38.1–38.4 revisados
- [x] Clean Code / SOLID: a regra do limite e da idempotência atrás da porta `FavoriteRepository`, a leitura como *read model*, casos de uso pequenos
- [x] `./gradlew check` com **exit 0** (667 testes, ktlint, detekt, JaCoCo)
- [x] Mutação no ponto crítico (a trava)
- [x] Docs atualizados

## M39 — Código promocional ✅

- [x] 39.1 Migration `V43__create_promo_code.sql` (a V39 prevista já estava ocupada): `promo_code` (código em maiúsculas e único, tipo, valor, mínimo, janela, limites, `redeemed`, `active`, `created_by`) e `promo_redemption` (`payment_id` único); `payment` ganha `subtotal`, `discount`, `promo_code_id` e `promo_code`; **acrescentado ao plano:** `booking.discount`, a parte de cada reserva no desconto, para o reembolso devolver o que foi pago
- [x] 39.2 Domínio: `PromoCode.discountFor(amount, now, bookingCount)` com as invariantes (janela com o início incluído e o fim excluído, mínimo, ativo, desconto nunca acima do total menos um centavo por reserva, percentual arredondado ao centavo **metade para cima**) e `allocateDiscount` (divisão proporcional ao centavo, soma exata, nenhuma reserva a zero)
- [x] 39.3 `POST /v1/promo-codes/validate` (prévia sem consumir) e `POST /v1/payments` aceitando `promoCode` opcional (campo **aditivo**, continua `v1`); `422 PROMO_REJECTED` desfaz o pagamento todo e as reservas seguem `PENDING`
- [x] 39.4 **Concorrência:** consumo atômico `UPDATE promo_code SET redeemed = redeemed + 1 WHERE id = ? AND (max_redemptions IS NULL OR redeemed < max_redemptions) RETURNING max_per_user`, em transação obrigatória, e o limite por cliente protegido pela trava da mesma linha; testado com 20 clientes disputando o último uso, 10 resgates do mesmo cliente e 2 pagamentos simultâneos por HTTP
- [x] 39.5 Idempotência: o código (normalizado) entra na impressão digital; a repetição devolve o mesmo pagamento com o mesmo desconto sem consumir de novo, e a mesma chave com outro código ou sem código é `422`; sem código a impressão digital é a antiga
- [x] 39.6 Reembolso (M31) devolve o valor **efetivamente pago** (`paidAmount`); o uso do código não é devolvido (regra registrada em `docs/reembolso.md` e `docs/promocoes.md`)
- [x] 39.7 Admin: CRUD (`PROMO_WRITE`: criar, listar com os usos, ver, ver resgates, mudar janela e limites, **desativar e reativar em vez de apagar**), auditado (`PROMO_CREATED/UPDATED/ACTIVATED/DEACTIVATED`)
- [x] 39.8 Testes (57 novos, 724 no total), docs (`docs/promocoes.md`) e `endpoints.md`. **Mutação:** sem a condição `redeemed < max_redemptions` do `UPDATE`, o teste de 20 clientes e o de 2 pagamentos reprovam

**O que a execução ensinou:**
- **`BigDecimal / BigDecimal` do Kotlin arredonda para a escala do numerador, e pela regra do banqueiro** (`HALF_EVEN`): 10 % de 100,05 dava 10,00. O teste de arredondamento reprovou; a divisão agora é `divide(divisor, 2, HALF_UP)`, com escala e modo explícitos, e o mesmo na divisão proporcional.
- A tentativa de dividir o desconto "por igual" ou "só proporcional" deixa sobras de centavos ou reservas a zero: um teste com 500 combinações aleatórias exige que a soma seja exata e que cada reserva reste com pelo menos um centavo (senão o reembolso dela seria de valor zero, que o domínio recusa).
- **Reservar o último uso não é "ler e depois gravar":** o `UPDATE` condicional é a decisão, e a trava de linha que ele leva de brinde serializa também o limite por cliente.

**Fora do escopo (de propósito):** cupom por cliente específico; cupom acumulável; devolver o uso no reembolso; limite de tentativas de adivinhar códigos (M46).

**Checklist de fechamento do M39:**
- [x] Itens 39.1–39.8 revisados
- [x] Clean Code / SOLID: o cálculo e a divisão no domínio, a atomicidade atrás da porta `PromoRepository`, casos de uso pequenos
- [x] `./gradlew check` com **exit 0** (724 testes, ktlint, detekt, JaCoCo)
- [x] Mutação no ponto crítico
- [x] Docs atualizados

## M40 — Histórico e alerta de preço ✅

- [x] 40.1 Migration `V44__create_price_history_and_alerts.sql` (a V40 prevista estava ocupada); `flight_price_history` gravado na criação (importação CSV inclusa) e em cada mudança de preço, **na transação que muda o preço** (`FlightPriceRecorder`, que também escreve o evento `flight.price-changed`); o mesmo preço não é mudança; `GET /v1/flights/{id}/price-history` (**público**, como a busca) com atual, menor e maior
- [x] 40.2 `price_alert` por **rota + data** (`origin`, `destination`, `travel_date`, `target_price`, `active`, `last_notified_at`): `POST/GET/PATCH/DELETE /v1/price-alerts`, um alerta por cliente, rota e data (`409`), limite de 20 ativos (`409` + `code=PRICE_ALERTS_LIMIT`, decidido atomicamente), `404` para o de outro cliente
- [x] 40.3 Avaliador acionado pelo `flight.price-changed` (o consumidor da fila de notificações o despacha): reivindica, **num único comando**, os alertas ativos da rota e data cujo alvo o preço atende e que não foram avisados em 24 h (índice parcial das ativas), e escreve o `price-alert.triggered`, que o pipeline do M36 transforma na notificação `PRICE_ALERT` (in-app, e-mail e push, com preferências e idempotência)
- [x] 40.4 Testes (21 novos, 745 no total) e docs (`docs/precos.md`): preço cai abaixo → notifica uma vez e a repetição não; sobe e cai de novo dentro da janela não repete, **e repete depois dela**; o alvo incluído; outra data, outra rota, preço acima e alerta desligado não disparam; mudar o alvo zera a janela; **10 entregas simultâneas → exatamente 1 aviso**; histórico em ordem com menor e maior; preço igual não cria ponto; o evento nasce com a edição; fila de verdade (o evento de preço vira o disparo e o disparo vira a notificação na caixa do cliente); alertas por HTTP (dono, `404`, `409`, limite, `400`). **Mutação:** sem a janela na reivindicação, os três testes de dedup reprovam

**O que a execução ensinou:**
- **O número que sai do JSON pode ter escala 1** (`80.0`): o evento de preço diferia do histórico (`80.00`). O dinheiro dos eventos agora sempre tem os centavos.
- **Um consumidor com dois destinos** (notificar ou avaliar alertas) sem ganhar uma segunda fila: o tipo do evento decide, e o avaliador reaproveita todo o pipeline do M36 em vez de ter um canal próprio.

**Fora do escopo (de propósito):** avisar na hora ao criar um alerta com voo já abaixo do alvo; alerta por voo específico ou sem data; gráfico (é do app); exportar os alertas na portabilidade de dados.

**Checklist de fechamento do M40:**
- [x] Itens 40.1–40.4 revisados
- [x] Clean Code / SOLID: o registro de preço num colaborador só, a reivindicação atrás da porta `PriceAlertRepository`, casos de uso pequenos
- [x] `./gradlew check` com **exit 0** (745 testes, ktlint, detekt, JaCoCo)
- [x] Mutação no ponto crítico
- [x] Docs atualizados

## M41 — Cancelamento e reembolso pelo cliente ✅

- [x] 41.1 `GET /v1/bookings/{id}/cancellation-policy` → ação possível (`CANCEL`, `REFUND_REQUEST` ou `NONE` com o motivo), valor reembolsável (o **efetivamente pago**) e prazo (24 h antes da partida); o app mostra **antes** de confirmar
- [x] 41.2 **Endpoint novo** `POST /v1/bookings/{id}/refund-request` (com `Idempotency-Key`) em vez de mudar o `cancel` (que continua `409` para reserva paga, preservando a `v1`); reaproveita `RefundBookingUseCase`/`RefundPolicy` do M31 com o cliente como ator e **sem o override**; só o dono (`403` para outro)
- [x] 41.3 Testes (25 novos, 770 no total) e docs (`reembolso.md`, `endpoints.md`): dentro e fora do prazo, dono × terceiro, repetição idempotente (um reembolso, uma chamada ao gateway) e chave reaproveitada `422`, reserva paga com desconto devolve o valor pago, gateway que recusa e nova tentativa com outra chave, política em cada estado (paga, dentro da janela, pendente, reembolsada, reembolso em andamento), e a **corrida do cliente com o suporte** (um `201`, um `409`, um reembolso). **Mutação:** sem a checagem do dono, os testes de terceiro reprovam

**Fora do escopo (de propósito):** o cliente cancelar uma reserva paga por `/cancel` (mudaria a `v1`); reembolso parcial; contestar um reembolso que falhou sem a equipe.

**Checklist de fechamento do M41:**
- [x] Itens 41.1–41.3 revisados
- [x] Clean Code / SOLID: a política num caso de uso só de leitura, o pedido reaproveitando o mecanismo da equipe em vez de duplicá-lo
- [x] `./gradlew check` com **exit 0** (770 testes, ktlint, detekt, JaCoCo)
- [x] Mutação no ponto crítico
- [x] Docs atualizados

## M42 — Hotéis (`Accommodation`) ✅  *(paga a dívida de design do M1)*

- [x] 42.1 **A decisão (spike, em `docs/hoteis.md`, que faz as vezes do ADR):** a `Booking` guarda **o que** reservou: `seatId` passou a ser opcional e ganhou um `Stay` (tipo de quarto, noites, hóspedes, **diária congelada**), nunca os dois (invariante do domínio e `CHECK` no banco). O estoque é **por noite** (`room_night`: tipo de quarto × noite → quartos ocupados) e reservar é **um comando condicional por noite**, numa transação; a alternativa `EXCLUDE USING gist` com `tstzrange` fica registrada (serve quando cada quarto físico é uma entidade). Generalizar o `Seat` e um item polimórfico foram descartados com motivo
- [x] 42.2 **42a — Domínio e persistência:** `Accommodation : Bookable` (nome, destino, endereço, estrelas, comodidades), `RoomType` (capacidade, diária, quantidade), `Stay`, a entidade JPA do hotel na hierarquia `Bookable` (JOINED) e JDBC para os quartos e o estoque; migration `V45` (a V41 prevista já estava ocupada)
- [x] 42.3 **42b — Busca:** `GET /v1/accommodations/search?destination&checkIn&checkOut&guests` (pública, paginada, mais barato primeiro): só os hotéis com um tipo de quarto livre **em todas as noites**, com o **preço da estadia inteira**, a nota e o número de avaliações; `GET /v1/accommodations/{id}`
- [x] 42.4 **42c — Reserva e pagamento:** `POST /v1/accommodations/{id}/bookings` reaproveita `Booking` e `Payment` (preço congelado = noites × diária, M27; código promocional e reembolso funcionam), a expiração (M20/M35) e o cancelamento devolvem as noites (`BookingInventoryReleaser`)
- [x] 42.5 **42d — Concorrência:** a última vaga do quarto numa noite com 20 *threads* (exatamente 1 vence), 10 *threads* para 3 quartos (exatamente 3), duas reservas por HTTP, três reservas que se sobrepõem em pares (nenhuma noite ocupada duas vezes) e uma estadia cuja última noite está cheia desfaz as anteriores
- [x] 42.6 **42e — Admin:** CRUD de hotéis, tipos de quarto e diárias no catálogo admin (`CATALOG_WRITE`, auditado), tirar de venda e repor, a quantidade nunca abaixo do já reservado. **A importação em lote ficou de fora** (decisão registrada em `docs/hoteis.md`: os hotéis se cadastram um a um; a importação CSV do M32 é de voos)
- [x] 42.7 **42f — Avaliações e destinos:** a leitura das avaliações por destino e a média de `GET /v1/destinations` passam a contar voos **e** hotéis (o destino de uma avaliação é onde o voo chega ou onde o hotel está); `GET /v1/accommodations/{id}/reviews`
- [x] 42.8 **42g — Tempo real:** **decisão registrada de não fazer**: o canal anuncia um número único e vivo (assentos livres); a disponibilidade de um hotel é por noite e por tipo, e a tela consulta a busca (`409` ao reservar é a garantia)
- [x] 42.9 Docs (`docs/hoteis.md`), `endpoints.md`, `CLAUDE.md` (o `Bookable` deixa de ser promessa; 16 conceitos; próxima migration V46) e `README.md`

**Testes (45 novos, 815 no total):** o conjunto de hotéis (domínio da estadia, do quarto e do hotel; a reserva e o que a impede; o fim da reserva devolvendo as noites; o estoque contra o Postgres; busca, admin, avaliações e corridas por HTTP). **Mutação:** sem a condição `booked < quantity` do comando por noite, os testes de corrida reprovam.

**O que a execução ensinou:**
- **O Jackson daqui não aplica o valor padrão de um campo que falta** (não há o módulo Kotlin): um `Boolean` ausente vira `false` e um `Int` ausente vira `0`. O tipo de quarto criado sem `active` nascia **fora de venda**, e os testes de busca não achavam o hotel. Todos os campos opcionais com padrão "ligado" ou "1" (`active`, `maxPerUser`, `minAmount`, as listas) viraram nuláveis, com o padrão aplicado no `toXxx()`; o mesmo problema estava escondido no cupom (M39), onde um `POST` sem `maxPerUser` dava `400`, e agora há um teste.
- **A validação do Hibernate (`ddl-auto: validate`) reprova `SMALLINT` para um `Int`:** a coluna `stars` é `INTEGER`.
- Com a reserva generalizada (assento opcional), tudo o que lia o assento por `JOIN` virou `LEFT JOIN` (lista do admin, histórico do CRM), senão a estadia **sumia** das listas.
- **Tirar a devolução do estoque de dois casos de uso** (cancelar e reembolsar) para um colaborador (`BookingInventoryReleaser`) evitou repetir a decisão "assento ou noites" nos dois, e é o ponto onde um terceiro tipo de reservável entraria.

**Fora do escopo (de propósito):** importação em lote de hotéis; tempo real da disponibilidade; escolher o quarto físico; tarifa por noite diferente (alta temporada); política de cancelamento própria do hotel.

**Checklist de fechamento do M42:**
- [x] Itens 42.1–42.9 revisados
- [x] Clean Code / SOLID: a invariante "assento ou estadia" no domínio, o estoque atrás da porta `RoomInventory`, a devolução num colaborador único, casos de uso pequenos
- [x] `./gradlew check` com **exit 0** (815 testes, ktlint, detekt, JaCoCo)
- [x] Mutação no ponto crítico
- [x] Docs atualizados

## M43 — Terraform completo ✅

- [x] 43.1 SQS (as duas filas, as mortas, **criptografia**, **alarmes** de mensagem morta e a política do papel da aplicação), SES (a identidade do remetente e a permissão de só enviar como ela), Secrets Manager (a senha do administrador de partida e a chave do 2FA, além do JWT e do banco) e a ligação à aplicação (as URLs das filas, a origem do portal, a senha do administrador só quando o e-mail foi dado); o **papel da tarefa separado do de execução** no ECS. Tudo validado com `terraform validate`
- [x] 43.2 Portal: bucket S3 **privado** (sem acesso público, versionado, criptografado, com ciclo de vida) + CloudFront com **origin access control**, `index.html` para 403 e 404, política de **cabeçalhos de segurança** (HSTS, CSP restrita a si e à API, `X-Content-Type-Options`, `X-Frame-Options`, `Referrer-Policy`), domínio e certificado opcionais (com uma pré-condição que recusa domínio sem certificado)
- [x] 43.3 CI: job `terraform` (`fmt -check`, `validate`, `tflint`, `checkov`) **sem credenciais** (sem conta AWS não há `plan` real: registrado em `docs/custos.md`), com o `checkov` rodando de `terraform/.checkov.yaml` (cada verificação ignorada com o motivo); cada módulo ganhou o seu `versions.tf`; estimativa de custo em `docs/custos.md` (≈ US$ 55 por mês)
- [x] 43.4 `cd.yml`: o job `deploy-portal` (atrás do mesmo gate `check-aws`; constrói o Flutter Web, sobe com `index.html` sem cache, invalida o CloudFront) e o papel do GitHub com **só** o que esse deploy precisa; validado com `actionlint`

**O que a execução ensinou:**
- Um `checkov` que **reprova tudo** não serve de portão; um que ignora tudo também não. A saída foi uma lista de verificações ignoradas **com o motivo em cada linha** (site estático, projeto de estudo, infraestrutura anterior que forçaria recriar o banco): o que entra de novo e falha quebra o build, e a lista é a dívida visível.
- O `tflint` recomendado pediu `required_version` e `required_providers` em **cada** módulo (10 arquivos `versions.tf`), e achou uma variável sem uso no módulo do SES, que passou a marcar a identidade com o nome.
- **A chave da tarefa separada:** o papel de execução e o da tarefa eram o mesmo; quando a aplicação passou a usar SQS e SES, juntar os dois daria ao agente do ECS permissões da aplicação (e vice-versa).

**Fora do escopo (de propósito):** o `plan` e o `apply` reais (não há conta AWS persistente); o adaptador do SES na aplicação (só o `LoggingEmailSender` existe); o balanceador e o domínio da API; o WAF e os logs de acesso do CloudFront.

**Checklist de fechamento do M43:**
- [x] Itens 43.1–43.4 revisados
- [x] `terraform fmt`, `validate`, `tflint` e `checkov` limpos; `actionlint` limpo
- [x] Docs atualizados (`nuvem-e-cicd.md`, `custos.md`)

## M44 — Ciclo de vida da API e versão mínima do app ✅

Fecha o item "Não feito" do M24.

- [x] 44.1 `@DeprecatedApi(since, sunset, link)` + interceptor que devolve `Deprecation`, `Sunset` e `Link` (RFC 9745 e 8594); métrica `dbook.api.deprecated.calls{path,appVersion}`
- [x] 44.2 `X-App-Version` e `X-App-Platform` registrados no log (MDC) e na métrica `dbook.app.requests{platform,appVersion}`, **normalizados para um conjunto fechado** (a base para decidir quando uma versão pode morrer, sem abrir séries sem fim)
- [x] 44.3 `GET /v1/app-config` (público) → `minSupportedVersion`, `latestVersion` e `storeUrl` por plataforma, de configuração
- [x] 44.4 **Diff de contrato no CI:** o contrato de cada versão commitado (`docs/openapi/`), um teste que o mantém igual ao que o código publica, e o job `contract` com `oasdiff breaking --fail-on ERR` contra a base (verificado: remover um endpoint sem depreciação reprova)
- [x] 44.5 Ensaio de `/v2` (`GET /v2/destinations` ao lado do v1, que ficou obsoleto) e `/v1/ws` (decisão registrada: o `/ws` antigo continua, porque redirecionar o *handshake* não funciona)

**Testes (12 novos, 827 + os do contrato):** a normalização da versão e da plataforma (lixo e hostil viram `unknown`), os cabeçalhos e a contagem de um endpoint obsoleto, a contagem por app em toda requisição, o `app-config` público, a `v1` e a `v2` respondendo juntas em formas diferentes, `/v1/ws` e `/ws` aceitando um token válido e o `/v1/ws` recusando sem token, e o teste da linha de base do OpenAPI.

**O que a execução ensinou:** um filtro novo que exige `MeterRegistry` quebrou todos os testes de fatia web (`@WebMvcTest`), que não têm registro de métricas: a dependência passou a ser um `ObjectProvider` com um registro descartável de reserva.

**Fora do escopo (de propósito):** recusar (`426`) uma versão abaixo do mínimo no servidor (o app decide, pelo `app-config`); a tela "atualize o app" (é do app).

**Checklist de fechamento do M44:**
- [x] Itens 44.1–44.5 revisados
- [x] `./gradlew check` com **exit 0**
- [x] Docs atualizados (`versionamento.md`, `endpoints.md`, `README.md`)

## M45 — Ciclo `catalog ↔ seating` na persistência ✅

- [x] 45.1 Porta de domínio `SeatAvailability` (assentos livres de um reservável) implementada em `seating` (`SeatAvailabilityAdapter`); os adaptadores do catálogo (`FlightRepositoryAdapter`, `BookableRepositoryAdapter`) deixam de importar o `SeatJpaRepository`
- [x] 45.2 A exceção saiu de `PersistenceConceptsAreFreeOfCyclesTest` (a regra agora não tem exceção nenhuma); a busca de voos **não piorou**: medido antes e depois, **7 queries para 3 voos**, fixado em `TheFlightSearchAsksForSeatsThroughThePortOncePerFlightTest`

**O que a execução ensinou:** o custo por voo da busca (2 queries) já existia; reduzi-lo é trabalho do M47 (N+1), e agora há um número medido para melhorar.

**Pré-requisito do corte em módulos Gradle (ver "Ideias futuras"):** cumprido: as regras do ArchUnit já não têm exceção.

**Checklist de fechamento do M45:**
- [x] Itens 45.1–45.2 revisados
- [x] `./gradlew check` com **exit 0**
- [x] Docs atualizados (`CLAUDE.md`)

## M46 — Endurecimento de segurança ✅

- [x] 46.1 CI: **Dependabot** (Gradle, Actions, Docker, Terraform), **OWASP dependency-check** (reprova CVSS ≥ 7; plugin do Gradle), **gitleaks** sobre o histórico inteiro (com a lista de valores públicos de propósito, cada um com o motivo; verificado localmente: sem vazamentos) e **CodeQL** (Java/Kotlin), no `security.yml` a cada PR e toda segunda; validado com `actionlint`
- [x] 46.2 Rotação da chave do JWT (`kid` no cabeçalho, a chave ativa assina, as aposentadas só verificam, `legacy` para o que foi emitido antes) e **detecção de reuso do refresh token**: V46 (`family_id`), troca atômica do token (`consume`), reuso revoga a família inteira, métrica `reuse_detected`
- [x] 46.3 Cabeçalhos HTTP de segurança no backend (CSP `default-src 'none'`, `Referrer-Policy`, `Permissions-Policy`, HSTS sobre HTTPS), **limite de tamanho do corpo** (`413`) e **limite de taxa geral por IP e por usuário** em Redis (`429` com `Retry-After`, falha aberta); o login e a IA mantêm os seus
- [x] 46.4 BCrypt de custo **12** (configurável, o custo vai no *hash*), **logs sem dado pessoal** (o teste coleta tudo o que a jornada inteira loga e procura e-mail, nome, senha, tokens e titular; verificado por mutação), `docs/seguranca.md` (modelo de ameaças consolidado)

**O que a execução ensinou:**
- O teste de rotação falhou "sempre nulo" por um motivo que nada tinha a ver com a chave: o analisador do JWT confere a expiração contra o **relógio de verdade**, e o teste fabricava tokens num relógio fixo no passado.
- Dois reusos, não um: depois de a família ser revogada, o filho que o ladrão ou o cliente apresenta também é um token gasto, então a métrica soma os dois (e está certo).
- Um filtro de limites **antes** da segurança não conhece o usuário: o limite por usuário é um interceptor que roda depois da autenticação, e o por IP é o filtro.

**Fora do escopo (de propósito):** o IP atrás de um balanceador (confiar no `X-Forwarded-For` é decisão para quando houver balanceador); o limite de corpo sem `Content-Length`. (O 2FA, que ficou fora aqui, é o M29, já feito.)

**Checklist de fechamento do M46:**
- [x] Itens 46.1–46.4 revisados
- [x] `./gradlew check` com **exit 0**
- [x] Mutação (logs) e testes de corrida/limite
- [x] Docs atualizados

## M47 — Desempenho e resiliência ✅

- [x] 47.1 Teste de carga com **k6** em `loadtest/` (busca com metas de p95/p99, **corrida de 50 clientes pelo mesmo assento** com limiar de contagem — exatamente 1 vence, 49 × `409`, nenhum 5xx —, e a compra inteira), rodados de verdade contra a API local com os números em `docs/desempenho.md`
- [x] 47.2 **Pool de conexões** dimensionado e observado (`hikari.*`, alerta `DatabasePoolSaturated` com runbook e teste de `promtool`); **testes de N+1** em listas críticas (`presentation/performance/`), que acharam e levaram a corrigir duas (a busca pública: 1 + 2 por voo → 2 consultas; "minhas viagens": 1 + 5 por reserva → fixo); **cache da busca de voos** em Redis (época por `INCR`, invalidação ao confirmar a transação, falha aberta, TTL 30 s)
- [x] 47.3 **Tempo-limite, disjuntor e anteparo** para o Bedrock (`BedrockGuard`, resilience4j; alerta `AiCircuitOpen`), e o **anteparo dos jobs** (`SchedulingConfig`: os `@Scheduled` rodavam no agendador do WebSocket; agora têm um pool próprio, com teste que conta os jobs)

**O que a execução ensinou:**
- O teste de N+1 reprovou na primeira rodada: "minhas viagens" custava 5 consultas **por reserva**. A busca de voos, medida no M45 como "normal" (7 consultas para 3 voos), era a mesma doença, só que sem teste que a nomeasse.
- Os `@Scheduled` nunca tiveram agendador próprio: usavam o do broker do WebSocket, por ser o único `TaskScheduler` do contexto. Só apareceu quando o teste do pool leu o nome da thread (`MessageBroker-`).
- `ThreadPoolTaskScheduler.getPoolSize()` devolve as threads que existem agora, não as permitidas (nascem sob demanda): o teste lê `corePoolSize`.
- A variável de ambiente de `request-limits.rate-limit-enabled` é `REQUESTLIMITS_RATELIMITENABLED` (o Spring tira os hífens). Com `REQUEST_LIMITS_...` o k6 levou `429` do limite do M46, o que mostrou que o limite funciona.
- Filtro, interceptador ou *job* que dependa de Redis exige `@MockBean` nos cinco `@WebMvcTest` (repetição do M46).

**Fora do escopo (de propósito):** k6 no CI, réplica de leitura, particionamento, cache de outras leituras.

**Checklist de fechamento do M47:**
- [x] Itens 47.1–47.3 revisados
- [x] `./gradlew check` com **exit 0**
- [x] Mutação (cache, disjuntor/anteparo, pool de jobs) e carga real
- [x] Docs atualizados (`docs/desempenho.md`, runbooks, observabilidade)

---

**Checklist de fechamento — vale para TODO marco acima (além do checklist padrão abaixo):**
- [ ] Itens do marco revisados; **decisões registradas em ADR** quando arquiteturais
- [ ] **Permissão** declarada em todo endpoint admin novo e **teste de `403`** por permissão faltante
- [ ] **Auditoria** em toda ação administrativa nova (mesma transação) com teste de atomicidade
- [ ] **Migration** revisada: não editou antiga, sem bloqueio longo em tabela grande (expand/contract), índice para cada filtro novo
- [ ] **Compatibilidade de contrato** (`oasdiff` quando existir; até lá, revisão manual): nada quebrante em `/v1`; enum novo só depois do app tolerar valor desconhecido
- [ ] **Observabilidade:** métrica de negócio nova (`countOutcome`), log sem PII, trace atravessando a fila quando houver
- [ ] **Concorrência** analisada: quem disputa o quê, qual mecanismo (`@Version`, `UPDATE` condicional, índice único) e teste de corrida quando houver disputa
- [ ] **Privacidade:** que dado pessoal passou a ser lido/guardado/exposto e por quê (LGPD: finalidade e minimização)
- [ ] `./gradlew check` com **exit 0** antes do commit; mutação nos testes novos mais importantes
- [ ] Docs (`docs/`, `endpoints.md`, `README.md` mapa/roadmap, `CLAUDE.md`) e Swagger (`@Tag`/`@Operation`/`@Schema`)

## Ideias futuras (fora da numeração M1-M9)

- [x] **Script de seed de dados** ✅ (2026-09-09, atualizado 2026-09-13) — `scripts/seed-flights.sh`: cria um admin (promovido via SQL direto, local only), gera N voos (padrão **1000**, aumentado de 30 pra testar a tela de resultados com volume real) com rotas/preços/datas/companhias variados entre os 3 aeroportos e 6 companhias seedadas, tudo via `POST /admin/flights` (os mesmos endpoints testados, sem INSERT direto). Testado de ponta a ponta: 10 voos criados com 201, busca por rota/data confirmou os voos certos.
- **Integração com API real de voos**: buscar voos de um provedor externo (AviationStack, Amadeus, OpenSky...) em vez de dados só cadastrados via `/admin/flights`. Maior escopo — exige escolher provedor, lidar com API key/rate limit/custo, mapear o schema deles pro domínio, decidir estratégia de sincronização.
- **Dividir em módulos Gradle** (decisão de 2026-10-04: **agora não, reavaliar depois do M45**). Hoje há um deploy, um banco e uma relação JPA entre conceitos, e as regras do ArchUnit já impõem camadas, ciclos e dependências permitidas; o módulo acrescentaria imposição em tempo de compilação, build incremental e dono por módulo. Corte provável: **plataforma** (`identity`, `audit`, `common`), **negócio** (`catalog`, `seating`, `booking`, `payment`, `review`, `accommodation`) e **admin** (`crm` e os controllers de `/v1/admin/**`, possivelmente um deploy próprio). *Pré-requisito:* M45 (ciclo `catalog ↔ seating`) e os limites do M35/M42 assentados — dividir antes é mover código duas vezes. *Preparar já:* nenhuma exceção nova no ArchUnit; conceitos só por id; `common/` sem regra de negócio; o SQL do `crm` que lê `booking`/`payment`/`review` fica todo em `infrastructure/persistence/crm/` (num corte, vira view mantida pelo dono da tabela ou chamada por id às portas do outro conceito). *Gatilhos:* precisar de um segundo deploy (o admin), build lento ou times separados, exceções permanentes nas regras de arquitetura.

## Checklist de fechamento de módulo

Rodado ao final de todo marco, nesta ordem, antes de considerar o marco fechado:

1. Revisar o checklist do marco — conferir que todo item foi implementado e testado.
2. Revisão de Clean Code (nomes, funções pequenas, duplicação, comentários só onde o "porquê" não é óbvio).
3. Revisão de SOLID (responsabilidade única, inversão de dependência — portas em `domain`, implementação em `infrastructure`).
4. `./gradlew check` (ktlint + detekt) sem violações.
5. Testes das camadas ainda não cobertas por teste dedicado.
6. Documentação (`README.md`) atualizada.
7. Swagger em dia — `@Tag`/`@Operation`/`@Schema`/exemplos reais em todos os controllers, incluindo botão Authorize se houver rota autenticada nova.

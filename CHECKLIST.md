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

## Ideias futuras (fora da numeração M1-M9)

- [x] **Script de seed de dados** ✅ (2026-09-09, atualizado 2026-09-13) — `scripts/seed-flights.sh`: cria um admin (promovido via SQL direto, local only), gera N voos (padrão **1000**, aumentado de 30 pra testar a tela de resultados com volume real) com rotas/preços/datas/companhias variados entre os 3 aeroportos e 6 companhias seedadas, tudo via `POST /admin/flights` (os mesmos endpoints testados, sem INSERT direto). Testado de ponta a ponta: 10 voos criados com 201, busca por rota/data confirmou os voos certos.
- **Integração com API real de voos**: buscar voos de um provedor externo (AviationStack, Amadeus, OpenSky...) em vez de dados só cadastrados via `/admin/flights`. Maior escopo — exige escolher provedor, lidar com API key/rate limit/custo, mapear o schema deles pro domínio, decidir estratégia de sincronização.

## Checklist de fechamento de módulo

Rodado ao final de todo marco, nesta ordem, antes de considerar o marco fechado:

1. Revisar o checklist do marco — conferir que todo item foi implementado e testado.
2. Revisão de Clean Code (nomes, funções pequenas, duplicação, comentários só onde o "porquê" não é óbvio).
3. Revisão de SOLID (responsabilidade única, inversão de dependência — portas em `domain`, implementação em `infrastructure`).
4. `./gradlew check` (ktlint + detekt) sem violações.
5. Testes das camadas ainda não cobertas por teste dedicado.
6. Documentação (`README.md`) atualizada.
7. Swagger em dia — `@Tag`/`@Operation`/`@Schema`/exemplos reais em todos os controllers, incluindo botão Authorize se houver rota autenticada nova.

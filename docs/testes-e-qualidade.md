# Testes e qualidade de código

[← Voltar ao README](../README.md)

Como o projeto é testado e quais verificações automáticas protegem a qualidade.

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
- **Aplicação** (`application/loginusecase`, `application/registeruserusecase`, `application/registerflightusecase`, `application/registerbookingusecase`, `application/cancelbookingusecase`, `application/getseatmapusecase`, `application/suggestflightsusecase`): regras dos casos de uso com repositórios/hasher/token service/broadcaster fake — inclui a geração do mapa de assentos, o lock otimista por assento e a liberação no cancelamento.
- **Apresentação** (`presentation/flightadmincontroller`, `presentation/flightsearchcontroller`, `presentation/bookablecontroller`): contrato HTTP (status code, shape do JSON, mapeamento de exceção) com o caso de uso mockado via `@WebMvcTest` — segurança desligada nesses slices de propósito (ver `securityintegration`).
- **Concorrência** (`BookingConcurrencyTest`): duas threads disputando o mesmo assento (lock otimista por `Seat`, item 10.6) contra o Postgres real.
- **Segurança** (`presentation/securityintegration`): `@SpringBootTest` completo — rota admin sem token (401) e com role errada (403), reserva exige autenticação, dono vs. não-dono de reserva vs. ADMIN, rotação de refresh token (reuso rejeitado), busca pública sem token.
- **Tempo real** (`infrastructure/messaging/availabilitybroadcast`): cliente STOMP real (não mock) conecta autenticado, assina o tópico de disponibilidade, dispara uma reserva e recebe o evento publicado via Redis Pub/Sub; e um `CONNECT` sem token válido é rejeitado.
- **IA** (`application/suggestflightsusecase`, `infrastructure/ai/bedrockaisuggestionservice`, `infrastructure/web/airatelimitinterceptor`): caso de uso com fakes (inclui o log sendo salvo tanto no sucesso quanto na falha), construção do prompt/parse da resposta do Bedrock isolados de qualquer chamada de rede, e o rate limiter (5/min, escopo por usuário) exercitado diretamente — nada disso depende de credencial AWS real pra rodar.

Esses últimos usam [Testcontainers](https://testcontainers.com/) (`AbstractIntegrationTest`) — sobem Postgres e Redis descartáveis sozinhos, não precisam mais de `docker compose up -d` manual. Só exigem Docker instalado e rodando.

**Cobertura (JaCoCo, 2026-09-09):** `./gradlew check` roda `jacocoTestCoverageVerification` com um mínimo de 75% de cobertura de linha, excluindo entidades JPA/DTOs (dados puros, sem lógica própria) e o `main()`. Relatório em `build/reports/jacoco/test/html/index.html` após `./gradlew jacocoTestReport`. Localmente, com o Docker rodando, `./gradlew check` mede o mesmo que o CI.

> **Nota:** o cliente Docker interno do Testcontainers 1.20.4 negocia a API 1.32 por padrão, e o Docker Desktop recente recusa isso (`Could not find a valid Docker environment`). O `build.gradle.kts` fixa `api.version=1.41` como propriedade de sistema da task `test` (a variável `DOCKER_API_VERSION` não é lida por esse cliente), então os testes de integração rodam localmente igual ao CI. Foi essa falha local que, por meses, fez parecer "normal" ver só o CI vermelho — e escondeu dois testes realmente quebrados.

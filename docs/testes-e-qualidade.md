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
- **Preço da reserva** (`domain/booking`, `application/payment`, `infrastructure/persistence/booking`, `presentation/securityintegration`): a reserva congela o preço do voo na criação e o mantém ao ser confirmada ou cancelada; o pagamento cobra o da **reserva** e não o atual do voo; e, contra o Postgres real, reajustar o voo depois da reserva não altera o preço dela nem o valor cobrado, e "Minhas Viagens" mostra os dois (`price` da reserva e `flight.price` atual).
- **Auditoria** (`domain/audit`, `application/audit`, `infrastructure/persistence/audit`, `presentation/audit`): a lista permitida do "antes/depois", os casos de uso que gravam (e o que **não** gravam: o dono cancelando a própria reserva, um voo recusado), o `rollback` conjunto da mudança e do registro, a imutabilidade pelo banco (gatilho), a paginação por cursor sem lacuna nem repetição, os filtros e, de ponta a ponta, o registro de um voo criado, de uma tentativa negada e a leitura da trilha só com `AUDIT_READ`.
- **Arquitetura** (`architecture`): regras do ArchUnit — camadas, ciclos entre conceitos, todo controller de negócio sob uma versão, todo caso de uso observado e, no M25, **todo endpoint `/admin` declara a sua permissão com `@PreAuthorize`** (`EveryAdminEndpointDeclaresItsPermissionTest`; só o controller marcado com `@PublicEndpoints` é exceção).
- **Concorrência** (`BookingConcurrencyTest`): duas threads disputando o mesmo assento (lock otimista por `Seat`, item 10.6) contra o Postgres real.
- **Segurança** (`presentation/securityintegration`): `@SpringBootTest` completo — rota admin sem token (401) e sem a permissão (403, por papel: `SUPPORT` não escreve voo, `CATALOG_MANAGER` escreve), reserva exige autenticação, dono vs. não-dono de reserva vs. quem tem `BOOKING_CANCEL_ANY`, rotação de refresh token (reuso rejeitado), busca pública sem token; e a **sessão do portal**: cookie `httpOnly`, cliente recusado como senha errada, renovação/saída com `Origin`, bloqueio e rebaixamento valendo na próxima chamada, limite de tentativas (429) e o `code` dos erros.
- **Contas e login** (`application/identity`, `domain/identity`, `infrastructure/security`, `infrastructure/persistence/identity`): matriz de permissões por papel, bloqueio/desbloqueio do `User` (e o `rename` que não pode desfazê-lo), política de senha, casos de uso de login/renovação/bloqueio/saída com fakes e relógio fixo, o limitador em Redis (e o seu comportamento com o Redis fora do ar) e o `UPDATE` direto do último acesso contra um bloqueio concorrente.
- **Equipe e convites** (`domain/identity`, `application/identity`, `infrastructure/persistence/identity`, `infrastructure/email`, `presentation/identity`, `presentation/staffmanagement`): o convite (estados pelo relógio, uso único, reemissão), a garantia de que a auditoria não leva o e-mail do convidado, os casos de uso com fakes em memória compartilhados (`application/identity/staff`) e um `committed {}`/`rolledBack {}` que roda — ou não — o que fica para depois do commit (o e-mail só sai se houve commit); o aceite (token desconhecido, expirado, revogado e usado dão o mesmo erro; senha fraca não gasta o link), as travas do último `SUPER_ADMIN`, auto-alteração e cliente-como-staff, a troca de senha, o bootstrap e o executor da subida (metade configurado derruba); a persistência do convite (um aberto por e-mail, versão velha recusada, trava de linha exige transação); e, de ponta a ponta, convite → e-mail → aceite → login no portal, 403 por permissão, o mesmo `400` para qualquer token ruim, reconvidar e reenviar matando o link anterior, a lista sem token, troca de papel encerrando a sessão com auditoria, bloquear/desbloquear. O e-mail de teste é capturado por um `RecordingEmailSender` (`RecordingEmailConfig`, importado por `AbstractIntegrationTest`: um único contexto para todos). A corrida entre dois `SUPER_ADMIN` não tem teste HTTP porque o banco é compartilhado entre as classes; a regra é testada com fakes e a trava (`lockActiveByRole`) na persistência.
- **Tempo real** (`infrastructure/messaging/availabilitybroadcast`): cliente STOMP real (não mock) conecta autenticado, assina o tópico de disponibilidade, dispara uma reserva e recebe o evento publicado via Redis Pub/Sub; e um `CONNECT` sem token válido é rejeitado.
- **IA** (`application/suggestflightsusecase`, `infrastructure/ai/bedrockaisuggestionservice`, `infrastructure/web/airatelimitinterceptor`): caso de uso com fakes (inclui o log sendo salvo tanto no sucesso quanto na falha), construção do prompt/parse da resposta do Bedrock isolados de qualquer chamada de rede, e o rate limiter (5/min, escopo por usuário) exercitado diretamente — nada disso depende de credencial AWS real pra rodar.

Esses últimos usam [Testcontainers](https://testcontainers.com/) (`AbstractIntegrationTest`) — sobem Postgres e Redis descartáveis sozinhos, não precisam mais de `docker compose up -d` manual. Só exigem Docker instalado e rodando.

**Cobertura (JaCoCo, 2026-09-09):** `./gradlew check` roda `jacocoTestCoverageVerification` com um mínimo de 75% de cobertura de linha, excluindo entidades JPA/DTOs (dados puros, sem lógica própria) e o `main()`. Relatório em `build/reports/jacoco/test/html/index.html` após `./gradlew jacocoTestReport`. Localmente, com o Docker rodando, `./gradlew check` mede o mesmo que o CI.

> **Nota:** o cliente Docker interno do Testcontainers 1.20.4 negocia a API 1.32 por padrão, e o Docker Desktop recente recusa isso (`Could not find a valid Docker environment`). O `build.gradle.kts` fixa `api.version=1.41` como propriedade de sistema da task `test` (a variável `DOCKER_API_VERSION` não é lida por esse cliente), então os testes de integração rodam localmente igual ao CI. Foi essa falha local que, por meses, fez parecer "normal" ver só o CI vermelho — e escondeu dois testes realmente quebrados.

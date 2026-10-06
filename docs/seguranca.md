# Segurança

O modelo de ameaças consolidado: o que protegemos, de quem, com o quê e onde cada defesa é **testada**. Cada linha aponta para o documento do assunto; este é o mapa.

## O que há para proteger

| Ativo | Por que importa |
|---|---|
| Contas e sessões (JWT, refresh tokens) | quem as toma age como o cliente, ou como a equipe |
| Dinheiro (pagamentos, reembolsos, cupons) | a cobrança duas vezes, o reembolso duas vezes, o cupom gasto além do limite |
| Dados pessoais (nome, e-mail, histórico) | LGPD: finalidade, minimização, apagar e exportar |
| Estoque (assentos, quartos por noite) | vender o mesmo duas vezes |
| Ações administrativas | quem pode fazer o quê, e a prova de quem fez |

## Quem ataca e como (e o que impede)

| Ameaça | Defesa | Onde | Teste |
|---|---|---|---|
| Adivinhar senha | limite de tentativas por e-mail e por IP (Redis, falha aberta), custo do BCrypt **12** escrito no próprio *hash*, tempo igual para e-mail inexistente | [autenticacao.md](autenticacao.md) | `ThePasswordCostIsInTheHash...`, os testes do login |
| Roubar um refresh token | rotação de uso único; **reuso de um token já gasto revoga a família inteira** (a cadeia daquele login), com a troca atômica (dois refreshes juntos: um vence, o outro é reuso); métrica `dbook.auth.refresh{outcome=reuse_detected}` | `RefreshTokenUseCase`, V46 | `ASpentTokenSeenAgainRevokesTheWholeFamilyTest` |
| Vazar a chave do JWT | **rotação**: a chave ativa assina e leva o `kid`; as aposentadas só verificam, até o último token assinado com elas morrer; sem `kid` vale a chave `legacy` ou a ativa | `JwtKeyProperties`, [Rotacionar a chave](#rotacionar-a-chave-do-jwt) | `TheSigningKeyRotatesWithoutLoggingEveryoneOutTest` |
| Cadastrar-se com o e-mail de outra pessoa; **tomar uma conta pelo "esqueci a senha"** | o link vai só para a caixa de entrada, é de **uso único, 1 hora** (48 h o de confirmação), guardado só como *hash*; o pedido responde igual exista a conta ou não (e o e-mail sai em outra *thread*); escolher a nova senha **encerra todas as sessões**; reservar e pagar esperam o e-mail confirmado | [autenticacao.md](autenticacao.md#recuperação-de-conta-e-mail-confirmado-e-senha-esquecida) | `AForgottenPasswordIsRecoveredWithTheLinkTest`, `WhenRequiredBookingAndPayingWaitForTheConfirmedEmailTest` (mutações verificadas) |
| Senha da equipe roubada ou adivinhada | **segundo fator TOTP** (RFC 6238, segredo cifrado com AES-GCM, código de uso único, recuperação por códigos de uma vez só), obrigatório para `SUPER_ADMIN` em produção; o `POST /v1/auth/login` do cliente **não** é uma porta em volta (`403 TWO_FACTOR_REQUIRED`); reset por outro `SUPER_ADMIN`, com motivo, auditado | [autenticacao.md](autenticacao.md#segundo-fator-para-a-equipe-totp) | `TheSecondStepOpensTheSessionWithAGoodCodeOnlyOnceTest`, `TheClientLoginIsNotADoorAroundTheSecondFactorTest` (mutações verificadas) |
| Usar um token no lugar de outro (o de renovação como acesso) | cada token leva `use`; só `access` abre endpoint | `JwtTokenService` | `AnAccessTokenIsTheOnlyTokenThatOpensAnEndpointTest` |
| Escalar privilégio | a permissão, nunca o papel, guarda cada endpoint; papel e status vêm **do banco** em `/v1/admin/**` (bloquear vale na próxima chamada); a regra do ArchUnit reprova endpoint admin sem `@PreAuthorize` | [autenticacao.md](autenticacao.md) | `EveryAdminEndpointDeclaresItsPermissionTest` |
| Ler ou mexer no dado de outro cliente | o id vem do token, **nunca** do corpo; o dono é checado no caso de uso (`403`/`404`) | cada controller | os testes de dono × terceiro de cada marco |
| Cobrar, reembolsar ou gastar duas vezes | `Idempotency-Key` por pedido, um reembolso vivo por reserva (índice único), consumo atômico do cupom, uma contagem condicional por noite | [reembolso.md](reembolso.md), [promocoes.md](promocoes.md), [hoteis.md](hoteis.md) | os testes de corrida (20 *threads*) |
| Inundar a API | **limite por IP** (300 por minuto) antes de qualquer processamento e **por usuário** (600) depois do login, em Redis, com `Retry-After`; o limite do login e o da IA seguem à parte | `RequestLimitsFilter`, `UserRateLimitInterceptor` | `TooManyRequestsFromOneAddressOrOneUserAreRefusedTest` |
| Corpo gigante | `Content-Length` acima de 256 KB é `413` na hora (5 MB só na importação CSV) | `RequestLimitsFilter` | `ABodyAboveTheLimitIsRefusedBeforeItIsReadTest` |
| Roubo de página / clickjacking / *sniffing* | `Content-Security-Policy: default-src 'none'; frame-ancestors 'none'`, `X-Frame-Options`, `X-Content-Type-Options`, `Referrer-Policy`, `Permissions-Policy`, HSTS sobre HTTPS (o Swagger UI, que é página, fica de fora da CSP); o portal tem os seus no CloudFront | `SecurityConfig`, `terraform/modules/portal` | `EveryResponseCarriesTheSecurityHeadersTest` |
| Dado pessoal ou segredo no log | o log nunca recebe corpo, cabeçalho de autorização nem *query string*; o **teste coleta tudo o que a aplicação loga** numa jornada inteira (registro, login errado, refresh, reserva, pagamento) e procura e-mail, nome, senha, tokens e titular do cartão | `RequestLoggingFilter` | `NoPersonalDataOrSecretReachesTheLogsTest` (verificado por mutação) |
| Abusar do contrato (`/v1` quebrada em silêncio) | o contrato de cada versão é commitado e o CI roda o `oasdiff` contra a base | [versionamento.md](versionamento.md) | job `contract` |
| Dependência vulnerável, segredo no repositório, código inseguro | Dependabot (semanal), OWASP *dependency-check* (CVSS ≥ 7 reprova), **gitleaks** no histórico inteiro, **CodeQL** (Java/Kotlin); rodam em todo PR e toda segunda-feira | `.github/workflows/security.yml`, `.github/dependabot.yml`, `.gitleaks.toml` | o próprio CI |
| Infraestrutura insegura | `checkov` e `tflint` sem credenciais, com a lista de exceções justificada | [custos.md](custos.md) | job `terraform` |
| Anonimizar e exportar | a anonimização apaga o que deve e guarda o que precisa; a exportação traz tudo | [crm.md](crm.md) | os testes do M30 |

## Rotacionar a chave do JWT

1. Gerar uma chave nova de 256 bits em Base64.
2. Implantar com `JWT_KEYS_RETIRED_<IDANTIGO>=<segredo antigo>` (a aposentada, só verifica), `JWT_SECRET=<segredo novo>` e `JWT_KEYS_ACTIVE_KEY_ID=<id novo>`: os tokens em circulação continuam valendo e os novos já saem com a chave nova.
3. Esperar o refresh token mais longo expirar (7 dias) e **remover** a chave aposentada: o que sobrou assinado com ela deixa de valer.
4. Para tokens emitidos **antes** de a rotação existir (sem `kid`), a primeira rotação deve aposentar o segredo antigo com o id `legacy`.

## O que ainda não está coberto

- **O *proxy* na frente da API:** o IP do limite é o do endereço remoto; atrás de um balanceador é preciso confiar explicitamente no `X-Forwarded-For` (ainda não há balanceador).
- **Corpo sem `Content-Length` declarado** (*chunked*) não passa pelo limite de tamanho do filtro.
- **WebAuthn / chaves de segurança** como segundo fator (hoje só TOTP): mais forte contra *phishing*, fica como evolução.
- **Tempo de resposta do "esqueci a senha":** o e-mail sai em outra *thread*, mas a conta existente faz algumas escritas a mais no banco (milissegundos): não é igual ao nanossegundo, só ao que se mede pela rede.
- **Cliente com 2FA:** o segundo fator é da equipe (M29); para o cliente do app seria outro desenho (recuperação de conta, SMS), não feito.
- **Rotação automática de segredos**, KMS gerenciado por nós e WAF: registrados como decisão de custo em `terraform/.checkov.yaml`.

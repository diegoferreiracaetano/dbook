---
name: code-review
description: Revisão de código do backend Kotlin/Spring do DBook, focada em problemas reais (violação de camada, autorização, transação, concorrência, contrato da API consumido pelo app Flutter, migrations, testes ausentes) e não em estilo cosmético. Use SEMPRE que o pedido for revisar um diff, PR, branch ou trecho de código deste repositório, ou quando o dono perguntar "tá bom?", "revisa isso", "antes de commitar" — e como autorrevisão final antes de declarar uma tarefa pronta.
---

# Code review — backend DBook

**Princípio:** um comentário só vale se aponta algo que quebra, vaza, corrompe dado, esconde bug ou torna a próxima mudança mais cara. Ktlint e detekt já cobrem formatação e várias regras de estilo — **não repita o que a ferramenta pega** (rode `./gradlew ktlintCheck detekt` e cite só o que sobra). Se não achou problema real, diga isso; não fabrique sugestão para justificar a revisão.

## Como revisar

1. Leia o diff inteiro e depois o **irmão mais próximo** de cada arquivo novo (use case parecido, teste parecido) — desvio do padrão existente é o achado mais comum.
2. Percorra os eixos abaixo, na ordem (mais grave primeiro).
3. Rode `./gradlew ktlintCheck detekt` e os testes do pacote afetado; relate o que rodou.
4. Classifique cada achado e escreva o **impacto concreto** ("usuário A cancela reserva do usuário B"), não a regra abstrata.

## Eixos (o que procurar neste projeto)

**1. Segurança / autorização (prioridade máxima)**
- Endpoint novo fora de `SecurityConfig.permitAll()` é autenticado por padrão — ele *deveria* ser público? Ou o contrário: algo sensível entrou em `permitAll`?
- Recurso do usuário: o use case valida dono (`booking.customerId != requestingUserId → NotBookingOwnerException`)? O id vem de `authentication.currentUserId()` e **não** do corpo/path?
- Endpoint admin: `@PreAuthorize("hasAuthority('...')")` presente (nunca `hasRole`) e coberto por teste 401/403?
- Segredo/credencial em código ou config commitada; log de token/senha; dado sensível na resposta (`passwordHash`, número de cartão — o projeto só guarda `cardLast4`).

**2. Arquitetura**
- Import proibido: `domain` → Spring/JPA; `application`/`presentation` → `infrastructure`; `infrastructure` → `application`. Regra de negócio em controller ou adapter. Entidade JPA vazando no JSON.
- Abstração nova sem 2º uso; camada/pacote por feature (o projeto é plano por camada).

**3. Correção e tratamento de erros**
- Exceção de "não encontrado" nova **não registrada** em `ApiExceptionHandler` (vira 500) — checar de verdade: `grep` a classe no handler.
- `require`/`check` no lugar certo (400 vs 409); `!!` em produção; `catch` genérico/engolido (`TooGenericExceptionCaught`, `SwallowedException`).
- Validação parcial: o use case processa N itens e falha no meio? Toda validação deve vir **antes** de mutar (ver `RegisterPaymentUseCase`: valida todos os donos antes de salvar).

**3b. Transação e concorrência**
- Escrita sem `@Transactional`; efeito externo (broadcast Redis/STOMP) fora de `afterCommit { }`.
- Estado compartilhado que precisa de lock: `@Version` presente? Dois clientes no mesmo assento → 409, não 500 nem duplicidade.
- Reserva de assento acontece na criação da `Booking`; pagamento só confirma. Mudança que inverta isso reabre a corrida do assento.

**4. Persistência / migrations**
- Migration editada em vez de nova; coluna `NOT NULL` sem backfill; `ddl-auto` ≠ `validate`; entidade e migration divergentes (o boot quebra na validação).
- N+1 / lazy fora de transação (`open-in-view: false`): acessar associação lazy no controller estoura `LazyInitializationException`.
- Mapper esquecendo o campo novo (ida e volta `toDomain`/`toJpaEntity`).

**5. Contrato da API (impacto no app Flutter)**
- O app `../dbook-mobile` espelha as respostas (`*ResponseDto` → entidades de `dbook_domain`). Renomear/remover campo, trocar tipo ou status code é **mudança quebrando cliente**: exija que o campo antigo continue ou que o mobile seja atualizado junto. Campo novo opcional é seguro.
- Swagger (`@Tag`/`@Operation`/`@Schema`) e `docs/endpoints.md` atualizados para endpoint novo.

**6. Testes**
- O caminho feliz **e** cada falha (400/401/403/404/409) tem cenário? Padrão um-cenário-por-classe / `given-when-then`? Fake em vez de mock onde o projeto usa fake?
- Teste que passa por motivo errado (asserção fraca, mocka a coisa testada), `Thread.sleep`, dependência de horário/ordem.
- Não exija teste para DTO/getter trivial.

**7. Complexidade e legibilidade** (só se atrapalhar de verdade)
- Método que estoura os limites do detekt (60 linhas / complexidade 15 / 6 params) ou mistura níveis de abstração; duplicação que um helper existente resolve. Nome enganoso. Comentário que explica *o quê* em vez de *por quê*.

**8. Performance** (só com evidência)
- Loop com query por item onde uma query por lista resolve (`findByCustomerId`-style); carregar tudo para filtrar em memória em tabela grande (`flight` cresce: a busca usa índice por rota/data?). Não sugira otimização especulativa.

## Formato do relatório

```
## Veredito: <aprovar | aprovar com ressalvas | pedir mudanças>
### Bloqueadores        (quebra, vaza, corrompe — corrigir antes de mergear)
- <arquivo:linha> — <problema> → <impacto concreto> → <correção sugerida>
### Importantes         (bug provável, teste faltando, quebra de padrão relevante)
### Observações         (opcional, no máximo 3; só o que tem benefício real)
### Validações executadas
- ktlintCheck/detekt: <resultado>; testes: <o que rodou / o que não pôde rodar e por quê>
```

Ordene por gravidade. Um achado por item. Sem elogio genérico, sem lista de "nits" de formatação, sem reescrever o código do autor quando uma frase basta. Se algo que você suspeita não pôde ser confirmado, marque como **suspeita** e diga como verificar.

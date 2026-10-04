# Versionamento da API

[← Voltar ao README](../README.md)

Como a API de negócio é versionada, por quê, e como evoluir o contrato sem quebrar quem já o usa.

## Por que versionar

O consumidor principal é o **app mobile**. Ao contrário de uma página web, não dá para forçar todo mundo a atualizar: versões antigas do app continuam chamando o backend por meses. Sem versão, qualquer mudança que quebre o contrato quebra esses usuários. Com versão, o backend passa a responder em `/v1` **e** em `/v2` ao mesmo tempo, e cada cliente migra quando atualizar.

## Como fica: a versão no caminho

| Parte | Caminho | Versionado? |
|---|---|---|
| API de negócio | `/v1/bookings`, `/v1/payments`, `/v1/auth/login`, ... | **sim** |
| Saúde da aplicação | `/health` | não (operacional) |
| WebSocket | `/ws` | não (por ora; o contrato dele — tópicos e formato da mensagem — pode ganhar versão se mudar) |
| Swagger | `/swagger-ui`, `/v3/api-docs/...` | não |
| Actuator | `:8081/actuator/...` | não (porta de gestão própria) |

Escolhi a versão **no caminho** (e não em cabeçalho ou tipo de mídia) porque ela aparece sozinha nos logs, nas métricas (a etiqueta `uri` já mostra `/v1/bookings`), no Swagger e em qualquer `curl`, e porque é o mais simples para o app.

Os caminhos antigos, sem versão, **deixaram de existir** (`GET /destinations` agora responde `401`/`404`). Como não havia app publicado, não valia manter dois contratos. Com clientes reais, o certo seria manter o alias antigo por um tempo, marcado como obsoleto (ver [Como aposentar uma versão](#como-aposentar-uma-versão)).

## O que é, e o que não é, mudança que quebra

| Quebra o cliente (pede `/v2`) | Não quebra (pode entrar na própria `/v1`) |
|---|---|
| remover ou renomear um campo | **acrescentar** um campo opcional na resposta (foi assim com `averageRating` e `review`) |
| mudar o tipo de um campo | acrescentar um endpoint novo |
| tornar obrigatório o que era opcional (ex.: o `Idempotency-Key` do `POST /payments` **seria** uma quebra com clientes reais) | aceitar um valor a mais num campo de entrada |
| mudar o significado de um código de status ou de um valor | corrigir um erro que devolvia `500` |

## Como é implementado

- **`ApiPaths.V1`** (`presentation/common/ApiPaths.kt`) é a constante `"/v1"`. Cada controller de negócio se mapeia sob ela: `@RequestMapping("${ApiPaths.V1}/bookings")`.
- **A versão vive só na camada `presentation`.** Casos de uso e domínio não sabem que existem versões; por isso uma `v2` nunca duplica regra de negócio, só controllers e DTOs.
- **Segurança:** as rotas públicas em `SecurityConfig` são listadas com o prefixo (`/v1/auth/login`, `/v1/flights/search`, ...). Uma rota pública nova precisa entrar lá como `/v1/...`, senão devolve `401`.
- **Limite de uso da IA:** o interceptor vale para `/v1/ai/**`.
- **Swagger:** um grupo por versão (`v1`) e um para os operacionais (`operational`); o Swagger UI mostra um seletor. O customizador dos exemplos de parâmetro é **global** (um customizador comum não é aplicado a grupos).
- **App mobile:** os repositórios usam caminhos relativos sobre um único `baseUrlProvider`; a versão é só o fim da base (`http://host:8080/v1`). O WebSocket usa só host e porta da base, então **não** herda o `/v1`.

## Como o projeto se protege

| Proteção | Onde |
|---|---|
| todo controller de negócio está sob uma versão | `EveryApiControllerIsVersionedTest` (ArchUnit; verificado por mutação) |
| `domain` e `application` não conhecem a versão | as regras de camada já proíbem importar `presentation`, onde `ApiPaths` mora |
| os grupos do Swagger mantêm os exemplos | `EachVersionGroupCarriesTheParameterExamplesTest` (verificado por mutação) |
| a base com versão e o WebSocket sem versão no app | `dbook_dio_client_test.dart` e `dbook_live_availability_versioned_base_test.dart` |

## Como lançar uma `v2` (quando precisar)

Suponha que `GET /destinations` passe a devolver `rating: { average, count }` em vez de `averageRating`. Isso quebra clientes da `v1`.

1. Crie o DTO novo (`DestinationResponseV2`) e um controller novo mapeado em `/v2/destinations`, que chama o **mesmo caso de uso**.
2. Os endpoints que **não** mudaram respondem nas duas versões, para o cliente trocar a base de uma vez: `@RequestMapping("/v1/bookings", "/v2/bookings")`.
3. Acrescente `ApiPaths.V2`, as rotas públicas `/v2/...` em `SecurityConfig` e um grupo `v2` no Swagger.
4. O app novo passa a usar `.../v2`; o app antigo continua na `v1`, intacto.

## Como aposentar uma versão

Uma versão só pode sair quando ninguém mais a usa, e a observabilidade responde isso com uma consulta:

```promql
sum(rate(http_server_requests_seconds_count{uri=~"/v1/.*"}[1d]))
```

O ciclo recomendado: lançar a `v2` → marcar a `v1` como obsoleta (cabeçalhos `Deprecation` e `Sunset`, a data de fim) → acompanhar no dashboard o tráfego da `v1` cair → remover. Este mecanismo de cabeçalhos **ainda não foi implementado**: não era necessário enquanto só existe a `v1`.

# IA: sugestões de voo via AWS Bedrock

[← Voltar ao README](../README.md)

Busca em linguagem natural, auditada e com limite de uso.

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

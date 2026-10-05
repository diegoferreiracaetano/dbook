# Custos

Uma estimativa do que a infraestrutura de `terraform/` custa por mês na AWS (região `us-east-1`, preços de tabela, sem
desconto e sem o nível gratuito), para decidir com os olhos abertos antes de um `apply`. **É uma estimativa de ordem de
grandeza, não uma fatura**: não há conta AWS persistente por trás deste projeto, então nenhum desses números foi medido
(o `terraform plan` real que daria o recurso exato também não existe aqui: ver "O que o CI prova e o que não prova").

| Recurso | Configuração | ≈ US$/mês |
|---|---|---|
| ECS Fargate | 1 tarefa de 0,5 vCPU e 1 GB, 24 h por dia | 18 |
| IPv4 público da tarefa | sem balanceador: a tarefa é a porta de entrada | 4 |
| RDS PostgreSQL 16 | `db.t4g.micro`, 20 GB, uma zona | 14 |
| ElastiCache Redis 7 | `cache.t3.micro`, um nó | 12 |
| Secrets Manager | 5 segredos (banco, JWT, administrador, chave do 2FA e o par do `random`) a US$ 0,40 | 2 |
| SQS | 2 filas e 2 mensagens mortas; o consumidor com *long polling* faz ~3 chamadas por minuto por fila | < 1 |
| CloudWatch | logs com 7 dias de retenção e 2 alarmes das filas de mensagens mortas | 2 |
| S3 + CloudFront | o portal estático, `PriceClass_100`, tráfego de equipe | < 1 |
| ECR | as imagens (retenção imutável, uma por commit) | 1 |
| SES | US$ 0,10 por mil e-mails (convites e avisos) | < 1 |
| **Total** | | **≈ 55** |

**O que mais pesa** são três coisas ligadas o tempo todo: a tarefa, o banco e o Redis (≈ 80 % da conta). Não há *NAT
Gateway* (a tarefa roda em sub-rede pública) justamente porque seria o maior item sozinho (≈ US$ 33 por mês mais o tráfego).

**O que faria a conta subir:** um balanceador de carga (≈ US$ 18, e o fim do IP público por tarefa), Multi-AZ no banco
(dobra o banco), mais de uma tarefa, o WAF no CloudFront (≈ US$ 5 mais US$ 1 por regra e por milhão de requisições), logs
com retenção longa e o tráfego real do portal.

**Para zerar:** `terraform destroy -var="use_localstack=false"`. O RDS, o Redis e a tarefa cobram por hora, mesmo sem uso.

## O que o CI prova e o que não prova

O job `terraform` do `ci.yml` roda **sem credenciais e sem estado**: `terraform fmt -check`, `terraform validate`,
`tflint` (declarações sem uso, versões, valores que a AWS recusaria) e `checkov` (configurações inseguras). Isso pega erro
de sintaxe, de referência e de segurança. **Não pega** o que só um `plan` contra uma conta enxerga (limites da conta,
nomes já usados, permissões que faltam ao papel de implantação): sem uma conta AWS persistente isso fica registrado como
pendente, e o `check-aws` do `cd.yml` mantém o pipeline desligado até o segredo `AWS_DEPLOY_ROLE_ARN` existir.

O `checkov` roda com `terraform/.checkov.yaml`, em que cada verificação ignorada **tem o motivo na linha** (um site
estático e um projeto de estudo; a infraestrutura anterior ao M43 que forçaria recriar o banco). Uma verificação nova que
falhe quebra o build.

## Antes de mandar e-mail de verdade (SES)

O SES começa em *sandbox*: só envia para endereços verificados. Sair dela é um pedido ao suporte da AWS (a Terraform não
faz). A identidade (`ses_from_address` ou `ses_domain`) é criada pelo módulo `ses`; com um domínio, os registros `CNAME` do
DKIM saem na saída `ses_dkim_tokens`. **Pendente na aplicação:** só existe o `LoggingEmailSender` (imprime no log); o
adaptador do SES é o que usaria essa identidade e essa permissão.

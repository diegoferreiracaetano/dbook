# Nuvem e CI/CD

[← Voltar ao README](../README.md)

Infraestrutura como código (Terraform + AWS) e os pipelines de CI e CD.

## Nuvem (Terraform + AWS)

Infraestrutura como código em `terraform/`, organizada em módulos reutilizáveis (`terraform/modules/{vpc,ecr,rds,redis,secrets,ecs}`) amarrados pelo módulo raiz (`terraform/`). `terraform/bootstrap` cria o bucket S3 + tabela DynamoDB usados como backend de state remoto — aplicado uma vez, separado do resto (não dá pra guardar o state de quem cria o lugar de guardar o state).

**Decisão central: LocalStack por padrão, AWS real só sob demanda.** Toda a stack roda de graça contra um [LocalStack](https://www.localstack.cloud/) local (sobe junto no `docker compose up -d`, serviço `localstack`) — é o alvo padrão (`use_localstack = true`). Apontar pra uma conta AWS real exige a flag explícita:

```bash
cd terraform
terraform init -reconfigure -backend-config=backend-aws.hcl
terraform plan -var="use_localstack=false"
terraform apply -var="use_localstack=false"
```

> **Nota:** a LocalStack **community** (gratuita) só emula de verdade S3, DynamoDB, EC2 (VPC/security groups), IAM e Secrets Manager. ECR, ECS, RDS, CloudWatch Logs e ElastiCache são recursos **Pro-only** — contra a community, esses módulos só validam via `terraform plan` (sintaxe e grafo de dependências), não `apply`. A validação de comportamento real desses módulos foi feita uma vez contra um AWS Academy Learner Lab (ver `CHECKLIST.md` pra detalhes e descobertas do processo).

**O que a infraestrutura provisiona:**
- VPC com 2 subnets públicas + 2 privadas (2 AZs), 1 NAT Gateway
- ECR (repositório de imagem, tags imutáveis)
- RDS Postgres + ElastiCache Redis, ambos em subnet privada, só alcançáveis pelo security group da aplicação
- Secrets Manager (senha do banco + segredo JWT — nunca hardcoded, gerados via `random_password`)
- ECS Fargate (cluster + task definition + service), sem Application Load Balancer neste marco (task recebe IP público direto)

```bash
docker build -t dbook:latest .
# depois de `terraform apply` criar o repositório ECR:
aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin <account-id>.dkr.ecr.us-east-1.amazonaws.com
docker tag dbook:latest <account-id>.dkr.ecr.us-east-1.amazonaws.com/dbook:latest
docker push <account-id>.dkr.ecr.us-east-1.amazonaws.com/dbook:latest
```

**Não esquecer:** `terraform destroy -var="use_localstack=false"` ao terminar de usar a AWS real — NAT Gateway e RDS cobram por hora rodando, mesmo em contas de estudo.


## O que o M43 acrescentou (SQS, SES, segredos, o portal)

- **Filas** (`modules/sqs`): `dbook-booking-expiration` e `dbook-notifications`, cada uma com a sua fila de mensagens mortas (3 tentativas), **criptografia gerenciada** e um **alarme** no CloudWatch para qualquer mensagem na morta (o par do `dbook_sqs_dlq_depth` e do alerta `DeadLetterQueueNotEmpty`). O módulo gera também a **política do papel da aplicação** (enviar, receber e apagar nas principais; ler a profundidade das mortas).
- **Dois papéis no ECS:** o de **execução** (puxa a imagem, escreve os logs, busca os segredos) e o da **tarefa** (o que o código faz na AWS: as filas e, com uma identidade criada, o e-mail). Antes era um só.
- **A aplicação recebe** as URLs das filas, a origem do portal (CORS e o link do convite) e, **só se** `bootstrap_admin_email` foi dado, a senha do primeiro administrador (as duas juntas ou nenhuma: a aplicação não sobe com metade).
- **Segredos:** a senha do administrador de partida e a chave que vai proteger os segredos do 2FA (M29), geradas pela Terraform, guardadas no Secrets Manager e entregues como `secrets` (nunca no texto da definição da tarefa).
- **SES** (`modules/ses`): a identidade do remetente (endereço e/ou domínio com DKIM) e a permissão de só enviar como ela. Ver [custos.md](custos.md) para o *sandbox*.
- **O portal** (`modules/portal`): um bucket **privado** (sem acesso público, versionado, criptografado, versões antigas expiram em 30 dias), servido pelo **CloudFront** com *origin access control* (só aquela distribuição lê o bucket); `index.html` para 403 e 404 (o app de página única é dono dos caminhos); cabeçalhos de segurança em toda resposta (HSTS, CSP restrita a si e à API, `X-Content-Type-Options`, `X-Frame-Options: DENY`, `Referrer-Policy: no-referrer`); domínio e certificado opcionais (o certificado do ACM precisa estar em `us-east-1`).
- **O CI** ganhou o job `terraform` (`fmt`, `validate`, `tflint`, `checkov`, sem credenciais) e o **CD** o `deploy-portal` (constrói o Flutter Web, sobe para o bucket com `index.html` sem cache e o resto com cache longo, e invalida o CloudFront); o papel do GitHub só ganhou o que esse deploy exige.

## CI/CD

`.github/workflows/ci.yml` (teste+lint+detekt, toda PR/push) e `.github/workflows/cd.yml` (build+push+deploy, só depois que o CI passa) são pipelines separados — o CD só começa via `workflow_run` quando o CI termina com sucesso no `main`.

Sem o secret `AWS_DEPLOY_ROLE_ARN` (não há conta AWS persistente — o Academy Lab é temporário), o job `check-aws` desliga o pipeline inteiro: os demais jobs aparecem como "skipped" em vez de falhar. Ao criar o secret, o CD passa a rodar sozinho.

Fluxo do CD:
1. **build-and-push**: builda a imagem Docker, autentica no ECR via OIDC (sem chave de longa duração guardada como secret) e publica com a tag sendo o SHA do commit — necessário porque o repositório ECR é `IMMUTABLE` (M6/6.3), não dá pra reusar uma tag como `latest` em pushes repetidos.
2. **deploy-dev**: automático após o build, roda `terraform apply -var="image_tag=<sha>"` — reaproveita a variável `image_tag` que a Task Definition do ECS já usa desde o M6.
3. **deploy-prod**: mesma infraestrutura que o dev neste projeto (um setup de produção de verdade teria state/ambiente separado) — o que importa aqui é o **gate**: só roda depois que um revisor aprova, via as regras de proteção do GitHub Environment `production`.

**Configuração manual única, necessária antes do pipeline funcionar de verdade** (nada disso é automatizável nem foi feito por mim — precisa de uma conta AWS persistente, diferente de um AWS Academy Lab, cuja sessão expira em horas):
1. `terraform apply` do módulo `terraform/modules/github_oidc` contra a conta AWS real (já incluso no `terraform apply` normal do módulo raiz).
2. Pegar o output `github_oidc.role_arn` e criar o secret `AWS_DEPLOY_ROLE_ARN` no repositório (Settings → Secrets and variables → Actions).
3. Criar os GitHub Environments `dev` e `production` (Settings → Environments) — `production` precisa de "Required reviewers" configurado pra virar um gate de aprovação de verdade.

**Nota honesta:** o pipeline nunca rodou de ponta a ponta nesta sessão — a única conta AWS disponível foi um Academy Lab, incompatível com credenciais persistentes de CI. O YAML e o Terraform estão corretos e prontos, mas a validação de um `workflow_run` verde fica pendente de uma conta AWS real.

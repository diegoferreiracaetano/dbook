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

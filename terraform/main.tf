module "vpc" {
  source = "./modules/vpc"

  name = var.project_name
}

module "sqs" {
  source = "./modules/sqs"

  name = var.project_name
}

module "ecr" {
  source = "./modules/ecr"

  name = var.project_name
}

# One security group for the running application, created here (not inside the ecs
# module) so the rds and redis modules can scope their ingress to it without creating a
# circular module dependency (ecs needs their endpoints, they need ecs's security group).
resource "aws_security_group" "app" {
  name_prefix = "${var.project_name}-app-"
  vpc_id      = module.vpc.vpc_id

  ingress {
    description = "Application HTTP"
    from_port   = 8080
    to_port     = 8080
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  lifecycle {
    create_before_destroy = true
  }
}

# Generated once, shared between the rds and secrets modules — never hardcoded, never
# printed (both variables that receive it are marked sensitive).
resource "random_password" "db_password" {
  length  = 32
  special = false
}

resource "random_password" "jwt_secret" {
  length  = 44
  special = false
}

# The first administrator's password, and the key that will protect the team's 2FA secrets (M29): generated here, kept
# in Secrets Manager, handed to the container as secrets and never written in the task definition.
resource "random_password" "bootstrap_admin" {
  length  = 24
  special = false
}

resource "random_password" "totp_encryption_key" {
  length  = 44
  special = false
}

module "secrets" {
  source = "./modules/secrets"

  name                     = var.project_name
  db_password              = random_password.db_password.result
  jwt_secret               = random_password.jwt_secret.result
  bootstrap_admin_password = random_password.bootstrap_admin.result
  totp_encryption_key      = random_password.totp_encryption_key.result
}

module "ses" {
  source = "./modules/ses"

  name         = var.project_name
  from_address = var.ses_from_address
  domain       = var.ses_domain
}

module "portal" {
  source = "./modules/portal"

  name            = var.project_name
  domain_name     = var.portal_domain_name
  certificate_arn = var.portal_certificate_arn
  api_origin      = var.api_origin
}

# What the application itself may do on AWS: the queues it uses and, when an identity exists, the mail it sends.
data "aws_iam_policy_document" "task" {
  source_policy_documents = compact([module.sqs.app_policy_json, module.ses.app_policy_json])
}

module "rds" {
  source = "./modules/rds"

  name                      = var.project_name
  vpc_id                    = module.vpc.vpc_id
  private_subnet_ids        = module.vpc.private_subnet_ids
  allowed_security_group_id = aws_security_group.app.id
  db_password               = random_password.db_password.result
}

module "redis" {
  source = "./modules/redis"

  name                      = var.project_name
  vpc_id                    = module.vpc.vpc_id
  private_subnet_ids        = module.vpc.private_subnet_ids
  allowed_security_group_id = aws_security_group.app.id
}

module "ecs" {
  source = "./modules/ecs"

  name               = var.project_name
  public_subnet_ids  = module.vpc.public_subnet_ids
  security_group_id  = aws_security_group.app.id
  ecr_repository_url = module.ecr.repository_url
  image_tag          = var.image_tag

  db_endpoint            = module.rds.endpoint
  db_name                = "dbook"
  db_username            = "dbook"
  db_password_secret_arn = module.secrets.db_password_arn
  jwt_secret_arn         = module.secrets.jwt_secret_arn

  redis_endpoint = module.redis.endpoint
  redis_port     = module.redis.port

  task_policy_json = data.aws_iam_policy_document.task.json

  # Spring relaxed binding: BOOKING_EXPIRATION_QUEUE_URL is booking-expiration.queue-url, and so on
  environment_extra = {
    BOOKING_EXPIRATION_QUEUE_URL = module.sqs.queue_urls["booking-expiration"]
    BOOKING_EXPIRATION_DLQ_URL   = module.sqs.dead_letter_queue_urls["booking-expiration"]
    NOTIFICATIONS_QUEUE_URL      = module.sqs.queue_urls["notifications"]
    NOTIFICATIONS_DLQ_URL        = module.sqs.dead_letter_queue_urls["notifications"]
    # empty: the SDK resolves the real SQS endpoint (the local default points at LocalStack)
    AWS_SQS_ENDPOINT = ""
    # where the invitation link points, and the only origin the browser may call the API from
    ADMIN_PORTAL_BASE_URL = module.portal.url
    CORS_ALLOWED_ORIGINS  = module.portal.url
    # both or neither: the application refuses to start with half of it
    DBOOK_BOOTSTRAP_ADMIN_EMAIL = var.bootstrap_admin_email
    # the roles that cannot sign in to the portal without an authenticator (see docs/autenticacao.md)
    ADMIN_2FA_REQUIRED_ROLES = "SUPER_ADMIN"
    # where the links of the confirmation and password reset mails point, and the policy that booking and paying wait
    # for a confirmed address (see docs/autenticacao.md)
    CUSTOMER_APP_BASE_URL          = var.customer_app_base_url
    ACCOUNT_REQUIRE_VERIFIED_EMAIL = "true"
  }

  secrets_extra = merge(
    # the key that keeps the authenticator secrets unreadable in the database
    { TOTP_ENCRYPTION_KEY = module.secrets.totp_encryption_key_arn },
    var.bootstrap_admin_email == "" ? {} : {
      DBOOK_BOOTSTRAP_ADMIN_PASSWORD = module.secrets.bootstrap_admin_password_arn
    },
  )
}

module "github_oidc" {
  source = "./modules/github_oidc"

  name              = var.project_name
  github_repo       = var.github_repo
  state_bucket_name = var.state_bucket_name
  lock_table_name   = var.lock_table_name

  portal_bucket_arn       = module.portal.bucket_arn
  portal_distribution_arn = module.portal.distribution_arn
}

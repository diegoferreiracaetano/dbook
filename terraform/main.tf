module "vpc" {
  source = "./modules/vpc"

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

module "secrets" {
  source = "./modules/secrets"

  name        = var.project_name
  db_password = random_password.db_password.result
  jwt_secret  = random_password.jwt_secret.result
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
  use_localstack     = var.use_localstack
  public_subnet_ids  = module.vpc.public_subnet_ids
  security_group_id  = aws_security_group.app.id
  ecr_repository_url = module.ecr.repository_url

  db_endpoint            = module.rds.endpoint
  db_name                = "dbook"
  db_username            = "dbook"
  db_password_secret_arn = module.secrets.db_password_arn
  jwt_secret_arn         = module.secrets.jwt_secret_arn

  redis_endpoint = module.redis.endpoint
  redis_port     = module.redis.port
}

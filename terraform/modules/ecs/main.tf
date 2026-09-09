resource "aws_ecs_cluster" "this" {
  name = var.name
}

resource "aws_cloudwatch_log_group" "this" {
  name              = "/ecs/${var.name}"
  retention_in_days = 7
}

# The pre-existing roles in this AWS Academy Lab account ("voclabs") aren't trusted by
# ecs-tasks.amazonaws.com — ECS rejects the task with "unable to assume the role" even
# though the ARN itself resolves fine. Creating a dedicated role turned out to work in
# this lab despite IAM being locked down for reads (iam:GetRole is denied) — so this is
# the same path for LocalStack and real AWS, no environment-specific branching needed.
resource "aws_iam_role" "ecs_execution_role" {
  name = "${var.name}-ecs-execution-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Action    = "sts:AssumeRole"
      Effect    = "Allow"
      Principal = { Service = "ecs-tasks.amazonaws.com" }
    }]
  })
}

resource "aws_iam_role_policy_attachment" "ecs_execution_role_policy" {
  role       = aws_iam_role.ecs_execution_role.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy"
}

# AmazonECSTaskExecutionRolePolicy only covers ECR pull + CloudWatch Logs — it does NOT
# include Secrets Manager, so the execution role needs this explicitly to fetch the
# `secrets` block values (db password, JWT secret) at container start.
resource "aws_iam_role_policy" "ecs_execution_role_secrets" {
  name = "${var.name}-ecs-secrets-access"
  role = aws_iam_role.ecs_execution_role.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect   = "Allow"
      Action   = "secretsmanager:GetSecretValue"
      Resource = [var.db_password_secret_arn, var.jwt_secret_arn]
    }]
  })
}

locals {
  # Reused for both execution (ECS agent: pull image, write logs, fetch secrets) and task
  # (the application itself) roles — the app doesn't call any AWS API yet, so it doesn't
  # need permissions beyond what the execution role already has. Revisit once M7 (Bedrock)
  # gives the application its own AWS calls to make.
  role_arn = aws_iam_role.ecs_execution_role.arn
}

resource "aws_ecs_task_definition" "this" {
  family                   = var.name
  requires_compatibilities = ["FARGATE"]
  network_mode             = "awsvpc"
  cpu                      = var.cpu
  memory                   = var.memory
  execution_role_arn       = local.role_arn
  task_role_arn            = local.role_arn

  container_definitions = jsonencode([
    {
      name      = "dbook"
      image     = "${var.ecr_repository_url}:${var.image_tag}"
      essential = true
      portMappings = [
        {
          containerPort = var.container_port
          protocol      = "tcp"
        }
      ]
      # Standard Spring Boot relaxed-binding env var names — override the values
      # hardcoded in application.yml without needing any code change (environment
      # variables outrank application.yml in Spring's property source order).
      environment = [
        { name = "SPRING_DATASOURCE_URL", value = "jdbc:postgresql://${var.db_endpoint}/${var.db_name}" },
        { name = "SPRING_DATASOURCE_USERNAME", value = var.db_username },
        { name = "SPRING_DATA_REDIS_HOST", value = var.redis_endpoint },
        { name = "SPRING_DATA_REDIS_PORT", value = tostring(var.redis_port) },
      ]
      secrets = [
        { name = "SPRING_DATASOURCE_PASSWORD", valueFrom = var.db_password_secret_arn },
        { name = "JWT_SECRET", valueFrom = var.jwt_secret_arn },
      ]
      logConfiguration = {
        logDriver = "awslogs"
        options = {
          "awslogs-group"         = aws_cloudwatch_log_group.this.name
          "awslogs-region"        = "us-east-1"
          "awslogs-stream-prefix" = "dbook"
        }
      }
    }
  ])
}

resource "aws_ecs_service" "this" {
  name            = var.name
  cluster         = aws_ecs_cluster.this.id
  task_definition = aws_ecs_task_definition.this.arn
  desired_count   = 1
  launch_type     = "FARGATE"

  network_configuration {
    subnets          = var.public_subnet_ids
    security_groups  = [var.security_group_id]
    assign_public_ip = true
  }
}

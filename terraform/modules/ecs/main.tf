resource "aws_ecs_cluster" "this" {
  name = var.name
}

resource "aws_cloudwatch_log_group" "this" {
  name              = "/ecs/${var.name}"
  retention_in_days = 7
}

# AWS Academy Learner Lab blocks both iam:CreateRole and iam:GetRole — real deployments
# there must reuse a pre-provisioned role, and its ARN has to be built directly (account
# ID from STS, which IS allowed) rather than looked up via the IAM API. The role name
# varies by lab template (this one has no "LabRole" at all, only "voclabs") — hence the
# variable instead of a hardcoded name. LocalStack's IAM has no such restriction, so a
# real execution role is created there instead, mirroring a normal AWS account.
data "aws_caller_identity" "current" {
  count = var.use_localstack ? 0 : 1
}

resource "aws_iam_role" "ecs_execution_role" {
  count = var.use_localstack ? 1 : 0
  name  = "${var.name}-ecs-execution-role"

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
  count      = var.use_localstack ? 1 : 0
  role       = aws_iam_role.ecs_execution_role[0].name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy"
}

locals {
  # Reused for both execution (ECS agent: pull image, write logs, fetch secrets) and task
  # (the application itself) roles — the app doesn't call any AWS API yet, so it doesn't
  # need permissions beyond what the execution role already has. Revisit once M7 (Bedrock)
  # gives the application its own AWS calls to make.
  role_arn = (
    var.use_localstack
    ? aws_iam_role.ecs_execution_role[0].arn
    : "arn:aws:iam::${data.aws_caller_identity.current[0].account_id}:role/${var.lab_role_name}"
  )
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

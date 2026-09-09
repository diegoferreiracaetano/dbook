# Root module: wires together the reusable modules under modules/ into the actual
# dbook infrastructure. State is remote (S3 + DynamoDB from terraform/bootstrap) — see
# backend-localstack.hcl / backend-aws.hcl, selected at `terraform init -backend-config=...`.

terraform {
  required_version = ">= 1.5"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }

  backend "s3" {}
}

provider "aws" {
  region = var.aws_region

  access_key = var.use_localstack ? "test" : null
  secret_key = var.use_localstack ? "test" : null

  s3_use_path_style           = var.use_localstack
  skip_credentials_validation = var.use_localstack
  skip_metadata_api_check     = var.use_localstack
  skip_requesting_account_id  = var.use_localstack

  dynamic "endpoints" {
    for_each = var.use_localstack ? [1] : []
    content {
      s3             = "http://localhost:4566"
      dynamodb       = "http://localhost:4566"
      ec2            = "http://localhost:4566"
      iam            = "http://localhost:4566"
      ecr            = "http://localhost:4566"
      ecs            = "http://localhost:4566"
      rds            = "http://localhost:4566"
      secretsmanager = "http://localhost:4566"
      sts            = "http://localhost:4566"
    }
  }
}

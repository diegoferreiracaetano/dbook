variable "aws_region" {
  description = "AWS region for all resources."
  type        = string
  default     = "us-east-1"
}

variable "use_localstack" {
  description = "When true (default), targets a local LocalStack container instead of real AWS. Set to false explicitly (e.g. -var=\"use_localstack=false\") to run against a real account."
  type        = bool
  default     = true
}

variable "project_name" {
  description = "Name prefix applied to resources across modules."
  type        = string
  default     = "dbook"
}

variable "image_tag" {
  description = "ECR image tag the ECS task should run — CI passes the git SHA here (M8); local applies fall back to \"latest\"."
  type        = string
  default     = "latest"
}

variable "github_repo" {
  description = "GitHub repo (\"owner/repo\") allowed to assume the CI/CD deploy role via OIDC."
  type        = string
  default     = "diegoferreiracaetano/dbook"
}

# Must match terraform/bootstrap's own defaults — duplicated here (not read from its
# outputs) because bootstrap is a separate Terraform root, not a module this one calls.
variable "state_bucket_name" {
  type    = string
  default = "dbook-terraform-state"
}

variable "lock_table_name" {
  type    = string
  default = "dbook-terraform-locks"
}

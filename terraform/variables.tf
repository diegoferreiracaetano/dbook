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

variable "aws_region" {
  description = "AWS region for the state bucket and lock table."
  type        = string
  default     = "us-east-1"
}

variable "use_localstack" {
  description = "When true (default), targets a local LocalStack container instead of real AWS. Set to false explicitly to run against a real account."
  type        = bool
  default     = true
}

variable "state_bucket_name" {
  description = "Name of the S3 bucket that will hold Terraform remote state."
  type        = string
  default     = "dbook-terraform-state"
}

variable "lock_table_name" {
  description = "Name of the DynamoDB table used for Terraform state locking."
  type        = string
  default     = "dbook-terraform-locks"
}

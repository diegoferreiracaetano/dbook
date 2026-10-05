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

variable "bootstrap_admin_email" {
  description = "E-mail of the first SUPER_ADMIN, created at startup while none exists. Empty: none is created (and the password secret is not given to the container)."
  type        = string
  default     = ""
}

variable "ses_from_address" {
  description = "E-mail address the application sends from; AWS sends a verification link to it. Empty: no address identity."
  type        = string
  default     = ""
}

variable "ses_domain" {
  description = "Domain the application sends mail from (verified by DNS). Empty: no domain identity."
  type        = string
  default     = ""
}

variable "portal_domain_name" {
  description = "The admin portal's own domain. Empty: served from the CloudFront domain."
  type        = string
  default     = ""
}

variable "portal_certificate_arn" {
  description = "ACM certificate for portal_domain_name, issued in us-east-1. Required with a domain."
  type        = string
  default     = ""
}

variable "api_origin" {
  description = "Public origin of the API (https://api.example.com), allowed by the portal's Content-Security-Policy."
  type        = string
  default     = ""
}

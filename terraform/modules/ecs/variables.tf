variable "name" {
  type = string
}

variable "use_localstack" {
  description = "AWS Academy Learner Lab can't create IAM roles — real AWS uses a pre-existing role instead. LocalStack has no such restriction, so a real role is created there to mirror what a normal (non-lab) AWS account would do."
  type        = bool
}

variable "lab_role_name" {
  description = "Name of the pre-provisioned IAM role to use as both execution and task role against real AWS. Varies by Academy Lab template — this account has no \"LabRole\", only \"voclabs\"."
  type        = string
  default     = "voclabs"
}

variable "public_subnet_ids" {
  description = "No Application Load Balancer in this milestone's scope — the task gets a public IP directly, so it needs to run in a public subnet."
  type        = list(string)
}

variable "security_group_id" {
  description = "Created in the root module (not here) — the rds/redis modules need to reference the same security group to scope their ingress rules to it, and this module needing their endpoint outputs would make a module-owned SG a circular dependency."
  type        = string
}

variable "ecr_repository_url" {
  type = string
}

variable "image_tag" {
  description = "The repository is IMMUTABLE (see the ecr module) — a tag can only be pushed once. \"latest\" works for this milestone's single manual push (6.5); per-build tags (e.g. the git SHA) become necessary once M8 automates repeated pushes."
  type        = string
  default     = "latest"
}

variable "container_port" {
  type    = number
  default = 8080
}

variable "cpu" {
  description = "Fargate task CPU units. 512 (not the Fargate minimum of 256) because a Spring Boot JVM needs headroom beyond the bare minimum."
  type        = number
  default     = 512
}

variable "memory" {
  type    = number
  default = 1024
}

variable "db_endpoint" {
  description = "host:port from the rds module."
  type        = string
}

variable "db_name" {
  type = string
}

variable "db_username" {
  type = string
}

variable "db_password_secret_arn" {
  type = string
}

variable "jwt_secret_arn" {
  type = string
}

variable "redis_endpoint" {
  type = string
}

variable "redis_port" {
  type = number
}

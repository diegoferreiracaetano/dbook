variable "name" {
  description = "Name prefix for the secrets."
  type        = string
}

variable "db_password" {
  description = "Same password given to the RDS module — stored here so ECS can inject it without it ever appearing in a Task Definition in plaintext."
  type        = string
  sensitive   = true
}

variable "jwt_secret" {
  description = "Signing key for JWT access/refresh tokens. Generated once by the root module (random_password), never hardcoded."
  type        = string
  sensitive   = true
}

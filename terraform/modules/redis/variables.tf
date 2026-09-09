variable "name" {
  description = "Name prefix for ElastiCache resources."
  type        = string
}

variable "vpc_id" {
  type = string
}

variable "private_subnet_ids" {
  type = list(string)
}

variable "allowed_security_group_id" {
  description = "Security group (the ECS service's) allowed to reach Redis."
  type        = string
}

variable "node_type" {
  description = "Smallest practical node for a learning workload — not required to run in production."
  type        = string
  default     = "cache.t3.micro"
}

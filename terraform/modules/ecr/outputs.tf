output "repository_url" {
  description = "URL to push/pull images (used by the ECS task definition)."
  value       = aws_ecr_repository.this.repository_url
}

output "repository_name" {
  value = aws_ecr_repository.this.name
}

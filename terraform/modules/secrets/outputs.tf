output "db_password_arn" {
  description = "Referenced by the ECS Task Definition's `secrets` block, not `environment` — the value is injected at container start, never written to the definition itself."
  value       = aws_secretsmanager_secret.db_password.arn
}

output "jwt_secret_arn" {
  value = aws_secretsmanager_secret.jwt_secret.arn
}

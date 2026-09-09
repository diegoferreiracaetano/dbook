output "endpoint" {
  description = "host:port the application connects to."
  value       = aws_db_instance.this.endpoint
}

output "security_group_id" {
  value = aws_security_group.db.id
}

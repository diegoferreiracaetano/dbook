output "state_bucket_name" {
  description = "Name of the S3 bucket to reference from the root module's backend config."
  value       = aws_s3_bucket.terraform_state.bucket
}

output "lock_table_name" {
  description = "Name of the DynamoDB table to reference from the root module's backend config."
  value       = aws_dynamodb_table.terraform_locks.name
}

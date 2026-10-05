output "queue_urls" {
  description = "Queue url by name; the application takes them as booking-expiration.queue-url and notifications.queue-url."
  value       = { for name, queue in aws_sqs_queue.main : name => queue.url }
}

output "dead_letter_queue_urls" {
  description = "Dead-letter queue url by name (the dbook.sqs.dlq.depth gauge reads them)."
  value       = { for name, queue in aws_sqs_queue.dlq : name => queue.url }
}

output "queue_arns" {
  description = "Queue ARN by name, for the IAM policy of the task role (wired in M43)."
  value       = { for name, queue in aws_sqs_queue.main : name => queue.arn }
}

output "app_policy_json" {
  description = "IAM policy for the application's task role: the queues it uses and the dead-letter depth it reads."
  value       = data.aws_iam_policy_document.app.json
}

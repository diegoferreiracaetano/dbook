output "portal_url" {
  description = "Where the admin portal is served."
  value       = module.portal.url
}

output "portal_bucket" {
  description = "Where the CI uploads the portal build."
  value       = module.portal.bucket
}

output "portal_distribution_id" {
  description = "What the CI invalidates after the upload."
  value       = module.portal.distribution_id
}

output "queue_urls" {
  description = "The application's queues, by name."
  value       = module.sqs.queue_urls
}

output "ses_dkim_tokens" {
  description = "CNAME records to create for the sending domain."
  value       = module.ses.dkim_tokens
}

output "bucket" {
  description = "Where the CI uploads the build of the portal."
  value       = aws_s3_bucket.portal.bucket
}

output "bucket_arn" {
  value = aws_s3_bucket.portal.arn
}

output "distribution_id" {
  description = "What the CI invalidates after an upload."
  value       = aws_cloudfront_distribution.portal.id
}

output "distribution_arn" {
  value = aws_cloudfront_distribution.portal.arn
}

output "url" {
  description = "Where the portal is served: its own domain, or the CloudFront one."
  value       = var.domain_name == "" ? "https://${aws_cloudfront_distribution.portal.domain_name}" : "https://${var.domain_name}"
}

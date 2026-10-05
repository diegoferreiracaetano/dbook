output "identity_arns" {
  value = local.identity_arns
}

output "dkim_tokens" {
  description = "CNAME records to create for the domain identity (token._domainkey.<domain> -> token.dkim.amazonses.com)."
  value       = flatten(aws_sesv2_email_identity.domain[*].dkim_signing_attributes[0].tokens)
}

output "app_policy_json" {
  description = "IAM policy for the task role; null when no identity was asked for."
  value       = length(data.aws_iam_policy_document.app) == 0 ? null : data.aws_iam_policy_document.app[0].json
}

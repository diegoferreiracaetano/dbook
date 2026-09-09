output "role_arn" {
  description = "Set this as the AWS_DEPLOY_ROLE_ARN secret in the GitHub repo (Settings -> Secrets and variables -> Actions)."
  value       = aws_iam_role.github_actions_deploy.arn
}

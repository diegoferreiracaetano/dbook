# Lets GitHub Actions assume an AWS role via OIDC federation — no long-lived
# AWS_ACCESS_KEY_ID/SECRET stored as a GitHub secret, no credential to rotate or leak.
# NOTE: an AWS account can only have ONE OIDC provider per URL — if this account already
# has a GitHub Actions OIDC provider from another project, import it instead of applying
# this resource fresh (`terraform import module.github_oidc.aws_iam_openid_connect_provider.github <arn>`).
resource "aws_iam_openid_connect_provider" "github" {
  url            = "https://token.actions.githubusercontent.com"
  client_id_list = ["sts.amazonaws.com"]
  # GitHub's OIDC intermediate CA thumbprint — public, documented by GitHub, doesn't
  # change often, but see https://github.blog/changelog/ if this provider stops trusting.
  thumbprint_list = ["6938fd4d98bab03faadb97b34396831e3780aea1"]
}

resource "aws_iam_role" "github_actions_deploy" {
  name = "${var.name}-github-actions-deploy"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect    = "Allow"
      Action    = "sts:AssumeRoleWithWebIdentity"
      Principal = { Federated = aws_iam_openid_connect_provider.github.arn }
      Condition = {
        StringEquals = {
          "token.actions.githubusercontent.com:aud" = "sts.amazonaws.com"
        }
        # Restricts to this specific repo, any branch/ref — tighten to
        # "repo:${var.github_repo}:ref:refs/heads/main" if only main should ever deploy.
        StringLike = {
          "token.actions.githubusercontent.com:sub" = "repo:${var.github_repo}:*"
        }
      }
    }]
  })
}

# Broad managed policies here are a deliberate simplification for a learning project's
# CI role, not a least-privilege production posture — a real setup should scope each of
# these down to the specific resource ARNs the pipeline actually touches.
resource "aws_iam_role_policy_attachment" "ecr" {
  role       = aws_iam_role.github_actions_deploy.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonEC2ContainerRegistryPowerUser"
}

resource "aws_iam_role_policy_attachment" "ecs" {
  role       = aws_iam_role.github_actions_deploy.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonECS_FullAccess"
}

resource "aws_iam_role_policy_attachment" "ec2" {
  role       = aws_iam_role.github_actions_deploy.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonEC2FullAccess"
}

resource "aws_iam_role_policy_attachment" "rds" {
  role       = aws_iam_role.github_actions_deploy.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonRDSFullAccess"
}

resource "aws_iam_role_policy_attachment" "elasticache" {
  role       = aws_iam_role.github_actions_deploy.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonElastiCacheFullAccess"
}

resource "aws_iam_role_policy_attachment" "secrets_manager" {
  role       = aws_iam_role.github_actions_deploy.name
  policy_arn = "arn:aws:iam::aws:policy/SecretsManagerReadWrite"
}

# The ecs module creates its own execution role (see 6.8's IAM discoveries) — the
# pipeline's own role needs permission to manage IAM roles/policies for that to work.
resource "aws_iam_role_policy_attachment" "iam" {
  role       = aws_iam_role.github_actions_deploy.name
  policy_arn = "arn:aws:iam::aws:policy/IAMFullAccess"
}

# Scoped precisely (not a managed policy) — this is the one thing the pipeline role
# strictly must be able to do, so it gets a real least-privilege policy instead of a
# blanket one.
resource "aws_iam_role_policy" "terraform_state_backend" {
  name = "${var.name}-terraform-state-backend"
  role = aws_iam_role.github_actions_deploy.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = ["s3:GetObject", "s3:PutObject", "s3:ListBucket"]
        Resource = [
          "arn:aws:s3:::${var.state_bucket_name}",
          "arn:aws:s3:::${var.state_bucket_name}/*",
        ]
      },
      {
        Effect   = "Allow"
        Action   = ["dynamodb:GetItem", "dynamodb:PutItem", "dynamodb:DeleteItem"]
        Resource = "arn:aws:dynamodb:*:*:table/${var.lock_table_name}"
      },
    ]
  })
}

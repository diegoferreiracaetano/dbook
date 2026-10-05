# Who the application may send mail as. SES starts in a sandbox (it sends only to verified addresses): leaving it is
# a request made to AWS Support, not something Terraform can do — see docs/custos.md.
resource "aws_sesv2_email_identity" "address" {
  count = var.from_address == "" ? 0 : 1

  email_identity = var.from_address
  tags           = { Name = "${var.name}-sender" }
}

resource "aws_sesv2_email_identity" "domain" {
  count = var.domain == "" ? 0 : 1

  email_identity = var.domain
  tags           = { Name = "${var.name}-domain" }

  dkim_signing_attributes {
    next_signing_key_length = "RSA_2048_BIT"
  }
}

locals {
  identity_arns = concat(
    aws_sesv2_email_identity.address[*].arn,
    aws_sesv2_email_identity.domain[*].arn,
  )
}

# Sending only, and only as the identities above.
data "aws_iam_policy_document" "app" {
  count = length(local.identity_arns) == 0 ? 0 : 1

  statement {
    sid       = "SendAsTheIdentities"
    actions   = ["ses:SendEmail", "ses:SendRawEmail"]
    resources = local.identity_arns
  }
}

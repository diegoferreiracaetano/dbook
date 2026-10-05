# Static checks that need no credentials: unused declarations, deprecated syntax, and (with the AWS ruleset) values
# AWS would refuse. Run in CI and locally with `tflint --init && tflint --recursive` from this folder.
plugin "terraform" {
  enabled = true
  preset  = "recommended"
}

plugin "aws" {
  enabled = true
  version = "0.34.0"
  source  = "github.com/terraform-linters/tflint-ruleset-aws"
}

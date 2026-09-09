# Real AWS backend config: terraform init -backend-config=backend-aws.hcl
# Credentials come from the environment (aws configure / env vars), never from this file.
bucket         = "dbook-terraform-state"
key            = "dbook/terraform.tfstate"
region         = "us-east-1"
dynamodb_table = "dbook-terraform-locks"

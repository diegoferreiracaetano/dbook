module "vpc" {
  source = "./modules/vpc"

  name = var.project_name
}

variable "name" {
  description = "Name prefix for the IAM role."
  type        = string
}

variable "github_repo" {
  description = "GitHub repo allowed to assume this role, as \"owner/repo\" (e.g. \"diegoferreiracaetano/dbook\")."
  type        = string
}

variable "state_bucket_name" {
  type = string
}

variable "lock_table_name" {
  type = string
}

variable "name" {
  description = "Name prefix."
  type        = string
}

variable "from_address" {
  description = "The e-mail address the application sends from (invitations, notifications). Verified by AWS with a link sent to it. Empty: no address identity is created."
  type        = string
  default     = ""
}

variable "domain" {
  description = "A domain to send from (verified with DNS records, DKIM signed). Empty: no domain identity is created."
  type        = string
  default     = ""
}

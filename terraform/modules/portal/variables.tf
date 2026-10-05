variable "name" {
  description = "Name prefix."
  type        = string
}

variable "domain_name" {
  description = "The portal's own domain (portal.example.com). Empty: the portal is served from the CloudFront domain (*.cloudfront.net) with its default certificate."
  type        = string
  default     = ""
}

variable "certificate_arn" {
  description = "ARN of an ACM certificate for domain_name, issued in us-east-1 (CloudFront only accepts that region). Required when domain_name is set."
  type        = string
  default     = ""
}

variable "api_origin" {
  description = "Where the portal calls the API (https://api.example.com): the only origin its Content-Security-Policy lets it connect to besides itself."
  type        = string
  default     = ""
}

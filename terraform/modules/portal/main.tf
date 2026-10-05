# The admin portal is a static site (Flutter Web): files in a PRIVATE bucket, served by CloudFront through an origin
# access control, so the bucket is never reachable except through the distribution.
resource "aws_s3_bucket" "portal" {
  bucket_prefix = "${var.name}-portal-"
  force_destroy = false
}

resource "aws_s3_bucket_public_access_block" "portal" {
  bucket                  = aws_s3_bucket.portal.id
  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

resource "aws_s3_bucket_versioning" "portal" {
  bucket = aws_s3_bucket.portal.id

  versioning_configuration {
    status = "Enabled"
  }
}

# Every upload keeps the previous version (so a bad deploy can be undone); the old ones do not pile up for ever.
resource "aws_s3_bucket_lifecycle_configuration" "portal" {
  bucket = aws_s3_bucket.portal.id

  rule {
    id     = "expire-old-versions"
    status = "Enabled"

    filter {}

    noncurrent_version_expiration {
      noncurrent_days = 30
    }

    abort_incomplete_multipart_upload {
      days_after_initiation = 7
    }
  }
}

resource "aws_s3_bucket_server_side_encryption_configuration" "portal" {
  bucket = aws_s3_bucket.portal.id

  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
  }
}

resource "aws_cloudfront_origin_access_control" "portal" {
  name                              = "${var.name}-portal"
  origin_access_control_origin_type = "s3"
  signing_behavior                  = "always"
  signing_protocol                  = "sigv4"
}

# What every response carries: the browser is told to talk to this site only over HTTPS, not to sniff types, not to be
# framed, to send no referrer, and to connect only to itself and to the API.
resource "aws_cloudfront_response_headers_policy" "security" {
  name = "${var.name}-portal-security"

  security_headers_config {
    strict_transport_security {
      access_control_max_age_sec = 31536000
      include_subdomains         = true
      preload                    = true
      override                   = true
    }

    content_type_options {
      override = true
    }

    frame_options {
      frame_option = "DENY"
      override     = true
    }

    referrer_policy {
      referrer_policy = "no-referrer"
      override        = true
    }

    content_security_policy {
      # CanvasKit (Flutter Web) needs wasm and inline styles; everything else stays on this origin and the API
      content_security_policy = "default-src 'self'; script-src 'self' 'wasm-unsafe-eval'; style-src 'self' 'unsafe-inline'; img-src 'self' data: blob:; font-src 'self' data:; connect-src 'self' ${var.api_origin}; frame-ancestors 'none'; base-uri 'self'; form-action 'self'"
      override                = true
    }
  }
}

resource "aws_cloudfront_distribution" "portal" {
  enabled             = true
  default_root_object = "index.html"
  aliases             = var.domain_name == "" ? [] : [var.domain_name]
  price_class         = "PriceClass_100"

  origin {
    domain_name              = aws_s3_bucket.portal.bucket_regional_domain_name
    origin_id                = "portal"
    origin_access_control_id = aws_cloudfront_origin_access_control.portal.id
  }

  default_cache_behavior {
    target_origin_id           = "portal"
    viewer_protocol_policy     = "redirect-to-https"
    allowed_methods            = ["GET", "HEAD"]
    cached_methods             = ["GET", "HEAD"]
    compress                   = true
    response_headers_policy_id = aws_cloudfront_response_headers_policy.security.id

    # the managed "CachingOptimized" policy
    cache_policy_id = "658327ea-f89d-4fab-a63d-7e88639e58f6"
  }

  # A single-page app: a path the bucket does not have (/customers/42 opened directly, a reload) is the app's, not an
  # error. The bucket answers 403 for a missing key (it hides what exists), so both codes serve index.html.
  custom_error_response {
    error_code            = 403
    response_code         = 200
    response_page_path    = "/index.html"
    error_caching_min_ttl = 0
  }

  custom_error_response {
    error_code            = 404
    response_code         = 200
    response_page_path    = "/index.html"
    error_caching_min_ttl = 0
  }

  restrictions {
    geo_restriction {
      restriction_type = "none"
    }
  }

  viewer_certificate {
    cloudfront_default_certificate = var.certificate_arn == ""
    acm_certificate_arn            = var.certificate_arn == "" ? null : var.certificate_arn
    ssl_support_method             = var.certificate_arn == "" ? null : "sni-only"
    minimum_protocol_version       = var.certificate_arn == "" ? null : "TLSv1.2_2021"
  }

  lifecycle {
    precondition {
      condition     = var.domain_name == "" || var.certificate_arn != ""
      error_message = "A custom domain needs the ARN of a certificate for it (issued in us-east-1)."
    }
  }
}

# Only this distribution may read the bucket.
data "aws_iam_policy_document" "bucket" {
  statement {
    sid       = "ReadFromThisDistributionOnly"
    actions   = ["s3:GetObject"]
    resources = ["${aws_s3_bucket.portal.arn}/*"]

    principals {
      type        = "Service"
      identifiers = ["cloudfront.amazonaws.com"]
    }

    condition {
      test     = "StringEquals"
      variable = "AWS:SourceArn"
      values   = [aws_cloudfront_distribution.portal.arn]
    }
  }
}

resource "aws_s3_bucket_policy" "portal" {
  bucket = aws_s3_bucket.portal.id
  policy = data.aws_iam_policy_document.bucket.json
}

resource "aws_ecr_repository" "this" {
  name = var.name

  # IMMUTABLE: every push must use a unique tag (e.g. the git SHA), instead of
  # repeatedly overwriting a shared "latest" tag — makes every deployed image
  # traceable back to exactly what was pushed.
  image_tag_mutability = "IMMUTABLE"

  image_scanning_configuration {
    scan_on_push = true
  }
}

# Keeps the repository from accumulating untagged images forever (every failed/superseded
# multi-arch push, or an image whose tag got overwritten, leaves an untagged layer behind).
resource "aws_ecr_lifecycle_policy" "this" {
  repository = aws_ecr_repository.this.name

  policy = jsonencode({
    rules = [
      {
        rulePriority = 1
        description  = "Expire untagged images after 7 days"
        selection = {
          tagStatus   = "untagged"
          countType   = "sinceImagePushed"
          countUnit   = "days"
          countNumber = 7
        }
        action = {
          type = "expire"
        }
      }
    ]
  })
}

resource "aws_db_subnet_group" "this" {
  name       = var.name
  subnet_ids = var.private_subnet_ids
}

# Only the ECS service's security group can reach Postgres — nothing else in the VPC,
# and nothing from the internet (this SG has no ingress from 0.0.0.0/0 at all).
resource "aws_security_group" "db" {
  name_prefix = "${var.name}-db-"
  vpc_id      = var.vpc_id

  ingress {
    description     = "Postgres from the application"
    from_port       = 5432
    to_port         = 5432
    protocol        = "tcp"
    security_groups = [var.allowed_security_group_id]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  lifecycle {
    create_before_destroy = true
  }
}

resource "aws_db_instance" "this" {
  identifier     = var.name
  engine         = "postgres"
  engine_version = "16"
  instance_class = var.instance_class

  allocated_storage = 20
  storage_type      = "gp3"

  db_name  = var.db_name
  username = var.db_username
  password = var.db_password

  db_subnet_group_name   = aws_db_subnet_group.this.name
  vpc_security_group_ids = [aws_security_group.db.id]

  # Learning-project trade-offs, not production defaults: no read replica, and skipping
  # the final snapshot on destroy so `terraform destroy` doesn't get stuck waiting on one.
  multi_az            = false
  skip_final_snapshot = true
  publicly_accessible = false
}

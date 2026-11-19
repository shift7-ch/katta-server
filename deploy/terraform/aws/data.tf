data "aws_availability_zones" "available" {}

data "aws_iam_policy_document" "assume_role_policy" {
  statement {
    effect  = "Allow"
    actions = ["sts:AssumeRole"]

    principals {
      type        = "Service"
      identifiers = ["ecs-tasks.amazonaws.com"]
    }
  }
}



data "aws_caller_identity" "current" {}

data "aws_route53_zone" "parent_zone" {
  name         = var.dns_suffix
  private_zone = false
}

resource "aws_iam_role" "ecsTaskExecutionRole" {
  name               = "${terraform.workspace}-execution-task-role"
  assume_role_policy = data.aws_iam_policy_document.assume_role_policy.json

  tags = {
    Name        = "${terraform.workspace}-iam-ecsTaskExecutionRole-role"
    Project     = var.project
    Environment = terraform.workspace
  }
}

resource "aws_iam_policy" "ecr_pullthroughcache_policy" {
  name        = "${terraform.workspace}-ecr-pullthrough-policy"
  description = "Policy to allow ECS task execution role to interact with ECR pull-through cache"
  policy = jsonencode({
    Version = "2012-10-17",
    Statement = [
      {
        Effect = "Allow",
        Action = [
          "ecr:GetAuthorizationToken"
        ],
        Resource = "*"
      },
      {
        Effect = "Allow",
        Action = [
          "ecr:BatchCheckLayerAvailability",
          "ecr:GetDownloadUrlForLayer",
          "ecr:BatchGetImage",
          "ecr:DescribeImages",
          "ecr:DescribeRepositories",
          "ecr:ListImages"
        ],
        Resource = [
          "arn:aws:ecr:${var.region}:${data.aws_caller_identity.current.account_id}:*"
        ]
      },
      {
        Effect = "Allow",
        Action = [
          "ecr:CreateRepository"
        ],
        Resource = "*"
      }
    ]
  })

  tags = {
    Name        = "${terraform.workspace}-ecr-pullthrough-policy"
    Project     = var.project
    Environment = terraform.workspace
  }
}

resource "aws_iam_policy" "secrets_manager_policy" {
  name        = "${terraform.workspace}-secrets-manager-policy"
  description = "Policy to allow ECS task execution role to access Secrets Manager secrets"
  policy = jsonencode({
    "Version" : "2012-10-17",
    "Statement" : [
      {
        "Effect" : "Allow",
        "Action" : [
          "secretsmanager:GetSecretValue",
          "secretsmanager:DescribeSecret"
        ],
        "Resource" : [
          aws_secretsmanager_secret.keycloak_db_credentials.arn,
          aws_secretsmanager_secret.keycloak_admin.arn,
          aws_secretsmanager_secret.hub_db_credentials.arn,
          aws_secretsmanager_secret.hub_oidc_client_secrets_credentials.arn,
          aws_secretsmanager_secret.hub_admin.arn,
        ]
      }
    ]
  })

  tags = {
    Name        = "${terraform.workspace}-secrets-manager-policy"
    Project     = var.project
    Environment = terraform.workspace
  }
}

resource "aws_iam_policy" "ecr_ECSFargateAllowExecuteCommand" {
  count       = var.ecs_enable_execute_command ? 1 : 0
  name        = "${terraform.workspace}-ecr-ECSFargateAllowExecuteCommand"
  description = "Policy to allow ECS task execution role to execute command."
  policy = jsonencode({
    Version = "2012-10-17",
    Statement = [
      {
        Effect = "Allow",
        Action = [
          "ssmmessages:CreateControlChannel",
          "ssmmessages:CreateDataChannel",
          "ssmmessages:OpenControlChannel",
          "ssmmessages:OpenDataChannel"
        ],
        Resource = "*"
      }
    ]
  })

  tags = {
    Name        = "${terraform.workspace}-ecr-pullthrough-policy"
    Project     = var.project
    Environment = terraform.workspace
  }
}

resource "aws_iam_role_policy_attachment" "ecsTaskExecutionPolicy_AmazonEC2ContainerServiceforEC2Role" {
  role       = aws_iam_role.ecsTaskExecutionRole.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonEC2ContainerServiceforEC2Role"
}

resource "aws_iam_role_policy_attachment" "ecsTaskExecutionPolicy_AmazonECSTaskExecutionRolePolicy" {
  role       = aws_iam_role.ecsTaskExecutionRole.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy"
}

resource "aws_iam_role_policy_attachment" "ecsTaskExecutionPolicy_AWSServiceRoleForECRPullThroughCache" {
  role       = aws_iam_role.ecsTaskExecutionRole.name
  policy_arn = aws_iam_policy.ecr_pullthroughcache_policy.arn
}

resource "aws_iam_role_policy_attachment" "ecsTaskExecutionPolicy_AWSServiceRoleForSecretManager" {
  role       = aws_iam_role.ecsTaskExecutionRole.name
  policy_arn = aws_iam_policy.secrets_manager_policy.arn
}


resource "aws_iam_role_policy_attachment" "ecsTaskExecutionPolicy_ecr_ECSFargateAllowExecuteCommand" {
  role       = aws_iam_role.ecsTaskExecutionRole.name
  policy_arn = aws_iam_policy.ecr_ECSFargateAllowExecuteCommand[0].arn
  count      = var.ecs_enable_execute_command ? 1 : 0
}

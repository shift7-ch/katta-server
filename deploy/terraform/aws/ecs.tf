locals {
  # Every origin the browser talks to directly: AWS S3/STS, the Katta license API, Keycloak,
  # plus the endpoints of any storage profile not hosted on AWS (var.hub_csp_additional_connect_src).
  hub_csp_connect_src = join(" ", concat([
    "'self'",
    "*.amazonaws.com",
    "api.katta.cloud",
    "https://${var.keycloak_prefix}.${terraform.workspace}.${var.dns_suffix}/",
  ], var.hub_csp_additional_connect_src))

  hub_content_security_policy = "default-src 'self'; connect-src ${local.hub_csp_connect_src}; object-src 'none'; child-src 'self'; img-src * data:; frame-ancestors 'none'"
}

resource "aws_ecr_pull_through_cache_rule" "github" {
  ecr_repository_prefix = "${terraform.workspace}-ghcr"
  upstream_registry_url = "ghcr.io"
  credential_arn        = aws_secretsmanager_secret.github_token.arn

  depends_on = [aws_secretsmanager_secret_version.github_token]
}

# Pre-populate ECR pull through cache to ensure images are available before ECS service starts
resource "null_resource" "prepopulate_ecr_cache" {
  # Trigger on changes to image versions or cache rule
  triggers = {
    keycloak_image = "${data.aws_caller_identity.current.account_id}.dkr.ecr.${var.region}.amazonaws.com/${terraform.workspace}-ghcr/shift7-ch/keycloak:${var.keycloak_version}"
    hub_image      = "${data.aws_caller_identity.current.account_id}.dkr.ecr.${var.region}.amazonaws.com/${terraform.workspace}-ghcr/shift7-ch/katta-server:${var.hub_version}"
    cache_rule     = aws_ecr_pull_through_cache_rule.github.id
  }

  provisioner "local-exec" {
    command = <<-EOT
      set -e
      echo "Authenticating with ECR..."
      aws ecr get-login-password --region ${var.region} | docker login --username AWS --password-stdin ${data.aws_caller_identity.current.account_id}.dkr.ecr.${var.region}.amazonaws.com

      docker ps || echo "Docker daemon not running, cache may populate on first ECS task start"

      echo "Pre-pulling Keycloak image to populate ECR cache..."
      docker pull ${data.aws_caller_identity.current.account_id}.dkr.ecr.${var.region}.amazonaws.com/${terraform.workspace}-ghcr/shift7-ch/keycloak:${var.keycloak_version} --platform linux/amd64 || echo "Failed to pull Keycloak image, cache may populate on first ECS task start"

      echo "Pre-pulling Hub image to populate ECR cache..."
      docker pull ${data.aws_caller_identity.current.account_id}.dkr.ecr.${var.region}.amazonaws.com/${terraform.workspace}-ghcr/shift7-ch/katta-server:${var.hub_version} --platform linux/amd64 || echo "Failed to pull Hub image, cache may populate on first ECS task start"

      echo "ECR cache pre-population complete"
    EOT
  }

  depends_on = [
    aws_ecr_pull_through_cache_rule.github,
    aws_secretsmanager_secret_version.github_token
  ]
}

resource "aws_security_group" "ecs_cluster_sg" {
  name        = "${terraform.workspace}-ecs_cluster_sg"
  description = "Security group for ECS cluster in private subnets"
  vpc_id      = aws_vpc.katta.id

  ingress {
    description     = "Allow communication to ECS tasks only from ALB"
    from_port       = 0
    to_port         = 0
    protocol        = "-1"
    security_groups = [aws_security_group.alb_sg.id]
  }

  egress {
    description = "Allow all outbound traffic"
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name        = "${terraform.workspace}-ecs_cluster_sg"
    Project     = var.project
    Environment = terraform.workspace
  }
}

resource "aws_ecs_cluster" "katta_ecs_cluster" {
  name = "${terraform.workspace}-cluster"

  service_connect_defaults {
    namespace = aws_service_discovery_http_namespace.katta_service_connect.arn
  }

  tags = {
    Name        = "${terraform.workspace}-ecs-cluster"
    Project     = var.project
    Environment = terraform.workspace
  }
}

resource "aws_service_discovery_http_namespace" "katta_service_connect" {
  name = "${terraform.workspace}-katta-services"

  tags = {
    Name        = "${terraform.workspace}-service-connect-namespace"
    Project     = var.project
    Environment = terraform.workspace
  }
}

resource "aws_cloudwatch_log_group" "keycloak_log_group" {
  name = "${terraform.workspace}-${var.keycloak_prefix}-log-group"

  tags = {
    Name        = "${terraform.workspace}-${var.keycloak_prefix}-log-group"
    Project     = var.project
    Environment = terraform.workspace
  }
}

resource "aws_cloudwatch_log_group" "hub_log_group" {
  name = "${terraform.workspace}-${var.hub_prefix}-log-group"

  tags = {
    Name        = "${terraform.workspace}-${var.hub_prefix}-log-group"
    Project     = var.project
    Environment = terraform.workspace
  }
}

# https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/ecs_task_definition
resource "aws_ecs_task_definition" "keycloak_ecs_task" {
  family = "${var.keycloak_prefix}-task"

  requires_compatibilities = ["FARGATE"]
  network_mode             = "awsvpc"
  memory                   = "2048"
  cpu                      = "1024"
  execution_role_arn       = aws_iam_role.ecsTaskExecutionRole.arn
  task_role_arn            = aws_iam_role.ecsTaskExecutionRole.arn

  container_definitions = jsonencode([
    {
      name       = "${terraform.workspace}-container-${var.keycloak_prefix}",
      image      = "${data.aws_caller_identity.current.account_id}.dkr.ecr.${var.region}.amazonaws.com/${terraform.workspace}-ghcr/shift7-ch/keycloak:${var.keycloak_version}"
      entryPoint = ["/bin/sh"]
      command = [
        "-c",
        join("", [
          "mkdir -p /opt/keycloak/data/import/ && ",
          "echo $CRYPTOMATOR_REALM_JSON | base64 -d > /opt/keycloak/data/import/cryptomator-realm.json && ",
          "/opt/keycloak/bin/kc.sh start --http-access-log-enabled=true --log-level=DEBUG --import-realm"
        ])
      ]
      # requests:
      # cpu: 25m
      # memory: 256Mi
      # limits:
      # cpu: 1000m
      # memory: 1024Mi
      memory    = 2048
      cpu       = 1024
      essential = true
      portMappings = [
        {
          containerPort = 8443
          protocol      = "tcp"
        },
        {
          name          = "keycloak"
          containerPort = 8080
          protocol      = "tcp"
        },
        {
          containerPort = 9000
          protocol      = "tcp"
        }
      ]
      environment = [
        {
          name  = "CRYPTOMATOR_REALM_JSON"
          value = local.cryptomator_realm_base64
        },
        {
          name  = "KC_DB"
          value = "postgres"
        },
        {
          name  = "KC_DB_URL"
          value = "jdbc:postgresql://${aws_db_instance.postgres.endpoint}/${var.keycloak_db_name}"
        },
        {
          name  = "DB_DATABASE"
          value = var.keycloak_db_name
        },
        {
          name  = "KC_HEALTH_ENABLED"
          value = "true"
        },
        {
          name  = "KC_METRICS_ENABLED"
          value = "true"
        },
        {
          name  = "KC_HOSTNAME"
          value = "${var.keycloak_prefix}.${terraform.workspace}.${var.dns_suffix}"
        },
        {
          name  = "KC_HTTP_ENABLED"
          value = "true"
        },
        {
          name  = "KC_PROXY_HEADERS"
          value = "xforwarded"
        }
      ]
      secrets = [
        # resolve the ${...} placeholders of the realm on import
        {
          name      = "HUB_ADMIN_PASSWORD"
          valueFrom = "${aws_secretsmanager_secret.hub_admin.arn}:password::"
        },
        {
          name      = "HUB_KEYCLOAK_SYSTEM_CLIENT_SECRET"
          valueFrom = "${aws_secretsmanager_secret.hub_oidc_client_secrets_credentials.arn}:hub_keycloak_system_client_secret::"
        },
        {
          name      = "HUB_KEYCLOAK_OIDC_CRYPTOMATOR_VAULTS_CLIENT_SECRET"
          valueFrom = "${aws_secretsmanager_secret.hub_oidc_client_secrets_credentials.arn}:hub_keycloak_oidc_cryptomator_vaults_client_secret::"
        },
        {
          name      = "KC_BOOTSTRAP_ADMIN_USERNAME"
          valueFrom = "${aws_secretsmanager_secret.keycloak_admin.arn}:username::"
        },
        {
          name      = "KC_BOOTSTRAP_ADMIN_PASSWORD"
          valueFrom = "${aws_secretsmanager_secret.keycloak_admin.arn}:password::"
        },
        {
          name      = "KC_DB_USERNAME"
          valueFrom = "${aws_secretsmanager_secret.keycloak_db_credentials.arn}:username::"
        },
        {
          name      = "KC_DB_PASSWORD"
          valueFrom = "${aws_secretsmanager_secret.keycloak_db_credentials.arn}:password::"
        }
      ]
      logConfiguration = {
        logDriver = "awslogs"
        options = {
          awslogs-group         = aws_cloudwatch_log_group.keycloak_log_group.name
          awslogs-region        = var.region
          awslogs-stream-prefix = "keycloak"
        }
      }
      healthCheck = {
        command     = ["CMD-SHELL", "curl -v --fail http://localhost:9000/health"]
        interval    = 30
        timeout     = 5
        retries     = 5
        startPeriod = 120
      },
      linuxParameters = {
        initProcessEnabled = var.ecs_enable_execute_command
      }
    }
  ])

  tags = {
    Name        = "${terraform.workspace}-ecs-task"
    Project     = var.project
    Environment = terraform.workspace
  }

  # secrets are resolved on task start, so their values must exist before the task is deployed
  depends_on = [
    null_resource.prepopulate_ecr_cache,
    aws_secretsmanager_secret_version.keycloak_admin_version,
    aws_secretsmanager_secret_version.keycloak_db_credentials_version,
    aws_secretsmanager_secret_version.hub_admin_version,
    aws_secretsmanager_secret_version.hub_oidc_client_secrets_credentials_version
  ]
}

resource "aws_ecs_service" "keycloak_ecs_service" {
  name                   = "${terraform.workspace}-${var.keycloak_prefix}-ecs-service"
  cluster                = aws_ecs_cluster.katta_ecs_cluster.id
  task_definition        = "${aws_ecs_task_definition.keycloak_ecs_task.family}:${aws_ecs_task_definition.keycloak_ecs_task.revision}"
  launch_type            = "FARGATE"
  scheduling_strategy    = "REPLICA"
  desired_count          = 1
  force_new_deployment   = true
  enable_execute_command = var.ecs_enable_execute_command
  wait_for_steady_state  = true

  health_check_grace_period_seconds = 300

  availability_zone_rebalancing = "ENABLED"
  propagate_tags                = "TASK_DEFINITION"

  deployment_circuit_breaker {
    enable   = true
    rollback = true
  }

  network_configuration {
    subnets          = aws_subnet.private[*].id
    assign_public_ip = false
    security_groups = [
      aws_security_group.ecs_cluster_sg.id,
    ]
  }

  load_balancer {
    target_group_arn = aws_lb_target_group.keycloak_ecs_target_group.arn
    container_name   = "${terraform.workspace}-container-${var.keycloak_prefix}"
    container_port   = 8080
  }

  service_connect_configuration {
    enabled   = true
    namespace = aws_service_discovery_http_namespace.katta_service_connect.arn

    service {
      port_name      = "keycloak"
      discovery_name = "keycloak"

      client_alias {
        port     = 8080
        dns_name = "keycloak"
      }
    }

    log_configuration {
      log_driver = "awslogs"
      options = {
        awslogs-group         = aws_cloudwatch_log_group.keycloak_log_group.name
        awslogs-region        = var.region
        awslogs-stream-prefix = "service-connect"
      }
    }
  }

  tags = {
    Name        = "${terraform.workspace}-ecs-task"
    Project     = var.project
    Environment = terraform.workspace
  }

  # the target group only gets attached to the ALB by its HTTPS listener
  depends_on = [aws_lb_listener.keycloak_https_listener]
}

# https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/ecs_task_definition
resource "aws_ecs_task_definition" "katta_server_ecs_task" {
  family = "${var.hub_prefix}-task"

  requires_compatibilities = ["FARGATE"]
  network_mode             = "awsvpc"
  # https://docs.aws.amazon.com/AmazonECS/latest/developerguide/task-cpu-memory-error.html
  # resources:
  #   requests:
  #   cpu: 25m
  # memory: 128Mi
  # limits:
  # cpu: 1000m
  # memory: 512Mi
  memory             = "512"
  cpu                = "256"
  execution_role_arn = aws_iam_role.ecsTaskExecutionRole.arn
  task_role_arn      = aws_iam_role.ecsTaskExecutionRole.arn

  container_definitions = jsonencode([
    {
      name  = "${terraform.workspace}-container-${var.hub_prefix}",
      image = "${data.aws_caller_identity.current.account_id}.dkr.ecr.${var.region}.amazonaws.com/${terraform.workspace}-ghcr/shift7-ch/katta-server:${var.hub_version}"
      # command = ["start", "--http-access-log-enabled=true", "--log-level=DEBUG"]
      memory    = 512
      cpu       = 256
      essential = true
      portMappings = [
        {
          containerPort = 8280
          protocol      = "tcp"
        },
        {
          containerPort = 9000
          protocol      = "tcp"
        },
      ]
      environment = concat([
        {
          name  = "HUB_KEYCLOAK_LOCAL_URL"
          value = "https://${var.keycloak_prefix}.${terraform.workspace}.${var.dns_suffix}"
        },
        {
          name  = "HUB_KEYCLOAK_PUBLIC_URL"
          value = "https://${var.keycloak_prefix}.${terraform.workspace}.${var.dns_suffix}"
        },
        {
          name  = "HUB_KEYCLOAK_REALM"
          value = "cryptomator"
        },
        {
          name  = "HUB_KEYCLOAK_SYSTEM_CLIENT_ID"
          value = "cryptomatorhub-system"
        },
        {
          name  = "HUB_KEYCLOAK_SYNCER_PERIOD"
          value = "30s"
        },
        {
          name  = "HUB_KEYCLOAK_OIDC_CRYPTOMATOR_CLIENT_ID"
          value = "cryptomator"
        },
        {
          name  = "HUB_PUBLIC_ROOT_PATH"
          value = "/"
        },
        {
          name  = "QUARKUS_HTTP_PORT"
          value = "8280"
        },
        {
          name  = "QUARKUS_OIDC_AUTH_SERVER_URL"
          value = "https://${var.keycloak_prefix}.${terraform.workspace}.${var.dns_suffix}/realms/cryptomator"
        },
        {
          name  = "QUARKUS_OIDC_TOKEN_ISSUER"
          value = "https://${var.keycloak_prefix}.${terraform.workspace}.${var.dns_suffix}/realms/cryptomator"
        },
        {
          name  = "QUARKUS_OIDC_CLIENT_ID"
          value = "cryptomatorhub"
        },
        {
          name  = "QUARKUS_DATASOURCE_JDBC_URL"
          value = "jdbc:postgresql://${aws_db_instance.hub_db.endpoint}/${var.hub_db_name}"
        },
        {
          name  = "QUARKUS_HTTP_HEADER__CONTENT_SECURITY_POLICY__VALUE"
          value = local.hub_content_security_policy
        },
        ], var.hub_initial_license == null ? [] : [
        {
          name  = "HUB_INITIAL_LICENSE"
          value = var.hub_initial_license
        },
        ], var.hub_initial_id == null ? [] : [
        {
          name  = "HUB_INITIAL_ID"
          value = var.hub_initial_id
        },
        ], var.hub_license_chain_required_cn == null ? [] : [
        {
          name  = "HUB_LICENSE_CHAIN_REQUIRED_CN"
          value = var.hub_license_chain_required_cn
        },
      ])
      secrets = [
        {
          name      = "QUARKUS_DATASOURCE_USERNAME"
          valueFrom = "${aws_secretsmanager_secret.hub_db_credentials.arn}:username::"
        },
        {
          name      = "QUARKUS_DATASOURCE_PASSWORD"
          valueFrom = "${aws_secretsmanager_secret.hub_db_credentials.arn}:password::"
        },
        {
          name      = "hub_keycloak_system_client_secret"
          valueFrom = "${aws_secretsmanager_secret.hub_oidc_client_secrets_credentials.arn}:hub_keycloak_system_client_secret::"
        },
        {
          name      = "hub_keycloak_oidc_cryptomator_vaults_client_secret"
          valueFrom = "${aws_secretsmanager_secret.hub_oidc_client_secrets_credentials.arn}:hub_keycloak_oidc_cryptomator_vaults_client_secret::"
        }
      ]
      logConfiguration = {
        logDriver = "awslogs"
        options = {
          awslogs-group         = aws_cloudwatch_log_group.hub_log_group.name
          awslogs-region        = var.region
          awslogs-stream-prefix = "hub"
        }
      }
      healthCheck = {
        command     = ["CMD-SHELL", "curl -v --fail http://localhost:9000/q/health/ready"]
        interval    = 10
        timeout     = 5
        retries     = 3
        startPeriod = 30
      },
      linuxParameters = {
        initProcessEnabled = var.ecs_enable_execute_command
      }
    }
  ])

  tags = {
    Name        = "${terraform.workspace}-ecs-task"
    Project     = var.project
    Environment = terraform.workspace
  }
  # secrets are resolved on task start, so their values must exist before the task is deployed
  depends_on = [
    null_resource.prepopulate_ecr_cache,
    aws_ecs_service.keycloak_ecs_service,
    aws_secretsmanager_secret_version.hub_db_credentials_version,
    aws_secretsmanager_secret_version.hub_oidc_client_secrets_credentials_version
  ]
}

resource "aws_ecs_service" "katta_server_ecs_service" {
  name                   = "${terraform.workspace}-${var.hub_prefix}-ecs-service"
  cluster                = aws_ecs_cluster.katta_ecs_cluster.id
  task_definition        = "${aws_ecs_task_definition.katta_server_ecs_task.family}:${aws_ecs_task_definition.katta_server_ecs_task.revision}"
  launch_type            = "FARGATE"
  scheduling_strategy    = "REPLICA"
  desired_count          = 1
  force_new_deployment   = true
  enable_execute_command = var.ecs_enable_execute_command
  wait_for_steady_state  = true

  health_check_grace_period_seconds = 60

  availability_zone_rebalancing = "ENABLED"
  propagate_tags                = "TASK_DEFINITION"

  deployment_circuit_breaker {
    enable   = true
    rollback = true
  }

  network_configuration {
    subnets          = aws_subnet.private[*].id
    assign_public_ip = false
    security_groups = [
      aws_security_group.ecs_cluster_sg.id,
    ]
  }

  load_balancer {
    target_group_arn = aws_lb_target_group.hub_ecs_target_group.arn
    container_name   = "${terraform.workspace}-container-${var.hub_prefix}"
    container_port   = 8280
  }

  service_connect_configuration {
    enabled   = true
    namespace = aws_service_discovery_http_namespace.katta_service_connect.arn

    log_configuration {
      log_driver = "awslogs"
      options = {
        awslogs-group         = aws_cloudwatch_log_group.hub_log_group.name
        awslogs-region        = var.region
        awslogs-stream-prefix = "service-connect"
      }
    }
  }

  tags = {
    Name        = "${terraform.workspace}-ecs-task"
    Project     = var.project
    Environment = terraform.workspace
  }

  # the target group only gets attached to the ALB by its HTTPS listener
  depends_on = [aws_ecs_service.keycloak_ecs_service, aws_lb_listener.hub_https_listener]
}

resource "aws_security_group" "vpc_endpoint_sg" {
  name   = "${terraform.workspace}-vpc-endpoint-sg"
  vpc_id = aws_vpc.katta.id

  ingress {
    from_port       = 80
    to_port         = 80
    protocol        = "tcp"
    security_groups = [aws_security_group.ecs_cluster_sg.id]
  }

  ingress {
    from_port       = 443
    to_port         = 443
    protocol        = "tcp"
    security_groups = [aws_security_group.ecs_cluster_sg.id]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name        = "${terraform.workspace}-vpc-endpoint-sg"
    Project     = var.project
    Environment = terraform.workspace
  }
}

resource "aws_appautoscaling_target" "ecs_autoscaling_target" {
  min_capacity       = 1
  max_capacity       = 12
  resource_id        = "service/${aws_ecs_cluster.katta_ecs_cluster.name}/${aws_ecs_service.keycloak_ecs_service.name}"
  scalable_dimension = "ecs:service:DesiredCount"
  service_namespace  = "ecs"
}

resource "aws_appautoscaling_policy" "cpu_scaling_policy" {
  name               = "cpu-scaling-policy"
  policy_type        = "TargetTrackingScaling"
  resource_id        = aws_appautoscaling_target.ecs_autoscaling_target.resource_id
  scalable_dimension = aws_appautoscaling_target.ecs_autoscaling_target.scalable_dimension
  service_namespace  = aws_appautoscaling_target.ecs_autoscaling_target.service_namespace

  target_tracking_scaling_policy_configuration {
    predefined_metric_specification {
      predefined_metric_type = "ECSServiceAverageCPUUtilization"
    }

    target_value       = 60.0 # Target CPU utilization percentage
    scale_in_cooldown  = 60
    scale_out_cooldown = 60
  }
}

resource "aws_appautoscaling_policy" "memory_scaling_policy" {
  name               = "memory-scaling-policy"
  policy_type        = "TargetTrackingScaling"
  resource_id        = aws_appautoscaling_target.ecs_autoscaling_target.resource_id
  scalable_dimension = aws_appautoscaling_target.ecs_autoscaling_target.scalable_dimension
  service_namespace  = aws_appautoscaling_target.ecs_autoscaling_target.service_namespace

  target_tracking_scaling_policy_configuration {
    predefined_metric_specification {
      predefined_metric_type = "ECSServiceAverageMemoryUtilization"
    }

    target_value       = 70.0 # Target Memory utilization percentage
    scale_in_cooldown  = 60
    scale_out_cooldown = 60
  }
}

resource "aws_appautoscaling_policy" "request_scaling_policy" {
  name               = "request-scaling-policy"
  policy_type        = "TargetTrackingScaling"
  resource_id        = aws_appautoscaling_target.ecs_autoscaling_target.resource_id
  scalable_dimension = aws_appautoscaling_target.ecs_autoscaling_target.scalable_dimension
  service_namespace  = aws_appautoscaling_target.ecs_autoscaling_target.service_namespace

  target_tracking_scaling_policy_configuration {
    customized_metric_specification {
      metric_name = "RequestCountPerTarget"
      namespace   = "AWS/ApplicationELB"
      statistic   = "Sum"
      dimensions {
        name  = "LoadBalancer"
        value = aws_lb.keycloak_public_alb.arn_suffix
      }

      dimensions {
        name  = "TargetGroup"
        value = aws_lb_target_group.keycloak_ecs_target_group.arn_suffix
      }
      unit = "Count"
    }

    target_value       = 300
    scale_in_cooldown  = 60
    scale_out_cooldown = 60
  }
}

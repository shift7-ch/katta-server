resource "aws_security_group" "alb_sg" {
  name        = "${terraform.workspace}-alb-sg"
  description = "Allow inbound traffic to ALB"
  vpc_id      = aws_vpc.katta.id

  ingress {
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"] # Allows public access to port 80
  }

  ingress {
    from_port   = 443
    to_port     = 443
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"] # Allows public access to port 443
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name        = "${terraform.workspace}-alb-sg"
    Project     = var.project
    Environment = terraform.workspace
  }
}

resource "aws_lb" "keycloak_public_alb" {
  name               = "${var.keycloak_prefix}-${terraform.workspace}-alb"
  internal           = false
  load_balancer_type = "application"
  security_groups    = [aws_security_group.alb_sg.id]
  subnets            = aws_subnet.public[*].id

  tags = {
    Name        = "${terraform.workspace}-alb"
    Project     = var.project
    Environment = terraform.workspace
  }
}

resource "aws_lb" "hub_public_alb" {
  name               = "${var.hub_prefix}-${terraform.workspace}-alb"
  internal           = false
  load_balancer_type = "application"
  security_groups    = [aws_security_group.alb_sg.id]
  subnets            = aws_subnet.public[*].id

  tags = {
    Name        = "${terraform.workspace}-alb"
    Project     = var.project
    Environment = terraform.workspace
  }
}

resource "aws_lb_target_group" "keycloak_ecs_target_group" {
  name        = "${terraform.workspace}-${var.keycloak_prefix}-ecs-tg"
  port        = 8080
  protocol    = "HTTP"
  vpc_id      = aws_vpc.katta.id
  target_type = "ip"

  health_check {
    path                = "/health"
    port                = "9000"
    interval            = 30
    timeout             = 5
    healthy_threshold   = 2
    unhealthy_threshold = 3
    matcher             = "200"
    protocol            = "HTTP"
  }

  tags = {
    Name        = "${terraform.workspace}-${var.keycloak_prefix}-ecs-tg"
    Project     = var.project
    Environment = terraform.workspace
  }
}

resource "aws_lb_target_group" "hub_ecs_target_group" {
  name        = "${terraform.workspace}-${var.hub_prefix}-ecs-tg"
  port        = 8280
  protocol    = "HTTP"
  vpc_id      = aws_vpc.katta.id
  target_type = "ip"

  health_check {
    path                = "/q/health/ready"
    port                = "9000"
    interval            = 30
    timeout             = 5
    healthy_threshold   = 2
    unhealthy_threshold = 3
    matcher             = "200"
    protocol            = "HTTP"
  }

  tags = {
    Name        = "${terraform.workspace}-${var.hub_prefix}-ecs-tg"
    Project     = var.project
    Environment = terraform.workspace
  }
}

# resource "aws_lb_target_group_attachment" "keycloak_ecs_target_group_attachment" {
#   target_group_arn = aws_lb_target_group.keycloak_ecs_target_group.arn
#   target_id        = aws_ecs_service.katta_server_ecs_service.id
# }
#
# resource "aws_lb_target_group_attachment" "hub_ecs_target_group_attachment" {
#   target_group_arn = aws_lb_target_group.hub_ecs_target_group.arn
#   target_id        = aws_ecs_service.katta_server_ecs_service.id
# }

resource "aws_lb_listener" "keycloak_https_listener" {
  load_balancer_arn = aws_lb.keycloak_public_alb.arn
  port              = 443
  protocol          = "HTTPS"

  ssl_policy      = "ELBSecurityPolicy-TLS13-1-2-2021-06"
  certificate_arn = aws_acm_certificate_validation.keycloak_cert_validation.certificate_arn


  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.keycloak_ecs_target_group.arn
  }
}

resource "aws_lb_listener" "hub_https_listener" {
  load_balancer_arn = aws_lb.hub_public_alb.arn
  port              = 443
  protocol          = "HTTPS"

  ssl_policy      = "ELBSecurityPolicy-TLS13-1-2-2021-06"
  certificate_arn = aws_acm_certificate_validation.hub_cert_validation.certificate_arn


  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.hub_ecs_target_group.arn
  }
}

resource "aws_lb_listener" "keycloak_http_listener" {
  load_balancer_arn = aws_lb.keycloak_public_alb.arn
  port              = 80
  protocol          = "HTTP"

  default_action {
    type = "redirect"

    redirect {
      protocol    = "HTTPS"
      port        = "443"
      status_code = "HTTP_301"
    }
  }
}
resource "aws_lb_listener" "hub_http_listener" {
  load_balancer_arn = aws_lb.hub_public_alb.arn
  port              = 80
  protocol          = "HTTP"

  default_action {
    type = "redirect"

    redirect {
      protocol    = "HTTPS"
      port        = "443"
      status_code = "HTTP_301"
    }
  }
}

resource "aws_route53_record" "keycloak_alb" {
  zone_id = aws_route53_zone.keycloak_subdomain_zone.zone_id
  name    = "${var.keycloak_prefix}.${terraform.workspace}.${var.dns_suffix}"
  type    = "A"

  alias {
    name                   = aws_lb.keycloak_public_alb.dns_name
    zone_id                = aws_lb.keycloak_public_alb.zone_id
    evaluate_target_health = true
  }
}

resource "aws_route53_record" "hub_alb" {
  zone_id = aws_route53_zone.hub_subdomain_zone.zone_id
  name    = "${var.hub_prefix}.${terraform.workspace}.${var.dns_suffix}"
  type    = "A"

  alias {
    name                   = aws_lb.hub_public_alb.dns_name
    zone_id                = aws_lb.hub_public_alb.zone_id
    evaluate_target_health = true
  }
}

resource "aws_route53_record" "keycloak_cert_validation" {
  for_each = {
    for dvo in aws_acm_certificate.keycloak_cert.domain_validation_options : dvo.domain_name => {
      name   = dvo.resource_record_name
      type   = dvo.resource_record_type
      record = dvo.resource_record_value
    }
  }

  zone_id = aws_route53_zone.keycloak_subdomain_zone.zone_id
  name    = each.value.name
  type    = each.value.type
  ttl     = 60
  records = [each.value.record]
}

resource "aws_route53_record" "hub_cert_validation" {
  for_each = {
    for dvo in aws_acm_certificate.hub_cert.domain_validation_options : dvo.domain_name => {
      name   = dvo.resource_record_name
      type   = dvo.resource_record_type
      record = dvo.resource_record_value
    }
  }

  zone_id = aws_route53_zone.hub_subdomain_zone.zone_id
  name    = each.value.name
  type    = each.value.type
  ttl     = 60
  records = [each.value.record]
}

resource "aws_acm_certificate" "keycloak_cert" {
  domain_name               = "${var.keycloak_prefix}.${terraform.workspace}.${var.dns_suffix}"
  validation_method         = "DNS"
  subject_alternative_names = ["www.${var.keycloak_prefix}.${terraform.workspace}.${var.dns_suffix}"]

  tags = {
    Environment = terraform.workspace
    Project     = var.project
  }

  lifecycle {
    create_before_destroy = true
  }
}

resource "aws_acm_certificate" "hub_cert" {
  domain_name               = "${var.hub_prefix}.${terraform.workspace}.${var.dns_suffix}"
  validation_method         = "DNS"
  subject_alternative_names = ["www.${var.hub_prefix}.${terraform.workspace}.${var.dns_suffix}"]

  tags = {
    Environment = terraform.workspace
    Project     = var.project
  }

  lifecycle {
    create_before_destroy = true
  }
}

resource "aws_acm_certificate_validation" "keycloak_cert_validation" {
  certificate_arn         = aws_acm_certificate.keycloak_cert.arn
  validation_record_fqdns = [for record in aws_route53_record.keycloak_cert_validation : record.fqdn]
}

resource "aws_acm_certificate_validation" "hub_cert_validation" {
  certificate_arn         = aws_acm_certificate.hub_cert.arn
  validation_record_fqdns = [for record in aws_route53_record.hub_cert_validation : record.fqdn]
}

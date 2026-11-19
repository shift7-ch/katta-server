resource "aws_route53_zone" "keycloak_subdomain_zone" {
  name = "${var.keycloak_prefix}.${terraform.workspace}.${var.dns_suffix}"

  tags = {
    Name        = "${var.project}.${terraform.workspace}.${var.dns_suffix}"
    Project     = var.project
    Environment = terraform.workspace
  }
}

resource "aws_route53_record" "keycloak_subdomain_delegation" {
  zone_id = data.aws_route53_zone.parent_zone.zone_id
  name    = "${var.keycloak_prefix}.${terraform.workspace}.${var.dns_suffix}"
  type    = "NS"

  records = aws_route53_zone.keycloak_subdomain_zone.name_servers
  ttl     = 300
}


resource "aws_route53_zone" "hub_subdomain_zone" {
  name = "${var.hub_prefix}.${terraform.workspace}.${var.dns_suffix}"

  tags = {
    Name        = "${var.project}.${terraform.workspace}.${var.dns_suffix}"
    Project     = var.project
    Environment = terraform.workspace
  }
}

resource "aws_route53_record" "hub_subdomain_delegation" {
  zone_id = data.aws_route53_zone.parent_zone.zone_id
  name    = "${var.hub_prefix}.${terraform.workspace}.${var.dns_suffix}"
  type    = "NS"

  records = aws_route53_zone.hub_subdomain_zone.name_servers
  ttl     = 300
}



output "keycloak_alb_dns" {
  value = aws_lb.keycloak_public_alb.dns_name
}

output "hub_alb_dns" {
  value = aws_lb.hub_public_alb.dns_name
}

output "workspace" {
  value = terraform.workspace
}

output "account_id" {
  value = data.aws_caller_identity.current.account_id
}

output "keycloak_hosted_zone" {
  value = aws_route53_zone.keycloak_subdomain_zone.name
}

output "hub_hosted_zone" {
  value = aws_route53_zone.hub_subdomain_zone.name
}

output "hub_name_servers" {
  value = aws_route53_zone.hub_subdomain_zone.name_servers
}

output "cryptomator_realm_base64" {
  value = nonsensitive(base64decode(local.cryptomator_realm_base64))
}

resource "random_id" "secret_suffix" {
  byte_length = 8
}

resource "aws_secretsmanager_secret" "keycloak_db_credentials" {
  name                    = "${terraform.workspace}-${var.keycloak_prefix}-db-credentials__${random_id.secret_suffix.hex}"
  description             = "Database credentials in ${terraform.workspace}"
  recovery_window_in_days = 7

  tags = {
    Project     = var.project
    Environment = terraform.workspace
  }
}

resource "aws_secretsmanager_secret_version" "keycloak_db_credentials_version" {
  secret_id = aws_secretsmanager_secret.keycloak_db_credentials.id
  secret_string = jsonencode({
    username = var.keycloak_db_username
    password = var.keycloak_db_password
  })
}

resource "aws_secretsmanager_secret" "keycloak_admin" {
  name                    = "${terraform.workspace}-${var.keycloak_prefix}-admin__${random_id.secret_suffix.hex}"
  description             = "Keycloak admin credentials in ${terraform.workspace}"
  recovery_window_in_days = 7

  tags = {
    Project     = var.project
    Environment = terraform.workspace
  }
}

resource "aws_secretsmanager_secret_version" "keycloak_admin_version" {
  secret_id = aws_secretsmanager_secret.keycloak_admin.id
  secret_string = jsonencode({
    username = var.keycloak_admin_username
    password = var.keycloak_admin_password
  })
}


resource "aws_secretsmanager_secret" "hub_db_credentials" {
  name                    = "${terraform.workspace}-${var.hub_prefix}-db-credentials__${random_id.secret_suffix.hex}"
  description             = "Database credentials in ${terraform.workspace}"
  recovery_window_in_days = 7

  tags = {
    Project     = var.project
    Environment = terraform.workspace
  }
}

resource "aws_secretsmanager_secret_version" "hub_db_credentials_version" {
  secret_id = aws_secretsmanager_secret.hub_db_credentials.id
  secret_string = jsonencode({
    username = var.hub_db_username
    password = var.hub_db_password
  })
}

resource "aws_secretsmanager_secret" "hub_oidc_client_secrets_credentials" {
  name                    = "${terraform.workspace}-${var.hub_prefix}-oidc-client-secrets__${random_id.secret_suffix.hex}"
  description             = "OIDC client secrets in ${terraform.workspace}"
  recovery_window_in_days = 7

  tags = {
    Project     = var.project
    Environment = terraform.workspace
  }
}

# Realm imported into Keycloak, rendered from the realm template `templates/_realm.tpl` of the Katta Server Helm chart in this repository.
# Secrets are rendered as ${...} placeholders, which Keycloak resolves on import from the environment of the Keycloak
# container, so that they stay in Secrets Manager.
data "helm_template" "katta_server" {
  name      = "katta-server"
  chart     = "${path.module}/../../../chart"
  show_only = ["templates/keycloak-secret.yaml"]
  # the chart requires a Kubernetes version, which is only checked when rendering
  kube_version = "1.33.0"
  set = [
    { name = "urls.hub.public", value = "https://${var.hub_prefix}.${terraform.workspace}.${var.dns_suffix}", type = "string" },
    { name = "urls.kc.public", value = "https://${var.keycloak_prefix}.${terraform.workspace}.${var.dns_suffix}", type = "string" },
    { name = "keycloak.realmBootstrap.realmId", value = "cryptomator", type = "string" },
    { name = "hub.admin.username", value = var.hub_admin_username, type = "string" },
    { name = "hub.admin.password", value = "$${HUB_ADMIN_PASSWORD}", type = "string" },
    { name = "hub.secrets.systemClientSecret", value = "$${HUB_KEYCLOAK_SYSTEM_CLIENT_SECRET}", type = "string" },
    { name = "hub.secrets.cryptomatorvaultsClientSecret", value = "$${HUB_KEYCLOAK_OIDC_CRYPTOMATOR_VAULTS_CLIENT_SECRET}", type = "string" },
  ]
}

locals {
  # Base64-encoded realm JSON for environment variable
  cryptomator_realm_base64 = base64encode(yamldecode(data.helm_template.katta_server.manifests["templates/keycloak-secret.yaml"]).stringData["realm.json"])
}

resource "aws_secretsmanager_secret_version" "hub_oidc_client_secrets_credentials_version" {
  secret_id = aws_secretsmanager_secret.hub_oidc_client_secrets_credentials.id
  secret_string = jsonencode({
    hub_keycloak_system_client_secret                  = var.hub_keycloak_system_client_secret
    hub_keycloak_oidc_cryptomator_vaults_client_secret = var.hub_keycloak_oidc_cryptomator_vaults_client_secret
  })
}

resource "aws_secretsmanager_secret" "hub_admin" {
  name                    = "${terraform.workspace}-${var.hub_prefix}-admin__${random_id.secret_suffix.hex}"
  description             = "Katta Server admin credentials in ${terraform.workspace}"
  recovery_window_in_days = 7

  tags = {
    Project     = var.project
    Environment = terraform.workspace
  }
}

resource "aws_secretsmanager_secret_version" "hub_admin_version" {
  secret_id = aws_secretsmanager_secret.hub_admin.id
  secret_string = jsonencode({
    username = var.hub_admin_username
    password = var.hub_admin_password
  })
}

resource "aws_secretsmanager_secret" "github_token" {
  name                    = "ecr-pullthroughcache/${terraform.workspace}-ghcr"
  recovery_window_in_days = 0

  tags = {
    Project     = var.project
    Environment = terraform.workspace
  }
}

resource "aws_secretsmanager_secret_version" "github_token" {
  secret_id = aws_secretsmanager_secret.github_token.id
  secret_string = jsonencode({
    username    = "oauth2"
    accessToken = var.github_token
  })
}

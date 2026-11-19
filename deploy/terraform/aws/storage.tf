locals {
  hub_url           = "https://${var.hub_prefix}.${terraform.workspace}.${var.dns_suffix}"
  keycloak_realm    = "${var.keycloak_prefix}.${terraform.workspace}.${var.dns_suffix}/realms/cryptomator"
  oidc_provider_arn = "arn:aws:iam::${data.aws_caller_identity.current.account_id}:oidc-provider/${local.keycloak_realm}"

  storage_profile_aws_role_name_prefix = coalesce(var.storage_profile_aws_role_name_prefix, "${terraform.workspace}-")
  storage_profile_aws_bucket_prefix    = coalesce(var.storage_profile_aws_bucket_prefix, "${var.project}-${terraform.workspace}-")
  storage_profile_aws_regions          = distinct(concat([var.region], var.storage_profile_aws_regions))

  # Wait for Hub to be reachable, the subdomain delegation may take a while to propagate to the local resolver
  wait_for_hub = <<-EOT
    for i in $(seq 1 60); do
      curl -sf -o /dev/null "${local.hub_url}/api/config" && break
      echo "Waiting for ${local.hub_url} ($i/60)..."
      sleep 10
    done
  EOT
}

# Identity provider for the Keycloak realm and IAM roles for bucket creation and access using STS
resource "terraform_data" "setup_aws" {
  count = var.storage_profile_aws_enabled ? 1 : 0

  input = {
    role_name_prefix  = local.storage_profile_aws_role_name_prefix
    bucket_prefix     = local.storage_profile_aws_bucket_prefix
    oidc_provider_arn = local.oidc_provider_arn
  }

  provisioner "local-exec" {
    command = <<-EOT
      set -e
      ${local.wait_for_hub}
      katta setup aws \
        --hubUrl "${local.hub_url}" \
        --profileName "$${AWS_PROFILE:-default}" \
        --roleNamePrefix "${self.input.role_name_prefix}" \
        --bucketPrefix "${self.input.bucket_prefix}"
    EOT
  }

  # Remove identity provider and roles not managed by Terraform
  provisioner "local-exec" {
    when    = destroy
    command = <<-EOT
      for role in create-bucket access-bucket-web-identity-role access-bucket-tagged-session-role; do
        aws iam delete-role-policy --role-name "${self.input.role_name_prefix}$role" --policy-name "${self.input.role_name_prefix}$role" || true
        aws iam delete-role --role-name "${self.input.role_name_prefix}$role" || true
      done
      aws iam delete-open-id-connect-provider --open-id-connect-provider-arn "${self.input.oidc_provider_arn}" || true
    EOT
  }

  lifecycle {
    precondition {
      # bucket names are limited to 63 characters, the prefix is followed by the vault ID (UUID)
      condition     = length(local.storage_profile_aws_bucket_prefix) <= 27
      error_message = "Bucket prefix \"${local.storage_profile_aws_bucket_prefix}\" must not exceed 27 characters. Set storage_profile_aws_bucket_prefix."
    }
  }

  depends_on = [
    aws_ecs_service.katta_server_ecs_service,
    aws_route53_record.hub_alb,
  ]
}

# Storage profile in Hub for vaults in AWS S3 using the roles from setup
resource "terraform_data" "storage_profile_aws" {
  count = var.storage_profile_aws_enabled ? 1 : 0

  triggers_replace = [
    terraform_data.setup_aws[0].output,
    local.storage_profile_aws_regions,
  ]

  provisioner "local-exec" {
    command = <<-EOT
      set -e
      ${local.wait_for_hub}
      ACCESS_TOKEN=$(katta accesstoken \
        --tokenUrl "https://${local.keycloak_realm}/protocol/openid-connect/token" \
        --clientId cryptomatorhub-system \
        --clientSecret "$HUB_KEYCLOAK_SYSTEM_CLIENT_SECRET")
      katta storageprofile aws sts \
        --skipIfExists \
        --hubUrl "${local.hub_url}" \
        --accessToken "$ACCESS_TOKEN" \
        --awsAccountId "${data.aws_caller_identity.current.account_id}" \
        --roleNamePrefix "${local.storage_profile_aws_role_name_prefix}" \
        --bucketPrefix "${local.storage_profile_aws_bucket_prefix}" \
        --region "${local.storage_profile_aws_regions[0]}" \
        ${join(" ", [for r in local.storage_profile_aws_regions : "--regions \"${r}\""])}
    EOT
    environment = {
      HUB_KEYCLOAK_SYSTEM_CLIENT_SECRET = var.hub_keycloak_system_client_secret
    }
  }
}

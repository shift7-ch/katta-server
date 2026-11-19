variable "project" {
  description = "The project Name where all resources will be launched."
  type        = string
  default     = "katta"
}

variable "dns_suffix" {
  description = "DNS suffix."
  type        = string
}

variable "keycloak_prefix" {
  description = "DNS prefix for Keycloak service endpoints."
  type        = string
  default     = "keycloak"
}

variable "hub_prefix" {
  description = "DNS prefix for Hub service endpoints."
  type        = string
  default     = "hub"
}

variable "region" {
  description = "The region to create resources."
  type        = string
  default     = "eu-central-1" # Override with TF_VAR_region or set TF_VAR_region=$AWS_DEFAULT_REGION
}

variable "vpc_cidr" {
  description = "The CIDR block for the VPC."
  type        = string
  default     = "10.1.0.0/16"
}

variable "keycloak_db_name" {
  description = "The name of the Keycloak database."
  type        = string
  default     = "keycloak_database"
}

variable "keycloak_db_username" {
  description = "The username for the Keycloak database."
  type        = string
  default     = "keycloak_admin"
}

variable "keycloak_db_password" {
  description = "The password for the Keycloak database."
  type        = string
  sensitive   = true
}

variable "hub_db_name" {
  description = "The name of the Hub database."
  type        = string
  default     = "hub_database"
}

variable "hub_db_username" {
  description = "The username for the Hub database."
  type        = string
  default     = "hub_admin"
}

variable "hub_db_password" {
  description = "The password for the Hub database."
  type        = string
  sensitive   = true
}

variable "keycloak_admin_username" {
  description = "The username for the Keycloak admin user."
  type        = string
  default     = "keycloak_admin"
}

variable "keycloak_admin_password" {
  description = "The password for the Keycloak admin user."
  type        = string
  sensitive   = true
}

variable "hub_admin_username" {
  description = "The username for the Katta Server admin user in the realm."
  type        = string
  default     = "admin"
}

variable "hub_admin_password" {
  description = "The initial password for the Katta Server admin user in the realm. It must be changed on first login."
  type        = string
  sensitive   = true
}

variable "github_token" {
  description = "GitHub Personal Access Token for authenticating to GitHub Container Registry (required for both public and private repos)"
  type        = string
  sensitive   = true
}

variable "keycloak_version" {
  description = "Docker image tag for ghcr.io/shift7-ch/keycloak."
  type        = string
  default     = "26.8.1"
}

variable "hub_version" {
  description = "Docker image tag for ghcr.io/shift7-ch/katta-server."
  type        = string
  default     = "latest-amd64"
}

variable "ecs_enable_execute_command" {
  description = "Enable ExecuteCommand to connecting to ecs services for debugging."
  type        = bool
  default     = false
}

variable "hub_keycloak_system_client_secret" {
  description = "Client secret for client 'cryptomatorhub-system'."
  type        = string
  sensitive   = true
}

variable "hub_keycloak_oidc_cryptomator_vaults_client_secret" {
  description = "Client secret for client 'cryptomatorvaults'."
  type        = string
  sensitive   = true
}

variable "hub_csp_additional_connect_src" {
  description = "Additional connect-src sources for the Content-Security-Policy header, required for the S3 and STS endpoints of storage profiles the browser talks to directly. CSP source expressions are used verbatim, so host wildcards are allowed."
  type        = list(string)
  default     = []
}

variable "hub_initial_license" {
  description = "Initial license token for Katta Server (HUB_INITIAL_LICENSE). Defaults to the shared test license. Null to start without a license."
  type        = string
  sensitive   = true
  default     = "eyJ4NWMiOlsiTUlJQ0FUQ0NBYk9nQXdJQkFnSVVSc1ZlYUM5cDI5amhKelB4TXExSk13MVR6UWN3QlFZREsyVndNRDh4Q3pBSkJnTlZCQVlUQWtSRk1SWXdGQVlEVlFRS0RBMVRhM2x0WVhScFl5QkhiV0pJTVJnd0ZnWURWUVFEREE5TWFXTmxibk5sSUZKdmIzUWdRMEV3SGhjTk1qWXdOekE1TURnMU9USTVXaGNOTXpZd056QTJNRGcxT1RJNVdqQk1NUXN3Q1FZRFZRUUdFd0pFUlRFV01CUUdBMVVFQ2d3TlUydDViV0YwYVdNZ1IyMWlTREVsTUNNR0ExVUVBd3djU0hWaUlFeHBZMlZ1YzJVZ1NYTnpkV1Z5SUVOQklDaFVaWE4wS1RDQm16QVFCZ2NxaGtqT1BRSUJCZ1VyZ1FRQUl3T0JoZ0FFQVJOQlc5QjZIZ3EzQzRKd1FvbXNJTEhlU0lLN0c3TXBpR1M5Qm45bFluVDBEYmNBQ3RPUCtGVWFEaWhUQnZERHhaTUxxNDNjbXdIaFp2SmUwY0VVZk5iekFMZ0R6UmtneWtCWEpCNTFkcVlEYUtcL0JWMXMxVnRUY0lSa1wvOWxqdEY2a2txcE1lVWYwUDd2U0hYWGFNMlwvT3R2SmZXN2lKRXpycjN6MHRtWEg3UHpLSFRvMEl3UURBZEJnTlZIUTRFRmdRVW1idlI4KytmZ3B5OGV5SVZ6NGlcL0ljemRMd2t3SHdZRFZSMGpCQmd3Rm9BVWFFcFNIU01LMXJiQmZQR3RTVjBnR29IZ0hQY3dCUVlESzJWd0EwRUFNU3hlS2tpRHBpUkVnNmtFTkw5MUhSdUNsWHNCMWZ1MjlIYW5BYVVTSVd0Ynh3cnE5MnJPbE1BazRZejhtTEh5VzhVNTJtRllKRWZ5WkR5UXlQVDBDdz09Il0sImFsZyI6IkVTNTEyIn0.eyJhdWQiOiJDcnlwdG9tYXRvciBIdWIiLCJzdWIiOiJodWJAZXhhbXBsZS5jb20iLCJyZWZyZXNoVXJsIjoiaHR0cDpcL1wvbG9jYWxob3N0OjgwODFcL1wvaHViXC9yZWZyZXNoIiwia2lkIjoiRV9rU2xTOG5tR0wza0oxeHl6UG5TdG1fdVl1NndRX1h4VFFoN2h6T0k2USIsIm9yZy5jcnlwdG9tYXRvci5odWIuZW50aXRsZW1lbnRzIjp7InNlYXRzIjo1LCJzaG93VHJpYWxIaW50IjpmYWxzZSwiYXVkaXRMb2dSZXRlbnRpb25EYXlzIjowLCJlbWVyZ2VuY3lBY2Nlc3NFbmFibGVkIjpmYWxzZSwia2V5Y2xvYWtBY2Nlc3NFbmFibGVkIjp0cnVlfSwiaXNzIjoiaHR0cDpcL1wvbG9jYWxob3N0OjgwODFcLyIsImV4cCI6MzI1MDM2ODAwMDAsImlhdCI6MTc4MzU4ODY0NCwic2VhdHMiOjUsImp0aSI6IjQyIn0.AZrb7lULe-bvea6LyFsTq7dZZBozgXuqEnr8M5nua-3KyM6gZw_hygBVkmAZw-u4NMlG8u0GEpKlK9rmLUPOpF36AaTa3h9WCQIzmiqn30Rm_zRd9T3WrmWXjkmWKnv7YpfufZI_zT-lpcKGasGm1w3YVWu_nSa9gQd_0Pm1r3QSQWTX"
}

variable "hub_license_chain_required_cn" {
  description = "Common name of the CA the license certificate chain must be issued by (HUB_LICENSE_CHAIN_REQUIRED_CN). Defaults to the test CA matching the shared test license. Null to use the server default \"Hub License Issuer CA (Prod)\"."
  type        = string
  default     = "Hub License Issuer CA (Test)"
}

variable "hub_initial_id" {
  description = "Initial Hub ID matching the license (HUB_INITIAL_ID). Defaults to the ID of the shared test license. Null to start without a license."
  type        = string
  default     = "42"
}

variable "storage_profile_aws_enabled" {
  description = "Setup identity provider and roles in IAM with `katta setup aws` and upload a storage profile for AWS S3 using STS with `katta storageprofile aws sts`. Requires the katta CLI."
  type        = bool
  default     = true
}

variable "storage_profile_aws_role_name_prefix" {
  description = "Prefix of the IAM role names for the AWS storage profile. Defaults to \"<workspace>-\"."
  type        = string
  default     = null
}

variable "storage_profile_aws_bucket_prefix" {
  description = "Prefix of the S3 bucket names for vaults in the AWS storage profile, at most 27 characters. Defaults to \"<project>-<workspace>-\"."
  type        = string
  default     = null
}

variable "storage_profile_aws_regions" {
  description = "Bucket regions for the AWS storage profile in addition to the region to create resources, which is the default. Defaults to the regions enabled by default in AWS accounts (not opt-in)."
  type        = list(string)
  default = [
    "us-east-1",
    "us-east-2",
    "us-west-1",
    "us-west-2",
    "ca-central-1",
    "sa-east-1",
    "eu-central-1",
    "eu-west-1",
    "eu-west-2",
    "eu-west-3",
    "eu-north-1",
    "ap-south-1",
    "ap-northeast-1",
    "ap-northeast-2",
    "ap-northeast-3",
    "ap-southeast-1",
    "ap-southeast-2",
  ]
}

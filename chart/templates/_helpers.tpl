{{- define "katta-server.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{- define "katta-server.fullname" -}}
{{- if .Values.fullnameOverride -}}
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" -}}
{{- else -}}
{{- .Release.Name | trunc 63 | trimSuffix "-" -}}
{{- end -}}
{{- end -}}

{{- define "katta-server.labels" -}}
app.kubernetes.io/name: {{ include "katta-server.name" . }}
helm.sh/chart: {{ printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" }}
app.kubernetes.io/instance: {{ .Release.Name }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end -}}

{{- define "katta-server.selectorLabels" -}}
app.kubernetes.io/name: {{ include "katta-server.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end -}}

{{- define "katta-server.hubRelativePath" -}}
{{- $path := regexReplaceAll "^https?://[^/]+" (required "urls.hub.public must be set" .Values.urls.hub.public) "" -}}
{{- $trimmed := trimAll "/" $path -}}
{{- printf "/%s" $trimmed -}}
{{- end -}}

{{- define "katta-server.keycloakRelativePath" -}}
{{- $path := regexReplaceAll "^https?://[^/]+" (required "urls.kc.public must be set" .Values.urls.kc.public) "" -}}
{{- $trimmed := trimAll "/" $path -}}
{{- printf "/%s" $trimmed -}}
{{- end -}}

{{- /* / katta start addition */ -}}
{{- define "katta-server.s3RelativePath" -}}
{{- $path := regexReplaceAll "^https?://[^/]+" .Values.urls.s3.public "" -}}
{{- $trimmed := trimAll "/" $path -}}
{{- printf "/%s" $trimmed -}}
{{- end -}}

{{- define "katta-server.minioRelativePath" -}}
{{- $path := regexReplaceAll "^https?://[^/]+" .Values.urls.minio.public "" -}}
{{- $trimmed := trimAll "/" $path -}}
{{- printf "/%s" $trimmed -}}
{{- end -}}
{{- /* \ katta end addition */ -}}

{{- define "katta-server.keycloakLocalUrl" -}}
{{- if .Values.urls.kc.clusterInternal -}}
{{- trimSuffix "/" .Values.urls.kc.clusterInternal -}}
{{- else if .Values.keycloak.enabled -}}
{{/* Bundled Keycloak serves at "/" (the external subpath is stripped by the ingress), so the in-cluster URL has no path. */}}
{{- printf "http://%s:%v" (print (include "katta-server.fullname" .) "-service-kc") .Values.keycloak.service.httpPort -}}
{{- else -}}
{{/* if keycloak isn't part of the deployment, use public url: */}}
{{- trimSuffix "/" (required "urls.kc.public must be set" .Values.urls.kc.public) -}}
{{- end -}}
{{- end -}}

{{- define "katta-server.hubJdbcUrl" -}}
{{- if .Values.hub.database.jdbcUrl -}}
{{- .Values.hub.database.jdbcUrl -}}
{{- else if .Values.postgres.enabled -}}
{{- printf "jdbc:postgresql://%s:%v/%s" (print (include "katta-server.fullname" .) "-service-pg") .Values.postgres.service.port .Values.hub.database.name -}}
{{- else -}}
{{- required "hub.database.jdbcUrl must be set when postgres.enabled=false" .Values.hub.database.jdbcUrl -}}
{{- end -}}
{{- end -}}

{{- define "katta-server.oidcAuthServerUrl" -}}
{{- if .Values.urls.kc.authServerUrl -}}
{{- .Values.urls.kc.authServerUrl -}}
{{- else -}}
{{- printf "%s/realms/%s" (trimSuffix "/" (include "katta-server.keycloakLocalUrl" .)) .Values.hub.config.keycloakRealm -}}
{{- end -}}
{{- end -}}

{{- define "katta-server.oidcTokenIssuer" -}}
{{- if .Values.urls.kc.tokenIssuer -}}
{{- .Values.urls.kc.tokenIssuer -}}
{{- else -}}
{{- printf "%s/realms/%s" (trimSuffix "/" .Values.urls.kc.public) .Values.hub.config.keycloakRealm -}}
{{- end -}}
{{- end -}}

{{/*

Auto-generated secrets below:

1. try to use cached value (required so we don't generate a new random value on every template rendering)
2. try to use value from values.yaml (if user has set it, e.g. `hub.secrets.systemClientSecret`)
3. try to look up existing secret in cluster and use it if it exists (this allows users to upgrade from older versions of the chart without losing their secrets)
4. if all else fails, generate a new random value

*/}}

{{- define "katta-server.resolvedSystemClientSecret" -}}
{{- if hasKey .Values "_resolvedSystemClientSecret" -}}
{{- index .Values "_resolvedSystemClientSecret" -}}
{{- else if .Values.hub.secrets.systemClientSecret -}}
{{- $_ := set .Values "_resolvedSystemClientSecret" .Values.hub.secrets.systemClientSecret -}}
{{- index .Values "_resolvedSystemClientSecret" -}}
{{- else -}}
{{- $secretName := print (include "katta-server.fullname" .) "-secrets-hub" -}}
{{- $existing := lookup "v1" "Secret" .Release.Namespace $secretName -}}
{{- if and $existing (hasKey $existing.data "hub_system_client_secret") -}}
{{- $_ := set .Values "_resolvedSystemClientSecret" (index $existing.data "hub_system_client_secret" | b64dec) -}}
{{- else -}}
{{- $_ := set .Values "_resolvedSystemClientSecret" (randAlphaNum 32) -}}
{{- end -}}
{{- index .Values "_resolvedSystemClientSecret" -}}
{{- end -}}
{{- end -}}

{{- define "katta-server.resolvedHubDbPassword" -}}
{{- if hasKey .Values "_resolvedHubDbPassword" -}}
{{- index .Values "_resolvedHubDbPassword" -}}
{{- else if .Values.hub.database.password -}}
{{- $_ := set .Values "_resolvedHubDbPassword" .Values.hub.database.password -}}
{{- index .Values "_resolvedHubDbPassword" -}}
{{- else -}}
{{- $secretName := print (include "katta-server.fullname" .) "-secrets-hub" -}}
{{- $existing := lookup "v1" "Secret" .Release.Namespace $secretName -}}
{{- if and $existing (hasKey $existing.data "hub_db_password") -}}
{{- $_ := set .Values "_resolvedHubDbPassword" (index $existing.data "hub_db_password" | b64dec) -}}
{{- else -}}
{{- $_ := set .Values "_resolvedHubDbPassword" (randAlphaNum 32) -}}
{{- end -}}
{{- index .Values "_resolvedHubDbPassword" -}}
{{- end -}}
{{- end -}}

{{- define "katta-server.resolvedHubAdminPassword" -}}
{{- if hasKey .Values "_resolvedHubAdminPassword" -}}
{{- index .Values "_resolvedHubAdminPassword" -}}
{{- else if .Values.hub.admin.password -}}
{{- $_ := set .Values "_resolvedHubAdminPassword" .Values.hub.admin.password -}}
{{- index .Values "_resolvedHubAdminPassword" -}}
{{- else -}}
{{- $secretName := print (include "katta-server.fullname" .) "-secrets-hub" -}}
{{- $existing := lookup "v1" "Secret" .Release.Namespace $secretName -}}
{{- if and $existing (hasKey $existing.data "hub_admin_password") -}}
{{- $_ := set .Values "_resolvedHubAdminPassword" (index $existing.data "hub_admin_password" | b64dec) -}}
{{- else -}}
{{- $_ := set .Values "_resolvedHubAdminPassword" (randAlphaNum 32) -}}
{{- end -}}
{{- index .Values "_resolvedHubAdminPassword" -}}
{{- end -}}
{{- end -}}

{{- define "katta-server.resolvedKeycloakDbPassword" -}}
{{- if hasKey .Values "_resolvedKeycloakDbPassword" -}}
{{- index .Values "_resolvedKeycloakDbPassword" -}}
{{- else if .Values.keycloak.database.password -}}
{{- $_ := set .Values "_resolvedKeycloakDbPassword" .Values.keycloak.database.password -}}
{{- index .Values "_resolvedKeycloakDbPassword" -}}
{{- else -}}
{{- $secretName := print (include "katta-server.fullname" .) "-secrets-kc" -}}
{{- $existing := lookup "v1" "Secret" .Release.Namespace $secretName -}}
{{- if and $existing (hasKey $existing.data "kc_db_password") -}}
{{- $_ := set .Values "_resolvedKeycloakDbPassword" (index $existing.data "kc_db_password" | b64dec) -}}
{{- else -}}
{{- $_ := set .Values "_resolvedKeycloakDbPassword" (randAlphaNum 32) -}}
{{- end -}}
{{- index .Values "_resolvedKeycloakDbPassword" -}}
{{- end -}}
{{- end -}}

{{- define "katta-server.resolvedKeycloakAdminPassword" -}}
{{- if hasKey .Values "_resolvedKeycloakAdminPassword" -}}
{{- index .Values "_resolvedKeycloakAdminPassword" -}}
{{- else if .Values.keycloak.admin.password -}}
{{- $_ := set .Values "_resolvedKeycloakAdminPassword" .Values.keycloak.admin.password -}}
{{- index .Values "_resolvedKeycloakAdminPassword" -}}
{{- else -}}
{{- $secretName := print (include "katta-server.fullname" .) "-secrets-kc" -}}
{{- $existing := lookup "v1" "Secret" .Release.Namespace $secretName -}}
{{- if and $existing (hasKey $existing.data "kc_admin_password") -}}
{{- $_ := set .Values "_resolvedKeycloakAdminPassword" (index $existing.data "kc_admin_password" | b64dec) -}}
{{- else -}}
{{- $_ := set .Values "_resolvedKeycloakAdminPassword" (randAlphaNum 32) -}}
{{- end -}}
{{- index .Values "_resolvedKeycloakAdminPassword" -}}
{{- end -}}
{{- end -}}

{{- define "katta-server.resolvedPostgresAdminPassword" -}}
{{- if hasKey .Values "_resolvedPostgresAdminPassword" -}}
{{- index .Values "_resolvedPostgresAdminPassword" -}}
{{- else if .Values.postgres.auth.adminPassword -}}
{{- $_ := set .Values "_resolvedPostgresAdminPassword" .Values.postgres.auth.adminPassword -}}
{{- index .Values "_resolvedPostgresAdminPassword" -}}
{{- else -}}
{{- $secretName := print (include "katta-server.fullname" .) "-secrets-pg" -}}
{{- $existing := lookup "v1" "Secret" .Release.Namespace $secretName -}}
{{- if and $existing (hasKey $existing.data "pg_admin_password") -}}
{{- $_ := set .Values "_resolvedPostgresAdminPassword" (index $existing.data "pg_admin_password" | b64dec) -}}
{{- else -}}
{{- $_ := set .Values "_resolvedPostgresAdminPassword" (randAlphaNum 32) -}}
{{- end -}}
{{- index .Values "_resolvedPostgresAdminPassword" -}}
{{- end -}}
{{- end -}}

{{- /* / katta start addition */ -}}
{{- define "katta-server.resolvedCryptomatorvaultsClientSecret" -}}
{{- if hasKey .Values "_resolvedCryptomatorvaultsClientSecret" -}}
{{- index .Values "_resolvedCryptomatorvaultsClientSecret" -}}
{{- else if .Values.hub.secrets.cryptomatorvaultsClientSecret -}}
{{- $_ := set .Values "_resolvedCryptomatorvaultsClientSecret" .Values.hub.secrets.cryptomatorvaultsClientSecret -}}
{{- index .Values "_resolvedCryptomatorvaultsClientSecret" -}}
{{- else -}}
{{- $secretName := print (include "katta-server.fullname" .) "-secrets-hub" -}}
{{- $existing := lookup "v1" "Secret" .Release.Namespace $secretName -}}
{{- if and $existing (hasKey $existing.data "hub_cryptomatorvaults_client_secret") -}}
{{- $_ := set .Values "_resolvedCryptomatorvaultsClientSecret" (index $existing.data "hub_cryptomatorvaults_client_secret" | b64dec) -}}
{{- else -}}
{{- $_ := set .Values "_resolvedCryptomatorvaultsClientSecret" (randAlphaNum 32) -}}
{{- end -}}
{{- index .Values "_resolvedCryptomatorvaultsClientSecret" -}}
{{- end -}}
{{- end -}}

{{- define "katta-server.resolvedMinioRootPassword" -}}
{{- if hasKey .Values "_resolvedMinioRootPassword" -}}
{{- index .Values "_resolvedMinioRootPassword" -}}
{{- else if .Values.minio.auth.rootPassword -}}
{{- $_ := set .Values "_resolvedMinioRootPassword" .Values.minio.auth.rootPassword -}}
{{- index .Values "_resolvedMinioRootPassword" -}}
{{- else -}}
{{- $secretName := print (include "katta-server.fullname" .) "-secrets-minio" -}}
{{- $existing := lookup "v1" "Secret" .Release.Namespace $secretName -}}
{{- if and $existing (hasKey $existing.data "minio_root_password") -}}
{{- $_ := set .Values "_resolvedMinioRootPassword" (index $existing.data "minio_root_password" | b64dec) -}}
{{- else -}}
{{- $_ := set .Values "_resolvedMinioRootPassword" (randAlphaNum 32) -}}
{{- end -}}
{{- index .Values "_resolvedMinioRootPassword" -}}
{{- end -}}
{{- end -}}

{{- define "katta-server.resolvedMinioProfileId.S3STATIC" -}}
{{- if hasKey .Values "_resolvedMinioProfileId.S3STATIC" -}}
{{- index .Values "_resolvedMinioProfileId.S3STATIC" -}}
{{- else if .Values.storageProfileSeed.static.profileId -}}
{{- $_ := set .Values "_resolvedMinioProfileId.S3STATIC" .Values.storageProfileSeed.static.profileId -}}
{{- index .Values "_resolvedMinioProfileId.S3STATIC" -}}
{{- else -}}
{{- $cmName := print (include "katta-server.fullname" .) "-storageprofile-seed-state" -}}
{{- $existing := lookup "v1" "ConfigMap" .Release.Namespace $cmName -}}
{{- if and $existing (hasKey $existing.data "profileId") -}}
{{- $_ := set .Values "_resolvedMinioProfileId.S3STATIC" (index $existing.data "profileId") -}}
{{- else -}}
{{- $_ := set .Values "_resolvedMinioProfileId.S3STATIC" (uuidv4) -}}
{{- end -}}
{{- index .Values "_resolvedMinioProfileId.S3STATIC" -}}
{{- end -}}
{{- end -}}

{{- define "katta-server.resolvedMinioProfileId.S3STS" -}}
{{- if hasKey .Values "_resolvedMinioProfileId.S3STS" -}}
{{- index .Values "_resolvedMinioProfileId.S3STS" -}}
{{- else if .Values.storageProfileSeed.sts.profileId -}}
{{- $_ := set .Values "_resolvedMinioProfileId.S3STS" .Values.storageProfileSeed.sts.profileId -}}
{{- index .Values "_resolvedMinioProfileId.S3STS" -}}
{{- else -}}
{{- $cmName := print (include "katta-server.fullname" .) "-storageprofile-seed-state" -}}
{{- $existing := lookup "v1" "ConfigMap" .Release.Namespace $cmName -}}
{{- if and $existing (hasKey $existing.data "stsProfileId") -}}
{{- $_ := set .Values "_resolvedMinioProfileId.S3STS" (index $existing.data "stsProfileId") -}}
{{- else -}}
{{- $_ := set .Values "_resolvedMinioProfileId.S3STS" (uuidv4) -}}
{{- end -}}
{{- index .Values "_resolvedMinioProfileId.S3STS" -}}
{{- end -}}
{{- end -}}

{{/*

OIDC discovery URL handed to MinIO. Defaults to the in-cluster Keycloak Service URL —
with Keycloak's KC_HOSTNAME_BACKCHANNEL_DYNAMIC=true, this produces a discovery doc
whose `issuer` field is the public KC_HOSTNAME (matching real-token `iss` claims) but
whose `jwks_uri` is the in-cluster URL (so MinIO can fetch JWKS without going through
the public ingress hop). Override via `minio.openid.configUrl` when needed.

*/}}
{{- define "katta-server.minioOidcConfigUrl" -}}
{{- if .Values.minio.openid.configUrl -}}
{{- .Values.minio.openid.configUrl -}}
{{- else -}}
{{- $kcLocal := include "katta-server.keycloakLocalUrl" . | trimSuffix "/" -}}
{{- printf "%s/realms/%s/.well-known/openid-configuration" $kcLocal .Values.hub.config.keycloakRealm -}}
{{- end -}}
{{- end -}}
{{- /* \ katta end addition */ -}}

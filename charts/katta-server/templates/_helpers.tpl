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

{{/*

Resolve the ingress controller's pod IPs by walking its EndpointSlices. Used to populate
the chart's port-translation proxy EndpointSlice — pointing at pod IPs (single DNAT)
rather than the controller's ClusterIP (broken double-DNAT in iptables kube-proxy).

EndpointSlices have generated names, so we list all slices in the controller's
namespace and filter by the `kubernetes.io/service-name` label. Returns a JSON-encoded
list of IPs (deduped, only `ready` endpoints), or empty if the lookup fails (e.g.
`helm template` without a live cluster, or the controller isn't installed yet).

*/}}
{{- define "katta-server.ingressControllerPodIPs" -}}
{{- $svcName := .Values.ingress.controllerService.name -}}
{{- $namespace := .Values.ingress.controllerService.namespace -}}
{{- $slices := lookup "discovery.k8s.io/v1" "EndpointSlice" $namespace "" -}}
{{- $ips := list -}}
{{- if $slices -}}
  {{- range $slice := $slices.items -}}
    {{- $labels := $slice.metadata.labels -}}
    {{- if and $labels (eq (index $labels "kubernetes.io/service-name") $svcName) -}}
      {{- range $ep := $slice.endpoints -}}
        {{- /* `conditions.ready` is *bool: nil/missing means "unknown, assume ready"; only skip if explicitly false. */ -}}
        {{- $skip := false -}}
        {{- if $ep.conditions -}}
          {{- if eq (index $ep.conditions "ready") false -}}
            {{- $skip = true -}}
          {{- end -}}
        {{- end -}}
        {{- if not $skip -}}
          {{- range $addr := $ep.addresses -}}
            {{- $ips = append $ips $addr -}}
          {{- end -}}
        {{- end -}}
      {{- end -}}
    {{- end -}}
  {{- end -}}
{{- end -}}
{{- $ips | uniq | toJson -}}
{{- end -}}

{{/*

Collect the set of hostnames from .Values.urls.*.public whose host portion is a
*.localhost subdomain (excluding bare "localhost"). These are the names we want to
add to pod hostAliases so internal calls route through the ingress.

*/}}
{{- define "katta-server.localhostHosts" -}}
{{- $hostSet := dict -}}
{{- range $name, $cfg := .Values.urls -}}
  {{- if (and (kindIs "map" $cfg) (hasKey $cfg "public")) -}}
    {{- $public := index $cfg "public" -}}
    {{- if $public -}}
      {{- $host := regexReplaceAll "^https?://([^:/]+).*" $public "${1}" -}}
      {{- if and $host (ne $host "localhost") (hasSuffix ".localhost" $host) -}}
        {{- $_ := set $hostSet $host true -}}
      {{- end -}}
    {{- end -}}
  {{- end -}}
{{- end -}}
{{- keys $hostSet | sortAlpha | toJson -}}
{{- end -}}

{{/*

Extract the port from the first *.localhost URL in .Values.urls.*.public. All such URLs
are expected to share the same port (the chart's ingress proxy listens on exactly one
port). Returns the empty string if there is no port (implicit 80) or no *.localhost URLs.

*/}}
{{- define "katta-server.localhostPort" -}}
{{- $port := "" -}}
{{- range $name, $cfg := .Values.urls -}}
  {{- if (and (kindIs "map" $cfg) (hasKey $cfg "public") (eq $port "")) -}}
    {{- $public := index $cfg "public" -}}
    {{- if $public -}}
      {{- $host := regexReplaceAll "^https?://([^:/]+).*" $public "${1}" -}}
      {{- if and $host (ne $host "localhost") (hasSuffix ".localhost" $host) -}}
        {{- $portMatch := regexReplaceAll "^https?://[^:/]+(?::(\\d+))?.*" $public "${1}" -}}
        {{- if $portMatch -}}{{- $port = $portMatch -}}{{- end -}}
      {{- end -}}
    {{- end -}}
  {{- end -}}
{{- end -}}
{{- $port -}}
{{- end -}}

{{/*

Resolve the ClusterIP of the chart-managed port-translation proxy Service. Prefers
ingress.proxy.clusterIP from values (deterministic on first install); falls back to
`lookup` on the proxy Service if it already exists. Returns empty otherwise.

*/}}
{{- define "katta-server.proxyServiceIP" -}}
{{- if .Values.ingress.proxy.clusterIP -}}
{{- .Values.ingress.proxy.clusterIP -}}
{{- else -}}
{{- $name := printf "%s-ingress-proxy" (include "katta-server.fullname" .) -}}
{{- $svc := lookup "v1" "Service" .Release.Namespace $name -}}
{{- if and $svc $svc.spec $svc.spec.clusterIP -}}
{{- $svc.spec.clusterIP -}}
{{- end -}}
{{- end -}}
{{- end -}}

{{/*

True when the chart should create the port-translation proxy. We create it when there
are *.localhost URLs with a non-default port AND we can resolve the in-cluster ingress
controller IP to populate the EndpointSlice.

*/}}
{{- define "katta-server.proxyEnabled" -}}
{{- $hosts := include "katta-server.localhostHosts" . | fromJsonArray -}}
{{- $port := include "katta-server.localhostPort" . -}}
{{- $podIPs := include "katta-server.ingressControllerPodIPs" . | fromJsonArray -}}
{{- if and $hosts $port $podIPs -}}true{{- end -}}
{{- end -}}

{{/*

Emit a `hostAliases:` YAML block (suitable for pod spec) that points every collected
*.localhost host at the chart's port-translation proxy Service. Emits nothing if there
are no matching hosts or the proxy isn't enabled (no non-default port, or the ingress
controller's Endpoints couldn't be discovered).

Usage:
  spec:
    {{- include "katta-server.hostAliases" . | nindent N }}

*/}}
{{- define "katta-server.hostAliases" -}}
{{- if eq (include "katta-server.proxyEnabled" .) "true" -}}
{{- $hosts := include "katta-server.localhostHosts" . | fromJsonArray -}}
{{- $ip := include "katta-server.proxyServiceIP" . -}}
{{- if and $ip $hosts -}}
hostAliases:
  - ip: {{ $ip | quote }}
    hostnames:
{{- range $h := $hosts }}
      - {{ $h | quote }}
{{- end }}
{{- end -}}
{{- end -}}
{{- end -}}

{{/*

Compose the Hub's Content-Security-Policy header. If hub.config.contentSecurityPolicy
is set explicitly, return it verbatim. Otherwise build a policy whose connect-src
includes every configured urls.*.public origin (scheme://host[:port]), so the SPA can
reach the chart's Keycloak, S3, MinIO console etc. without browser CSP violations.

*/}}
{{- define "katta-server.contentSecurityPolicy" -}}
{{- if .Values.hub.config.contentSecurityPolicy -}}
{{- .Values.hub.config.contentSecurityPolicy -}}
{{- else -}}
{{- $origins := list "'self'" "api.cryptomator.org" -}}
{{- range $name, $cfg := .Values.urls -}}
  {{- if (and (kindIs "map" $cfg) (hasKey $cfg "public")) -}}
    {{- $public := index $cfg "public" -}}
    {{- if $public -}}
      {{- $origin := regexReplaceAll "^(https?://[^/]+).*" $public "${1}" -}}
      {{- if and $origin (not (has $origin $origins)) -}}
        {{- $origins = append $origins $origin -}}
      {{- end -}}
    {{- end -}}
  {{- end -}}
{{- end -}}
{{- printf "default-src 'self'; connect-src %s; object-src 'none'; child-src 'self'; img-src * data:; frame-ancestors 'none'" (join " " $origins) -}}
{{- end -}}
{{- end -}}

{{- define "katta-server.keycloakLocalUrl" -}}
{{- if .Values.urls.kc.clusterInternal -}}
{{- trimSuffix "/" .Values.urls.kc.clusterInternal -}}
{{- else if .Values.keycloak.enabled -}}
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

{{- define "katta-server.resolvedHubMetricsPassword" -}}
{{- if hasKey .Values "_resolvedHubMetricsPassword" -}}
{{- index .Values "_resolvedHubMetricsPassword" -}}
{{- else if .Values.hub.metrics.password -}}
{{- $_ := set .Values "_resolvedHubMetricsPassword" .Values.hub.metrics.password -}}
{{- index .Values "_resolvedHubMetricsPassword" -}}
{{- else -}}
{{- $secretName := print (include "katta-server.fullname" .) "-secrets-hub-metrics" -}}
{{- $existing := lookup "v1" "Secret" .Release.Namespace $secretName -}}
{{- if and $existing (hasKey $existing.data "password") -}}
{{- $_ := set .Values "_resolvedHubMetricsPassword" (index $existing.data "password" | b64dec) -}}
{{- else -}}
{{- $_ := set .Values "_resolvedHubMetricsPassword" (randAlphaNum 32) -}}
{{- end -}}
{{- index .Values "_resolvedHubMetricsPassword" -}}
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

{{/*

Resolve a stable UUID for the bundled-MinIO storage profile. The Job is idempotent
(the backend returns 409 on duplicate id), but we still want every upgrade to target
the SAME row rather than spamming new rows that all fail. Lookup order:

1. cached in-render value
2. explicit pin via .Values.storageProfileSeed.profileId
3. existing ConfigMap `<release>-storageprofile-seed-state` (data.profileId)
4. generate a fresh UUID

*/}}
{{- define "katta-server.resolvedMinioProfileId" -}}
{{- if hasKey .Values "_resolvedMinioProfileId" -}}
{{- index .Values "_resolvedMinioProfileId" -}}
{{- else if .Values.storageProfileSeed.static.profileId -}}
{{- $_ := set .Values "_resolvedMinioProfileId" .Values.storageProfileSeed.static.profileId -}}
{{- index .Values "_resolvedMinioProfileId" -}}
{{- else -}}
{{- $cmName := print (include "katta-server.fullname" .) "-storageprofile-seed-state" -}}
{{- $existing := lookup "v1" "ConfigMap" .Release.Namespace $cmName -}}
{{- if and $existing (hasKey $existing.data "profileId") -}}
{{- $_ := set .Values "_resolvedMinioProfileId" (index $existing.data "profileId") -}}
{{- else -}}
{{- $_ := set .Values "_resolvedMinioProfileId" (uuidv4) -}}
{{- end -}}
{{- index .Values "_resolvedMinioProfileId" -}}
{{- end -}}
{{- end -}}

{{/* Analogous to resolvedMinioProfileId but for the STS profile. */}}
{{- define "katta-server.resolvedMinioStsProfileId" -}}
{{- if hasKey .Values "_resolvedMinioStsProfileId" -}}
{{- index .Values "_resolvedMinioStsProfileId" -}}
{{- else if .Values.storageProfileSeed.sts.profileId -}}
{{- $_ := set .Values "_resolvedMinioStsProfileId" .Values.storageProfileSeed.sts.profileId -}}
{{- index .Values "_resolvedMinioStsProfileId" -}}
{{- else -}}
{{- $cmName := print (include "katta-server.fullname" .) "-storageprofile-seed-state" -}}
{{- $existing := lookup "v1" "ConfigMap" .Release.Namespace $cmName -}}
{{- if and $existing (hasKey $existing.data "stsProfileId") -}}
{{- $_ := set .Values "_resolvedMinioStsProfileId" (index $existing.data "stsProfileId") -}}
{{- else -}}
{{- $_ := set .Values "_resolvedMinioStsProfileId" (uuidv4) -}}
{{- end -}}
{{- index .Values "_resolvedMinioStsProfileId" -}}
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

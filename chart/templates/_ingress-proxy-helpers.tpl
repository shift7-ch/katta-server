{{/*

Helpers for the chart's port-translation proxy — the mechanism that makes *.localhost:<port>
URLs reachable from inside the cluster (active only when any urls.*.public is a *.localhost
host on a non-default port, i.e. the local-demo mode). Together with templates/ingress-proxy.yaml
this forms a self-contained subsystem: ingressControllerPodIPs / localhostHosts / localhostPort /
proxyServiceIP / proxyEnabled are internal plumbing; hostAliases is what the consumer pods
(Hub / MinIO / seed Job) actually include in their podSpec.

These defines are split into their own file so the cluster is easy to find, audit, and eventually
peel out as a separate subchart. They're still resolved globally by Helm, so callers in other
templates work unchanged.

*/}}

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

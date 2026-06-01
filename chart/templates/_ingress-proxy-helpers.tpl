{{/*

Helpers for the chart's port-translation proxy — the mechanism that makes a public URL
served on a non-default port (e.g. http://kc.local.katta.cloud:9090) reachable from
INSIDE the cluster. Together with templates/ingress-proxy.yaml this forms a self-contained
subsystem: ingressControllerPodIPs / proxyPort / proxyServiceIP are internal plumbing;
proxyEnabled is the single gate used by ingress-proxy.yaml itself.

DNS resolution for the public hostnames is handled separately by templates/coredns-patch.yaml
(CoreDNS rewrite plugin), not by per-pod hostAliases. This file used to emit hostAliases
back when the demo ran on *.localhost names (RFC 6761 forces /etc/hosts plumbing); those
helpers are gone now that the demo uses real DNS names.

These defines live in their own file so the cluster is easy to find, audit, and eventually
peel out as a separate subchart. They're still resolved globally by Helm, so callers in
other templates work unchanged.

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

Extract the first non-default port from .Values.urls.*.public. The proxy listens on
exactly one port (the same port every public URL is expected to use), so we just need
to find it once. Returns the empty string if every URL uses the implicit/default port
(80 for http, 443 for https) — in which case the proxy is unnecessary and proxyEnabled
returns false.

*/}}
{{- define "katta-server.proxyPort" -}}
{{- $port := "" -}}
{{- range $name, $cfg := .Values.urls -}}
  {{- if (and (kindIs "map" $cfg) (hasKey $cfg "public") (eq $port "")) -}}
    {{- $public := index $cfg "public" -}}
    {{- if $public -}}
      {{- $scheme := regexReplaceAll "^(https?)://.*" $public "${1}" -}}
      {{- $portMatch := regexReplaceAll "^https?://[^:/]+(?::(\\d+))?.*" $public "${1}" -}}
      {{- if $portMatch -}}
        {{- $defaultPort := ternary "443" "80" (eq $scheme "https") -}}
        {{- if ne $portMatch $defaultPort -}}
          {{- $port = $portMatch -}}
        {{- end -}}
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
{{- $name := printf "%s-ingress-proxy" (include "cryptomator-hub.fullname" .) -}}
{{- $svc := lookup "v1" "Service" .Release.Namespace $name -}}
{{- if and $svc $svc.spec $svc.spec.clusterIP -}}
{{- $svc.spec.clusterIP -}}
{{- end -}}
{{- end -}}
{{- end -}}

{{/*

True when the chart should create the port-translation proxy. We create it whenever
any urls.*.public uses a non-default port (so the public URL doesn't naturally work
from inside the cluster) AND we can resolve the in-cluster ingress controller's pod
IPs to populate the proxy's EndpointSlice.

*/}}
{{- define "katta-server.proxyEnabled" -}}
{{- $port := include "katta-server.proxyPort" . -}}
{{- $podIPs := include "katta-server.ingressControllerPodIPs" . | fromJsonArray -}}
{{- if and $port $podIPs -}}true{{- end -}}
{{- end -}}

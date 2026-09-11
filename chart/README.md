# Katta Server Helm Chart

Helm chart for [Katta Server](https://github.com/shift7-ch/katta-server), a downstream fork of Cryptomator Hub.

This chart deploys:

- Katta Server (the "Hub" backend, required)
- Keycloak (optional, enabled by default) — uses the Katta-customized image with the token-exchange SPI
- PostgreSQL (optional, enabled by default)
- MinIO (optional, disabled by default — see [Bundled MinIO](#bundled-minio-for-evaluation))

Image repositories are fixed in templates; the core-component tags are overridable per workload:
- Hub: `ghcr.io/shift7-ch/katta-server:<hub.image.tag>` (defaults to chart `appVersion`)
- Keycloak: `ghcr.io/shift7-ch/keycloak:<keycloak.image.tag>` (default `26.6.2`)
- PostgreSQL: `postgres:<postgres.image.tag>` (default `17-alpine`)
- MinIO (StatefulSet + seed-Job setup container, shares one tag): `quay.io/minio/minio:<minio.image.tag>` (default `RELEASE.2025-09-07T16-13-09Z`)
- Storage-profile seed Job: `alpine/curl:8.17.0` (hardcoded)
- Init waits (DB/OIDC readiness): `busybox:1.36`, `alpine/curl:8.17.0` (hardcoded)

TLS termination is currently expected to be done by the ingress controller.
Supported ingress controller templates:
- `ingress.controller=nginx`
- `ingress.controller=traefik`

## Quick Start (Local Demo with Bundled MinIO)

The fastest way to spin up a complete Katta stack — Hub + Keycloak + Postgres + MinIO + a pre-seeded storage profile — is via `values-demo.yaml` against any local single-user cluster with the nginx-ingress addon (tested on minikube + Podman).

**Prerequisites:**

- **Single-user local cluster** (kind, minikube, k3d, Docker Desktop). The installer needs cluster-admin: the chart provisions a `Role` + `RoleBinding` in `kube-system` so a post-install hook Job can patch the CoreDNS ConfigMap. Don't use these values on a shared or managed cluster.
- **nginx-ingress addon enabled** (`minikube addons enable ingress` etc.).
- Hostname split: Hub UI lives on `hub.localhost` (a browser "secure context" so WebCrypto works without HTTPS); Keycloak, MinIO console, and S3 live under the katta-controlled wildcard A-record `*.local.katta.cloud → 127.0.0.1` (resolvable from inside the cluster via the CoreDNS patch below, which `.localhost` is not — and the desktop client doesn't accept `*.localhost` URLs anyway). Both halves resolve to `127.0.0.1` browser-side without any `/etc/hosts` edits.

```bash
# one-off (skip if your cluster already has nginx-ingress)
minikube addons enable ingress

# deploy
helm install katta chart \
  --namespace katta \
  --create-namespace \
  -f chart/values-demo.yaml
```

Expose the ingress controller on `localhost:9090`:

```bash
kubectl port-forward -n ingress-nginx svc/ingress-nginx-controller 9090:80
```

Once both commands are running:

| URL | Credentials |
|---|---|
| Hub UI: <http://hub.localhost:9090> | `admin` / `admin` |
| Keycloak admin: <http://kc.local.katta.cloud:9090> | `admin` / `admin` |
| MinIO console: <http://minio.local.katta.cloud:9090> | `minioadmin` / `minioadmin` |
| MinIO S3 API: <http://s3.local.katta.cloud:9090> | (used by the seeded storage profile) |

A post-install Helm hook Job (`<release>-storageprofile-seed`) registers an `S3STATIC` storage profile named "Bundled MinIO" pointing at `http://s3.local.katta.cloud:9090`, so vault creation works end-to-end immediately after install. Re-runs are idempotent (the seed Job skips profiles whose name already exists).

**How in-cluster DNS works in demo mode:** the same hostnames the browser uses need to resolve inside the cluster too (MinIO has to fetch Keycloak's OIDC discovery URL, and the issuer it sees must match the browser-facing one). The chart's demo profile sets `coredns.patch.enabled=true`, which runs a post-install hook Job that adds a release-scoped `# BEGIN katta:<release>` / `# END katta:<release>` stanza to `kube-system/coredns`'s Corefile, rewriting `*.local.katta.cloud` queries to the chart's port-translation proxy Service. A matching `pre-delete` Job removes the stanza on `helm uninstall`. CoreDNS's `reload` plugin picks up the change within ~30 s; the apply Job sleeps 45 s as a settling buffer before the storage-profile seed Job runs.

## Quick Start (Production-shaped, no MinIO, real DNS)

Assumes a real domain with public DNS and a **pre-existing Traefik ingress controller** in the cluster — this chart only registers `Ingress` and `Middleware` resources against it; it does not install Traefik. Confirm the `IngressClass` you want to use (`kubectl get ingressclass`) and substitute its name below if it isn't `traefik`.

```bash
helm install katta chart \
  --namespace katta \
  --create-namespace \
  --wait --timeout 5m \
  --set urls.hub.public=https://hub.example.com \
  --set urls.kc.public=https://kc.example.com \
  --set ingress.controller=traefik \
  --set ingress.className=traefik \
  --set hub.admin.password=changeme
```

Real public DNS handles in-cluster resolution naturally (the chart's port-translation proxy stays disabled when URLs use the default ports 80/443, and `coredns.patch.enabled` defaults to off), so neither the CoreDNS patch nor the port-translation proxy is created for production deployments.

Passwords are optional by default. If unset, the chart generates random values and
prints commands in `helm` notes to retrieve them from Kubernetes Secrets.

### TLS

`ingress.tls.*` is opt-in. The chart emits Ingress resources without a `spec.tls:` block by default, which is the right choice if Traefik is already configured with a default certificate or wildcard. Three common variants:

| Scenario | Add to `helm install` |
|---|---|
| Traefik default cert / wildcard at the controller | — (no extra flags) |
| cert-manager provisions per-host certs | `--set ingress.tls.enabled=true --set ingress.tls.secretName=katta-tls` plus a matching `Certificate` referencing your `ClusterIssuer` |
| Bring-your-own Secret (pre-created in the release namespace) | `--set ingress.tls.enabled=true --set ingress.tls.secretName=<your-secret>` |

When `ingress.tls.enabled=true`, each Ingress gets a `tls:` block binding the host to the named Secret; that's what per-host certificate selection and cert-manager pickup hook into.

The Keycloak realm import is rendered from a dedicated template using:

- `keycloak.realmBootstrap.realmId`
- `hub.secrets.systemClientSecret` (optional; auto-generated when chart-managed Hub secret is used)
- `hub.secrets.cryptomatorvaultsClientSecret` (optional; auto-generated when chart-managed Hub secret is used; required for the Katta token-exchange flow)
- `hub.admin.*` (realm-level Hub admin user; separate from `keycloak.admin.*` bootstrap user)

## Bundled MinIO (for evaluation)

Set `minio.enabled=true` to deploy a single-replica MinIO StatefulSet with a PVC alongside the Hub.

When `minio.enabled=true` and either `storageProfileSeed.static.enabled=true` or `storageProfileSeed.sts.enabled=true`, a `post-install,post-upgrade` Helm hook Job:

1. Waits for the Hub `/q/health/ready` endpoint to return 200.
2. Obtains an admin access token via Keycloak `client_credentials` (using the `cryptomatorhub-system` service account).
3. POSTs a `S3STATIC` storage profile pointing at the bundled MinIO service to the polymorphic `/api/storageprofile/` endpoint (dispatching on the `protocol` discriminator).
4. Skips seeding any profile whose name already exists on the Hub, so re-runs are idempotent. Profile UUIDs are assigned by the server on creation.

### MinIO ingress exposure

MinIO is exposed via ingress only for the hostnames you explicitly configure:

- Set `urls.s3.public` to expose the **S3 API**. This **must be a dedicated host served at the root** (e.g. `https://s3.example.com`), **not** a subpath. S3 path-style addressing ignores any base path — clients address buckets at the host root (`<host>/<bucket>/<key>`) — so a subpath in the seeded profile's endpoint URL would be silently dropped and the client's root requests would 404 at the ingress (surfacing as a misleading CORS error). The chart **fails fast** if `urls.s3.public` contains a path. This address is baked into the seeded storage profile, so it must resolve for both the Hub pod and external clients.
- Set `urls.minio.public` to expose the **web console**. Unlike the S3 API, the console *may* be served under a subpath (e.g. `https://minio.example.com/minio`); the chart applies the same strip-prefix routing as Hub/Keycloak and sets `MINIO_BROWSER_REDIRECT_URL` so the console emits correctly-prefixed asset/redirect URLs.

Leave either blank and that ingress isn't created — the corresponding service is then reachable only in-cluster (or via `kubectl port-forward svc/<release>-service-minio 9001:9001` for the console). The demo serves the S3 API at `http://s3.local.katta.cloud:9090` (its own root host) and the console at `http://minio.local.katta.cloud:9090`.

### MinIO IdP debugging

````shell
mc alias set helm http://s3.local.katta.cloud:9090 minioadmin minioadmin
mc idp openid ls helm                                                                                                                                                                                                                                                                                                         a05d2a4c
╭──────────────────────────────────────────────────────────────────────────╮
│ On?        Name                             RoleARN                      │
│ 🔴           (default)                                                   │
│ 🟢         cryptomator  arn:minio:iam:::role/IqZpDC5ahW_DCAvZPZA4ACjEnDE │
│ 🟢      cryptomatorhub  arn:minio:iam:::role/HGKdlY4eFFsXVvJmwlMYMhmbnDE │
│ 🟢   cryptomatorvaults  arn:minio:iam:::role/Hdms6XDZ6oOpuWYI3gu4gmgHN94 │
╰──────────────────────────────────────────────────────────────────────────╯
mc admin policy list helm
# ...
mc admin policy info helm katta_access_bucket_policy                                                                                                                                                                                                                                                                          a05d2a4c
# {
#  "PolicyName": "katta_access_bucket_policy",
# ...
# }
```

## Telemetry (OpenTelemetry)

Hub exports metrics, traces and logs via OpenTelemetry / OTLP. Telemetry is **off by default**; enable it via:

- `hub.metrics.enabled` (default `false`)
- `hub.metrics.endpoint` — OTLP endpoint, default `https://otel-collector:443`
- `hub.metrics.protocol` — OTLP wire protocol: `http/protobuf` (default) or `grpc`
- `hub.metrics.resourceAttributes` — extra OTel resource attributes merged into the chart defaults (`service.name`, `service.version`). Setting a key with the same name overrides the default.
- `hub.metrics.otlp.username` / `hub.metrics.otlp.password` — Credentials used to add `QUARKUS_OTEL_EXPORTER_OTLP_HEADERS` header `Authorization: Basic <base64(user:pass)>`.

When disabled, the chart sets `QUARKUS_OTEL_SDK_DISABLED=true` so the SDK does not start. When enabled, the chart sets `QUARKUS_OTEL_EXPORTER_OTLP_ENDPOINT` and Hub pushes to your collector — there is no `/q/metrics` scrape endpoint. To bridge to Prometheus, run an OpenTelemetry Collector with a `prometheus` or `prometheusremotewrite` exporter.

## Hub with External PostgreSQL and Keycloak

```bash
helm install katta chart \
  --namespace katta \
  --create-namespace \
  --wait --timeout 5m \
  --set keycloak.enabled=false \
  --set postgres.enabled=false \
  --set hub.database.jdbcUrl='jdbc:postgresql://db.example:5432/hub' \
  --set hub.database.username='hub' \
  --set urls.kc.public='https://sso.example/kc' \
  --set urls.kc.clusterInternal='http://keycloak.svc.cluster.local:8080/kc' \
  --set urls.kc.authServerUrl='http://keycloak.svc.cluster.local:8080/kc/realms/cryptomator' \
  --set urls.kc.tokenIssuer='https://sso.example/kc/realms/cryptomator'
```

### Importing `realm.json`

Even with `keycloak.enabled=false`, the chart still renders `realm.json` in Secret `<release>-secrets-kc` so you can manually export/import it for your existing Keycloak.

Assuming namespace `katta` and name `hub`:

```bash
kubectl get secret -n katta hub-secrets-kc -o jsonpath='{.data.realm\.json}' | base64 -d | ...
```

When pointing Katta Server at an external Keycloak, you must ensure the realm contains:
- A `cryptomatorhub` public OIDC client
- A `cryptomator` public OIDC client (for desktop/mobile)
- A `cryptomatorvaults` confidential client with `standard.token.exchange.enabled=true` (required by the Katta token-exchange flow)
- A `cryptomatorhub-system` service-account client with `realm-admin` and `view-system` roles

## Verify Published Chart (Signature + Provenance)

This chart contains an OCI chart signature, which can be verified as follows (assuming chart version `0.1.3`):

```bash
cosign verify \
  --certificate-identity-regexp 'https://github.com/shift7-ch/katta-server/.github/workflows/helm-chart.yml@refs/(heads|tags)/.+' \
  --certificate-oidc-issuer https://token.actions.githubusercontent.com \
  ghcr.io/shift7-ch/charts/katta-server:0.1.3
```

You can additionally inspect provenance attestations:

```bash
cosign verify-attestation \
  --type https://slsa.dev/provenance/v1 \
  --certificate-identity-regexp 'https://github.com/shift7-ch/katta-server/.github/workflows/helm-chart.yml@refs/(heads|tags)/.+' \
  --certificate-oidc-issuer https://token.actions.githubusercontent.com \
  ghcr.io/shift7-ch/charts/katta-server:0.1.3
```

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

The fastest way to spin up a complete Katta stack — Hub + Keycloak + Postgres + MinIO + a pre-seeded storage profile — is via `values-demo.yaml` against any local cluster with the nginx-ingress addon (tested on minikube + Podman).

```bash
# one-off (skip if your cluster already has nginx-ingress)
minikube addons enable ingress

# deploy
helm install katta chart \
  --namespace katta \
  --create-namespace \
  -f charts/cryptomator-hub/values-demo.yaml
```

In a separate terminal, expose the ingress controller on `localhost:9090`:

```bash
kubectl port-forward -n ingress-nginx svc/ingress-nginx-controller 9090:80
```

Once both commands are running:

| URL | Credentials |
|---|---|
| Hub UI: <http://hub.localhost:9090> | `admin` / `admin` |
| Keycloak admin: <http://kc.localhost:9090> | `admin` / `admin` |
| MinIO console: <http://minio.localhost:9090> | `minioadmin` / `minioadmin` |
| MinIO S3 API: <http://s3.localhost:9090> | (used by the seeded storage profile) |

A post-install Helm hook Job (`<release>-storageprofile-seed`) registers an `S3STATIC` storage profile named "Bundled MinIO" pointing at `http://s3.localhost:9090`, so vault creation works end-to-end immediately after install. Re-runs are idempotent (the seed Job treats HTTP 409 as success).

If the demo's pinned proxy ClusterIP `10.96.250.250` collides with something in your cluster, override it: `--set ingress.proxy.clusterIP=<another-free-IP-in-the-Service-CIDR>`.

## Quick Start (Production-shaped, no MinIO, real DNS)

Assumes a real domain with public DNS and a **pre-existing Traefik ingress controller** in the cluster — this chart only registers `Ingress` and `Middleware` resources against it; it does not install Traefik. Confirm the `IngressClass` you want to use (`kubectl get ingressclass`) and substitute its name below if it isn't `traefik`.

```bash
helm install katta charts/cryptomator-hub \
  --namespace katta \
  --create-namespace \
  --wait --timeout 5m \
  --set urls.hub.public=https://hub.example.com \
  --set urls.kc.public=https://kc.example.com \
  --set ingress.controller=traefik \
  --set ingress.className=traefik \
  --set hub.admin.password=changeme
```

Real public DNS handles in-cluster resolution naturally (the chart's port-translation proxy stays disabled when URLs use the default ports 80/443), so `hostAliases` is a no-op for production deployments.

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
3. POSTs a `S3STATIC` storage profile pointing at the bundled MinIO service to `/api/storageprofile/s3static`.
4. Treats both `201 Created` and `409 Conflict` as success, so re-runs are idempotent.

The profile UUID is generated on first install and persisted in a `<release>-storageprofile-seed-state` ConfigMap, so subsequent upgrades reuse the same row.

### MinIO ingress exposure

MinIO is exposed via ingress only for the hostnames you explicitly configure:

- Set `urls.s3.public` to expose the **S3 API**. This **must be a dedicated host served at the root** (e.g. `https://s3.example.com`), **not** a subpath. S3 path-style addressing ignores any base path — clients address buckets at the host root (`<host>/<bucket>/<key>`) — and the seeded storage profile stores only scheme/host/port, so a subpath would be silently dropped and the client's root requests would 404 at the ingress (surfacing as a misleading CORS error). The chart **fails fast** if `urls.s3.public` contains a path. This address is baked into the seeded storage profile, so it must resolve for both the Hub pod and external clients.
- Set `urls.minio.public` to expose the **web console**. Unlike the S3 API, the console *may* be served under a subpath (e.g. `https://minio.example.com/minio`); the chart applies the same strip-prefix routing as Hub/Keycloak and sets `MINIO_BROWSER_REDIRECT_URL` so the console emits correctly-prefixed asset/redirect URLs.

Leave either blank and that ingress isn't created — the corresponding service is then reachable only in-cluster (or via `kubectl port-forward svc/<release>-service-minio 9001:9001` for the console). The demo serves the S3 API at `http://s3.localhost:9090` (its own root host) and the console at `http://minio.localhost:9090/minio`.

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

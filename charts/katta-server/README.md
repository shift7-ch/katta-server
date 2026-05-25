# Katta Server Helm Chart

Helm chart for [Katta Server](https://github.com/shift7-ch/katta-server), a downstream fork of Cryptomator Hub.

This chart deploys:

- Katta Server (the "Hub" backend, required)
- Keycloak (optional, enabled by default) — uses the Katta-customized image with the token-exchange SPI
- PostgreSQL (optional, enabled by default)
- MinIO (optional, disabled by default — see [Bundled MinIO](#bundled-minio-for-evaluation))

Image repositories/tags are fixed in templates:
- Hub: `ghcr.io/shift7-ch/katta-server:<appVersion from Chart.yaml>`
- Keycloak: `ghcr.io/shift7-ch/keycloak:26.5.7`
- PostgreSQL: `postgres:17-alpine`
- MinIO (StatefulSet + seed-Job setup container): `quay.io/minio/minio:RELEASE.2025-09-07T16-13-09Z`
- Storage-profile seed Job: `alpine/curl:8.17.0`
- Init waits (DB/OIDC readiness): `busybox:1.36`, `alpine/curl:8.17.0`

TLS termination is currently expected to be done by the ingress controller.
Supported ingress controller templates:
- `ingress.controller=nginx`
- `ingress.controller=traefik`
- `ingress.controller=contour`

## Quick Start (Local Demo with Bundled MinIO)

The fastest way to spin up a complete Katta stack — Hub + Keycloak + Postgres + MinIO + a pre-seeded storage profile — is via `values-demo.yaml` against any local cluster with the nginx-ingress addon (tested on minikube + Podman).

```bash
# one-off (skip if your cluster already has nginx-ingress)
minikube addons enable ingress

# deploy
helm install katta charts/katta-server \
  --namespace katta \
  --create-namespace \
  -f charts/katta-server/values-demo.yaml
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
helm install katta charts/katta-server \
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

- Set `urls.s3.public` to expose the **S3 API** on its own hostname (required: S3 path-style addressing can't share a hostname with a subpath). This is the address baked into the seeded storage profile, so it must resolve for both the Hub pod and external clients.
- Set `urls.minio.public` to expose the **web console**.

Leave either blank and that ingress isn't created — the corresponding service is then reachable only in-cluster (or via `kubectl port-forward svc/<release>-service-minio 9001:9001` for the console). The demo values set both to `*.localhost:9090`.

## Metrics Endpoint

Hub metrics are configured via:

- `hub.metrics.enabled`
- `hub.metrics.username`
- `hub.metrics.password` (optional; auto-generated if unset)

When metrics are enabled, the chart creates:

- Secret `<release>-secrets-hub-metrics` of type `kubernetes.io/basic-auth`
- Metrics ingress route on Hub management endpoint path `/q/metrics`
- Basic-auth protection for metrics ingress on `nginx` and `traefik` controllers

## Hub with External PostgreSQL and Keycloak

```bash
helm install katta charts/katta-server \
  --namespace katta \
  --create-namespace \
  --wait --timeout 5m \
  --set keycloak.enabled=false \
  --set postgres.enabled=false \
  --set hub.database.jdbcUrl='jdbc:postgresql://db.example:5432/hub' \
  --set hub.database.username='hub' \
  --set hub.config.keycloakPublicUrl='https://sso.example/kc' \
  --set hub.config.keycloakLocalUrl='http://keycloak.svc.cluster.local:8080/kc' \
  --set hub.oidc.authServerUrl='http://keycloak.svc.cluster.local:8080/kc/realms/cryptomator' \
  --set hub.oidc.tokenIssuer='https://sso.example/kc/realms/cryptomator'
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

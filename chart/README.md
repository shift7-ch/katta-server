# Katta Server Helm Chart

Helm chart for [Katta Server](https://github.com/shift7-ch/katta-server), a downstream fork of Cryptomator Hub.

## What this chart deploys

| Component | Image | Bundled by default | Notes |
|---|---|---|---|
| Katta Server | `ghcr.io/cryptomator/hub` | always | The "Hub" backend; runs as a native binary. |
| Keycloak | `ghcr.io/shift7-ch/keycloak` | yes, optional | Identity provider. Users, groups and logins are managed here; the chart creates Katta's realm on first start. Can be replaced by a Keycloak you already operate. |
| PostgreSQL | `postgres` | yes, optional | Database for Hub and Keycloak. Can be replaced by an existing server (two databases required if Keycloak is bundled). |
| MinIO | `docker.io/alpine/minio` | yes, optional | S3-compatible object storage for the demo setup (community rebuild of the last MinIO community release; MinIO no longer publishes images). Can be replaced by any S3 endpoint via storage profiles. |

The chart relies on an **nginx** or **Traefik** ingress controller in your cluster and on two public URLs — one for Katta, one for Keycloak — either as separate hostnames (`katta.example.com`, `kc.example.com`) or as paths on one host (`example.com/katta`, `example.com/kc`). TLS is terminated by the ingress controller; the chart can attach an existing certificate or request one through cert-manager.

Passwords you don't set are generated on install and stored in Kubernetes Secrets; the install notes show how to retrieve them.

> [!IMPORTANT]
> Keycloak creates Katta's realm — including the public URLs — only on its **first** start. Decide on the final URLs before installing; changing them later means editing the `cryptomatorhub` client in the Keycloak admin console or reinstalling with a fresh database.

## Installing

**Helm CLI:**

```bash
kubectl port-forward -n ingress-nginx svc/ingress-nginx-controller 9090:80
helm install hub oci://ghcr.io/shift7-ch/charts/katta-server \
  --namespace cryptomator --create-namespace \
  --set urls.hub.public=https://katta.example.com \
  --set urls.kc.public=https://kc.example.com \
  --set ingress.controller=nginx
```

All values are documented in `values.yaml` and validated against `values.schema.json`. Step-by-step guides — local clusters, connecting an existing Keycloak, upgrading — are in the [deployment docs](https://docs.katta.cloud/self-hosting-guide/deployment).

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

| URL | Credentials |
|---|---|
| Hub UI: <http://hub.localhost:9090> | `admin` / `admin` |
| Keycloak admin: <http://kc.local.katta.cloud:9090> | `admin` / `admin` |
| MinIO console: <http://minio.local.katta.cloud:9090> | `minioadmin` / `minioadmin` |
| MinIO S3 API: <http://s3.local.katta.cloud:9090> | (used by the seeded storage profile) |

A post-install Helm hook Job (`<release>-storageprofile-seed`) registers an `S3STATIC` storage profile named "Bundled MinIO" pointing at `http://s3.local.katta.cloud:9090`, so vault creation works end-to-end immediately after install. Re-runs are idempotent (the seed Job skips profiles whose name already exists).

**How in-cluster DNS works in demo mode:** the same hostnames the browser uses need to resolve inside the cluster too (MinIO has to fetch Keycloak's OIDC discovery URL, and the issuer it sees must match the browser-facing one). The chart's demo profile sets `coredns.patch.enabled=true`, which runs a post-install hook Job that adds a release-scoped `# BEGIN katta:<release>` / `# END katta:<release>` stanza to `kube-system/coredns`'s Corefile, rewriting `*.local.katta.cloud` queries to the chart's port-translation proxy Service. A matching `pre-delete` Job removes the stanza on `helm uninstall`. CoreDNS's `reload` plugin picks up the change within ~30 s; the apply Job sleeps 45 s as a settling buffer before the storage-profile seed Job runs.

### MinIO IdP debugging

````shell
mc alias set helm http://s3.local.katta.cloud:9090 minioadmin minioadmin
mc idp openid ls helm                                                   a05d2a4c
╭──────────────────────────────────────────────────────────────────────────╮
│ On?        Name                             RoleARN                      │
│ 🔴           (default)                                                   │
│ 🟢         cryptomator  arn:minio:iam:::role/IqZpDC5ahW_DCAvZPZA4ACjEnDE │
│ 🟢      cryptomatorhub  arn:minio:iam:::role/HGKdlY4eFFsXVvJmwlMYMhmbnDE │
│ 🟢   cryptomatorvaults  arn:minio:iam:::role/Hdms6XDZ6oOpuWYI3gu4gmgHN94 │
╰──────────────────────────────────────────────────────────────────────────╯
mc admin policy list helm
# ...
mc admin policy info helm katta_access_bucket_policy                    a05d2a4c
# {
#  "PolicyName": "katta_access_bucket_policy",
# ...
# }
```

## Verify Published Chart (Signature + Provenance)

This chart contains an OCI chart signature, which can be verified as follows (assuming chart version `0.1.3`):
Once both commands are running:

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

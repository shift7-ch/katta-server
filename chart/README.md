# Katta Server Helm Chart

Helm chart for [Katta Server](https://github.com/shift7-ch/katta-server), a downstream fork of Cryptomator Hub.

## What this chart deploys

| Component | Image | Bundled by default | Notes |
|---|---|---|---|
| Katta Server | `ghcr.io/shift7-ch/katta-server` | always | The "Hub" backend; runs as a native binary. |
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
helm install hub oci://ghcr.io/shift7-ch/charts/katta-server --version 0.0.0-f9b88f4 \
  --namespace cryptomator --create-namespace \
  --set urls.hub.public=https://katta.example.com \
  --set urls.kc.public=https://kc.example.com \
  --set ingress.controller=nginx
```

All values are documented in `values.yaml` and validated against `values.schema.json`. Step-by-step guides — local clusters, connecting an existing Keycloak, upgrading — are in the [deployment docs](https://docs.katta.cloud/self-hosting-guide/deployment).

For a local demo with bundled MinIO and pre-seeded storage profiles, see [`deploy/helm/demo/`](../deploy/helm/demo/README.md).

## Verify Published Chart (Signature + Provenance)

This chart contains a OCI chart signature, which can be verified as follows (assuming chart version `0.0.0-f9b88f4`):

```bash
cosign verify \
  --certificate-identity-regexp 'https://github.com/shift7-ch/katta-server/.github/workflows/helm-chart.yml@refs/(heads|tags)/.+' \
  --certificate-oidc-issuer https://token.actions.githubusercontent.com \
  ghcr.io/shift7-ch/charts/katta-server:0.0.0-f9b88f4
```

You can additionally inspect provenance attestations:

```bash
cosign verify-attestation \
  --type https://slsa.dev/provenance/v1 \
  --certificate-identity-regexp 'https://github.com/shift7-ch/katta-server/.github/workflows/helm-chart.yml@refs/(heads|tags)/.+' \
  --certificate-oidc-issuer https://token.actions.githubusercontent.com \
  ghcr.io/shift7-ch/charts/katta-server:0.0.0-f9b88f4
```

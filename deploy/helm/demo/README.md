# Helm demo with bundled MinIO

Installs a complete Katta stack — Hub, Keycloak, PostgreSQL, MinIO and pre-seeded storage profiles — from **this checkout** into a local single-user Kubernetes cluster, using [`values-demo.yaml`](values-demo.yaml). Not for production; see [`../prod/`](../prod/README.md) for that.

Prerequisites:

- A checkout of this repository; all commands below run from its root.
- `kubectl`, Helm 3.8+, and a **single-user local cluster**. Tested on [kind](https://kind.sigs.k8s.io/); minikube, k3d and Docker Desktop work the same way once an nginx ingress controller runs.
- Cluster-admin rights. The chart provisions a `Role` + `RoleBinding` in `kube-system` so a post-install hook Job can patch the CoreDNS ConfigMap, so don't use these values on a shared or managed cluster.

Hostnames: the Hub UI lives on `hub.localhost`, which browsers treat as a secure context, so WebCrypto works without HTTPS. Keycloak, the MinIO console and S3 live under the wildcard A-record `*.local.katta.cloud → 127.0.0.1`. Unlike `*.localhost`, these names also resolve inside the cluster via the CoreDNS patch below, and the desktop client accepts them. Both resolve to `127.0.0.1` in the browser without `/etc/hosts` edits.

1. Create the cluster and install the nginx ingress controller, then wait until it and its admission webhook accept requests:

   ```bash
   cat <<'EOF' | kind create cluster --config -
   kind: Cluster
   apiVersion: kind.x-k8s.io/v1alpha4
   nodes:
     - role: control-plane
       labels:
         ingress-ready: "true"
   EOF
   kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/controller-v1.15.1/deploy/static/provider/kind/deploy.yaml
   kubectl wait -n ingress-nginx --for=condition=ready pod -l app.kubernetes.io/component=controller --timeout=300s
   # the admission webhook can refuse connections for a while after the controller is ready
   until kubectl create ingress webhook-probe --class nginx --rule "probe.local/*=probe:80" --dry-run=server > /dev/null; do sleep 5; done
   ```

   On minikube, `minikube addons enable ingress` replaces the first two commands.

   > [!IMPORTANT]
   > The ingress controller must be running **before** `helm install`. The chart looks up the controller's pod IPs at render time to create the port-translation proxy Service (`<release>-ingress-proxy`) that in-cluster DNS for `*.local.katta.cloud` points at. If no controller pods are found, the proxy is silently skipped and Hub fails to reach MinIO (see [Troubleshooting](#troubleshooting)). If you installed the controller afterwards, re-run `helm upgrade katta chart --namespace katta -f deploy/helm/demo/values-demo.yaml`.
2. Install the chart:

   ```bash
   helm install katta chart \
     --namespace katta --create-namespace \
     -f deploy/helm/demo/values-demo.yaml \
     --wait --timeout 20m
   ```

   Installing from the registry (`oci://ghcr.io/shift7-ch/charts/katta-server`) instead of `chart` needs a chart version that includes the Katta Admin CLI seed Job and the test license values. The published chart doesn't contain `values-demo.yaml`, so pass it from the checkout either way.
3. Check that the ingress controller has picked up the chart's Ingress resources (the `ADDRESS` column is populated after a minute or so), then expose it on `localhost:9090` and keep the port-forward running:

   ```bash
   kubectl get ingress -n katta
   kubectl port-forward -n ingress-nginx svc/ingress-nginx-controller 9090:80
   ```

   | URL | Credentials |
   |---|---|
   | Hub UI: <http://hub.localhost:9090> | `admin` / `admin` |
   | Keycloak admin: <http://kc.local.katta.cloud:9090> | `admin` / `admin` |
   | MinIO console: <http://minio.local.katta.cloud:9090> | `minioadmin` / `minioadmin` |
   | MinIO S3 API: <http://s3.local.katta.cloud:9090> | (used by the seeded storage profiles) |

The demo values configure a test license with 5 seats, as Hub otherwise stays in setup mode until a license is provided.

A post-install Helm hook Job (`<release>-storageprofile-seed`) registers the MinIO OIDC providers and policies and uploads two storage profiles pointing at `http://s3.local.katta.cloud:9090` with the Katta Admin CLI: "Bundled MinIO" (static credentials) and "Bundled MinIO (STS)". Vault creation works right after install. Re-runs are idempotent (`--skipIfExists` skips profiles whose name already exists).

**How in-cluster DNS works:** the hostnames the browser uses must resolve inside the cluster too (MinIO fetches Keycloak's OIDC discovery URL, and the issuer it sees must match the browser-facing one). The demo values set `coredns.patch.enabled=true`, which runs a post-install hook Job that adds a release-scoped stanza to `kube-system/coredns`'s Corefile, rewriting `*.local.katta.cloud` queries to the chart's port-translation proxy Service. A matching `pre-delete` Job removes it on `helm uninstall`.

**MinIO ingress exposure:** `urls.s3.public` exposes the S3 API and must be a dedicated host served at the root, not a subpath. S3 path-style addressing ignores any base path, so a subpath would be dropped from the seeded profile's endpoint and the client's root requests would 404 at the ingress, surfacing as a misleading CORS error. The chart fails fast if `urls.s3.public` contains a path. `urls.minio.public` exposes the web console, which may be served under a subpath. Leave either blank and that ingress isn't created; the service is then reachable only in-cluster or via `kubectl port-forward svc/<release>-service-minio 9001:9001`.

## Troubleshooting

When the Hub API returns an error, check the Hub server logs. API error responses include an `error id` that matches the stack trace in the log (add `-f` to follow while reproducing):

```bash
kubectl logs -n katta deploy/katta-hub -c hub --since=1h | grep -E "ERROR|Caused by"
```

Logs of a crashed or restarted container are available with `--previous`. The init containers (`wait-for-postgres`, `wait-for-oidc`) log separately; select them with `-c <name>` if the pod is stuck in `Init`. Hub logs only to stdout; for logs that survive pod deletion, configure an OpenTelemetry/OTLP endpoint (`hub.metrics.enabled` / `hub.metrics.endpoint`) or run a cluster log collector.

**`java.net.UnknownHostException: s3.local.katta.cloud`** (e.g. a 500 on `PUT /api/storage/...`) means in-cluster DNS for the public hostnames isn't working. Check that the proxy Service exists and that the name resolves from inside the cluster:

```bash
kubectl get svc -n katta katta-ingress-proxy
kubectl run dnstest -n katta --rm -i --restart=Never --image=busybox:1.36 -- nslookup s3.local.katta.cloud
```

If the Service is missing, the ingress controller wasn't running at install time. Make sure it is ready, then re-render the chart with `helm upgrade katta chart --namespace katta -f deploy/helm/demo/values-demo.yaml`.

**MinIO OIDC providers and policies:**

```shell
mc alias set helm http://s3.local.katta.cloud:9090 minioadmin minioadmin
mc idp openid ls helm
╭──────────────────────────────────────────────────────────────────────────╮
│ On?        Name                             RoleARN                      │
│ 🔴           (default)                                                   │
│ 🟢         cryptomator  arn:minio:iam:::role/IqZpDC5ahW_DCAvZPZA4ACjEnDE │
│ 🟢      cryptomatorhub  arn:minio:iam:::role/HGKdlY4eFFsXVvJmwlMYMhmbnDE │
│ 🟢   cryptomatorvaults  arn:minio:iam:::role/Hdms6XDZ6oOpuWYI3gu4gmgHN94 │
╰──────────────────────────────────────────────────────────────────────────╯
mc admin policy info helm katta-accessbucketpolicy
```

## Clean up

```bash
kind delete cluster
```

# Local testing with Docker Compose

A Cryptomator Hub stack for trying Hub on your machine. [`compose.yaml`](compose.yaml) includes [`katta.yaml`](katta.yaml), mounts [`minio/`](minio/) and renders the realm from the Helm chart in `chart/`, so it runs from a checkout of this repository.

Requirements: Docker Compose ≥ 2.23.1.

1. Clone this repository and change to this directory.
2. `docker compose up`
3. Wait until all services are `healthy` (Keycloak imports the realm on first start), then open Hub at <http://localhost:8280> or the Keycloak admin console at <http://localhost:8380>, both `admin` / `admin`.

Everything runs on plain HTTP with well-known passwords — for evaluation only. To change ports or hostnames, follow the comments in `compose.yaml`; the realm is imported on Keycloak's first boot only, so `docker compose down -v` to start over after changing URLs.

<!-- / start katta extension -->
Katta additions:

- Hub starts with a public test license with 5 seats.
- MinIO serves S3 and STS at <http://minio.localhost:9100> and its console at <http://localhost:9101> (`minioadmin` / `minioadmin`).
- `minio_setup` creates the `katta-*` policies and registers the `cryptomator`, `cryptomatorhub` and `cryptomatorvaults` OIDC providers. [`../demo/`](../demo/README.md) also uploads matching storage profiles.
- `keycloak-realm` and `minio_setup` run once and then show as `Exited (0)` in `docker compose ps -a`.
<!-- \ end katta extension -->

For real deployments see [`../prod/`](../prod/README.md) or [`../../helm/prod/`](../../helm/prod/README.md); for everything else — license, user management, connecting Cryptomator apps — see <https://docs.cryptomator.org/hub/>.

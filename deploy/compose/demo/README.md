# Demo with Docker Compose

The [local stack](../local/README.md) plus two storage profiles for its bundled MinIO, so vaults can be created right after startup. Run it from a checkout of this repository: [`compose.yaml`](compose.yaml) includes `../local/compose.yaml`, which renders the realm from the Helm chart in `chart/`.

Requirements: Docker Compose ≥ 2.23.1. The local and demo stacks publish the same ports, so only one of them can run at a time.

1. `docker compose up --wait` in this directory.
2. Open Katta at <http://localhost:8280> and sign in as `admin` / `admin`.

The `hub_setup_storage_profile` service uploads the profiles with the [Katta Admin CLI](https://github.com/shift7-ch/katta-admin-cli) and then exits; reruns skip profiles that already exist:

| Profile | Endpoint | Credentials |
|---|---|---|
| MinIO S3 STS | `http://minio.localhost:9100` | Short-lived, per vault: MinIO exchanges Keycloak tokens via its `cryptomator`, `cryptomatorhub` and `cryptomatorvaults` OIDC providers |
| MinIO S3 static | `http://minio.localhost:9100` | Static access keys you enter when creating a vault, see below |

The MinIO console only browses objects, so create static access keys with [`mc`](https://github.com/minio/mc) on your machine:

```bash
mc alias set demo http://localhost:9100 minioadmin minioadmin
mc admin user add demo <access-key>  # prompts for the secret key
mc admin policy attach demo katta-staticcreatebucketpolicy katta-staticaccessbucketpolicy --user <access-key>
```

| Service | URL | Login |
|---|---|---|
| Katta | <http://localhost:8280> | `admin` / `admin` |
| Keycloak admin console | <http://localhost:8380> | `admin` / `admin` |
| MinIO console | <http://localhost:9101> | `minioadmin` / `minioadmin` |

`docker compose down -v` removes all data and starts over.

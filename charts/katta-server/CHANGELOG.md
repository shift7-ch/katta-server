# Changelog

## Unreleased

### Breaking

- **Chart renamed** from `cryptomator-hub` to `katta-server`. Existing releases installed
  under the old chart name cannot be `helm upgrade`'d in place; reinstall using the new
  chart. The Helm release name is independent of the chart name and can stay the same.
- Helper template names changed from `cryptomator-hub.*` to `katta-server.*`. Affects
  anyone forking or extending the chart with custom templates.

### Added

- Optional bundled MinIO (`minio.enabled`, off by default): single-replica StatefulSet
  with a PVC, plus a `ClusterIP` Service exposing the S3 API (9000) and web console
  (9001).
- Optional post-install Job (`storageProfileSeed.static.enabled` / `storageProfileSeed.sts.enabled`, gated on
  `minio.enabled`) that registers a matching `S3STATIC` storage profile in the Hub
  database via the `/api/storageprofile/s3static` endpoint. The Job:
  - waits for `/q/health/ready` to return 200,
  - obtains an admin token via Keycloak `client_credentials` (`cryptomatorhub-system`),
  - posts the profile, treating 201 and 409 as success (idempotent).
- `<release>-storageprofile-seed-state` ConfigMap pinning the seeded profile's UUID
  across upgrades (annotated `helm.sh/resource-policy: keep`).
- `cryptomatorvaults` Keycloak client + audience mapper on the `cryptomator` client,
  required for the Katta token-exchange flow. Its secret is auto-generated and surfaced
  to the backend via `HUB_KEYCLOAK_OIDC_CRYPTOMATOR_VAULTS_CLIENT_SECRET`.
- `values-demo.yaml` enabling the full local stack (Hub + Keycloak + Postgres + MinIO +
  seed Job) with fixed demo passwords.

### Changed

- Default images now point to `ghcr.io/shift7-ch/katta-server` and
  `ghcr.io/shift7-ch/keycloak:26.5.7` (was `ghcr.io/cryptomator/hub` and
  `ghcr.io/cryptomator/keycloak:26.5.3`).
- Realm display names and the `cryptomatorhub` / `cryptomatorhub-system` client names
  rebranded to "Katta Server". The Keycloak `loginTheme` is still `cryptomator` until
  the theme directory in `keycloak/themes/` is renamed.
- Chart is now published to `oci://ghcr.io/shift7-ch/charts` (was
  `oci://ghcr.io/cryptomator/charts`).

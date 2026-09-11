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
  `minio.enabled`) that registers a matching `S3STATIC` (and optionally `S3STS`) storage
  profile in the Hub database via the polymorphic `/api/storageprofile/` endpoint. The Job:
  - waits for `/q/health/ready` to return 200,
  - obtains an admin token via Keycloak `client_credentials` (`cryptomatorhub-system`),
  - posts the profile, skipping any profile whose name already exists (idempotent;
    profile UUIDs are assigned by the server).
- `cryptomatorvaults` Keycloak client + audience mapper on the `cryptomator` client,
  required for the Katta token-exchange flow. Its secret is auto-generated and surfaced
  to the backend via `HUB_KEYCLOAK_OIDC_CRYPTOMATOR_VAULTS_CLIENT_SECRET`.
- `values-demo.yaml` enabling the full local stack (Hub + Keycloak + Postgres + MinIO +
  seed Job) with fixed demo passwords.
- `hub.config.additionalConnectSrc`, a list of extra `connect-src` sources appended to the
  chart's Content-Security-Policy. Required for the S3 and STS endpoints of storage profiles
  the browser talks to directly, which the chart cannot derive. Entries are CSP source
  expressions used verbatim, so host wildcards such as `https://*.wasabisys.com` are allowed.
  Ignored when `hub.config.contentSecurityPolicy` replaces the whole header.

### Changed

- Default Content-Security-Policy aligned with the image default in `application.properties`:
  `api.cryptomator.org` dropped from `connect-src`, `api.katta.cloud` added.
- Default images now point to `ghcr.io/shift7-ch/katta-server` and
  `ghcr.io/shift7-ch/keycloak:26.5.7` (was `ghcr.io/cryptomator/hub` and
  `ghcr.io/cryptomator/keycloak:26.5.3`).
- Realm display names and the `cryptomatorhub` / `cryptomatorhub-system` client names
  rebranded to "Katta Server", and the Keycloak realm `loginTheme` set to `katta`
  (the theme shipped in the Katta Keycloak image at `keycloak/themes/katta`).
- Chart is now published to `oci://ghcr.io/shift7-ch/charts` (was
  `oci://ghcr.io/cryptomator/charts`).

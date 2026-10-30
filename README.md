# Katta Server

[![CI Build](https://github.com/shift7-ch/katta-server/actions/workflows/build.yml/badge.svg)](https://github.com/shift7-ch/katta-server/actions/workflows/build.yml)

> [Katta](https://katta.cloud/): transform your S3 storage into a secure, team-friendly workspace with client-side encryption.

Easily integrates into your S3 storage and existing identity management, including OpenID Connect, SAML, and LDAP.

## Components

Katta Server consists of the following components.

### Web Frontend

During development, run Vite from the `frontend` directory as explained in [its README file](frontend/README.md).

### Web Backend

During development, run Quarkus from the `backend` directory as explained in [its README file](backend/README.md).

### Custom Keycloak Image

We add a custom Katta theme to the base Keycloak image, as explained in [its README file](keycloak/README.md).
Katta relies on Keycloak's [Standard Token Exchange (V2)](https://www.keycloak.org/securing-apps/token-exchange#_standard-token-exchange)
as per [RFC 8693](https://www.rfc-editor.org/rfc/rfc8693.html) to obtain vault-specific tokens for STS/S3 calls. The `keycloak`
module contains integration tests verifying this setup.

## Setup

See [Katta Documentation &rarr; Self-Hosting Guide
 &rarr; Deployment](https://docs.katta.cloud/self-hosting-guide/deployment/).

## Changelog

See [CHANGELOG.md](CHANGELOG.md).

## License

This project is licensed under the [GNU Affero General Public License v3.0](LICENSE.txt).

## Developing Downstream of Cryptomator Hub

Katta Server is a downstream fork of [Cryptomator Hub](https://github.com/cryptomator/hub) (`upstream` remote) and regularly merges its `develop` branch. Everything in this section exists to keep those merges cheap: Katta-specific code stays recognizable and separable, upstream files are changed as little as possible, and the few things that *must* diverge (brand, images, schema) follow fixed rules.

### Where downstream code lives

- **Backend**: Katta-only classes go into `katta` sub-packages, e.g. `org.cryptomator.hub.api.katta`, `org.cryptomator.hub.entities.katta`, `org.cryptomator.hub.katta`. The root package deliberately stays `org.cryptomator.hub` to avoid renaming every upstream file. Only the standalone Keycloak tests use `cloud.katta`.
- **Frontend**: Katta-only components go into `frontend/src/components/katta/`, shared helpers into `frontend/src/common/katta.ts` or a dedicated file.
- **Single-line diffs inside upstream files**: if the change is obvious (e.g. replacing "Cryptomator Hub" with "Katta Server", swapping an upstream image or artifact name for the downstream one), just make it. If it is not obvious why the line differs (e.g. replacing `i < 3` with `i < 5`), add a short inline comment on the same line, e.g. `// katta allows up to five i`. Either way, do not add extra lines: the file should stay line-by-line comparable with upstream so the next merge is easy.
- **Multi-line diffs inside upstream files**: wrap larger extensions in `start katta extension` / `end katta extension` markers so they survive a merge and are easy to find and re-apply. Use the comment syntax of the surrounding language:

  ```java
  // / start katta extension
  ...
  // \ end katta extension
  ```

  ```html
  <!-- / start katta extension -->
  ...
  <!-- \ end katta extension -->
  ```

  Keep the marked block as small as possible and prefer a call into a `katta` package or file over inline logic.
- **Helm chart** (`chart/`) is Katta's deployment artifact and has its own `CHANGELOG.md` and version. Upstream's `deploy/` directory (Compose files, upstream Helm READMEs) is kept verbatim from upstream and is not adjusted or used by Katta.

### Brand and assets

Upstream is "Cryptomator Hub", Katta is "Katta". The following places are branded and need a check after every merge, especially when upstream adds a new page or component:

- **Logo**: `frontend/public/logo.png` (upstream ships `logo.svg`, which is deleted downstream). Use `<img src="/logo.png" alt="Katta Logo">` with the sizes used by existing Katta pages (`h-9` in navigation bars, `h-16` on centered cards). A build failure "Could not resolve '/logo.svg'" means a newly merged component still references the upstream logo.
- **Wordmark and title**: `KATTA` instead of `CRYPTOMATOR HUB` in navigation bars, `<title>Katta</title>` in `frontend/index.html`. Headline font is Roboto Slab (`frontend/src/css/fonts.css`), upstream's Quicksand subset is removed.
- **Visible strings**: "Hub" and "Cryptomator Hub" are replaced by "Katta" in `frontend/src/i18n/en-US.json` and in the other locale files. Every upstream merge brings new or re-translated strings, so search the i18n files for `Hub` after merging. Technical identifiers such as JSON keys (`admin.serverInfo.hubId.title`), config prefixes (`hub.*`) and REST paths keep the upstream `hub` name.
- **Keycloak**: the login theme is `keycloak/themes/katta` and the realm definition in `backend/src/main/resources/cryptomator-realm.json` uses `loginTheme: katta`, `displayName: Katta` and `/logo.png`. Katta also adds clients and mappers to that realm (token exchange for `cryptomatorvaults`), so merge conflicts in the realm JSON need manual care.
- **External endpoints**: the update check calls `api.katta.cloud` with `Katta-Version` headers (`frontend/src/common/updatecheck.ts`), and desktop download URLs come from `hub.download-url.*` in `application.properties`. Katta documentation lives at `docs.katta.cloud`; in-app help links for upstream features still point to `docs.cryptomator.org` where no Katta page exists.

### Container images, versions and CI

- Images are published as `ghcr.io/shift7-ch/katta-server`, `ghcr.io/shift7-ch/keycloak` and the chart as `ghcr.io/shift7-ch/charts/katta-server`. Upstream's `ghcr.io/cryptomator/hub` and `ghcr.io/cryptomator/keycloak` must not leak into Katta-owned files: check `application.properties` (dev services), `chart/values.yaml` and the workflows in `.github/workflows/` after a merge. Upstream's `deploy/` directory is the exception and keeps upstream images.
- The application version in `backend/pom.xml` and `frontend/package.json` currently tracks upstream's version; the chart has its own version in `chart/Chart.yaml`.
- `CHANGELOG.md` is upstream's changelog and is merged as-is; chart changes are recorded in `chart/CHANGELOG.md`.
- The workflows in `.github/workflows/` are Katta's own (build, tag, keycloak image, helm chart). Resolve conflicts there in favor of the Katta version and port upstream changes over deliberately.

### Database migrations

Both projects share the same [Flyway](https://flywaydb.org/) migration directory, `backend/src/main/resources/org/cryptomator/hub/flyway`, so the version numbers of both projects must interleave without collisions.

- **Upstream** migrations use plain integer versions: `V30__…`, `V31__…`, `V32__…`. Katta never adds, renames, or edits a plain integer migration.
- **Downstream** (Katta-specific) migrations use a fractional version `V<base>.<nn>__<Description>.sql`, where `<base>` is the latest upstream migration present in the repository at the time the downstream migration is written, and `<nn>` is a two-digit counter starting at `01`. Example: `V31.01__StorageProfile.sql` was written on top of upstream `V31` and is followed by `V31.02__…` until upstream `V32` arrives.
- Flyway compares versions numerically, part by part, so the order is `V31` < `V31.01` < `V31.02` < … < `V31.10` < `V32` < `V32.01`. The leading zero is cosmetic: `31.01` and `31.1` are the *same* version to Flyway. Always use two digits to avoid an accidental duplicate. Versions may only contain digits and dots, so a marker like `V31.katta.01` is rejected by Flyway; the fractional part is the only way to tell downstream migrations apart.
- Test data lives in `backend/src/test/resources/…/V9999__Test_Data.sql` and must stay above every real migration.

When developing downstream:

1. Pick `<base>` = the highest plain integer migration currently in the directory and `<nn>` = the next free counter under that base.
2. Only depend on DDL from migrations that sort before yours. Never modify a migration, upstream or downstream, that has already been applied to a production database; add a new one instead.
3. Renaming or deleting a migration is only acceptable while it has been applied to test systems exclusively. Flyway's validation then fails on those systems ("Detected applied migration not resolved locally"), so wipe their databases before starting the server again. Locally this means removing the reused Testcontainers Postgres container *and* running `mvn clean`, because `target/classes` keeps a copy of the old file and Flyway would still resolve it.

When merging from upstream:

1. New upstream migrations (`V32__…`) sort after all existing `V31.xx` downstream migrations and are applied in that order on existing installations. Out-of-order migration is **not** enabled, so a migration that sorts below the database's current version is never applied and makes startup fail validation. This is exactly what happens if a downstream migration carries a version above upstream's (e.g. `V101` with upstream at `V31`): once `V101` is applied, a merged `V31` is stuck behind it.
2. Review each incoming upstream migration against the downstream ones: upstream may rename or drop tables and columns that downstream migrations or entities depend on, or may add objects with the same name. Resolve conflicts with a new downstream migration `V<new base>.01__…` that runs *after* the upstream change, never by editing the upstream file.
3. Check that downstream JPA entities and Panache queries still match the merged schema, then run the backend integration tests, which migrate a fresh database from `V1` through `V9999`.

### Merge checklist

1. `git fetch upstream && git merge upstream/develop` on a feature branch, resolve conflicts. In files with extension markers, keep the marked blocks; in Katta-owned files (workflows, chart, `katta` packages, `katta/` components) keep the Katta side.
2. Search the merged tree for upstream brand leftovers: `logo.svg`, `CRYPTOMATOR HUB`, `Cryptomator Hub`, `ghcr.io/cryptomator` outside `deploy/`, and `Hub` in the i18n files.
3. Check new or changed upstream migrations against the rules above, then renumber nothing that is already deployed.
4. Re-apply Katta behavior that upstream removed or restructured. Upstream may delete a feature or a settings section that Katta still uses; a clean merge of such a deletion silently breaks the Katta UI (e.g. a form whose initialization was removed).
5. Build and test all modules: `pnpm build` and `pnpm test` in `frontend/`, `./mvnw verify` in `backend/` (needs a container runtime for Postgres, Keycloak and MinIO), the Maven build in `keycloak/`, and `helm lint chart/`.
6. Merge-only commits should not mix in unrelated refactorings; downstream adjustments of newly merged upstream code belong in follow-up commits so the next merge can tell them apart.
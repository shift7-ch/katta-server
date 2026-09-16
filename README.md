[![CI Build](https://github.com/shift7-ch/katta-server/actions/workflows/build.yml/badge.svg)](https://github.com/shift7-ch/katta-server/actions/workflows/build.yml)

# Katta: transform your S3 storage into a secure, team-friendly workspace with client-side encryption

Katta brings zero-config storage management and zero-knowledge key management to teams and organizations.

It easily integrates into your existing identity management, including OpenID Connect, SAML, and LDAP.
As usual, your favorite cloud service remains your free choice [^1].

[^1]: Currently, we support AWS S3 and MinIO S3.

Katta consists of Katta Server and Katta Desktop:

* Katta Desktop is based on [Mountain Duck](https://mountainduck.io/).
* Katta Server is a downstream fork of [Cryptomator Hub](https://github.com/cryptomator/hub/), extended with storage profiles and
  automatic bucket provisioning using OAuth 2.0 token exchange.

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

### Helm Chart

Katta Server, Keycloak, PostgreSQL, and optionally MinIO can be deployed to Kubernetes using the Helm chart
in the `chart` directory, as explained in [its README file](chart/README.md).

## Setup

See [Katta Documentation &rarr; Setup Katta Server](https://docs.katta.cloud/setup/server-setup/).

## Changelog

See [CHANGELOG.md](CHANGELOG.md).

## License

This project is licensed under the [GNU Affero General Public License v3.0](LICENSE.txt).

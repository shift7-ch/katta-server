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
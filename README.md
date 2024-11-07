[![CI Build](https://github.com/shift7-ch/katta-server/actions/workflows/build.yml/badge.svg)](https://github.com/shift7-ch/katta-server/actions/workflows/build.yml)

# Katta: the secure and easy way to work in teams

Katta bring zero-config storage management and zero-knowledge key management for teams and organizations.

It easily integrates into your existing identity management incl. OpenID Connect, SAML, and LDAP.
As usual, your favorite cloud service remains your free choice [^1].

[^1]: Currently, we support AWS S3 and MinIO S3.

Katta consists of Katta Server and Katta Client:

* Katta Client is based on [Mountain Duck](https://mountainduck.io/),
* Katta Server is based on [Cryptomator Hub](https://github.com/cryptomator/hub/).

Katta Server consists of these components:

## Web Frontend

During development, run vite from the `frontend` dir as explained in [its README file](frontend/README.md).

## Web Backend

During development, run Quarkus from the `backend` dir as explained in [its README file](backend/README.md).:

## Custom Keycloak Image

We add a custom token exchange SPI implementation and custom theme to the base keycloak image, as explained
in [its README file](keycloak/README.md).

# Setup

See [Katta Documentiona &rarr; Setup Katta Server](https://github.com/shift7-ch/katta-docs/blob/main/SETUP_KATTA_SERVER.md).



[![Build and test](https://github.com/cryptomator/hub/actions/workflows/buildAndTest.yml/badge.svg)](https://github.com/cryptomator/hub/actions/workflows/buildAndTest.yml)

# Cipherduck Hub: the secure and easy way to work in teams

Cipherduck Hub bring zero-config storage management and zero-knowledge key management for teams and organizations. 

It easily integrates into your existing identity management incl. OpenID Connect, SAML, and LDAP. 
As usual, your favorite cloud service remains your free choice [^1].

[^1]: Currently, we support AWS S3 and MinIO S3. Further cloud services coming soon.

Cipherduck consists of Cipherduck Hub and Cipherduck Client. Cipherduck Client is based on [Mountain Duck](https://mountainduck.io/).
Cipherduck Hub is based on [Cryptomator Hub](https://github.com/cryptomator/hub/), consisting of these components:

## Web Frontend

During development, run vite from the `frontend` dir as explained in [its README file](frontend/README.md).

## Web Backend

During development, run Quarkus from the `backend` dir as explained in [its README file](backend/README.md).:

## Custom Keycloak Image

We add a custom theme to the base keycloak image, as explained in [its README file](keycloak/README.md).:



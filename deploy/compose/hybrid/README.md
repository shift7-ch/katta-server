# Hybrid setup with Docker Compose

Runs Katta Server and its PostgreSQL database on your machine against a Keycloak that already runs elsewhere. Use it to test a Katta Server image without running Keycloak locally.

> [!WARNING]
> Use a realm that no other Katta Server uses. Hub deletes the `cryptomatorvaults` client roles of vaults missing from its own database, so against a realm shared with another Hub (e.g. staging), this stack's fresh database revokes the other Hub's vault roles within a minute.

Requirements: Docker Compose ≥ 2.23.1, Helm and [yq](https://github.com/mikefarah/yq), and admin access to a Keycloak 26.x instance with Standard Token Exchange (V2).

1. Render a realm for your Keycloak. Run this from the repository root, with your Keycloak's public URL in `KC_URL`, and keep the shell open for step 2. The secrets go to Helm on stdin rather than as arguments, so other users can't read them from the process list:

   ```bash
   KC_URL=https://kc.example.com
   REALM=katta-hybrid
   SYSTEM_CLIENT_SECRET=$(openssl rand -hex 32)
   VAULTS_CLIENT_SECRET=$(openssl rand -hex 32)
   ADMIN_PASSWORD=$(openssl rand -hex 16)
   REALM_JSON=$(mktemp)  # owner-only and outside the checkout: the file holds all three secrets
   helm template katta chart --show-only templates/keycloak-secret.yaml -f - <<EOF | yq '.stringData["realm.json"]' > "$REALM_JSON"
   urls:
     hub:
       public: http://localhost:8280
     kc:
       public: "$KC_URL"
   hub:
     config:
       keycloakRealm: "$REALM"
     admin:
       password: "$ADMIN_PASSWORD"
     secrets:
       systemClientSecret: "$SYSTEM_CLIENT_SECRET"
       cryptomatorvaultsClientSecret: "$VAULTS_CLIENT_SECRET"
   keycloak:
     realmBootstrap:
       realmId: "$REALM"
   EOF
   echo "$REALM_JSON"
   ```

   Import that file in the Keycloak admin console (realm selector → **Create realm** → **Browse…**), then `rm "$REALM_JSON"`.
2. In the same shell, write `.env` from the template with the values above. The template preconfigures the public test license; set `HUB_KEYCLOAK_LOCAL_URL` in `.env` if Hub reaches Keycloak under another URL than the browser does.

   ```bash
   cd deploy/compose/hybrid
   { grep -v -E '^(HUB_KEYCLOAK_PUBLIC_URL|HUB_KEYCLOAK_REALM|HUB_KEYCLOAK_SYSTEM_CLIENT_SECRET|HUB_KEYCLOAK_OIDC_CRYPTOMATOR_VAULTS_CLIENT_SECRET)=' .env.template
     cat <<EOF
   HUB_KEYCLOAK_PUBLIC_URL=$KC_URL
   HUB_KEYCLOAK_REALM=$REALM
   HUB_KEYCLOAK_SYSTEM_CLIENT_SECRET=$SYSTEM_CLIENT_SECRET
   HUB_KEYCLOAK_OIDC_CRYPTOMATOR_VAULTS_CLIENT_SECRET=$VAULTS_CLIENT_SECRET
   EOF
   } > .env
   ```

   Add the S3 and STS endpoints of your storage profiles to `CSP_CONNECT_SRC_EXTRA` in `.env`, since the browser talks to them directly.
3. `docker compose up`
4. Wait until `hub` is `healthy`, then open Katta at <http://localhost:8280> and sign in as `admin` with `$ADMIN_PASSWORD`; you must change it on first login.

`docker compose down -v` removes the local database and starts over; the external Keycloak is not touched.

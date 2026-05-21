{{- define "katta-server.realmJson" -}}
{
  {{- if .Values.keycloak.realmBootstrap.realmId }}
  "id": {{ .Values.keycloak.realmBootstrap.realmId | quote }},
  {{- end }}
  "realm": {{ .Values.hub.config.keycloakRealm | quote }},
  "displayName": "Katta Server",
  {{/* TODO: rename the Keycloak theme directory keycloak/themes/cryptomator → keycloak/themes/katta, then update this value. */}}
  "loginTheme": "cryptomator",
  "enabled": true,
  "sslRequired": "external",
  "defaultRole": {
    "name": "user",
    "description": "User"
  },
  "roles": {
    "realm": [
      {
        "name": "user",
        "description": "User",
        "composite": false
      },
      {
        "name": "create-vaults",
        "description": "Can create vaults",
        "composite": false
      },
      {
        "name": "admin",
        "description": "Administrator",
        "composite": true,
        "composites": {
          "realm": [
            "user",
            "create-vaults"
          ],
          "client": {
            "realm-management": [
              "realm-admin"
            ]
          }
        }
      }
    ]
  },
  "users": [
    {
      "username": {{ .Values.hub.admin.username | quote }},
      "enabled": true,
      "credentials": [
        {
          "type": "password",
          "value": {{ include "katta-server.resolvedHubAdminPassword" . | quote }},
          "temporary": {{ .Values.hub.admin.passwordTemporary }}
        }
      ],
      {{- if .Values.hub.admin.passwordTemporary }}
      "requiredActions": [
        "UPDATE_PASSWORD"
      ],
      {{- end }}
      "realmRoles": [
        "admin"
      ]
    },
    {
      "username": "system",
      "email": "system@localhost",
      "enabled": true,
      "serviceAccountClientId": "cryptomatorhub-system",
      "clientRoles": {
        "realm-management": [
          "realm-admin",
          "view-system"
        ]
      }
    }
  ],
  "scopeMappings": [
    {
      "client": "cryptomatorhub",
      "roles": [
        "user",
        "admin"
      ]
    }
  ],
  "clients": [
    {{- $hubPublicUrl := trimSuffix "/" .Values.urls.hub.public -}}
    {
      "clientId": "cryptomatorhub",
      "serviceAccountsEnabled": false,
      "publicClient": true,
      "name": "Katta Server",
      "enabled": true,
      "redirectUris": [
        {{ printf "%s/*" $hubPublicUrl | quote }}
      ],
      "webOrigins": [
        "+"
      ],
      "bearerOnly": false,
      "frontchannelLogout": false,
      "protocol": "openid-connect",
      "attributes": {
        "pkce.code.challenge.method": "S256"
      },
      "protocolMappers": [
        {
          "name": "realm roles",
          "protocol": "openid-connect",
          "protocolMapper": "oidc-usermodel-realm-role-mapper",
          "consentRequired": false,
          "config": {
            "access.token.claim": "true",
            "claim.name": "realm_access.roles",
            "jsonType.label": "String",
            "multivalued": "true"
          }
        },
        {
          "name": "client roles",
          "protocol": "openid-connect",
          "protocolMapper": "oidc-usermodel-client-role-mapper",
          "consentRequired": false,
          "config": {
            "access.token.claim": "true",
            "claim.name": "resource_access.${client_id}.roles",
            "jsonType.label": "String",
            "multivalued": "true"
          }
        }
      ]
    },
    {
      "clientId": "cryptomator",
      "serviceAccountsEnabled": false,
      "publicClient": true,
      "name": "Cryptomator App",
      "enabled": true,
      "redirectUris": [
        "http://127.0.0.1/*",
        "org.cryptomator.ios:/hub/auth",
        "org.cryptomator.android:/hub/auth"
      ],
      "webOrigins": [
        "+"
      ],
      "bearerOnly": false,
      "frontchannelLogout": false,
      "protocol": "openid-connect",
      "attributes": {
        "pkce.code.challenge.method": "S256"
      }
    },
    {
      "clientId": "cryptomatorhub-system",
      "serviceAccountsEnabled": true,
      "publicClient": false,
      "name": "Katta Server System",
      "enabled": true,
      "clientAuthenticatorType": "client-secret",
      "secret": {{ include "katta-server.resolvedSystemClientSecret" . | quote }},
      "standardFlowEnabled": false
    }
  ],
  "browserSecurityHeaders": {
    "contentSecurityPolicy": {{ printf "frame-src 'self'; frame-ancestors 'self' %s; object-src 'none';" $hubPublicUrl | quote }}
  }
}
{{- end -}}

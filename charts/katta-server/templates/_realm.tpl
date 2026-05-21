{{- define "katta-server.realmJson" -}}
{
  {{- if .Values.keycloak.realmBootstrap.realmId }}
  "id": {{ .Values.keycloak.realmBootstrap.realmId | quote }},
  {{- end }}
  "realm": {{ .Values.hub.config.keycloakRealm | quote }},
  "displayName": "Katta Server",
  "loginTheme": "katta",
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
      "realmRoles": [
        "admin"
      ],
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
    },
    {
      "client": "cryptomatorhub-system",
      "roles": [
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
      },
      "protocolMappers": [
        {
          "name": "aud",
          "protocol": "openid-connect",
          "protocolMapper": "oidc-audience-mapper",
          "consentRequired": false,
          "config": {
            "included.client.audience": "cryptomator",
            "id.token.claim": "false",
            "access.token.claim": "true",
            "userinfo.token.claim": "false",
            "multivalued": "true"
          }
        },
        {
          "name": "aud-cryptomatorvaults",
          "protocol": "openid-connect",
          "protocolMapper": "oidc-audience-mapper",
          "consentRequired": false,
          "config": {
            "included.client.audience": "cryptomatorvaults",
            "id.token.claim": "false",
            "access.token.claim": "true",
            "userinfo.token.claim": "false",
            "multivalued": "true"
          }
        }
      ]
    },
    {
      "clientId": "cryptomatorvaults",
      "name": "Cryptomator S3 Access",
      "serviceAccountsEnabled": false,
      "publicClient": false,
      "enabled": true,
      "clientAuthenticatorType": "client-secret",
      "secret": {{ include "katta-server.resolvedCryptomatorvaultsClientSecret" . | quote }},
      "bearerOnly": false,
      "frontchannelLogout": false,
      "standardFlowEnabled": false,
      "implicitFlowEnabled": false,
      "directAccessGrantsEnabled": false,
      "protocol": "openid-connect",
      "attributes": {
        "standard.token.exchange.enabled": "true"
      },
      "protocolMappers": [
        {
          "name": "aud",
          "protocol": "openid-connect",
          "protocolMapper": "oidc-audience-mapper",
          "consentRequired": false,
          "config": {
            "included.client.audience": "cryptomatorvaults",
            "id.token.claim": "false",
            "access.token.claim": "true",
            "userinfo.token.claim": "false",
            "multivalued": "true"
          }
        }
      ],
      "defaultClientScopes": [
        "basic"
      ],
      "optionalClientScopes": [
        "address"
      ]
    },
    {
      "clientId": "cryptomatorhub-system",
      "serviceAccountsEnabled": true,
      "publicClient": false,
      "name": "Katta Server System",
      "enabled": true,
      "clientAuthenticatorType": "client-secret",
      "secret": {{ include "katta-server.resolvedSystemClientSecret" . | quote }},
      "standardFlowEnabled": false,
      "fullScopeAllowed": true,
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
        }
      ]
    }
  ],
  "browserSecurityHeaders": {
    "contentSecurityPolicy": {{ printf "frame-src 'self'; frame-ancestors 'self' %s; object-src 'none';" $hubPublicUrl | quote }}
  }
}
{{- end -}}

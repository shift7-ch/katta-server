Cipherduck hub setup admin documentation
========================================


MinIO
-----

### Setup MinIO

Documentation

* [MinIO OpenID Connect Access Management](https://min.io/docs/minio/linux/administration/identity-access-management/oidc-access-management.html)
* [MinIO Client Reference `mc idp openid`](https://min.io/docs/minio/linux/reference/minio-mc/mc-idp-openid.html)
* [MinIO Security Token Service `AssumeRoleWithWebIdentity](https://min.io/docs/minio/linux/developers/security-token-service/AssumeRoleWithWebIdentity.html)

#### Policy and OIDC provider for MinIO

Add role for creating buckets with prefix `cipherduck` and uploading `vault.cryptomator`, as well as RW to access to
buckets through `client_id` claim in JWT token. Adapt bucket prefix in

* [minio/cipherduck.json](setup%2Fminio%2Fcipherduckpolicy.json).

Side-note: MinIO does not allow for multiple OIDC providers with the same client ID:

> mc: <ERROR> Unable to add OpenID IDP config to server. Client ID XYZ is present with multiple OpenID configurations.

This is not a problem as we leave the claim specifying the vault unset or pointing to a non-existing vault.

```shell
mc alias set myminio http://127.0.0.1:9000 minioadmin minioadmin
mc admin policy create myminio cipherduckcreatebucket src/main/resources/cipherduck/setup/minio_sts/createbucketpolicy.json
mc admin policy create myminio cipherduckaccessbucket src/main/resources/cipherduck/setup/minio_sts/accessbucketpolicy.json
```

Add a new OIDC provider using the policy:

vault creation and vault access policy in minio

```shell
mc idp openid add myminio cryptomator \
    config_url="https://testing.hub.cryptomator.org/kc/realms/cipherduck/.well-known/openid-configuration" \
    client_id="cryptomator" \
    client_secret="ignore-me" \
    role_policy="cipherduckcreatebucket"
mc idp openid add myminio cryptomatorhub \
    config_url="https://testing.hub.cryptomator.org/kc/realms/cipherduck/.well-known/openid-configuration" \
    client_id="cryptomatorhub" \
    client_secret="ignore-me" \
    role_policy="cipherduckcreatebucket"    
mc idp openid add myminio cryptomatorvaults \
    config_url="https://testing.hub.cryptomator.org/kc/realms/cipherduck/.well-known/openid-configuration" \
    client_id="cryptomatorvaults" \
    client_secret="ignore-me" \
    role_policy="cipherduckaccessbucket"    
mc admin service restart myminio
```

Extract the policy ARN:

```shell
mc idp openid ls myminio 
╭──────────────────────────────────────────────────────────────────────────╮
│ On?        Name                             RoleARN                      │
│ 🔴           (default)                                                   │
│ 🟢         cryptomator  arn:minio:iam:::role/IqZpDC5ahW_DCAvZPZA4ACjEnDE │
│ 🟢      cryptomatorhub  arn:minio:iam:::role/HGKdlY4eFFsXVvJmwlMYMhmbnDE │
│ 🟢   cryptomatorvaults  arn:minio:iam:::role/Hdms6XDZ6oOpuWYI3gu4gmgHN94 │
╰──────────────────────────────────────────────────────────────────────────╯


 mc idp openid info myminio cryptomator
╭─────────────────────────────────────────────────────────────────────────────────────────────────────────╮
│    client_id: cryptomator                                                                               │
│client_secret: ignore-me                                                                                 │
│   config_url: https://testing.hub.cryptomator.org/kc/realms/cipherduck/.well-known/openid-configuration │
│       enable: on                                                                                        │
│      roleARN: arn:minio:iam:::role/IqZpDC5ahW_DCAvZPZA4ACjEnDE                                          │
│  role_policy: cipherduckcreatebucket                                                                    │
╰─────────────────────────────────────────────────────────────────────────────────────────────────────────╯

```

### Hub configuration

See [application.properties](config%2Fapplication.properties)

AWS
---

### Setup AWS

#### Setup AWS: OIDC provider

Documentation: https://docs.aws.amazon.com/IAM/latest/UserGuide/id_roles_providers_create_oidc_verify-thumbprint.html

```shell
openssl s_client -servername testing.hub.cryptomator.org -showcerts -connect testing.hub.cryptomator.org:443 > testing.hub.cryptomator.org.crt

vi testing.hub.cryptomator.org.crt ...

cat testing.hub.cryptomator.org.crt
-----BEGIN CERTIFICATE-----
MIIGBDCCBOygAwIBAgISA1CGKN3OkGJihg/qGhz2fl3fMA0GCSqGSIb3DQEBCwUA
MDIxCzAJBgNVBAYTAlVTMRYwFAYDVQQKEw1MZXQncyBFbmNyeXB0MQswCQYDVQQD
EwJSMzAeFw0yMzExMTIxMzAyMTdaFw0yNDAyMTAxMzAyMTZaMCYxJDAiBgNVBAMT
G3Rlc3RpbmcuaHViLmNyeXB0b21hdG9yLm9yZzCCAiIwDQYJKoZIhvcNAQEBBQAD
ggIPADCCAgoCggIBALWWmJr7lckOPCysl8p8FywJ2BwfCfdqMqTeb7KdOa3Zd9kb
rb0dYUAs6cs4XKIxSBzKTDJAZiE5d2/iXUgHIBS8hDjG8U40EFaKDTc/JugOSovs
HB6FQTi4YCMNfm3oMBiREMXYQTEKErBFfECbtGw8mTua2suT6Uc7lwj91qbPO6BN
TROk0Az1NcifYOz8lMZhelg0WXEa10YfalaKGtjh4srMBv0rT85PpXaJXaNp58Ls
4Psf/YlPjGJOhevnyAuqZouUD9sz7gZX8WvQ87y9uTXpDoarySh/0nppYLPZTDty
sI3LeVwwrf4ir5jObVgjkH1CdS8kj/ueKLLW0BBqSX/9oji9o1zFJlBeRcWbeW08
SD3+7292cy+zpNo3Y7xEFxGs0SVlJjTRk4cf6edkVq5QzTPqIF9FSn6tgXC6OTJi
ISHnLGvkuSOzCieADPwjlYJiix3duK+0rpeN3xH3/NnyvPnncbWr/KLwwGE/tsHx
orv1XLXkV0nmD9MDvE1gqRd7m7n3PwXEojz2Ih37i4bowFx2jYy6acAyY0KJSWwE
3Rl2BRvOqXY1AOZC2MKOp7mb3hbryr8pzUPb0j4p3iOmOG9MgUQydKLyE97W1Ucd
PRQMHdoG+EKnDeaauKdZ/3Lj0jMJ1CKlmYOB5qShHv1XCR5uimouioQkoJTFAgMB
AAGjggIeMIICGjAOBgNVHQ8BAf8EBAMCBaAwHQYDVR0lBBYwFAYIKwYBBQUHAwEG
CCsGAQUFBwMCMAwGA1UdEwEB/wQCMAAwHQYDVR0OBBYEFHkBSFhuApvRJvGqRHZg
5t183UMCMB8GA1UdIwQYMBaAFBQusxe3WFbLrlAJQOYfr52LFMLGMFUGCCsGAQUF
BwEBBEkwRzAhBggrBgEFBQcwAYYVaHR0cDovL3IzLm8ubGVuY3Iub3JnMCIGCCsG
AQUFBzAChhZodHRwOi8vcjMuaS5sZW5jci5vcmcvMCYGA1UdEQQfMB2CG3Rlc3Rp
bmcuaHViLmNyeXB0b21hdG9yLm9yZzATBgNVHSAEDDAKMAgGBmeBDAECATCCAQUG
CisGAQQB1nkCBAIEgfYEgfMA8QB2ADtTd3U+LbmAToswWwb+QDtn2E/D9Me9AA0t
cm/h+tQXAAABi8PXIB0AAAQDAEcwRQIhAPOlsQr63JOSMbTFWOM746oA7i4HQ+hl
p7M3pRpG4HYQAiBKqLSDsx1FdI18Fax3k7zkCgsY8x96ZAQvVUfdch0xoAB3AO7N
0GTV2xrOxVy3nbTNE6Iyh0Z8vOzew1FIWUZxH7WbAAABi8PXIBwAAAQDAEgwRgIh
AOZskIE18A5sTthKz6w3wMvIocbaoj3UCTCIAXWVJJNzAiEAmMWS709vLq/WOPG0
5hb6lBPn6NRnjizJaNEnj/ts71EwDQYJKoZIhvcNAQELBQADggEBADiSgsGpOKqZ
0kzeIS9x7vJlc3I0lnScB9JjxJyLoZFs//T4SNWE18zFxnzVspWRnwu4NTmuGURv
6RWJ8RAznYwjZCnVDdQREUSX7wahzGdz+3GalRaIYngkvwHOhT+aGLbrKRjz+Pfh
13qMStwjlfA6iSofHqVeQFCf48itgeVjNbpdZKEOLwdiV+JMwpT4n/i0nfVwWkaG
RcEWn8S4gfSq1iZ/LAhWdyB0QJ4EcCO6mx02wABxbQibPc5FM8Q64j37TizHniVu
hs+X7qFNDF/jvbob3sL09e0BLjiZWxVasAHiAAaZONTRV0N5YYV56F5br/vnegic
u3AvSS5HW70=
-----END CERTIFICATE-----


openssl x509 -in testing.hub.cryptomator.org.crt -fingerprint -sha1 -noout | sed -e 's/://g' | sed -e 's/[Ss][Hh][Aa]1 [Ff]ingerprint=//'
BE21B29075BF9F3265353F8B85208A8981DAEC2A

aws iam create-open-id-connect-provider --url https://testing.hub.cryptomator.org/kc/realms/cipherduck --client-id-list cryptomator cryptomatorhub  --thumbprint-list BE21B29075BF9F3265353F8B85208A8981DAEC2A
{
    "OpenIDConnectProviderArn": "arn:aws:iam::930717317329:oidc-provider/testing.hub.cryptomator.org/kc/realms/cipherduck1"
}

aws iam list-open-id-connect-providers

aws iam get-open-id-connect-provider --open-id-connect-provider-arn arn:aws:iam::930717317329:oidc-provider/testing.hub.cryptomator.org/kc/realms/cipherduck
{
    "Url": "testing.hub.cryptomator.org/kc/realms/cipherduck",
    "ClientIDList": [
        "cryptomatorhub",
        "cryptomator"
    ],
    "ThumbprintList": [
        "a053375bfe84e8b748782c7cee15827a6af5a405"
    ],
    "CreateDate": "2023-11-13T13:51:32.729000+00:00",
    "Tags": []
}
```

#### Setup AWS: roles

Add role for creating buckets with prefix `cipherduck` and uploading `vault.cryptomator`, adapt OIDC provider in trust
policy and bucket prefix in permission policy:

* [aws/createbuckettrustpolicy.json](./setup%2Faws%2Fcreatebuckettrustpolicy.json)
* [aws/createbucketpermissionpolicy.json](setup%2Faws%2Fcreatebucketpermissionpolicy.json)

Add roles for role chaining, adapt OIDC provider in trust policy and bucket prefix in permission policy:

* [aws/cipherduck_chain_01_trustpolicy.json](setup%2Faws%2Fcipherduck_chain_01_trustpolicy.json)
* [aws/cipherduck_chain_01_permissionpolicy.json](setup%2Faws%2Fcipherduck_chain_01_permissionpolicy.json)
* [aws/cipherduck_chain_02_trustpolicy.json](setup%2Faws%2Fcipherduck_chain_02_trustpolicy.json)
* [aws/cipherduck_chain_02_permissionpolicy.json](setup%2Faws%2Fcipherduck_chain_02_permissionpolicy.json)

```shell
aws iam create-role --role-name cipherduck-createbucket --assume-role-policy-document file://src/main/resources/cipherduck/setup/aws_stscreatebuckettrustpolicy.json
aws iam put-role-policy --role-name cipherduck-createbucket --policy-name cipherduck-createbucket --policy-document file://src/main/resources/cipherduck/setup/aws_stscreatebucketpermissionpolicy.json


aws iam create-role --role-name cipherduck_chain_01 --assume-role-policy-document file://src/main/resources/cipherduck/setup/aws_stscipherduck_chain_01_trustpolicy.json
aws iam put-role-policy --role-name cipherduck_chain_01 --policy-name cipherduck_chain_01 --policy-document file://src/main/resources/cipherduck/setup/aws_stscipherduck_chain_01_permissionpolicy.json

sleep 10;

aws iam create-role --role-name cipherduck_chain_02 --assume-role-policy-document file://src/main/resources/cipherduck/setup/aws_stscipherduck_chain_02_trustpolicy.json
aws iam put-role-policy --role-name cipherduck_chain_02 --policy-name cipherduck_chain_02 --policy-document file://src/main/resources/cipherduck/setup/aws_stscipherduck_chain_02_permissionpolicy.json
```

Checking roles:

```shell
aws iam get-role --role-name cipherduck-createbucket
aws iam get-role-policy --role-name cipherduck-createbucket --policy-name cipherduck-createbucket
```

```shell
TOKEN=`curl -v -X POST https://testing.hub.cryptomator.org/kc/realms/cipherduck/protocol/openid-connect/token \
     -H "Content-Type: application/x-www-form-urlencoded" \
     -d "client_id=cryptomator" \
     -d "scope=openid" \
     -d "grant_type=password" \
     -d "username=admin" \
     -d "password=$PASSWORD"    | jq ".id_token" | tr -d '"'`

jwtd $TOKEN
aws sts assume-role-with-web-identity --role-arn "arn:aws:iam::930717317329:role/cipherduck-createbucket" --role-session-name="blabla" --web-identity-token $TOKEN
```

### Hub configuration

See [application.properties](config%2Fapplication.properties). The configured prefix must match the ones configured in
the AWS/MinIO setup. Take the role arns from the AWS/MinIO setup.

### AWS cleanup

```shell
aws iam delete-role-policy --role-name cipherduck-createbucket --policy-name cipherduck-createbucket
aws iam delete-role --role-name cipherduck-createbucket 
aws iam delete-role-policy --role-name cipherduck_chain_01 --policy-name cipherduck_chain_01
aws iam delete-role --role-name cipherduck_chain_01
aws iam delete-role-policy --role-name cipherduck_chain_02 --policy-name cipherduck_chain_02
aws iam delete-role --role-name cipherduck_chain_02
```

Hub Configuration (`application.properties`) and Vault JWE
----------------------------------------------------------

### Introduction

| Term                                                                | Description                                                    | Usage in Cipherdu^ck                                                                                      |
|---------------------------------------------------------------------|----------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------|
| [Bookmark](https://docs.cyberduck.io/cyberduck/bookmarks/)          | Refers to connection profile/profile and adds properties like. | For hubs and vaults.                                                                                      |
| [Protocol](https://docs.cyberduck.io/protocols/)                    |                                                                | `hub` and `s3-sts`                                                                                        |
| [Connection Profile](https://docs.cyberduck.io/protocols/profiles/) | Refers to protocol and overrides properties                    | Used internally.                                                                                          |
| Vault [JWE](https://datatracker.ietf.org/doc/html/rfc7516)          | JSON Web Encryption for encrypted JSON-based data structures   | Contains the vault masterkey for decrypting data plus all information required to create vault bookmarks. |

Note that properties in `application.properties` use dashed notation instead of Camel Case in JWE and Java Dtos,
see [Quarkus Config Reference Guid](https://quarkus.io/guides/config-reference) for details.

```
curl -X PUT http://localhost:8080/api/storageprofile/ -d @setup/minio_sts/minio_sts_profile.json -v  -H "Content-Type: application/json"
curl -X PUT http://localhost:8080/api/storageprofile/ -d @setup/minio_static/minio_static_profile.json -v  -H "Content-Type: application/json"
curl -X PUT http://localhost:8080/api/storageprofile/ -d @setup/aws_static/aws_static_profile.json -v  -H "Content-Type: application/json"
curl -X PUT http://localhost:8080/api/storageprofile/ -d @setup/aws_sts/aws_sts_profile.json -v  -H "Content-Type: application/json"
curl  http://localhost:8080/api/storageprofile/
```

### (0) backend configuration

TODO https://github.com/chenkins/cipherduck-hub/issues/31 do we need both? How do we localization in dropdown?

| Backend property | Description                                 |
|------------------|---------------------------------------------|
| `id`             | Technical identifier for a backend          |
| `name`           | Displayed when choosing type of a new vault |

#### (0a) bucket creation

| Backend property             | Description                                                                                      |
|------------------------------|--------------------------------------------------------------------------------------------------|
| `bucketPrefix`               | Prefix for all new buckets.                                                                      |
| `stsRoleArnHub`              | Role for `AssumeRoleWithWebIdentity` when creating buckets in hub.                               |
| `stsRoleArnClient`           | Role for `AssumeRoleWithWebIdentity` when creating buckets in client.                            |
| `stsEndpoint`                | Endpoint `AssumeRoleWithWebIdentity` when creating buckets. Leave empty for defaults in AWS SDK. |
| `region`                     | Default region to create buckets in. Defaults to `us-east-1` if left empty.                      |
| `regions`                    | Allowed regions to create buckets in. Defaults to full list of regions.                          |
| `withPathStyleAccessEnabled` | Configures the client to use path-style access for all S3 requests.                              |
| `s3Endpoint`                 | Configures the endpoint for template upload (in case of shared (i.e. non-STS) credentials).      |

### (1) bookmark properties

In the `jwe` section of the backend configurations, you can specify the connection profile with the following
properties:

| JWE attribute | Description                                                                  | Source                               | 
|---------------|------------------------------------------------------------------------------|--------------------------------------|
| `protocol`    | &rarr; [Cyberduck Bookmarks](https://docs.cyberduck.io/cyberduck/bookmarks/) | `backend.jwe.protocol`               |
| `provider`    | &rarr; [Cyberduck Bookmarks](https://docs.cyberduck.io/cyberduck/bookmarks/) | `backend.jwe.provider`               |
| `hostname`    | &rarr; [Cyberduck Bookmarks](https://docs.cyberduck.io/cyberduck/bookmarks/) | `backend.jwe.hostname` or user input |
| `port`        | &rarr; [Cyberduck Bookmarks](https://docs.cyberduck.io/cyberduck/bookmarks/) | `backend.jwe.port` or user input     |
| `defaultPath` | &rarr; [Cyberduck Bookmarks](https://docs.cyberduck.io/cyberduck/bookmarks/) | `backend.bucket-prefix`              |
| `nickname`    | &rarr; [Cyberduck Bookmarks](https://docs.cyberduck.io/cyberduck/bookmarks/) | vault name (user input)              |
| `uuid`        | &rarr; [Cyberduck Bookmarks](https://docs.cyberduck.io/cyberduck/bookmarks/) | vault ID (generated)                 |

#### (2) hub-specific protocol settings (go into bookmark's custom properties)

The following hub-specific properties are injected into the vault JWE from  `/api/config` upon vault creation:

| JWE attribute           | Description                                                                                                                                                    | Source                                                     |
|-------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------|------------------------------------------------------------|
| `oAuthAuthorizationUrl` | &rarr; [Custom connection profile using OpenID Connect provider and AssumeRoleWithWebIdentity STS API](https://docs.cyberduck.io/protocols/profiles/aws_oidc/) | &rarr; `/api/config` &rarr; `keycloakTokenEndpoint`        | 
| `oAuthTokenUrl`         | &rarr; [Custom connection profile using OpenID Connect provider and AssumeRoleWithWebIdentity STS API](https://docs.cyberduck.io/protocols/profiles/aws_oidc/) | &rarr; `/api/config` &rarr;  `keycloakAuthEndpoint`        |         
| `oAuthClientId`         | &rarr; [Custom connection profile using OpenID Connect provider and AssumeRoleWithWebIdentity STS API](https://docs.cyberduck.io/protocols/profiles/aws_oidc/) | &rarr; `/api/config` &rarr;  `keycloakClientIdCryptomator` |         
| `stsEndpoint`           | &rarr; [Custom connection profile using OpenID Connect provider and AssumeRoleWithWebIdentity STS API](https://docs.cyberduck.io/protocols/profiles/aws_oidc/) | `backend.jwe.sts-endpoint`                                 |    
| `region`                | &rarr; [Cyberduck Bookmarks](https://docs.cyberduck.io/protocols/profiles/)                                                                                    | user input (dropdown)                                      |

### (3) boookmark custom properties

The following properties in the `jwe` section of backend configurations go into the  `Custom` properties section of
vault bookmarks:

| JWE attribute                | Description                                      | Source                                           | 
|------------------------------|--------------------------------------------------|--------------------------------------------------|
| `stsRoleArn`                 | Role for `AssumeRoleWithWebIdentity` call        | `backend.jwe.sts-role-arn`                       |
| `stsRoleArn2`                | Role for subsequent `AssumeRole` call            | `backend.jwe.sts-role-arn2`                      |
| `stsDurationSeconds`         | Time to life for the STS token                   | `backend.jwe.sts-duration-seconds`               |
| `parentUUID`                 | Hub ID used to group vault bookmarks by hub.     | hub UUID (injected)                              |
| `oAuthTokenExchangeAudience` | Client ID for exchanged tokens for STS profiles. | `hub.keycloak.oidc.cryptomator-vaults-client-id` |

### (4) keychain credentials

The following properties in the `jwe` section of backend configurations go into the credentials associated with a vault
bookmark (they are stored in OS keychain):

| JWE attribute | Description                                | Source     | 
|---------------|--------------------------------------------|------------|
| `username`    | Username for shared permanent credentials. | user input |
| `password`    | Password for shared permanent credentials. | user input |

### (5) misc

| JWE attribute          | Description                             | Source              | 
|------------------------|-----------------------------------------|---------------------|
| `automaticAccessGrant` | Should access be granted automatically? | user input checkbox |


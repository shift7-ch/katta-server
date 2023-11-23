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
mc admin policy create myminio cipherduckcreatebucket src/main/resources/cipherduck/setup/minio/createbucketpolicy.json
mc admin policy create myminio cipherduckaccessbucket src/main/resources/cipherduck/setup/minio/accessbucketpolicy.json
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
mc idp openid ls myminio                                                                                                                                                                                                                                                                                                    feature/cipherduck
╭──────────────────────────────────────────────────────────────────────────╮
│ On?        Name                             RoleARN                      │
│ 🔴           (default)                                                   │
│ 🟢         cryptomator  arn:minio:iam:::role/IqZpDC5ahW_DCAvZPZA4ACjEnDE │
│ 🟢      cryptomatorhub  arn:minio:iam:::role/HGKdlY4eFFsXVvJmwlMYMhmbnDE │
│ 🟢   cryptomatorvaults  arn:minio:iam:::role/Hdms6XDZ6oOpuWYI3gu4gmgHN94 │
╰──────────────────────────────────────────────────────────────────────────╯


 mc idp openid info myminio cryptomator                                                                                                                                                                                                                                                                                      feature/cipherduck
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
MIIFFjCCAv6gAwIBAgIRAJErCErPDBinU/bWLiWnX1owDQYJKoZIhvcNAQELBQAw
TzELMAkGA1UEBhMCVVMxKTAnBgNVBAoTIEludGVybmV0IFNlY3VyaXR5IFJlc2Vh
cmNoIEdyb3VwMRUwEwYDVQQDEwxJU1JHIFJvb3QgWDEwHhcNMjAwOTA0MDAwMDAw
WhcNMjUwOTE1MTYwMDAwWjAyMQswCQYDVQQGEwJVUzEWMBQGA1UEChMNTGV0J3Mg
RW5jcnlwdDELMAkGA1UEAxMCUjMwggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEK
AoIBAQC7AhUozPaglNMPEuyNVZLD+ILxmaZ6QoinXSaqtSu5xUyxr45r+XXIo9cP
R5QUVTVXjJ6oojkZ9YI8QqlObvU7wy7bjcCwXPNZOOftz2nwWgsbvsCUJCWH+jdx
sxPnHKzhm+/b5DtFUkWWqcFTzjTIUu61ru2P3mBw4qVUq7ZtDpelQDRrK9O8Zutm
NHz6a4uPVymZ+DAXXbpyb/uBxa3Shlg9F8fnCbvxK/eG3MHacV3URuPMrSXBiLxg
Z3Vms/EY96Jc5lP/Ooi2R6X/ExjqmAl3P51T+c8B5fWmcBcUr2Ok/5mzk53cU6cG
/kiFHaFpriV1uxPMUgP17VGhi9sVAgMBAAGjggEIMIIBBDAOBgNVHQ8BAf8EBAMC
AYYwHQYDVR0lBBYwFAYIKwYBBQUHAwIGCCsGAQUFBwMBMBIGA1UdEwEB/wQIMAYB
Af8CAQAwHQYDVR0OBBYEFBQusxe3WFbLrlAJQOYfr52LFMLGMB8GA1UdIwQYMBaA
FHm0WeZ7tuXkAXOACIjIGlj26ZtuMDIGCCsGAQUFBwEBBCYwJDAiBggrBgEFBQcw
AoYWaHR0cDovL3gxLmkubGVuY3Iub3JnLzAnBgNVHR8EIDAeMBygGqAYhhZodHRw
Oi8veDEuYy5sZW5jci5vcmcvMCIGA1UdIAQbMBkwCAYGZ4EMAQIBMA0GCysGAQQB
gt8TAQEBMA0GCSqGSIb3DQEBCwUAA4ICAQCFyk5HPqP3hUSFvNVneLKYY611TR6W
PTNlclQtgaDqw+34IL9fzLdwALduO/ZelN7kIJ+m74uyA+eitRY8kc607TkC53wl
ikfmZW4/RvTZ8M6UK+5UzhK8jCdLuMGYL6KvzXGRSgi3yLgjewQtCPkIVz6D2QQz
CkcheAmCJ8MqyJu5zlzyZMjAvnnAT45tRAxekrsu94sQ4egdRCnbWSDtY7kh+BIm
lJNXoB1lBMEKIq4QDUOXoRgffuDghje1WrG9ML+Hbisq/yFOGwXD9RiX8F6sw6W4
avAuvDszue5L3sz85K+EC4Y/wFVDNvZo4TYXao6Z0f+lQKc0t8DQYzk1OXVu8rp2
yJMC6alLbBfODALZvYH7n7do1AZls4I9d1P4jnkDrQoxB3UqQ9hVl3LEKQ73xF1O
yK5GhDDX8oVfGKF5u+decIsH4YaTw7mP3GFxJSqv3+0lUFJoi5Lc5da149p90Ids
hCExroL1+7mryIkXPeFM5TgO9r0rvZaBFOvV2z0gp35Z0+L4WPlbuEjN/lxPFin+
HlUjr8gRsI3qfJOQFy/9rKIJR0Y/8Omwt/8oTWgy1mdeHmmjk7j1nYsvC9JSQ6Zv
MldlTTKB3zhThV1+XWYp6rjd5JW1zbVWEkLNxE7GJThEUG3szgBVGP7pSWTUTsqX
nLRbwHOoq7hHwg==
-----END CERTIFICATE-----


openssl x509 -in testing.hub.cryptomator.org.crt -fingerprint -sha1 -noout | sed -e 's/://g' | sed -e 's/[Ss][Hh][Aa]1 [Ff]ingerprint=//'
A053375BFE84E8B748782C7CEE15827A6AF5A405

aws iam create-open-id-connect-provider --url https://testing.hub.cryptomator.org/kc/realms/cipherduck --client-id-list cryptomator cryptomatorhub  --thumbprint-list A053375BFE84E8B748782C7CEE15827A6AF5A405
{
    "OpenIDConnectProviderArn": "arn:aws:iam::930717317329:oidc-provider/testing.hub.cryptomator.org/kc/realms/cipherduck"
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
aws iam create-role --role-name cipherduck-createbucket --assume-role-policy-document file://src/main/resources/cipherduck/setup/aws/createbuckettrustpolicy.json
aws iam put-role-policy --role-name cipherduck-createbucket --policy-name cipherduck-createbucket --policy-document file://src/main/resources/cipherduck/setup/aws/createbucketpermissionpolicy.json


aws iam create-role --role-name cipherduck_chain_01 --assume-role-policy-document file://src/main/resources/cipherduck/setup/aws/cipherduck_chain_01_trustpolicy.json
aws iam put-role-policy --role-name cipherduck_chain_01 --policy-name cipherduck_chain_01 --policy-document file://src/main/resources/cipherduck/setup/aws/cipherduck_chain_01_permissionpolicy.json

sleep 10;

aws iam create-role --role-name cipherduck_chain_02 --assume-role-policy-document file://src/main/resources/cipherduck/setup/aws/cipherduck_chain_02_trustpolicy.json
aws iam put-role-policy --role-name cipherduck_chain_02 --policy-name cipherduck_chain_02 --policy-document file://src/main/resources/cipherduck/setup/aws/cipherduck_chain_02_permissionpolicy.json
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

### (0) backend configuration

TODO https://github.com/chenkins/cipherduck-hub/issues/31 do we need both? How do we localization/configuration in
dropdown?

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


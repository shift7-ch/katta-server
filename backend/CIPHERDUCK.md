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
Add role for creating buckets with prefix `cipherduck` and uploading `vault.cryptomator`, as well as RW to access to buckets through `aud` claim in JWT token: 
see 
[cipherduck.json](src%2Fmain%2Fresources%2Fcipherduck%2Fsetup%2Fminio%2Fcipherduckpolicy.json).

Side-note: MinIO does not allow for multiple OIDC providers with the same client ID:

> mc: <ERROR> Unable to add OpenID IDP config to server. Client ID XYZ is present with multiple OpenID configurations.

This is not a problem as we leave the claim specifying the vault unset or pointing to a non-existing vault.




```shell
mc alias set myminio http://127.0.0.1:9000 minioadmin minioadmin
mc admin policy create myminio cipherduck src/main/resources/cipherduck/setup/minio/cipherduckpolicy.json
```

Add a new OIDC provider using the policy:
```shell
mc idp openid add myminio cryptomator \
    config_url="http://keycloak:8180/realms/cryptomator/.well-known/openid-configuration" \
    client_id="cryptomator" \
    client_secret="ignore-me" \
    role_policy="cipherduck"
mc idp openid add myminio cryptomatorhub \
    config_url="http://keycloak:8180/realms/cryptomator/.well-known/openid-configuration" \
    client_id="cryptomatorhub" \
    client_secret="ignore-me" \
    role_policy="cipherduck" 
mc admin service restart myminio


mc idp openid add myminio cryptomator \
    config_url="https://login1.staging.cryptomator.cloud/realms/cipherduck/.well-known/openid-configuration" \
    client_id="cryptomator" \
    client_secret="ignore-me" \
    role_policy="cipherduck"
mc idp openid add myminio cryptomatorhub \
    config_url="https://login1.staging.cryptomator.cloud/realms/cipherduck/.well-known/openid-configuration" \
    client_id="cryptomatorhub" \
    client_secret="ignore-me" \
    role_policy="cipherduck"    
mc admin service restart myminio
```

Extract the policy ARN:
```shell
mc idp openid ls myminio                                                                                                                                                                     feature/cipherduck
╭───────────────────────────────────────────────────────────────────────╮
│ On?       Name                           RoleARN                      │
│ 🔴        (default)                                                   │
│ 🟢      cryptomator  arn:minio:iam:::role/IqZpDC5ahW_DCAvZPZA4ACjEnDE │
│ 🟢   cryptomatorhub  arn:minio:iam:::role/HGKdlY4eFFsXVvJmwlMYMhmbnDE │
╰───────────────────────────────────────────────────────────────────────╯


mc idp openid info myminio                                                                                                                                                                   feature/cipherduck
╭───────────────────────────────────────────────────────────────────────────────────────────────────────────────────────╮
│  client_id: cryptomator  (environment)                                                                                │
│ config_url: https://login1.staging.cryptomator.cloud/realms/cipherduck/.well-known/openid-configuration  (environment)│
│     enable: on                                                                                                        │
│    roleARN: arn:minio:iam:::role/IqZpDC5ahW_DCAvZPZA4ACjEnDE                                                          │
│role_policy: cryptomator  (environment)                                                                                │
╰───────────────────────────────────────────────────────────────────────────────────────────────────────────────────────╯
```


### Hub configuration

```properties
backends.backends[0].id=http://minio:9000
backends.backends[0].name=MinIO S3 STS
backends.backends[0].bucket-prefix=cipherduck
backends.backends[0].s3-type=minio
backends.backends[0].region=eu-central-1
backends.backends[0].jwe.protocol=s3
backends.backends[0].jwe.vendor=s3-sts-http
backends.backends[0].jwe.hostname=minio
backends.backends[0].jwe.port=9000
backends.backends[0].jwe.scheme=http
backends.backends[0].jwe.sts-endpoint=http://minio:9000
backends.backends[0].jwe.oauth-redirect-url=x-cipherduck-action:oauth
backends.backends[0].jwe.oauth-authorization-url=http://localhost:8180/realms/cryptomator/protocol/openid-connect/auth
backends.backends[0].jwe.oauth-token-url=http://localhost:8180/realms/cryptomator/protocol/openid-connect/token
backends.backends[0].jwe.oauth-client-id=cryptomator
backends.backends[0].jwe.authorization=AuthorizationCode
```

AWS
---



### Setup AWS

TODO https://github.com/chenkins/cipherduck-hub/issues/23 create OIDC provider

#### OIDC provider

```shell
aws iam list-open-id-connect-providers
aws iam get-open-id-connect-provider --open-id-connect-provider-arn "arn:aws:iam::930717317329:oidc-provider/login1.staging.cryptomator.cloud/realms/cipherduck"                                                                                                                                                      feature/cipherduck
{
    "Url": "login1.staging.cryptomator.cloud/realms/cipherduck",
    "ClientIDList": [
        "sts.amazonaws.com",
        "cipherduckclient",
        "cryptomator"
    ],
    "ThumbprintList": [
        "933c6ddee95c9c41a40f9f50493d82be03ad87bf"
    ],
    "CreateDate": "2023-06-21T12:13:03.042000+00:00",
    "Tags": []
}
```
Add role for creating buckets with prefix `cipherduck` and uploading `vault.cryptomator`, 
see 
[createbucketpermissionpolicy.json](src%2Fmain%2Fresources%2Fcipherduck%2Fsetup%2Faws%2Fcreatebucketpermissionpolicy.json) 
and 
[createbuckettrustpolicy.json](./src%2Fmain%2Fresources%2Fcipherduck%2Fsetup%2Faws%2Fcreatebuckettrustpolicy.json).

```shell
aws iam create-role --role-name cipherduck-createbucket --assume-role-policy-document file://src/main/resources/cipherduck/setup/createbuckettrustpolicy.json
aws iam put-role-policy --role-name cipherduck-createbucket --policy-name cipherduck-createbucket --policy-document file://src/main/resources/cipherduck/setup/createbucketpermissionpolicy.json
```

#### checking
```shell
aws iam get-role-policy --role-name cipherduck-createbucket
aws iam get-role-policy --role-name cipherduck-createbucket --policy-name cipherduck-createbucket
```


```shell
TOKEN=`curl -v -X POST https://login1.staging.cryptomator.cloud/realms/cipherduck/protocol/openid-connect/token \                                                 
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

```properties
backends.backends[1].id=https://sts.amazonaws.com
backends.backends[1].name=AWS S3 STS
backends.backends[1].bucket-prefix=cipherduck
backends.backends[1].s3-type=aws
backends.backends[1].oidc-provider=arn:aws:iam::XXXX:oidc-provider/login1.staging.cryptomator.cloud/realms/cipherduck
backends.backends[1].sts-role-arn-prefix=arn:aws:iam::XXXX:role/
backends.backends[1].region=eu-central-1
backends.backends[1].jwe.protocol=s3
backends.backends[1].jwe.vendor=s3-sts-https
backends.backends[1].jwe.oauth-redirect-url=x-cipherduck-action:oauth
backends.backends[1].jwe.oauth-authorization-url=https://login1.staging.cryptomator.cloud/realms/cipherduck/protocol/openid-connect/auth
backends.backends[1].jwe.oauth-token-url=https://login1.staging.cryptomator.cloud/realms/cipherduck/protocol/openid-connect/token
backends.backends[1].jwe.oauth-client-id=cryptomator
backends.backends[1].jwe.authorization=AuthorizationCode
```

### AWS cleanup
```shell
aws iam delete-role-policy --role-name cipherduck-createbucket --policy-name cipherduck-createbucket
aws iam delete-role --role-name cipherduck-createbucket 
```
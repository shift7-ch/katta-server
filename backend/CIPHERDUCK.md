Cipherduck hub setup admin documentation
========================================


MinIO
-----

### OIDC Provider

```
export MINIO_IDENTITY_OPENID_CONFIG_URL=https://login1.staging.cryptomator.cloud/realms/cipherduck/.well-known/openid-configuration
export MINIO_IDENTITY_OPENID_CLIENT_ID=cryptomator
export MINIO_IDENTITY_OPENID_CLAIM_NAME="amr"
minio server tmp_data --console-address :9001
```

### Configuration

```properties
backends.backends[0].id=http://minio:9000
backends.backends[0].name=MinIO STS
backends.backends[0].bucket-prefix=cipherduck
backends.backends[0].s3-type=minio
backends.backends[0].admin-access-key-id=minioadmin
backends.backends[0].admin-secret-key=minioadmin
backends.backends[0].jwe.protocol=s3
backends.backends[0].jwe.vendor=s3-sts
backends.backends[0].jwe.scheme=http
backends.backends[0].jwe.hostname=minio
backends.backends[0].jwe.port=9000
backends.backends[0].jwe.sts-endpoint=http://minio:9000
backends.backends[0].jwe.oauth-redirect-url=x-cipherduck-action:oauth
backends.backends[1].jwe.oauth-authorization-url=https://login1.staging.cryptomator.cloud/realms/cipherduck/protocol/openid-connect/auth
backends.backends[1].jwe.oauth-token-url=https://login1.staging.cryptomator.cloud/realms/cipherduck/protocol/openid-connect/token
backends.backends[0].jwe.oauth-client-id=cryptomator
backends.backends[0].jwe.authorization=AuthorizationCode
```

AWS
---

### OIDC provider

TODO create OIDC provider

```
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

### Configuration

```properties
backends.backends[1].id=https://sts.amazonaws.com
backends.backends[1].name=AWS S3
backends.backends[1].bucket-prefix=cipherduck
backends.backends[1].s3-type=aws
backends.backends[1].admin-access-key-id=XXXX
backends.backends[1].admin-secret-key=XXXX
backends.backends[1].oidc-provider=arn:aws:iam::XXXX:oidc-provider/login1.staging.cryptomator.cloud/realms/cipherduck
backends.backends[1].sts-role-arn-prefix=arn:aws:iam::XXXX:role/
backends.backends[1].region=eu-central-1
backends.backends[1].jwe.protocol=s3
backends.backends[1].jwe.vendor=s3-sts
backends.backends[1].jwe.oauth-redirect-url=x-cipherduck-action:oauth
backends.backends[1].jwe.oauth-authorization-url=https://login1.staging.cryptomator.cloud/realms/cipherduck/protocol/openid-connect/auth
backends.backends[1].jwe.oauth-token-url=https://login1.staging.cryptomator.cloud/realms/cipherduck/protocol/openid-connect/token
backends.backends[1].jwe.oauth-client-id=cryptomator
backends.backends[1].jwe.authorization=AuthorizationCode
```
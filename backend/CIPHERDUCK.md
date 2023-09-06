Cipherduck hub setup admin documentation
========================================

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

MinIO
-----

### OIDC Provider

```
export MINIO_IDENTITY_OPENID_CONFIG_URL=https://login1.staging.cryptomator.cloud/realms/cipherduck/.well-known/openid-configuration
export MINIO_IDENTITY_OPENID_CLIENT_ID=cryptomator
export MINIO_IDENTITY_OPENID_CLAIM_NAME="amr"
minio server tmp_data --console-address :9001
```


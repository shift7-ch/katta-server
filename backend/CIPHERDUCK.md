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
* [minio/cipherduck.json](src%2Fmain%2Fresources%2Fcipherduck%2Fsetup%2Fminio%2Fcipherduckpolicy.json).

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

See [application.properties](config%2Fapplication.properties)

AWS
---

### Setup AWS

#### Setup AWS: OIDC provider

Documentation: https://docs.aws.amazon.com/IAM/latest/UserGuide/id_roles_providers_create_oidc_verify-thumbprint.html

```shell
openssl s_client -servername login1.staging.cryptomator.cloud -showcerts -connect login1.staging.cryptomator.cloud:443

cat login1.staging.cryptomator.cloud.crt
-----BEGIN CERTIFICATE-----
MIIFYDCCBEigAwIBAgIQQAF3ITfU6UK47naqPGQKtzANBgkqhkiG9w0BAQsFADA/
MSQwIgYDVQQKExtEaWdpdGFsIFNpZ25hdHVyZSBUcnVzdCBDby4xFzAVBgNVBAMT
DkRTVCBSb290IENBIFgzMB4XDTIxMDEyMDE5MTQwM1oXDTI0MDkzMDE4MTQwM1ow
TzELMAkGA1UEBhMCVVMxKTAnBgNVBAoTIEludGVybmV0IFNlY3VyaXR5IFJlc2Vh
cmNoIEdyb3VwMRUwEwYDVQQDEwxJU1JHIFJvb3QgWDEwggIiMA0GCSqGSIb3DQEB
AQUAA4ICDwAwggIKAoICAQCt6CRz9BQ385ueK1coHIe+3LffOJCMbjzmV6B493XC
ov71am72AE8o295ohmxEk7axY/0UEmu/H9LqMZshftEzPLpI9d1537O4/xLxIZpL
wYqGcWlKZmZsj348cL+tKSIG8+TA5oCu4kuPt5l+lAOf00eXfJlII1PoOK5PCm+D
LtFJV4yAdLbaL9A4jXsDcCEbdfIwPPqPrt3aY6vrFk/CjhFLfs8L6P+1dy70sntK
4EwSJQxwjQMpoOFTJOwT2e4ZvxCzSow/iaNhUd6shweU9GNx7C7ib1uYgeGJXDR5
bHbvO5BieebbpJovJsXQEOEO3tkQjhb7t/eo98flAgeYjzYIlefiN5YNNnWe+w5y
sR2bvAP5SQXYgd0FtCrWQemsAXaVCg/Y39W9Eh81LygXbNKYwagJZHduRze6zqxZ
Xmidf3LWicUGQSk+WT7dJvUkyRGnWqNMQB9GoZm1pzpRboY7nn1ypxIFeFntPlF4
FQsDj43QLwWyPntKHEtzBRL8xurgUBN8Q5N0s8p0544fAQjQMNRbcTa0B7rBMDBc
SLeCO5imfWCKoqMpgsy6vYMEG6KDA0Gh1gXxG8K28Kh8hjtGqEgqiNx2mna/H2ql
PRmP6zjzZN7IKw0KKP/32+IVQtQi0Cdd4Xn+GOdwiK1O5tmLOsbdJ1Fu/7xk9TND
TwIDAQABo4IBRjCCAUIwDwYDVR0TAQH/BAUwAwEB/zAOBgNVHQ8BAf8EBAMCAQYw
SwYIKwYBBQUHAQEEPzA9MDsGCCsGAQUFBzAChi9odHRwOi8vYXBwcy5pZGVudHJ1
c3QuY29tL3Jvb3RzL2RzdHJvb3RjYXgzLnA3YzAfBgNVHSMEGDAWgBTEp7Gkeyxx
+tvhS5B1/8QVYIWJEDBUBgNVHSAETTBLMAgGBmeBDAECATA/BgsrBgEEAYLfEwEB
ATAwMC4GCCsGAQUFBwIBFiJodHRwOi8vY3BzLnJvb3QteDEubGV0c2VuY3J5cHQu
b3JnMDwGA1UdHwQ1MDMwMaAvoC2GK2h0dHA6Ly9jcmwuaWRlbnRydXN0LmNvbS9E
U1RST09UQ0FYM0NSTC5jcmwwHQYDVR0OBBYEFHm0WeZ7tuXkAXOACIjIGlj26Ztu
MA0GCSqGSIb3DQEBCwUAA4IBAQAKcwBslm7/DlLQrt2M51oGrS+o44+/yQoDFVDC
5WxCu2+b9LRPwkSICHXM6webFGJueN7sJ7o5XPWioW5WlHAQU7G75K/QosMrAdSW
9MUgNTP52GE24HGNtLi1qoJFlcDyqSMo59ahy2cI2qBDLKobkx/J3vWraV0T9VuG
WCLKTVXkcGdtwlfFRjlBz4pYg1htmf5X6DYO8A4jqv2Il9DjXA6USbW1FzXSLr9O
he8Y4IWS6wY7bCkjCWDcRQJMEhg76fsO3txE+FiYruq9RUWhiF1myv4Q6W+CyBFC
Dfvp7OOGAN6dEOM4+qR9sdjoSYKEBpsr6GtPAQw4dy753ec5
-----END CERTIFICATE-----

openssl x509 -in login1.staging.cryptomator.cloud.crt -fingerprint -sha1 -noout | sed -e 's/://g' | sed -e 's/SHA1 Fingerprint=//'
933C6DDEE95C9C41A40F9F50493D82BE03AD87BF

aws iam create-open-id-connect-provider --url https://testing.hub.cryptomator.org/kc/realms/cipherduck/ --client-id-list cryptomator   --thumbprint-list DEF08E0D6AC5577D3436E4D6AA8E9F13721B00DD
{
    "OpenIDConnectProviderArn": "arn:aws:iam::930717317329:oidc-provider/login1.staging.cryptomator.cloud/realms/cipherduck"
}

aws iam list-open-id-connect-providers

aws iam get-open-id-connect-provider --open-id-connect-provider-arn "arn:aws:iam::930717317329:oidc-provider/login1.staging.cryptomator.cloud/realms/cipherduck"
{
    "Url": "login1.staging.cryptomator.cloud/realms/cipherduck",
    "ClientIDList": [
        "cryptomator"
    ],
    "ThumbprintList": [
        "933c6ddee95c9c41a40f9f50493d82be03ad87bf"
    ],
    "CreateDate": "2023-06-21T12:13:03.042000+00:00",
    "Tags": []
}
```

#### Setup AWS: roles

Add role for creating buckets with prefix `cipherduck` and uploading `vault.cryptomator`, adapt OIDC provider in trust policy and bucket prefix in permission policy:

* [aws/createbuckettrustpolicy.json](./src%2Fmain%2Fresources%2Fcipherduck%2Fsetup%2Faws%2Fcreatebuckettrustpolicy.json)
* [aws/createbucketpermissionpolicy.json](src%2Fmain%2Fresources%2Fcipherduck%2Fsetup%2Faws%2Fcreatebucketpermissionpolicy.json)


Add roles for role chaining, adapt OIDC provider in trust policy and bucket prefix in permission policy:

* [aws/cipherduck_chain_01_trustpolicy.json](src%2Fmain%2Fresources%2Fcipherduck%2Fsetup%2Faws%2Fcipherduck_chain_01_trustpolicy.json)
* [aws/cipherduck_chain_01_permissionpolicy.json](src%2Fmain%2Fresources%2Fcipherduck%2Fsetup%2Faws%2Fcipherduck_chain_01_permissionpolicy.json)
* [aws/cipherduck_chain_02_trustpolicy.json](src%2Fmain%2Fresources%2Fcipherduck%2Fsetup%2Faws%2Fcipherduck_chain_02_trustpolicy.json)
* [aws/cipherduck_chain_02_permissionpolicy.json](src%2Fmain%2Fresources%2Fcipherduck%2Fsetup%2Faws%2Fcipherduck_chain_02_permissionpolicy.json)


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

See [application.properties](config%2Fapplication.properties). The configured prefix must match the ones configured in the AWS/MinIO setup. Take the role arns from the AWS/MinIO setup.


### AWS cleanup

```shell
aws iam delete-role-policy --role-name cipherduck-createbucket --policy-name cipherduck-createbucket
aws iam delete-role --role-name cipherduck-createbucket 
aws iam delete-role-policy --role-name cipherduck_chain_01 --policy-name cipherduck_chain_01
aws iam delete-role --role-name cipherduck_chain_01
aws iam delete-role-policy --role-name cipherduck_chain_02 --policy-name cipherduck_chain_02
aws iam delete-role --role-name cipherduck_chain_02
```
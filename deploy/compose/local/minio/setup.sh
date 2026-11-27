#!/bin/sh
# Creates the MinIO policies in policies/ (named after their files) and registers one OIDC provider per Keycloak client, since MinIO checks
# `azp == client_id` strictly. Idempotent: rerunning updates the policies and skips providers that already exist.
#
# policies/katta-{create,access}bucketpolicy.json mirror chart/templates/minio-policies-configmap.yaml with the `katta-` bucket prefix.
# The katta-static* policies repeat their actions for all `katta-*` buckets. Keep them in sync.
set -eu

# Last mc release published on GitHub (MinIO no longer publishes mc images or dl.min.io binaries); same as the chart's storage-profile seed Job.
MC_VERSION=RELEASE.2025-08-13T08-35-41Z
MC_SHA256_AMD64=01f866e9c5f9b87c2b09116fa5d7c06695b106242d829a8bb32990c00312e891
MC_SHA256_ARM64=14c8c9616cfce4636add161304353244e8de383b2e2752c0e9dad01d4c27c12c
CONFIG_URL="http://keycloak:8080/realms/$HUB_KEYCLOAK_REALM/.well-known/openid-configuration"
PROVIDERS="cryptomator:katta-createbucketpolicy cryptomatorhub:katta-createbucketpolicy cryptomatorvaults:katta-accessbucketpolicy"

case "$(uname -m)" in
  x86_64)  mc_arch=amd64; mc_sha256="$MC_SHA256_AMD64" ;;
  aarch64) mc_arch=arm64; mc_sha256="$MC_SHA256_ARM64" ;;
  *) echo "unsupported architecture: $(uname -m)" >&2; exit 1 ;;
esac
curl -fsSL --retry 5 --retry-all-errors -o /tmp/mc "https://github.com/minio/mc/releases/download/$MC_VERSION/mc.linux-$mc_arch.$MC_VERSION"
echo "$mc_sha256  /tmp/mc" | sha256sum -c -
chmod +x /tmp/mc
mc() { /tmp/mc --config-dir /tmp/.mc "$@"; }

mc alias set local "http://minio:$MINIO_PORT" minioadmin minioadmin

for policy in /setup/policies/*.json; do
  mc admin policy create local "$(basename "$policy" .json)" "$policy"
done

for entry in $PROVIDERS; do
  name="${entry%%:*}"
  if mc idp openid info local "$name" > /dev/null 2>&1; then
    echo "OIDC provider $name already registered"
  else
    mc idp openid add local "$name" config_url="$CONFIG_URL" client_id="$name" client_secret=ignore-me role_policy="${entry#*:}"
  fi
done

# role ARNs are computed only after a restart
mc --json admin service restart local
attempts=0
until mc admin info local > /dev/null 2>&1; do
  attempts=$((attempts + 1))
  if [ "$attempts" -ge 60 ]; then
    echo "MinIO did not come back after the restart" >&2
    exit 1
  fi
  sleep 2
done

# `info` looks a provider up by its exact name; the role ARN appears once MinIO has fetched Keycloak's discovery document
for entry in $PROVIDERS; do
  name="${entry%%:*}"
  if ! mc idp openid info local "$name" | grep -q "arn:minio:iam:::role/"; then
    echo "OIDC provider $name is not registered with a role ARN" >&2
    exit 1
  fi
  echo "OIDC provider $name registered"
done

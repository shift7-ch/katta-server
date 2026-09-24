import { mount } from '@vue/test-utils';
import { describe, expect, it, vi } from 'vitest';
import i18n from '../../src/i18n';

// --- mock plumbing -----------------------------------------------------------------------------
// vi.mock(...) is hoisted by vitest to the top of this file, above the `import CreateVault ...`
// below, so CreateVault.vue's own imports of these modules resolve to the mocks. vi.hoisted(...) is
// hoisted even further up (above the vi.mock calls themselves), which is what lets those calls
// close over `mocks` without a temporal-dead-zone error - see https://vitest.dev/api/vi#vi-hoisted.
// None of this is specific to what this test asserts; the fixture/expected values live below, in
// the test body.
const mocks = vi.hoisted(() => {
  const s3Send = vi.fn();
  return {
    settingsGetMock: vi.fn(),
    licenseGetUserInfoMock: vi.fn(),
    storageProfilesGetMock: vi.fn(),
    createOrUpdateVaultMock: vi.fn(),
    grantAccessMock: vi.fn(),
    s3Send,
    // vitest's mock fns are only constructible (usable with `new`, as the AWS SDK classes are) when
    // their implementation is a `function` expression, not an arrow function.
    s3ClientCtor: vi.fn(function () {
      return { send: s3Send };
    }),
    putObjectCommandCtor: vi.fn(function (input: Record<string, unknown>) {
      return { __type: 'PutObjectCommand', ...input };
    }),
    listObjectsV2CommandCtor: vi.fn(function (input: Record<string, unknown>) {
      return { __type: 'ListObjectsV2Command', ...input };
    }),
    getBucketLocationCommandCtor: vi.fn(function (input: Record<string, unknown>) {
      return { __type: 'GetBucketLocationCommand', ...input };
    })
  };
});

vi.mock('@aws-sdk/client-s3', () => ({
  S3Client: mocks.s3ClientCtor,
  PutObjectCommand: mocks.putObjectCommandCtor,
  ListObjectsV2Command: mocks.listObjectsV2CommandCtor,
  GetBucketLocationCommand: mocks.getBucketLocationCommandCtor,
  S3ServiceException: class S3ServiceException extends Error {}
}));

// unused by the S3STATIC flow this spec exercises, but CreateVault.vue imports it unconditionally.
vi.mock('@aws-sdk/client-sts', () => ({
  STSClient: vi.fn(),
  AssumeRoleWithWebIdentityCommand: vi.fn()
}));

// avoids the real module's eager top-level Keycloak init (a network call) running on import.
vi.mock('../../src/common/auth', () => ({
  default: Promise.resolve({ bearerToken: vi.fn().mockResolvedValue('test-token') })
}));

// avoids the real module's eager top-level `await ConfigWrapper.build()` (a network call to /config)
// running on import - reached transitively via backend.ts/vaultFormat8.ts/etc., even though
// CreateVault.vue itself only uses the (harmless, synchronous) absBackendBaseURL/backendBaseURL consts.
vi.mock('../../src/common/config', () => ({
  baseURL: '/',
  frontendBaseURL: '/app/',
  absFrontendBaseURL: 'http://localhost/app/',
  backendBaseURL: '/api/',
  absBackendBaseURL: 'http://localhost/api/',
  default: {
    get: () => ({}),
    reload: vi.fn().mockResolvedValue({}),
    serverTimeDiff: 0
  }
}));

// encryptForUser() imports this as an SPKI-encoded P-384 ECDH public key (see asPublicKey() in
// src/common/crypto.ts) - it must be a real, validly-encoded key or crypto.subtle.importKey() throws.
async function generateFakeEcdhPublicKeySpki(): Promise<Uint8Array> {
  const keyPair = await crypto.subtle.generateKey({ name: 'ECDH', namedCurve: 'P-384' }, true, ['deriveBits', 'deriveKey']);
  return new Uint8Array(await crypto.subtle.exportKey('spki', keyPair.publicKey));
}

vi.mock('../../src/common/userdata', () => ({
  default: {
    me: Promise.resolve({ id: 'user-1', setupCode: 'code', name: 'Test User' }),
    ecdhPublicKey: generateFakeEcdhPublicKeySpki()
  }
}));

vi.mock('../../src/common/backend', async (importOriginal) => {
  const actual = await importOriginal<typeof import('../../src/common/backend')>();
  return {
    ...actual,
    default: {
      ...actual.default,
      settings: { get: mocks.settingsGetMock },
      license: { getUserInfo: mocks.licenseGetUserInfoMock },
      storageprofiles: { get: mocks.storageProfilesGetMock },
      vaults: { createOrUpdateVault: mocks.createOrUpdateVaultMock, grantAccess: mocks.grantAccessMock }
    }
  };
});
// --- end mock plumbing --------------------------------------------------------------------------

import CreateVault from '../../src/components/CreateVault.vue';
import { UniversalVaultFormat } from '../../src/common/universalVaultFormat';

describe('CreateVault.vue - S3STATIC bucket name', () => {
  // https://github.com/shift7-ch/katta-server/issues/204
  // The bucket prefix configured on the storage profile is shown as a non-editable badge; the user
  // may only enter the suffix. The resulting bucket name (prefix + suffix) must be what is actually
  // written to S3 - this is what regressed before the fix (metadata.backend.bucket was persisted as
  // just the user-entered suffix, see CreateVault.vue's createVault()).
  it('prefixes the user-entered suffix with the storage profile bucketPrefix when writing the vault to S3', async () => {
    // a non-AWS endpoint so validateVaultDetails() skips its AWS-only GetBucketLocation/ListObjectsV2
    // pre-flight check; uploadVaultTemplate() (called from createVault()) still always goes through S3Client.
    const storageProfileFixture = {
      protocol: 'S3STATIC' as const,
      id: 'profile-1',
      name: 'Test S3 Profile',
      archived: false,
      endpoint: 'https://minio.example.com',
      pathStyleAccessEnabled: true,
      storageClass: 'STANDARD' as const,
      region: 'us-east-1',
      regions: [] as string[],
      bucketPrefix: 'katta-'
    };
    const enteredBucketSuffix = 'my-suffix';
    const expectedBucket = storageProfileFixture.bucketPrefix + enteredBucketSuffix;

    mocks.settingsGetMock.mockResolvedValue({
      hubId: 'hub-1',
      wotMaxDepth: 0,
      wotIdVerifyLen: 0,
      defaultRequiredEmergencyKeyShares: 0,
      defaultMinMembers: 0,
      allowChoosingEmergencyCouncil: false,
      emergencyCouncilMemberIds: [],
      enableEmergencyAccess: false,
      enableAutomaticAccessGrant: false,
      automaticAccessGrantTrustThreshold: 0,
      allowAutomaticAccessGrantOverride: false
    });
    mocks.licenseGetUserInfoMock.mockResolvedValue({ licensedSeats: 5, usedSeats: 1, expiresAt: undefined, gracePeriodEndsAt: undefined });
    mocks.storageProfilesGetMock.mockResolvedValue([storageProfileFixture]);
    mocks.createOrUpdateVaultMock.mockResolvedValue({ id: 'vault-1' });
    mocks.grantAccessMock.mockResolvedValue(undefined);
    mocks.s3Send.mockImplementation(async (command: { __type: string }) => {
      if (command.__type === 'ListObjectsV2Command') {
        return { Contents: [], KeyCount: 0 };
      }
      return {};
    });

    // the bucket that ends up in metadata.backend.bucket is encrypted into vault.uvfMetadataFile before
    // being sent anywhere, so it can't be asserted on via a mocked backend call - capture it directly
    // at the point createVault() hands it off for serialization/encryption instead.
    let capturedMetadataBucket: string | undefined;
    const originalCreateMetadataFile = UniversalVaultFormat.prototype.createMetadataFile;
    vi.spyOn(UniversalVaultFormat.prototype, 'createMetadataFile').mockImplementation(function (this: UniversalVaultFormat, ...args: Parameters<typeof originalCreateMetadataFile>) {
      capturedMetadataBucket = this.metadata.backend.bucket;
      return originalCreateMetadataFile.apply(this, args);
    });

    const wrapper = mount(CreateVault, {
      props: { recover: false },
      global: {
        plugins: [i18n],
        stubs: { RouterLink: true }
      }
    });

    // initialize() chains several awaits (settings/license/storage profiles fetch + real UVF crypto
    // creation); poll rather than guessing how many flushPromises() ticks that needs.
    await vi.waitFor(() => {
      expect(wrapper.find('#vaultBucketName').exists()).toBe(true);
    }, { timeout: 5000, interval: 25 });

    // the prefix must be displayed, non-editable, next to the suffix input
    expect(wrapper.text()).toContain(storageProfileFixture.bucketPrefix);

    await wrapper.find('#vaultName').setValue('Test Vault');
    await wrapper.find('#vaultAccessKeyId').setValue('AKIATESTKEY');
    await wrapper.find('#vaultSecretKey').setValue('sekrit');
    await wrapper.find('#vaultBucketName').setValue(enteredBucketSuffix);

    await wrapper.find('form').trigger('submit');

    // moved on to the recovery-key confirmation step, where createVault() is actually submitted
    await vi.waitFor(() => {
      expect(wrapper.find('#confirmRecoveryKey').exists()).toBe(true);
    }, { timeout: 5000, interval: 25 });
    await wrapper.find('#confirmRecoveryKey').setValue(true);
    await wrapper.find('form').trigger('submit');

    await vi.waitFor(() => {
      expect(mocks.createOrUpdateVaultMock).toHaveBeenCalled();
    }, { timeout: 5000, interval: 25 });

    // this is the regression check for #204: metadata.backend.bucket used to be persisted as the raw
    // suffix (just enteredBucketSuffix) instead of prefix + suffix.
    expect(capturedMetadataBucket).toBe(expectedBucket);

    expect(mocks.listObjectsV2CommandCtor).toHaveBeenCalledWith(expect.objectContaining({ Bucket: expectedBucket }));
    expect(mocks.putObjectCommandCtor).toHaveBeenCalledWith(expect.objectContaining({ Bucket: expectedBucket, Key: 'vault.uvf' }));
    // never called with just the raw suffix - that was the bug fixed for #204
    expect(mocks.putObjectCommandCtor).not.toHaveBeenCalledWith(expect.objectContaining({ Bucket: enteredBucketSuffix }));

    expect(mocks.createOrUpdateVaultMock).toHaveBeenCalledTimes(1);
    expect(mocks.grantAccessMock).toHaveBeenCalledTimes(1);
  });
});

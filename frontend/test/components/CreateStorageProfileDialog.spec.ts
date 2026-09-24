import { flushPromises, mount, VueWrapper } from '@vue/test-utils';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { nextTick } from 'vue';
import CreateStorageProfileDialog from '../../src/components/katta/CreateStorageProfileDialog.vue';
import i18n from '../../src/i18n';

const { createProfile } = vi.hoisted(() => ({ createProfile: vi.fn() }));

vi.mock('../../src/common/backend', () => ({
  default: { storageprofiles: { create: createProfile } },
  ConflictError: class extends Error {},
  ForbiddenError: class extends Error {}
}));

describe('CreateStorageProfileDialog', () => {
  let wrapper: VueWrapper<InstanceType<typeof CreateStorageProfileDialog>>;

  beforeEach(async () => {
    createProfile.mockReset().mockImplementation(async profile => profile);
    wrapper = mount(CreateStorageProfileDialog, {
      global: {
        plugins: [i18n],
        renderStubDefaultSlot: true,
        stubs: {
          Dialog: true,
          DialogOverlay: true,
          DialogPanel: true,
          DialogTitle: true
        }
      }
    });
    wrapper.vm.show();
    await nextTick();
  });

  afterEach(() => {
    wrapper.unmount();
    vi.restoreAllMocks();
  });

  it.each([
    { protocol: 'S3STATIC', provider: 'AWS', endpoint: 'https://s3.amazonaws.com', pathStyleAccessEnabled: false },
    { protocol: 'S3STS', provider: 'AWS', endpoint: 'https://s3.amazonaws.com', pathStyleAccessEnabled: false },
    { protocol: 'S3STATIC', provider: 'GENERIC', endpoint: 'https://minio.example.com', pathStyleAccessEnabled: true },
    { protocol: 'S3STS', provider: 'GENERIC', endpoint: 'https://minio.example.com', pathStyleAccessEnabled: true }
  ])('submits $provider with $protocol', async ({ protocol, provider, endpoint, pathStyleAccessEnabled }) => {
    await wrapper.get('#protocol').setValue(protocol);
    await wrapper.get('#provider').setValue(provider);
    await wrapper.get('#profileName').setValue('Test storage');
    await wrapper.get('#bucketPrefix').setValue('katta-');
    if (provider === 'GENERIC') {
      await wrapper.get('#endpoint').setValue(endpoint);
    }
    expect(wrapper.get<HTMLInputElement>('#endpoint').element.value).toBe(endpoint);
    expect(wrapper.get<HTMLInputElement>('#endpoint').element.readOnly).toBe(provider === 'AWS');
    expect(wrapper.get<HTMLInputElement>('#endpoint').element.required).toBe(true);
    if (protocol === 'S3STS') {
      await wrapper.get('#stsRoleCreateBucketClient').setValue('arn:aws:iam::123456789012:role/client');
      await wrapper.get('#stsRoleCreateBucketHub').setValue('arn:aws:iam::123456789012:role/hub');
      await wrapper.get('#stsRoleAccessBucketAssumeRoleWithWebIdentity').setValue('arn:aws:iam::123456789012:role/access');
    }

    await wrapper.get('form').trigger('submit');
    await flushPromises();

    expect(createProfile).toHaveBeenCalledExactlyOnceWith(expect.objectContaining({
      name: 'Test storage', protocol, endpoint, pathStyleAccessEnabled, bucketPrefix: 'katta-'
    }));
    expect(wrapper.emitted('created')).toEqual([[createProfile.mock.calls[0][0]]]);
  });

  it('clears the AWS endpoint when switching back to Generic', async () => {
    await wrapper.get('#endpoint').setValue('https://minio.example.com');
    await wrapper.get('#provider').setValue('AWS');
    expect(wrapper.get<HTMLInputElement>('#endpoint').element.value).toBe('https://s3.amazonaws.com');

    await wrapper.get('#provider').setValue('GENERIC');
    expect(wrapper.get<HTMLInputElement>('#endpoint').element.value).toBe('');
    expect(wrapper.get<HTMLInputElement>('#endpoint').element.readOnly).toBe(false);
  });

  it('resets the provider, protocol and endpoint when reopened', async () => {
    await wrapper.get('#protocol').setValue('S3STS');
    await wrapper.get('#provider').setValue('AWS');
    await wrapper.get('button[type="button"]').trigger('click');
    await vi.waitFor(() => expect(wrapper.find('form').exists()).toBe(false));

    wrapper.vm.show();
    await nextTick();

    expect(wrapper.get<HTMLSelectElement>('#protocol').element.value).toBe('S3STATIC');
    expect(wrapper.get<HTMLSelectElement>('#provider').element.value).toBe('GENERIC');
    expect(wrapper.get<HTMLInputElement>('#endpoint').element.value).toBe('');
    expect(wrapper.get<HTMLInputElement>('#endpoint').element.readOnly).toBe(false);
  });

  it.each(['', 'not-a-url'])('rejects an invalid generic endpoint in the submit handler (%j)', async endpoint => {
    vi.spyOn(console, 'error').mockImplementation(() => {});
    await wrapper.get('#endpoint').setValue(endpoint);

    // Trigger the handler directly; native browser constraint validation is not under test.
    await wrapper.get('form').trigger('submit');
    await flushPromises();

    expect(createProfile).not.toHaveBeenCalled();
    expect(wrapper.emitted('created')).toBeUndefined();
    expect(wrapper.text()).toContain(i18n.global.t('createStorageProfileDialog.error.invalidEndpoint'));
  });
});

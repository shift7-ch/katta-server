<template>
  <TransitionRoot as="template" :show="open" @after-leave="$emit('close')">
    <Dialog as="div" class="fixed z-10 inset-0 overflow-y-auto" @close="open = false">
      <TransitionChild as="template" enter="ease-out duration-300" enter-from="opacity-0" enter-to="opacity-100" leave="ease-in duration-200" leave-from="opacity-100" leave-to="opacity-0">
        <DialogOverlay class="fixed inset-0 bg-gray-500/75 transition-opacity" />
      </TransitionChild>

      <div class="fixed inset-0 z-10 overflow-y-auto">
        <div class="flex min-h-full items-end justify-center p-4 text-center sm:items-center sm:p-0">
          <TransitionChild as="template" enter="ease-out duration-300" enter-from="opacity-0 translate-y-4 sm:translate-y-0 sm:scale-95" enter-to="opacity-100 translate-y-0 sm:scale-100" leave="ease-in duration-200" leave-from="opacity-100 translate-y-0 sm:scale-100" leave-to="opacity-0 translate-y-4 sm:translate-y-0 sm:scale-95">
            <DialogPanel class="relative transform overflow-hidden rounded-lg bg-white text-left shadow-xl transition-all sm:my-8 sm:w-full sm:max-w-2xl">
              <form @submit.prevent="submit">
                <div class="bg-white px-4 pt-5 pb-4 sm:p-6 sm:pb-4">
                  <DialogTitle as="h3" class="text-lg leading-6 font-medium text-gray-900">
                    {{ t('createStorageProfileDialog.title') }}
                  </DialogTitle>
                  <p class="mt-2 text-sm text-gray-500">
                    {{ t('createStorageProfileDialog.description') }}
                  </p>

                  <div class="mt-5 grid grid-cols-6 gap-4">
                    <div class="col-span-6 sm:col-span-3">
                      <label for="protocol" class="block text-sm font-medium text-gray-700">{{ t('storageprofile.protocol') }} <span class="text-red-600">*</span></label>
                      <select id="protocol" v-model="protocol" :disabled="processing" class="mt-1 block w-full rounded-md border-gray-300 shadow-xs focus:ring-primary focus:border-primary sm:text-sm disabled:bg-gray-200">
                        <option v-for="p in protocols" :key="p" :value="p">{{ p }}</option>
                      </select>
                    </div>

                    <div class="col-span-6 sm:col-span-3">
                      <label for="profileName" class="block text-sm font-medium text-gray-700">{{ t('storageprofile.name') }} <span class="text-red-600">*</span></label>
                      <input id="profileName" v-model="state.name" :disabled="processing" type="text" required class="mt-1 block w-full rounded-md border-gray-300 shadow-xs focus:ring-primary focus:border-primary sm:text-sm disabled:bg-gray-200">
                    </div>

                    <!-- (1) Common: S3 endpoint URL — split into scheme/hostname/port at submit time -->
                    <div class="col-span-6">
                      <label for="endpoint" class="block text-sm font-medium text-gray-700">{{ t('createStorageProfileDialog.endpoint.label') }}</label>
                      <input id="endpoint" v-model="state.endpoint" :disabled="processing" type="url" placeholder="https://s3.example.com" class="mt-1 block w-full rounded-md border-gray-300 shadow-xs focus:ring-primary focus:border-primary sm:text-sm disabled:bg-gray-200">
                      <p class="mt-1 text-xs text-gray-500">{{ t('createStorageProfileDialog.endpoint.hint') }}</p>
                    </div>

                    <div class="col-span-6 sm:col-span-3 flex items-center">
                      <label class="inline-flex items-center mt-6">
                        <input v-model="state.withPathStyleAccessEnabled" :disabled="processing" type="checkbox" class="h-4 w-4 text-primary border-gray-300 rounded focus:ring-primary">
                        <span class="ml-2 text-sm text-gray-700">{{ t('storageprofile.withPathStyleAccessEnabled') }}</span>
                      </label>
                    </div>

                    <div class="col-span-6 sm:col-span-3">
                      <label for="storageClass" class="block text-sm font-medium text-gray-700">{{ t('storageprofile.storageClass') }} <span class="text-red-600">*</span></label>
                      <select id="storageClass" v-model="state.storageClass" :disabled="processing" class="mt-1 block w-full rounded-md border-gray-300 shadow-xs focus:ring-primary focus:border-primary sm:text-sm disabled:bg-gray-200">
                        <option v-for="c in storageClasses" :key="c" :value="c">{{ c }}</option>
                      </select>
                    </div>

                    <!-- region/regions are needed by the create-vault flow (AWS SDK region setting), so shown for both protocols -->
                    <div class="col-span-6 sm:col-span-3">
                      <label for="region" class="block text-sm font-medium text-gray-700">{{ t('storageprofile.region') }} <span class="text-red-600">*</span></label>
                      <input id="region" v-model="state.region" :disabled="processing" type="text" required placeholder="us-east-1" class="mt-1 block w-full rounded-md border-gray-300 shadow-xs focus:ring-primary focus:border-primary sm:text-sm disabled:bg-gray-200">
                    </div>
                    <div class="col-span-6 sm:col-span-3">
                      <label for="regions" class="block text-sm font-medium text-gray-700">{{ t('storageprofile.regions') }}</label>
                      <input id="regions" v-model="regionsCsv" :disabled="processing" type="text" placeholder="us-east-1,eu-west-1" class="mt-1 block w-full rounded-md border-gray-300 shadow-xs focus:ring-primary focus:border-primary sm:text-sm disabled:bg-gray-200">
                      <p class="mt-1 text-xs text-gray-500">{{ t('createStorageProfileDialog.hint.regions') }}</p>
                    </div>

                    <!-- (2) STS-only: bucket-creation config (Desktop client) -->
                    <template v-if="protocol === 'S3STS'">
                      <div class="col-span-6">
                        <hr class="border-gray-200 my-2">
                        <h4 class="text-sm font-semibold text-gray-700">{{ t('createStorageProfileDialog.section.bucketCreation') }}</h4>
                      </div>

                      <div class="col-span-6">
                        <label for="bucketPrefix" class="block text-sm font-medium text-gray-700">{{ t('storageprofile.bucketPrefix') }} <span class="text-red-600">*</span></label>
                        <input id="bucketPrefix" v-model="state.bucketPrefix" :disabled="processing" type="text" required class="mt-1 block w-full rounded-md border-gray-300 shadow-xs focus:ring-primary focus:border-primary sm:text-sm disabled:bg-gray-200">
                      </div>

                      <div class="col-span-6">
                        <label for="stsRoleCreateBucketClient" class="block text-sm font-medium text-gray-700">{{ t('storageprofile.stsRoleCreateBucketClient') }} <span class="text-red-600">*</span></label>
                        <input id="stsRoleCreateBucketClient" v-model="state.stsRoleCreateBucketClient" :disabled="processing" type="text" required class="mt-1 block w-full rounded-md border-gray-300 shadow-xs focus:ring-primary focus:border-primary sm:text-sm disabled:bg-gray-200">
                      </div>
                      <div class="col-span-6">
                        <label for="stsRoleCreateBucketHub" class="block text-sm font-medium text-gray-700">{{ t('storageprofile.stsRoleCreateBucketHub') }} <span class="text-red-600">*</span></label>
                        <input id="stsRoleCreateBucketHub" v-model="state.stsRoleCreateBucketHub" :disabled="processing" type="text" required class="mt-1 block w-full rounded-md border-gray-300 shadow-xs focus:ring-primary focus:border-primary sm:text-sm disabled:bg-gray-200">
                      </div>

                      <div class="col-span-6">
                        <label for="stsEndpoint" class="block text-sm font-medium text-gray-700">{{ t('storageprofile.stsEndpoint') }}</label>
                        <input id="stsEndpoint" v-model="state.stsEndpoint" :disabled="processing" type="text" class="mt-1 block w-full rounded-md border-gray-300 shadow-xs focus:ring-primary focus:border-primary sm:text-sm disabled:bg-gray-200">
                      </div>

                      <div class="col-span-6 sm:col-span-3 flex items-center">
                        <label class="inline-flex items-center mt-6">
                          <input v-model="state.bucketVersioning" :disabled="processing" type="checkbox" class="h-4 w-4 text-primary border-gray-300 rounded focus:ring-primary">
                          <span class="ml-2 text-sm text-gray-700">{{ t('storageprofile.bucketVersioning') }}</span>
                        </label>
                      </div>
                      <div class="col-span-6 sm:col-span-3 flex items-center">
                        <label class="inline-flex items-center mt-6">
                          <input v-model="state.bucketAcceleration" :disabled="processing" type="checkbox" class="h-4 w-4 text-primary border-gray-300 rounded focus:ring-primary">
                          <span class="ml-2 text-sm text-gray-700">{{ t('storageprofile.bucketAcceleration') }}</span>
                        </label>
                      </div>

                      <div class="col-span-6">
                        <label for="bucketEncryption" class="block text-sm font-medium text-gray-700">{{ t('storageprofile.bucketEncryption') }} <span class="text-red-600">*</span></label>
                        <select id="bucketEncryption" v-model="state.bucketEncryption" :disabled="processing" class="mt-1 block w-full rounded-md border-gray-300 shadow-xs focus:ring-primary focus:border-primary sm:text-sm disabled:bg-gray-200">
                          <option v-for="e in encryptionOptions" :key="e" :value="e">{{ e }}</option>
                        </select>
                      </div>

                      <!-- (3) STS-only: bucket-access config (token vending) -->
                      <div class="col-span-6">
                        <hr class="border-gray-200 my-2">
                        <h4 class="text-sm font-semibold text-gray-700">{{ t('createStorageProfileDialog.section.bucketAccess') }}</h4>
                      </div>

                      <div class="col-span-6">
                        <label for="stsRoleAccessBucketAssumeRoleWithWebIdentity" class="block text-sm font-medium text-gray-700">{{ t('storageprofile.stsRoleAccessBucketAssumeRoleWithWebIdentity') }} <span class="text-red-600">*</span></label>
                        <input id="stsRoleAccessBucketAssumeRoleWithWebIdentity" v-model="state.stsRoleAccessBucketAssumeRoleWithWebIdentity" :disabled="processing" type="text" required class="mt-1 block w-full rounded-md border-gray-300 shadow-xs focus:ring-primary focus:border-primary sm:text-sm disabled:bg-gray-200">
                      </div>
                      <div class="col-span-6">
                        <label for="stsRoleAccessBucketAssumeRoleTaggedSession" class="block text-sm font-medium text-gray-700">{{ t('storageprofile.stsRoleAccessBucketAssumeRoleTaggedSession') }}</label>
                        <input id="stsRoleAccessBucketAssumeRoleTaggedSession" v-model="state.stsRoleAccessBucketAssumeRoleTaggedSession" :disabled="processing" type="text" class="mt-1 block w-full rounded-md border-gray-300 shadow-xs focus:ring-primary focus:border-primary sm:text-sm disabled:bg-gray-200">
                      </div>
                      <div class="col-span-6 sm:col-span-3">
                        <label for="stsDurationSeconds" class="block text-sm font-medium text-gray-700">{{ t('storageprofile.stsDurationSeconds') }}</label>
                        <input id="stsDurationSeconds" v-model.number="state.stsDurationSeconds" :disabled="processing" type="number" min="0" class="mt-1 block w-full rounded-md border-gray-300 shadow-xs focus:ring-primary focus:border-primary sm:text-sm disabled:bg-gray-200">
                      </div>
                      <div class="col-span-6 sm:col-span-3">
                        <label for="stsSessionTag" class="block text-sm font-medium text-gray-700">{{ t('storageprofile.stsSessionTag') }} <span class="text-red-600">*</span></label>
                        <input id="stsSessionTag" v-model="state.stsSessionTag" :disabled="processing" type="text" required class="mt-1 block w-full rounded-md border-gray-300 shadow-xs focus:ring-primary focus:border-primary sm:text-sm disabled:bg-gray-200">
                      </div>
                    </template>
                  </div>
                </div>

                <div class="bg-gray-50 px-4 py-3 sm:px-6 sm:flex sm:flex-row-reverse">
                  <button type="submit" :disabled="processing" class="w-full inline-flex justify-center rounded-md border border-transparent shadow-xs px-4 py-2 bg-primary text-base font-medium text-white hover:bg-primary-d1 focus:outline-hidden focus:ring-2 focus:ring-offset-2 focus:ring-primary sm:ml-3 sm:w-auto sm:text-sm disabled:opacity-50">
                    {{ t('createStorageProfileDialog.confirm') }}
                  </button>
                  <button type="button" :disabled="processing" class="mt-3 w-full inline-flex justify-center rounded-md border border-gray-300 shadow-xs px-4 py-2 bg-white text-base font-medium text-gray-700 hover:bg-gray-50 focus:outline-hidden focus:ring-2 focus:ring-offset-2 focus:ring-primary sm:mt-0 sm:ml-3 sm:w-auto sm:text-sm" @click="open = false">
                    {{ t('common.cancel') }}
                  </button>
                </div>
                <p v-if="onSubmitError != null" class="text-sm text-red-900 px-4 sm:px-6 text-right bg-red-50 py-2">
                  <span v-if="onSubmitError instanceof ConflictError">{{ t('createStorageProfileDialog.error.conflict') }}</span>
                  <span v-else-if="onSubmitError instanceof ForbiddenError">{{ t('createStorageProfileDialog.error.forbidden') }}</span>
                  <span v-else-if="onSubmitError instanceof InvalidEndpointError">{{ onSubmitError.message }}</span>
                  <span v-else>{{ t('common.unexpectedError', [onSubmitError.message]) }}</span>
                </p>
              </form>
            </DialogPanel>
          </TransitionChild>
        </div>
      </div>
    </Dialog>
  </TransitionRoot>
</template>

<script setup lang="ts">
import { Dialog, DialogOverlay, DialogPanel, DialogTitle, TransitionChild, TransitionRoot } from '@headlessui/vue';
import { ref } from 'vue';
import { useI18n } from 'vue-i18n';
import backend, { ConflictError, ForbiddenError, S3ServerSideEncryption, S3StorageClass, StorageProfileDto, StorageProfileS3StaticDto, StorageProfileS3STSDto, StorageProtocol } from '../../common/backend';

const { t } = useI18n({ useScope: 'global' });

const open = ref(false);
const processing = ref(false);
const onSubmitError = ref<Error | null>();

const protocols: StorageProtocol[] = ['S3STATIC', 'S3STS'];
const storageClasses: S3StorageClass[] = ['STANDARD', 'INTELLIGENT_TIERING', 'STANDARD_IA', 'ONEZONE_IA', 'REDUCED_REDUNDANCY', 'GLACIER', 'GLACIER_IR', 'DEEP_ARCHIVE'];
const encryptionOptions: S3ServerSideEncryption[] = ['NONE', 'SSE_AES256', 'SSE_KMS_DEFAULT'];

type FormState = {
  name: string;
  endpoint: string;
  withPathStyleAccessEnabled: boolean;
  storageClass: S3StorageClass;
  region: string;
  bucketPrefix: string;
  stsRoleCreateBucketClient: string;
  stsRoleCreateBucketHub: string;
  stsEndpoint: string;
  bucketVersioning: boolean;
  bucketAcceleration: boolean;
  bucketEncryption: S3ServerSideEncryption;
  stsRoleAccessBucketAssumeRoleWithWebIdentity: string;
  stsRoleAccessBucketAssumeRoleTaggedSession: string;
  stsDurationSeconds: number | null;
  stsSessionTag: string;
};

type EndpointParts = {
  scheme?: string;
  hostname?: string;
  port?: number;
};

const protocol = ref<StorageProtocol>('S3STATIC');
const state = ref<FormState>(emptyState());
const regionsCsv = ref('');

const emit = defineEmits<{
  close: []
  created: [profile: StorageProfileDto]
}>();

defineExpose({ show });

function emptyState(): FormState {
  return {
    name: '',
    endpoint: '',
    withPathStyleAccessEnabled: false,
    storageClass: 'STANDARD',
    region: 'us-east-1',
    bucketPrefix: '',
    stsRoleCreateBucketClient: '',
    stsRoleCreateBucketHub: '',
    stsEndpoint: '',
    bucketVersioning: true,
    bucketAcceleration: false,
    bucketEncryption: 'NONE',
    stsRoleAccessBucketAssumeRoleWithWebIdentity: '',
    stsRoleAccessBucketAssumeRoleTaggedSession: '',
    stsDurationSeconds: null,
    stsSessionTag: 'Vault'
  };
}

function show() {
  protocol.value = 'S3STATIC';
  state.value = emptyState();
  regionsCsv.value = '';
  onSubmitError.value = null;
  processing.value = false;
  open.value = true;
}

function nullIfBlank(s: string): string | null {
  const trimmed = s.trim();
  return trimmed === '' ? null : trimmed;
}

function newId(): string {
  return typeof crypto !== 'undefined' && 'randomUUID' in crypto ? crypto.randomUUID() : '';
}

function parsedRegions(): string[] {
  const list = regionsCsv.value.split(',').map(r => r.trim()).filter(r => r.length > 0);
  // CreateVault.vue copies `regions` into the region picker — an empty list breaks vault creation,
  // so default to the single configured region.
  return list.length > 0 ? list : [state.value.region];
}

class InvalidEndpointError extends Error {}

// Parses the user-entered endpoint URL into scheme/hostname/port. Default scheme `https` and
// default port `443` are normalized to null so the backend stores them as "use the default".
function parseEndpoint(input: string): EndpointParts {
  const trimmed = input.trim();
  if (trimmed === '') {
    return {};
  }
  let url: URL;
  try {
    url = new URL(trimmed);
  } catch {
    throw new InvalidEndpointError(t('createStorageProfileDialog.error.invalidEndpoint'));
  }
  const scheme = url.protocol.replace(/:$/, '');
  const port = url.port === '' ? undefined : Number(url.port);
  return {
    scheme: scheme === 'https' ? undefined : scheme,
    hostname: url.hostname,
    port: port === 443 ? undefined : port
  };
}

function buildS3StaticDto(endpoint: EndpointParts): StorageProfileS3StaticDto {
  // bucketPrefix and stsRoleCreateBucket* are required by the JSON schema but unused for S3STATIC
  // per the backend's own DTO comments — submit empty values.
  return {
    id: newId(),
    name: state.value.name,
    protocol: 'S3STATIC',
    archived: false,
    scheme: endpoint.scheme,
    hostname: endpoint.hostname,
    port: endpoint.port,
    withPathStyleAccessEnabled: state.value.withPathStyleAccessEnabled,
    storageClass: state.value.storageClass,
    region: state.value.region,
    regions: parsedRegions(),
    bucketPrefix: '',
    stsRoleCreateBucketClient: '',
    stsRoleCreateBucketHub: '',
    stsEndpoint: null,
    bucketVersioning: true,
    bucketAcceleration: null,
    bucketEncryption: 'NONE'
  };
}

function buildS3STSDto(endpoint: EndpointParts): StorageProfileS3STSDto {
  return {
    id: newId(),
    name: state.value.name,
    protocol: 'S3STS',
    archived: false,
    scheme: endpoint.scheme,
    hostname: endpoint.hostname,
    port: endpoint.port,
    withPathStyleAccessEnabled: state.value.withPathStyleAccessEnabled,
    storageClass: state.value.storageClass,
    region: state.value.region,
    regions: parsedRegions(),
    bucketPrefix: state.value.bucketPrefix,
    stsRoleCreateBucketClient: state.value.stsRoleCreateBucketClient,
    stsRoleCreateBucketHub: state.value.stsRoleCreateBucketHub,
    stsEndpoint: nullIfBlank(state.value.stsEndpoint),
    bucketVersioning: state.value.bucketVersioning,
    bucketAcceleration: state.value.bucketAcceleration,
    bucketEncryption: state.value.bucketEncryption,
    stsRoleAccessBucketAssumeRoleWithWebIdentity: state.value.stsRoleAccessBucketAssumeRoleWithWebIdentity,
    stsRoleAccessBucketAssumeRoleTaggedSession: nullIfBlank(state.value.stsRoleAccessBucketAssumeRoleTaggedSession),
    stsDurationSeconds: state.value.stsDurationSeconds ?? null,
    stsSessionTag: state.value.stsSessionTag
  };
}

async function submit() {
  onSubmitError.value = null;
  processing.value = true;
  try {
    const endpoint = parseEndpoint(state.value.endpoint);
    const created = protocol.value === 'S3STATIC'
      ? await backend.storageprofiles.createS3Static(buildS3StaticDto(endpoint))
      : await backend.storageprofiles.createS3STS(buildS3STSDto(endpoint));
    emit('created', created);
    open.value = false;
  } catch (error) {
    console.error('Creating storage profile failed.', error);
    onSubmitError.value = error instanceof Error ? error : new Error('Unknown Error');
  } finally {
    processing.value = false;
  }
}
</script>

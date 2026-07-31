<template>
  <div v-if="storageprofile == null">
    <div v-if="onFetchError == null">
      {{ t('common.loading') }}
    </div>
    <div v-else>
      <FetchError :error="onFetchError" :retry="allowRetryFetch ? fetchData : undefined" />
    </div>
  </div>

  <div v-else class="pb-16 space-y-6">
    <div v-if="storageprofile.archived" class="rounded-md bg-yellow-50 p-4">
      <div class="flex">
        <div class="shrink-0">
          <ExclamationTriangleIcon class="h-5 w-5 text-yellow-400" aria-hidden="true" />
        </div>
        <p class="ml-3 text-sm text-yellow-700">{{ t('storageProfileDetails.warning.archived') }}</p>
      </div>
    </div>

    <dl class="divide-y divide-gray-100">
      <div class="px-4 py-6 sm:grid sm:grid-cols-3 sm:gap-4 sm:px-0">
        <dt class="text-sm/6 font-medium text-gray-900">Name</dt>
        <dd class="mt-1 text-sm/6 text-gray-700 sm:col-span-2 sm:mt-0">{{ storageprofile.name }}</dd>
      </div>

      <div class="px-4 py-6 sm:grid sm:grid-cols-3 sm:gap-4 sm:px-0">
        <dt class="text-sm/6 font-medium text-gray-900">Endpoint</dt>
        <dd class="mt-1 text-sm/6 text-gray-700 sm:col-span-2 sm:mt-0">{{ storageprofile.endpoint }}</dd>
      </div>

      <div class="px-4 py-6 sm:grid sm:grid-cols-3 sm:gap-4 sm:px-0">
        <dt class="text-sm/6 font-medium text-gray-900">Path Style Access</dt>
        <dd class="mt-1 text-sm/6 text-gray-700 sm:col-span-2 sm:mt-0">{{ storageprofile.pathStyleAccessEnabled ? 'Enabled' : 'Disabled' }}</dd>
      </div>

      <div class="px-4 py-6 sm:grid sm:grid-cols-3 sm:gap-4 sm:px-0">
        <dt class="text-sm/6 font-medium text-gray-900">Storage Class</dt>
        <dd class="mt-1 text-sm/6 text-gray-700 sm:col-span-2 sm:mt-0">{{ storageprofile.storageClass }}</dd>
      </div>

      <div class="px-4 py-6 sm:grid sm:grid-cols-3 sm:gap-4 sm:px-0">
        <dt class="text-sm/6 font-medium text-gray-900">Region</dt>
        <dd class="mt-1 text-sm/6 text-gray-700 sm:col-span-2 sm:mt-0">{{ storageprofile.region }}</dd>
      </div>

      <div class="px-4 py-6 sm:grid sm:grid-cols-3 sm:gap-4 sm:px-0">
        <dt class="text-sm/6 font-medium text-gray-900">Bucket Prefix</dt>
        <dd class="mt-1 text-sm/6 text-gray-700 sm:col-span-2 sm:mt-0">{{ storageprofile.bucketPrefix }}</dd>
      </div>

      <template v-if="storageprofile.protocol === 'S3STS'">
        <div class="px-4 py-6 sm:grid sm:grid-cols-3 sm:gap-4 sm:px-0">
          <dt class="text-sm/6 font-medium text-gray-900">STS Endpoint</dt>
          <dd class="mt-1 text-sm/6 text-gray-700 sm:col-span-2 sm:mt-0">{{ storageprofile.stsEndpoint }}</dd>
        </div>

        <div class="px-4 py-6 sm:grid sm:grid-cols-3 sm:gap-4 sm:px-0">
          <dt class="text-sm/6 font-medium text-gray-900">CreateBucket ARN (for Clients)</dt>
          <dd class="mt-1 text-sm/6 text-gray-700 sm:col-span-2 sm:mt-0">{{ storageprofile.stsRoleCreateBucketClient }}</dd>
        </div>

        <div class="px-4 py-6 sm:grid sm:grid-cols-3 sm:gap-4 sm:px-0">
          <dt class="text-sm/6 font-medium text-gray-900">CreateBucket ARN (for Hub)</dt>
          <dd class="mt-1 text-sm/6 text-gray-700 sm:col-span-2 sm:mt-0">{{ storageprofile.stsRoleCreateBucketHub }}</dd>
        </div>

        <div class="px-4 py-6 sm:grid sm:grid-cols-3 sm:gap-4 sm:px-0">
          <dt class="text-sm/6 font-medium text-gray-900">AssumeRoleWithWebIdentity ARN</dt>
          <dd class="mt-1 text-sm/6 text-gray-700 sm:col-span-2 sm:mt-0">{{ storageprofile.stsRoleAccessBucketAssumeRoleWithWebIdentity }}</dd>
        </div>

        <div class="px-4 py-6 sm:grid sm:grid-cols-3 sm:gap-4 sm:px-0">
          <dt class="text-sm/6 font-medium text-gray-900">AssumeRole ARN</dt>
          <dd class="mt-1 text-sm/6 text-gray-700 sm:col-span-2 sm:mt-0">{{ storageprofile.stsRoleAccessBucketAssumeRoleTaggedSession }}</dd>
        </div>

        <div class="px-4 py-6 sm:grid sm:grid-cols-3 sm:gap-4 sm:px-0">
          <dt class="text-sm/6 font-medium text-gray-900">STS Token Lifetime</dt>
          <dd class="mt-1 text-sm/6 text-gray-700 sm:col-span-2 sm:mt-0">{{ storageprofile.stsDurationSeconds }}</dd>
        </div>

        <div class="px-4 py-6 sm:grid sm:grid-cols-3 sm:gap-4 sm:px-0">
          <dt class="text-sm/6 font-medium text-gray-900">STS Session Tag</dt>
          <dd class="mt-1 text-sm/6 text-gray-700 sm:col-span-2 sm:mt-0">{{ storageprofile.stsSessionTag }}</dd>
        </div>
      </template>
    </dl>

    <div v-if="isAdmin" class="pt-4 border-t border-gray-200 space-y-3">
      <button v-if="!storageprofile.archived" type="button" class="w-full inline-flex justify-center bg-red-600 py-2 px-4 border border-transparent rounded-md shadow-xs text-sm font-medium text-white hover:bg-red-700 focus:outline-hidden focus:ring-2 focus:ring-offset-2 focus:ring-red-500" @click="openArchiveDialog">
        {{ t('storageProfileDetails.button.archive') }}
      </button>
      <button v-else type="button" class="w-full inline-flex justify-center bg-primary py-2 px-4 border border-transparent rounded-md shadow-xs text-sm font-medium text-white hover:bg-primary-d1 focus:outline-hidden focus:ring-2 focus:ring-offset-2 focus:ring-primary" @click="openReactivateDialog">
        {{ t('storageProfileDetails.button.reactivate') }}
      </button>
    </div>

    <ArchiveStorageProfileDialog v-if="archivingProfile && storageprofile" ref="archiveDialog" :profile="storageprofile" @close="archivingProfile = false" @archived="onArchivedOrReactivated"/>
    <ReactivateStorageProfileDialog v-if="reactivatingProfile && storageprofile" ref="reactivateDialog" :profile="storageprofile" @close="reactivatingProfile = false" @reactivated="onArchivedOrReactivated"/>
  </div>
</template>

<script setup lang="ts">
import { ExclamationTriangleIcon } from '@heroicons/vue/20/solid';
import { computed, nextTick, onMounted, ref } from 'vue';
import { useI18n } from 'vue-i18n';
import auth from '../../common/auth';
import backend, { NotFoundError, StorageProfileDto } from '../../common/backend';
import FetchError from '../FetchError.vue';
import ArchiveStorageProfileDialog from './ArchiveStorageProfileDialog.vue';
import ReactivateStorageProfileDialog from './ReactivateStorageProfileDialog.vue';

const { t } = useI18n({ useScope: 'global' });

const props = defineProps<{
  storageprofileId: string
}>();

const emit = defineEmits<{
  storageprofileUpdated: [updateStorageprofile: StorageProfileDto]
}>();

const onFetchError = ref<Error>();
const allowRetryFetch = computed(() => onFetchError.value && !(onFetchError.value instanceof NotFoundError));

const storageprofile = ref<StorageProfileDto>();
const isAdmin = ref(false);

const archivingProfile = ref(false);
const reactivatingProfile = ref(false);
const archiveDialog = ref<typeof ArchiveStorageProfileDialog>();
const reactivateDialog = ref<typeof ReactivateStorageProfileDialog>();

onMounted(async () => {
  isAdmin.value = (await auth).hasRole('admin');
  await fetchData();
});

async function fetchData() {
  onFetchError.value = undefined;
  try {
    storageprofile.value = await backend.storageprofiles.getSingle(props.storageprofileId);
  } catch (error) {
    console.error('Fetching data failed.', error);
    onFetchError.value = error instanceof Error ? error : new Error('Unknown Error');
  }
}

function openArchiveDialog() {
  archivingProfile.value = true;
  nextTick(() => archiveDialog.value?.show());
}

function openReactivateDialog() {
  reactivatingProfile.value = true;
  nextTick(() => reactivateDialog.value?.show());
}

function onArchivedOrReactivated(updated: StorageProfileDto) {
  storageprofile.value = updated;
  archivingProfile.value = false;
  reactivatingProfile.value = false;
  emit('storageprofileUpdated', updated);
}
</script>

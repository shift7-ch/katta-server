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
        <div class="flex-shrink-0">
          <ExclamationTriangleIcon class="h-5 w-5 text-yellow-400" aria-hidden="true" />
        </div>
        <p class="ml-3 text-sm text-yellow-700">{{ t('storageProfileDetails.warning.archived') }}</p>
      </div>
    </div>
    <div v-if="storageprofile['protocol'] == 'S3STATIC'">
      <div v-for="(item,key) in openapi.components.schemas.StorageProfileS3StaticDto.properties" :key="key">
        <h3 class="font-medium text-gray-900"><span v-if="!(item as OpenapiType).nullable" style="color: red;">* </span>{{ key }}</h3>
        <div class="mt-2 flex items-center justify-between">
          <p class="text-sm text-gray-600">{{ storageprofile[key] }}</p>
        </div>
        <div class="mt-2 flex items-center justify-between">
          <p class="text-sm text-gray-400">{{ item.description }}</p>
        </div>
        <div class="mt-2 flex items-center justify-between">
          <p class="text-sm text-gray-400">type: {{ openapi.components.schemas.StorageProfileS3StaticDto.properties[key].type }}</p>
        </div>
        <div v-if="(openapi.components.schemas.StorageProfileS3StaticDto.properties[key] as OpenapiType)?.allOf">
          <div class="mt-2 flex items-center justify-between">
            <p v-if="((openapi.components.schemas as OpenapiSchemas)?.[((openapi.components.schemas.StorageProfileS3StaticDto.properties[key] as OpenapiType)?.allOf?.[0].$ref.split('/').pop()) ?? ''] as OpenapiSchema)?.enum" class="text-sm text-gray-400">enum: {{ ((openapi.components.schemas as OpenapiSchemas)?.[((openapi.components.schemas.StorageProfileS3StaticDto.properties[key] as OpenapiType)?.allOf?.[0].$ref.split('/').pop()) ?? ''] as OpenapiSchema)?.enum }}</p>
          </div>
          <div class="mt-2 flex items-center justify-between">
            <p v-if="((openapi.components.schemas as OpenapiSchemas)?.[((openapi.components.schemas.StorageProfileS3StaticDto.properties[key] as OpenapiType)?.allOf?.[0].$ref.split('/').pop()) ?? ''] as OpenapiSchema)?.pattern" class="text-sm text-gray-400">pattern: {{ ((openapi.components.schemas as OpenapiSchemas)?.[((openapi.components.schemas.StorageProfileS3StaticDto.properties[key] as OpenapiType)?.allOf?.[0].$ref.split('/').pop()) ?? ''] as OpenapiSchema)?.pattern }}</p>
          </div>
        </div>
        <div class="mt-2 flex items-center justify-between">
          <p v-if="(openapi.components.schemas.StorageProfileS3STSDto.properties[key] as OpenapiType)?.example" class="text-sm text-gray-400">example: {{ (openapi.components.schemas.StorageProfileS3STSDto.properties[key] as OpenapiType)?.example }}</p>
        </div>
        <br />
      </div>
    </div>
    <div v-if="storageprofile['protocol'] == 'S3STS'">
      <div v-for="(item,key) in openapi.components.schemas.StorageProfileS3STSDto.properties" :key="key">
        <h3 class="font-medium text-gray-900"><span v-if="!(item as OpenapiType).nullable" style="color: red;">* </span>{{ key }}</h3>
        <div class="mt-2 flex items-center justify-between">
          <p class="text-sm text-gray-600">{{ storageprofile[key] }}</p>
        </div>
        <div class="mt-2 flex items-center justify-between">
          <p class="text-sm text-gray-400">{{ item.description }}</p>
        </div>
        <div class="mt-2 flex items-center justify-between">
          <p class="text-sm text-gray-400">type: {{ openapi.components.schemas.StorageProfileS3STSDto.properties[key].type }}</p>
        </div>
        <div v-if="(openapi.components.schemas.StorageProfileS3STSDto.properties[key] as OpenapiType)?.allOf">
          <div class="mt-2 flex items-center justify-between">
            <p v-if="((openapi.components.schemas as OpenapiSchemas)?.[((openapi.components.schemas.StorageProfileS3STSDto.properties[key] as OpenapiType)?.allOf?.[0].$ref.split('/').pop()) ?? ''] as OpenapiSchema)?.enum" class="text-sm text-gray-400">enum: {{ ((openapi.components.schemas as OpenapiSchemas)?.[((openapi.components.schemas.StorageProfileS3STSDto.properties[key] as OpenapiType)?.allOf?.[0].$ref.split('/').pop()) ?? ''] as OpenapiSchema)?.enum }}</p>
          </div>
          <div class="mt-2 flex items-center justify-between">
            <p v-if="((openapi.components.schemas as OpenapiSchemas)?.[((openapi.components.schemas.StorageProfileS3STSDto.properties[key] as OpenapiType)?.allOf?.[0].$ref.split('/').pop()) ?? ''] as OpenapiSchema)?.pattern" class="text-sm text-gray-400">pattern: {{ ((openapi.components.schemas as OpenapiSchemas)?.[((openapi.components.schemas.StorageProfileS3STSDto.properties[key] as OpenapiType)?.allOf?.[0].$ref.split('/').pop()) ?? ''] as OpenapiSchema)?.pattern }}</p>
          </div>
        </div>
        <div class="mt-2 flex items-center justify-between">
          <p v-if="(openapi.components.schemas.StorageProfileS3STSDto.properties[key] as OpenapiType)?.example" class="text-sm text-gray-400">example: {{ (openapi.components.schemas.StorageProfileS3STSDto.properties[key] as OpenapiType)?.example }}</p>
        </div>
        <br />
      </div>
    </div>

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
import { openapi, OpenapiSchema, OpenapiSchemas, OpenapiType } from '../../openapi/index';
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

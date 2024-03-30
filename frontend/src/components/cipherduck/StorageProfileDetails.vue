<template>
  <div v-if="storageprofile == null">
    <div v-if="onFetchError == null">
      {{ t('common.loading') }}
    </div>
    <div v-else>
      <FetchError :error="onFetchError" :retry="allowRetryFetch ? fetchData : null"/>
    </div>
  </div>

  <div v-else class="pb-16 space-y-6">
    <div v-if="storageprofile.archived" class="rounded-md bg-yellow-50 p-4">
      <div class="flex">
        <div class="flex-shrink-0">
          <ExclamationTriangleIcon class="h-5 w-5 text-yellow-400" aria-hidden="true" />
        </div>
        <p class="ml-3 text-sm text-yellow-700">{{ t('vaultDetails.warning.archived') }}</p>
      </div>
    </div>
    <div v-if="storageprofile['protocol'] == 'S3STS'">
      <div v-for="(item,key) in openapi.components.schemas.StorageProfileS3STSDto.properties">
        <h3 class="font-medium text-gray-900"><span v-if="!item.nullable" style="color: red;">* </span>{{ key }}</h3>
        <div class="mt-2 flex items-center justify-between">
          <p class="text-sm text-gray-600">{{ storageprofile[key] }}</p>
        </div>
        <div class="mt-2 flex items-center justify-between">
          <p class="text-sm text-gray-400">{{ item.description }}</p>
        </div>
        <div class="mt-2 flex items-center justify-between">
          <p class="text-sm text-gray-400">type: {{ openapi.components.schemas.StorageProfileS3STSDto.properties[key].type }}</p>
        </div>
        <div v-if="openapi.components.schemas.StorageProfileS3STSDto.properties[key].allOf">
          <div class="mt-2 flex items-center justify-between">
            <p v-if="openapi.components.schemas[openapi.components.schemas.StorageProfileS3STSDto.properties[key].allOf[0].$ref.split('/').pop()].enum" class="text-sm text-gray-400">enum: {{ openapi.components.schemas[openapi.components.schemas.StorageProfileS3STSDto.properties[key].allOf[0].$ref.split('/').pop()].enum }}</p>
          </div>
          <div class="mt-2 flex items-center justify-between">
            <p v-if="openapi.components.schemas[openapi.components.schemas.StorageProfileS3STSDto.properties[key].allOf[0].$ref.split('/').pop()].pattern" class="text-sm text-gray-400">pattern: {{ openapi.components.schemas[openapi.components.schemas.StorageProfileS3STSDto.properties[key].allOf[0].$ref.split('/').pop()].pattern }}</p>
          </div>
        </div>
        <div class="mt-2 flex items-center justify-between" v-if="openapi.components.schemas.StorageProfileS3STSDto.properties[key].example">
          <p class="text-sm text-gray-400">Example: {{ openapi.components.schemas.StorageProfileS3STSDto.properties[key].example }}</p>
        </div>
        <br/>
      </div>
    </div>
    <div v-if="storageprofile['protocol'] == 'S3'">
      <div v-for="(item,key) in openapi.components.schemas.StorageProfileS3Dto.properties">
        <h3 class="font-medium text-gray-900"><span v-if="!item.nullable" style="color: red;">* </span>{{ key }}</h3>
        <div class="mt-2 flex items-center justify-between">
          <p class="text-sm text-gray-600">{{ storageprofile[key] }}</p>
        </div>
        <div class="mt-2 flex items-center justify-between">
          <p class="text-sm text-gray-400">{{ item.description }}</p>
        </div>
        <div class="mt-2 flex items-center justify-between">
          <p class="text-sm text-gray-400">type: {{ openapi.components.schemas.StorageProfileS3Dto.properties[key].type }}</p>
        </div>
        <div v-if="openapi.components.schemas.StorageProfileS3Dto.properties[key].allOf">
          <div class="mt-2 flex items-center justify-between">
            <p v-if="openapi.components.schemas[openapi.components.schemas.StorageProfileS3Dto.properties[key].allOf[0].$ref.split('/').pop()].enum" class="text-sm text-gray-400">enum: {{ openapi.components.schemas[openapi.components.schemas.StorageProfileS3Dto.properties[key].allOf[0].$ref.split('/').pop()].enum }}</p>
          </div>
          <div class="mt-2 flex items-center justify-between">
            <p v-if="openapi.components.schemas[openapi.components.schemas.StorageProfileS3Dto.properties[key].allOf[0].$ref.split('/').pop()].pattern" class="text-sm text-gray-400">pattern: {{ openapi.components.schemas[openapi.components.schemas.StorageProfileS3Dto.properties[key].allOf[0].$ref.split('/').pop()].pattern }}</p>
          </div>
        </div>
        <div class="mt-2 flex items-center justify-between" v-if="openapi.components.schemas.StorageProfileS3Dto.properties[key].example">
          <p class="text-sm text-gray-400">example: {{ openapi.components.schemas.StorageProfileS3Dto.properties[key].example }}</p>
        </div>
        <br/>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ExclamationTriangleIcon } from '@heroicons/vue/20/solid';
import { computed, onMounted, ref } from 'vue';
import { useI18n } from 'vue-i18n';
import backend, { NotFoundError, StorageProfileDto as StorageProfileDto2 } from '../../common/backend';
import FetchError from '../FetchError.vue';
import { openapi } from '../../openapi/index';


const { t } = useI18n({ useScope: 'global' });

const props = defineProps<{
  vaultId: string
}>();


const onFetchError = ref<Error | null>();
const allowRetryFetch = computed(() => onFetchError.value != null && !(onFetchError.value instanceof NotFoundError));  //fetch requests either list something, or query from th storageprofile In the latter, a 404 indicates the vault does not exists anymore.

const storageprofile = ref<StorageProfileDto2>();

onMounted(fetchData);

async function fetchData() {
  onFetchError.value = null;
  try {
    storageprofile.value = await backend.storageprofiles.getSingle(props.vaultId);
  } catch (error) {
    console.error('Fetching data failed.', error);
    onFetchError.value = error instanceof Error ? error : new Error('Unknown Error');
  }
}
</script>

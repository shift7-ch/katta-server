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
    <!--

    TODO https://github.com/shift7-ch/cipherduck-hub/issues/44
     - get description and default vaules and possible values from /q/openapi/openapi.json
     - add red start for mandatory etc.
     - filter for archived
     - fully dynamic: full list of attributes from openapi?
    -->

    <div>
      <h3 class="font-medium text-gray-900">{{ t('storageprofile.id') }}</h3>
      <div class="mt-2 flex items-center justify-between">
        <p class="text-sm text-gray-500">{{ storageprofile.id }}</p>
      </div>
    </div>
    <div>
      <h3 class="font-medium text-gray-900">{{ t('storageprofile.name') }}</h3>
      <div class="mt-2 flex items-center justify-between">
        <p class="text-sm text-gray-500">{{ storageprofile.name }}</p>
      </div>
    </div>
    <div>
      <h3 class="font-medium text-gray-900">{{ t('storageprofile.protocol') }}</h3>
      <div class="mt-2 flex items-center justify-between">
        <p class="text-sm text-gray-500">{{ storageprofile.protocol }}</p>
      </div>
    </div>
    <div v-if="storageprofile.protocol == 'S3STS'">
      <h3 class="font-medium text-gray-900">{{ t('storageprofile.bucketPrefix') }}</h3>
      <div class="mt-2 flex items-center justify-between">
        <p class="text-sm text-gray-500">{{ storageprofile.bucketPrefix }}</p>
      </div>
    </div>
    <div v-if="storageprofile.protocol == 'S3STS'">
      <h3 class="font-medium text-gray-900">{{ t('storageprofile.stsRoleArnClient') }}</h3>
      <div class="mt-2 flex items-center justify-between">
        <p class="text-sm text-gray-500">{{ storageprofile.stsRoleArnClient }}</p>
      </div>
    </div>
    <div v-if="storageprofile.protocol == 'S3STS'">
      <h3 class="font-medium text-gray-900">{{ t('storageprofile.stsRoleArnHub') }}</h3>
      <div class="mt-2 flex items-center justify-between">
        <p class="text-sm text-gray-500">{{ storageprofile.stsRoleArnHub }}</p>
      </div>
    </div>
    <div v-if="storageprofile.protocol == 'S3STS'">
      <h3 class="font-medium text-gray-900">{{ t('storageprofile.stsEndpoint') }}</h3>
      <div class="mt-2 flex items-center justify-between">
        <p class="text-sm text-gray-500">{{ storageprofile.stsEndpoint }}</p>
      </div>
    </div>
    <div>
      <h3 class="font-medium text-gray-900">{{ t('storageprofile.withPathStyleAccessEnabled') }}</h3>
      <div class="mt-2 flex items-center justify-between">
        <p class="text-sm text-gray-500">{{ storageprofile.withPathStyleAccessEnabled }}</p>
      </div>
    </div>
    <div v-if="storageprofile.protocol == 'S3STS'">
      <h3 class="font-medium text-gray-900">{{ t('storageprofile.region') }}</h3>
      <div class="mt-2 flex items-center justify-between">
        <p class="text-sm text-gray-500">{{ storageprofile.region }}</p>
      </div>
    </div>
    <div v-if="storageprofile.protocol == 'S3STS'">
      <h3 class="font-medium text-gray-900">{{ t('storageprofile.regions') }}</h3>
      <div class="mt-2 flex items-center justify-between">
        <p class="text-sm text-gray-500">{{ storageprofile.regions }}</p>
      </div>
    </div>
    <div>
      <h3 class="font-medium text-gray-900">{{ t('storageprofile.scheme') }}</h3>
      <div class="mt-2 flex items-center justify-between">
        <p class="text-sm text-gray-500">{{ storageprofile.scheme }}</p>
      </div>
    </div>
    <div>
      <h3 class="font-medium text-gray-900">{{ t('storageprofile.hostname') }}</h3>
      <div class="mt-2 flex items-center justify-between">
        <p class="text-sm text-gray-500">{{ storageprofile.hostname }}</p>
      </div>
    </div>
    <div>
      <h3 class="font-medium text-gray-900">{{ t('storageprofile.port') }}</h3>
      <div class="mt-2 flex items-center justify-between">
        <p class="text-sm text-gray-500">{{ storageprofile.port }}</p>
      </div>
    </div>
    <div>
      <h3 class="font-medium text-gray-900">{{ t('storageprofile.oauthClientId') }}</h3>
      <div class="mt-2 flex items-center justify-between">
        <p class="text-sm text-gray-500">{{ storageprofile.oauthClientId }}</p>
      </div>
    </div>
    <div>
      <h3 class="font-medium text-gray-900">{{ t('storageprofile.oauthTokenUrl') }}</h3>
      <div class="mt-2 flex items-center justify-between">
        <p class="text-sm text-gray-500">{{ storageprofile.oauthTokenUrl }}</p>
      </div>
    </div>
    <div>
      <h3 class="font-medium text-gray-900">{{ t('storageprofile.oauthAuthorizationUrl') }}</h3>
      <div class="mt-2 flex items-center justify-between">
        <p class="text-sm text-gray-500">{{ storageprofile.oauthAuthorizationUrl }}</p>
      </div>
    </div>
    <div v-if="storageprofile.protocol == 'S3STS'">
      <h3 class="font-medium text-gray-900">{{ t('storageprofile.stsRoleArn') }}</h3>
      <div class="mt-2 flex items-center justify-between">
        <p class="text-sm text-gray-500">{{ storageprofile.stsRoleArn }}</p>
      </div>
    </div>
    <div v-if="storageprofile.protocol == 'S3STS'">
      <h3 class="font-medium text-gray-900">{{ t('storageprofile.stsRoleArn2') }}</h3>
      <div class="mt-2 flex items-center justify-between">
        <p class="text-sm text-gray-500">{{ storageprofile.stsRoleArn2 }}</p>
      </div>
    </div>
    <div v-if="storageprofile.protocol == 'S3STS'">
      <h3 class="font-medium text-gray-900">{{ t('storageprofile.stsDurationSeconds') }}</h3>
      <div class="mt-2 flex items-center justify-between">
        <p class="text-sm text-gray-500">{{ storageprofile.stsDurationSeconds }}</p>
      </div>
    </div>
    <div>
      <h3 class="font-medium text-gray-900">{{ t('storageprofile.oAuthTokenExchangeAudience') }}</h3>
      <div class="mt-2 flex items-center justify-between">
        <p class="text-sm text-gray-500">{{ storageprofile.oAuthTokenExchangeAudience }}</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ExclamationTriangleIcon } from '@heroicons/vue/20/solid';
import { computed, onMounted, ref } from 'vue';
import { useI18n } from 'vue-i18n';
import backend, { StorageProfileDto, NotFoundError } from '../../common/backend';
import FetchError from '../FetchError.vue';


const { t } = useI18n({ useScope: 'global' });

const props = defineProps<{
  vaultId: string
}>();


const onFetchError = ref<Error | null>();
const allowRetryFetch = computed(() => onFetchError.value != null && !(onFetchError.value instanceof NotFoundError));  //fetch requests either list something, or query from th storageprofile In the latter, a 404 indicates the vault does not exists anymore.

const storageprofile = ref<StorageProfileDto>();

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

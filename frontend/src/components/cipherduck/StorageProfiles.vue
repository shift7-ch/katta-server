<template>
  <div v-if="storageprofiles == null">
    <div v-if="onFetchError == null">
      {{ t('common.loading') }}
    </div>
    <div v-else>
      <FetchError :error="onFetchError" :retry="fetchData"/>
    </div>
  </div>

  <LicenseAlert v-if="isAdmin" />

  <h2 class="text-2xl font-bold leading-7 text-gray-900 sm:text-3xl sm:truncate">
    {{ t('storageProfileList.title') }}
  </h2>
  <div class="space-y-6 mt-5">
    <div class="bg-white px-4 py-5 shadow sm:rounded-lg sm:p-6">
      <div class="md:grid md:grid-cols-3 md:gap-6">
        <div class="md:col-span-3">
          <h3 class="text-lg font-medium leading-6 text-gray-900">
            {{ t('storageProfileList.setup.title') }}
          </h3>
          <div class="col-span-6 sm:col-span-3">
            <p id="keycloakAdminRealmURL" class="inline-flex mt-2 text-sm">
              <LinkIcon class="shrink-0 text-primary mr-1 h-5 w-5" aria-hidden="true" />
              <!-- TODO final URL? -->
              <a role="button" href="https://github.com/shift7-ch/cipherduck-hub/blob/feature/cipherduck/backend/CIPHERDUCK.md" target="_blank" class="underline text-gray-500 hover:text-gray-900">{{ $t('storageProfileList.setup.description') }}</a>
            </p>
          </div>
        </div>
      </div>
    </div>
  </div>

  <div class="pb-5 mt-3 border-b border-gray-200 flex flex-wrap sm:flex-nowrap gap-3 items-center whitespace-nowrap">
    <input id="storageprofileSearch" v-model="query" :placeholder="t('storageProfileList.search.placeholder')" type="text" class="focus:ring-primary focus:border-primary block w-full shadow-sm text-sm border-gray-300 rounded-md disabled:bg-gray-200"/>
  </div>

  <div v-if="filteredStorageprofiles != null && filteredStorageprofiles.length > 0" class="mt-5 bg-white shadow overflow-hidden rounded-md">
    <ul role="list" class="divide-y divide-gray-200">
      <li v-for="(storageprofile, index) in filteredStorageprofiles" :key="storageprofile.name">
        <a role="button" tabindex="0" class="block hover:bg-gray-50" :class="{'ring-2 ring-inset ring-primary': selectedStorageprofile == storageprofile, 'rounded-t-md': index == 0, 'rounded-b-md': index == filteredStorageprofiles.length - 1}" @click="showStorageProfileDetails(storageprofile)">
          <div class="px-4 py-4 flex items-center sm:px-6">
            <div class="min-w-0 flex-1">
              <div class="flex items-center gap-3">
                <p class="truncate text-sm font-medium text-primary">{{ storageprofile.name }}</p>
                <div v-if="storageprofile.archived" class="inline-flex items-center rounded-md bg-yellow-400/10 px-2 py-1 text-xs font-medium text-yellow-500 ring-1 ring-inset ring-yellow-400/20">{{ t('storageprofileList.badge.archived') }}</div>
              </div>
            </div>
            <div class="ml-5 shrink-0">
              <ChevronRightIcon class="h-5 w-5 text-gray-400" aria-hidden="true" />
            </div>
          </div>
        </a>
      </li>
    </ul>
  </div>

  <div v-else-if="query === '' && filteredStorageprofiles != null && filteredStorageprofiles.length == 0" class="mt-3 text-center">
    <svg xmlns="http://www.w3.org/2000/svg" class="mx-auto h-12 w-12 text-gray-400" fill="none" viewBox="0 0 24 24" stroke="currentColor" aria-hidden="true">
      <path vector-effect="non-scaling-stroke" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 10.5v6m3-3H9m4.06-7.19l-2.12-2.12a1.5 1.5 0 00-1.061-.44H4.5A2.25 2.25 0 002.25 6v12a2.25 2.25 0 002.25 2.25h15A2.25 2.25 0 0021.75 18V9a2.25 2.25 0 00-2.25-2.25h-5.379a1.5 1.5 0 01-1.06-.44z" />
    </svg>
    <h3 class="mt-2 text-sm font-medium text-gray-900">{{ t('storageProfileList.empty.title') }}</h3>
    <p class="mt-1 text-sm text-gray-500">{{ t('storageProfileList.empty.description') }}</p>
  </div>

  <div v-else-if="query !== '' && filteredStorageprofiles != null && filteredStorageprofiles.length == 0" class="mt-3 text-center">
    <svg xmlns="http://www.w3.org/2000/svg" class="mx-auto h-12 w-12 text-gray-400" fill="none" viewBox="0 0 24 24" stroke="currentColor" aria-hidden="true">
      <path vector-effect="non-scaling-stroke" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15.75 15.75l-2.489-2.489m0 0a3.375 3.375 0 10-4.773-4.773 3.375 3.375 0 004.774 4.774zM21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
    </svg>
    <h3 class="mt-2 text-sm font-medium text-gray-900">{{ t('storageProfileList.filter.result.empty.title') }}</h3>
    <p class="mt-1 text-sm text-gray-500">{{ t('storageProfileList.filter.result.empty.description') }}</p>
  </div>

  <SlideOver v-if="selectedStorageprofile != null" ref="StorageProfileDetailsSlideOver" :title="selectedStorageprofile.name" @close="selectedStorageprofile = null">
    <StorageProfileDetails :vault-id="selectedStorageprofile.id" @storageprofile-updated="v => onSelectedStorageProfileUpdate(v)"></StorageProfileDetails>
  </SlideOver>
</template>

<script setup lang="ts">
import { ChevronRightIcon, LinkIcon } from '@heroicons/vue/24/solid';
import { computed, nextTick, onMounted, ref } from 'vue';
import { useI18n } from 'vue-i18n';
import auth from '../../common/auth';
import backend, { StorageProfileDto } from '../../common/backend';
import FetchError from '../FetchError.vue';
import SlideOver from '../SlideOver.vue';
import StorageProfileDetails from './StorageProfileDetails.vue';
import LicenseAlert from '../LicenseAlert.vue';

const { t } = useI18n({ useScope: 'global' });

const StorageProfileDetailsSlideOver = ref<typeof SlideOver>();
const onFetchError = ref<Error | null>();

const storageprofiles = ref<StorageProfileDto[]>();
const selectedStorageprofile = ref<StorageProfileDto | null>(null);


const isAdmin = ref<boolean>();

const query = ref('');
const filteredStorageprofiles = computed(() =>
  query.value === ''
    ? storageprofiles.value
    : storageprofiles.value?.filter((storageprofile) => {
      return storageprofile.name.toLowerCase().includes(query.value.toLowerCase());
    })
);

onMounted(fetchData);

async function fetchData() {
  onFetchError.value = null;
  try {
    isAdmin.value = (await auth).isAdmin();
    storageprofiles.value = (await backend.storageprofiles.get(undefined));
  } catch (error) {
    console.error('Retrieving storageprofile list failed.', error);
    onFetchError.value = error instanceof Error ? error : new Error('Unknown Error');
  }
}

function showStorageProfileDetails(storageprofile: StorageProfileDto) {
  selectedStorageprofile.value = storageprofile;
  nextTick(() => StorageProfileDetailsSlideOver.value?.show());
}

async function onSelectedStorageProfileUpdate(storageprofile: StorageProfileDto) {
  await fetchData();
  if (storageprofiles.value == null || storageprofile.id !== selectedStorageprofile.value?.id) {
    return;
  }
  const index = storageprofiles.value?.findIndex(v => v.id === storageprofile.id);
  if (index !== undefined && index !== -1) {
    selectedStorageprofile.value = storageprofiles.value[index];
  } else {
    selectedStorageprofile.value = storageprofile;
  }
}

</script>

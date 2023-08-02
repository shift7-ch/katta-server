<template>
    <div class="max-w-7xl mx-auto px-2 sm:px-6 lg:px-8">
      <div class="relative flex justify-between h-16">

        <div class="flex-1 flex items-center justify-center sm:items-stretch sm:justify-start">
          <router-link to="/app" class="shrink-0 flex items-center">
            <img src="/logo.svg" class="h-8" alt="Logo"/>
            <span class="font-headline font-bold text-primary ml-2 pb-px">CRYPTOMATOR HUB</span>
          </router-link>
          <div class="hidden sm:ml-6 sm:flex sm:space-x-8">
            <router-link v-for="item in navigation" :key="item.name" v-slot="{ isActive, href, navigate }" :to="item.to" custom>
              <a :href="href" :class="[isActive ? 'border-primary text-white' : 'border-transparent text-gray-300 hover:border-gray-300 hover:text-white', ' inline-flex items-center px-1 pt-1 border-b-2 text-sm font-medium']" @click="navigate">
                {{ t(item.name) }}
              </a>
            </router-link>
          </div>
        </div>
      </div>
    </div>
    <div class="max-w-7xl mx-auto px-4 py-12 sm:px-6 lg:px-8">
      <div class="fixed inset-0 z-10 overflow-y-auto">
          <div class="flex min-h-full items-end justify-center p-4 text-center sm:items-center sm:p-0">
              <div class="bg-gray-50 px-4 py-3 sm:px-6 sm:flex sm:flex-row-reverse">
                <button type="button" class="inline-flex items-center px-4 py-2 border border-transparent shadow-sm text-sm font-medium rounded-md text-white bg-primary hover:bg-primary-d1 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary" @click="downloadVault()">
                    <ArrowDownTrayIcon class="-ml-1 mr-2 h-5 w-5" aria-hidden="true" />
                    {{ t('cipherduckbookmark.download') }}
                </button>
                <p v-if="onDownloadError != null" class="text-sm text-red-900">
                  {{ t('common.unexpectedError', [onDownloadError.message]) }}
                </p>
              </div>
        </div>
      </div>
    </div>
</template>


<script setup lang="ts">
import { saveAs } from 'file-saver';
import { ref } from 'vue';
import { useI18n } from 'vue-i18n';
import backend from '../common/backend';
import { ArrowDownTrayIcon } from '@heroicons/vue/24/solid';

const { t } = useI18n({ useScope: 'global' });
const open = ref(false);

const onDownloadError = ref<Error|null>();


async function downloadVault() {
  onDownloadError.value = null;
  try {
    const bookmark = await backend.config.cipherduckhubbookmark();
    const blob = new Blob([bookmark['data']], {type: "application/xml;charset=utf-8"});
    saveAs(blob, `bookmark.duck`);
    open.value = false;
  } catch (error) {
    console.error('Downloading vault template failed.', error);
    onDownloadError.value = error instanceof Error ? error : new Error('Unknown Error');
  }
}

</script>
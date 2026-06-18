<template>
  <nav class="bg-tertiary2">
    <div class="max-w-7xl mx-auto px-2 sm:px-6 lg:px-8">
      <div class="relative flex justify-between h-16">
        <div class="flex-1 flex items-center justify-center sm:items-stretch sm:justify-start">
          <div class="shrink-0 flex items-center">
            <img src="/logo.png" class="h-9" alt="Katta Logo" />
            <span class="font-headline font-bold text-white ml-2 pb-px">KATTA</span>
          </div>
        </div>
        <div class="absolute inset-y-0 right-0 flex items-center pr-2 sm:static sm:inset-auto sm:ml-6 sm:pr-0">
          <router-link to="/app/vaults" class="text-sm font-medium text-gray-300 hover:text-white">
            {{ t('onboarding.login') }}
          </router-link>
        </div>
      </div>
    </div>
  </nav>

  <div class="max-w-7xl mx-auto px-4 py-12 sm:px-6 lg:px-8 flex justify-center">
    <div class="bg-white px-4 py-5 shadow-sm sm:rounded-lg sm:p-6 text-center sm:w-full sm:max-w-lg">
      <div class="flex justify-center mb-3 sm:mb-5">
        <img src="/logo.png" class="h-16" alt="Katta Logo" aria-hidden="true" />
      </div>
      <h1 class="text-2xl leading-6 font-medium text-gray-900">
        {{ t('onboarding.title') }}
      </h1>
      <p class="mt-6 text-sm text-gray-500">
        {{ t('onboarding.intro') }}
      </p>

      <div class="mt-8 flex flex-col gap-6 text-left">
        <div v-if="downloadAvailable">
          <h2 class="text-sm font-medium text-gray-900">
            {{ t('onboarding.download.title') }}
          </h2>
          <p class="mt-1 text-sm text-gray-500">
            {{ t('onboarding.download.description') }}
          </p>
          <a :href="downloadUrl" target="_blank" rel="noopener" class="mt-3 inline-flex w-full justify-center items-center rounded-md border border-transparent bg-primary px-4 py-2 text-base font-medium text-white shadow-xs hover:bg-primary-d1 focus:outline-hidden focus:ring-2 focus:ring-primary focus:ring-offset-2 sm:text-sm">
            <ArrowDownTrayIcon class="-ml-1 mr-2 h-5 w-5 shrink-0" aria-hidden="true" />
            {{ t('onboarding.download.button') }}
          </a>
        </div>

        <div>
          <h2 class="text-sm font-medium text-gray-900">
            {{ t('onboarding.openInKatta.title') }}
          </h2>
          <p class="mt-1 text-sm text-gray-500">
            {{ t('onboarding.openInKatta.description') }}
          </p>
          <button type="button" class="mt-3 inline-flex w-full justify-center items-center rounded-md border px-4 py-2 text-base font-medium shadow-xs focus:outline-hidden focus:ring-2 focus:ring-primary focus:ring-offset-2 sm:text-sm" :class="downloadAvailable ? 'border-gray-300 bg-white text-gray-700 hover:bg-gray-50' : 'border-transparent bg-primary text-white hover:bg-primary-d1'" @click="openBookmark()">
            <ArrowTopRightOnSquareIcon class="-ml-1 mr-2 h-5 w-5 shrink-0" aria-hidden="true" />
            {{ t('onboarding.openInKatta.button') }}
          </button>
        </div>
      </div>

      <div class="mt-8 pt-5 border-t border-gray-200">
        <i18n-t keypath="onboarding.accountKey.description" scope="global" tag="p" class="text-sm text-gray-500">
          <template #link>
            <router-link to="/app/profile" class="font-medium text-primary hover:text-primary-d1">
              {{ t('onboarding.accountKey.link') }}
            </router-link>
          </template>
        </i18n-t>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ArrowDownTrayIcon, ArrowTopRightOnSquareIcon } from '@heroicons/vue/20/solid';
import { useI18n } from 'vue-i18n';
import config from '../../common/config';
import { openInKatta } from '../../common/deeplink';

const { t } = useI18n({ useScope: 'global' });

const downloadUrl = config.get().desktopDownloadUrl;
const downloadAvailable = downloadUrl.length > 0;

function openBookmark() {
  try {
    openInKatta();
  } catch (error) {
    console.error('Opening bookmark from browser failed.', error);
  }
}
</script>

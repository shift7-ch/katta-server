<template>
  <TransitionRoot as="template" :show="open" @after-leave="$emit('close')">
    <Dialog as="div" class="fixed z-10 inset-0 overflow-y-auto" @close="open = false">
      <TransitionChild as="template" enter="ease-out duration-300" enter-from="opacity-0" enter-to="opacity-100" leave="ease-in duration-200" leave-from="opacity-100" leave-to="opacity-0">
        <DialogOverlay class="fixed inset-0 bg-gray-500/75 transition-opacity" />
      </TransitionChild>

      <div class="fixed inset-0 z-10 overflow-y-auto">
        <div class="flex min-h-full items-end justify-center p-4 text-center sm:items-center sm:p-0">
          <TransitionChild as="template" enter="ease-out duration-300" enter-from="opacity-0 translate-y-4 sm:translate-y-0 sm:scale-95" enter-to="opacity-100 translate-y-0 sm:scale-100" leave="ease-in duration-200" leave-from="opacity-100 translate-y-0 sm:scale-100" leave-to="opacity-0 translate-y-4 sm:translate-y-0 sm:scale-95">
            <DialogPanel class="relative transform overflow-hidden rounded-lg bg-white text-left shadow-xl transition-all sm:my-8 sm:w-full sm:max-w-lg">
              <form novalidate @submit.prevent="reactivate">
                <div class="bg-white px-4 pt-5 pb-4 sm:p-6 sm:pb-4">
                  <div class="sm:flex sm:items-start">
                    <div class="mx-auto shrink-0 flex items-center justify-center h-12 w-12 rounded-full bg-yellow-100 sm:mx-0 sm:h-10 sm:w-10">
                      <ExclamationTriangleIcon class="h-6 w-6 text-yellow-600" aria-hidden="true" />
                    </div>
                    <div class="mt-3 grow text-center sm:mt-0 sm:ml-4 sm:text-left">
                      <DialogTitle as="h3" class="text-lg leading-6 font-medium text-gray-900">
                        {{ t('reactivateStorageProfileDialog.title') }}
                      </DialogTitle>
                      <div class="mt-2">
                        <p class="text-sm text-gray-500">
                          {{ t('reactivateStorageProfileDialog.description', [profile.name]) }}
                        </p>
                      </div>
                    </div>
                  </div>
                </div>
                <div class="bg-gray-50 px-4 py-3 sm:px-6 sm:flex sm:flex-row-reverse">
                  <button type="submit" :disabled="processing" class="w-full inline-flex justify-center rounded-md border border-transparent shadow-xs px-4 py-2 bg-primary text-base font-medium text-white hover:bg-primary-d1 focus:outline-hidden focus:ring-2 focus:ring-offset-2 focus:ring-primary sm:ml-3 sm:w-auto sm:text-sm disabled:opacity-50">
                    {{ t('reactivateStorageProfileDialog.confirm') }}
                  </button>
                  <button type="button" :disabled="processing" class="mt-3 w-full inline-flex justify-center rounded-md border border-gray-300 shadow-xs px-4 py-2 bg-white text-base font-medium text-gray-700 hover:bg-gray-50 focus:outline-hidden focus:ring-2 focus:ring-offset-2 focus:ring-primary sm:mt-0 sm:ml-3 sm:w-auto sm:text-sm" @click="open = false">
                    {{ t('common.cancel') }}
                  </button>
                </div>
                <p v-if="onReactivateError != null" class="text-sm text-red-900 px-4 sm:px-6 text-right bg-red-50 py-2">
                  {{ t('common.unexpectedError', [onReactivateError.message]) }}
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
import { ExclamationTriangleIcon } from '@heroicons/vue/24/outline';
import { ref } from 'vue';
import { useI18n } from 'vue-i18n';
import backend, { StorageProfileDto } from '../../common/backend';

const { t } = useI18n({ useScope: 'global' });

const open = ref(false);
const processing = ref(false);
const onReactivateError = ref<Error | null>();

const props = defineProps<{
  profile: StorageProfileDto
}>();

const emit = defineEmits<{
  close: []
  reactivated: [updatedProfile: StorageProfileDto]
}>();

defineExpose({ show });

function show() {
  onReactivateError.value = null;
  open.value = true;
}

async function reactivate() {
  onReactivateError.value = null;
  processing.value = true;
  try {
    await backend.storageprofiles.setArchived(props.profile.id, false);
    emit('reactivated', { ...props.profile, archived: false });
    open.value = false;
  } catch (error) {
    console.error('Reactivating storage profile failed.', error);
    onReactivateError.value = error instanceof Error ? error : new Error('Unknown Error');
  } finally {
    processing.value = false;
  }
}
</script>

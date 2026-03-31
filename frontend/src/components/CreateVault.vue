<template>
  <div v-if="state == State.Initial">
    {{ t('common.loading') }}
  </div>

  <div v-else-if="state == State.EnterRecoveryKey" @drop.prevent="" @dragover.prevent="">
    <form ref="form" novalidate @submit.prevent="validateRecoveryKey()" >
      <div class="flex justify-center">
        <div class="bg-white px-4 py-5 shadow-sm sm:rounded-lg sm:p-6 text-center sm:w-full sm:max-w-lg">
          <div class="mx-auto flex items-center justify-center h-12 w-12 rounded-full bg-emerald-100">
            <ArrowPathIcon class="h-6 w-6 text-emerald-600" aria-hidden="true" />
          </div>
          <div class="mt-3 sm:mt-5">
            <h3 class="text-lg leading-6 font-medium text-gray-900">
              {{ t('createVault.enterRecoveryKey.title') }}
            </h3>
            <div class="mt-2">
              <p class="text-sm text-gray-500">
                {{ t('createVault.enterRecoveryKey.description') }}
              </p>
            </div>
          </div>
          <!-- Textarea -->
          <div class="mt-5 sm:mt-6">
            <label for="recoveryKey" class="sr-only">{{ t('createVault.enterRecoveryKey.recoveryKey') }}</label>
            <label for="metadata-file" class="block text-sm font-medium leading-6 text-gray-900">{{ t('createVault.enterRecoveryKey.recoveryKey') }}</label>
            <textarea
              id="recoveryKey" v-model="recoveryKeyStr" rows="6" name="recoveryKey"
              class="block w-full rounded-md border-gray-300 shadow-xs focus:border-primary focus:ring-primary sm:text-sm"
              :class="{ 'invalid:border-red-300 invalid:text-red-900 focus:invalid:ring-red-500 focus:invalid:border-red-500': onRecoverError instanceof FormValidationFailedError }"
              required
            />
          </div>
          <!-- Dropzone -->
          <div class="mt-5 sm:mt-6">
            <label for="metadata-file" class="block text-sm font-medium leading-6 text-gray-900">Vault Metadata File</label>
            <div
              class="relative mt-2 flex justify-center rounded-lg border-2  px-6 py-10 border-gray-300 hover:border-gray-400 active:border-primary focus-within:border-primary focus-within:ring-primary  focus-within:ring-offset-2"
              :class="{ 'border-primary': isDraggingOver, 'border-dashed': (vaultMetadata?.length ?? 0) == 0, 'border-red-300 active:border-red-500 focus-within:ring-red-500 focus-within:border-red-500': (vaultMetadata?.length ?? 0) == 0 && onRecoverError instanceof FormValidationFailedError}"
              @dragenter.prevent="event => handleDragEnterAndOver(event)"
              @dragover.prevent="event => handleDragEnterAndOver(event)"
              @dragleave="handleDragLeave()"
              @drop.prevent="event => handleDrop(event)"
            >
              <input id="file-upload" ref="fileUpload" name="file-upload" type="file" class="cursor-pointer absolute inset-0 opacity-0" accept=".cryptomator, .uvf" @change="event => handleUpload(event)" >
              <div v-if="(vaultMetadata?.length ?? 0) == 0" class="text-center">
                <ArrowUpOnSquareIcon class="mx-auto h-12 w-12 text-gray-300" aria-hidden="true" />
                <p class="mt-2 block text-sm font-semibold text-gray-900">
                  {{ t('createVault.enterRecoveryKey.uploadCaption') }}
                </p>
                <p class="text-xs leading-5 text-gray-600">{{ t('createVault.enterRecoveryKey.uploadSubcaption') }}</p>
              </div>
              <div v-else class="text-center">
                <DocumentCheckIcon class="mx-auto h-12 w-12 text-primary" aria-hidden="true" />
                <p class="mt-2 block text-sm font-semibold text-gray-900">
                  {{ t('createVault.enterRecoveryKey.uploadSuccessCaption', [metadataFilename]) }}
                </p>
                <p class="text-xs leading-5 text-gray-600">{{ t('createVault.enterRecoveryKey.uploadSuccessSubcaption') }}</p>
              </div>
            </div>
            <div v-if="onUploadError" class="rounded-md bg-red-50 p-4">
              <div class="flex">
                <div class="flex-shrink-0">
                  <XCircleIcon class="h-5 w-5 text-red-400" aria-hidden="true" />
                </div>
                <div class="ml-3 flex-1 md:flex md:justify-between">
                  <p v-if="onUploadError instanceof FileTooBigError" class="text-sm text-red-700">{{ t('createVault.error.uploadTooBig') }}</p>
                  <p v-else-if="onUploadError instanceof WrongFileNameError" class="text-sm text-red-700">{{ t('createVault.error.wrongFileName') }}</p>
                  <p v-else class="text-sm text-red-700">{{ t('createVault.error.failedUpload') }}</p>
                </div>
              </div>
            </div>
          </div>
          <!-- Button -->
          <div class="mt-5 sm:mt-6">
            <button
              type="submit" :disabled="processing" class="inline-flex w-full justify-center rounded-md border border-transparent bg-primary px-4 py-2 text-base font-medium text-white shadow-xs hover:bg-primary-d1 focus:outline-hidden focus:ring-2 focus:bg-primary focus:ring-offset-2 sm:text-sm disabled:opacity-50 disabled:hover:bg-primary disabled:cursor-not-allowed"
            >
              {{ t('createVault.enterRecoveryKey.submit') }}
            </button>
            <div v-if="onRecoverError">
              <p v-if="onRecoverError instanceof FormValidationFailedError" class="text-sm text-red-900 mt-2">{{ t('createVault.error.formValidationFailed') }}</p>
              <p v-else-if="onRecoverError instanceof DecodeUvfRecoveryKeyError || onRecoverError instanceof DecodeVf8RecoveryKeyError" class="text-sm text-red-900 mt-2">{{ t('createVault.error.invalidRecoveryKey') }}</p>
              <p v-else class="text-sm text-red-900 mt-2">{{ t('createVault.error.invalidRecoveryKey') }}</p>
            </div>
          </div>
        </div>
      </div>
    </form>
  </div>

  <div v-else-if="state == State.EnterVaultDetails">
    <BreadcrumbNav :crumbs="[ { label: t('vaultList.title'), to: '/app/vaults' }, { label: t('createVault.enterVaultDetails.title') } ]"/>
    <VaultCreationProgress :state="State.EnterVaultDetails" :steps="getCurrentStates" class="flex justify-center mb-4" />
    <form ref="form" class="space-y-6" novalidate @submit.prevent="validateVaultDetails()">
      <div class="flex justify-center text-center">
        <div class="bg-white shadow-sm rounded-lg overflow-hidden sm:w-full sm:max-w-lg">
          <div class="mx-auto mt-5 flex items-center justify-center h-12 w-12 rounded-full bg-emerald-100">
            <PlusIcon class="h-6 w-6 text-emerald-600" aria-hidden="true" />
          </div>
          <div class="mt-3 sm:mt-5 px-4 text-center">
            <h3 class="text-lg font-medium leading-6 text-gray-900">
              {{ t('createVault.enterVaultDetails.title') }}
            </h3>
            <p class="mt-2 text-sm text-gray-500">
              {{ t('createVault.enterVaultDetails.description') }}
            </p>
          </div>

          <div class="mt-6 px-4 space-y-6">
            <div>
              <label for="vaultName" class="block text-sm font-medium text-gray-700 text-left">{{ t('createVault.enterVaultDetails.vaultName') }}</label>
              <input id="vaultName" v-model="vault.name" :disabled="processing" type="text" class="mt-1 focus:ring-primary focus:border-primary block w-full shadow-xs sm:text-sm border-gray-300 rounded-md disabled:bg-gray-200" :class="{ 'invalid:border-red-300 invalid:text-red-900 focus:invalid:ring-red-500 focus:invalid:border-red-500': onCreateError instanceof FormValidationFailedError }" pattern="^(?! )([^\x5C\x2F:*?\x22<>\x7C])+(?<![ \x2E])$" required />
              <p v-if="(onCreateError instanceof FormValidationFailedError)" class="text-sm text-red-900 text-left mt-2">
                {{ t('createVault.error.illegalVaultName') }} \, /, :, *, ?, ", &lt;, >, |
              </p>
            </div>

            <div>
              <label for="vaultDescription" class="block text-sm font-medium text-gray-700 text-left">
                {{ t('createVault.enterVaultDetails.vaultDescription') }}
                <span class="text-xs text-gray-500">({{ t('common.optional') }})</span>
              </label>
              <input id="vaultDescription" v-model="vault.description" :disabled="processing" type="text" class="mt-1 focus:ring-primary focus:border-primary block w-full shadow-xs sm:text-sm border-gray-300 rounded-md disabled:bg-gray-200"/>
            </div>

            <!-- / start katta extension -->
            <div class="col-span-6 sm:col-span-3">
                <label for="vaultName" class="block text-sm font-medium text-gray-700">{{ t('CreateVaultS3.enterVaultDetails.storage') }}</label>
                <Listbox as="div" class="mt-1 focus:ring-primary focus:border-primary block w-full shadow-sm sm:text-sm border-gray-300 rounded-md disabled:bg-gray-200" v-model="selectedBackend"
                   @update:modelValue="value => { setRegionsOnSelectStorage(value);}"
                >
                  <ListboxButton class="relative w-full cursor-default rounded-lg bg-white py-2 pl-3 pr-10 text-left shadow-md focus:outline-none focus-visible:border-indigo-500 focus-visible:ring-2 focus-visible:ring-white focus-visible:ring-opacity-75 focus-visible:ring-offset-2 focus-visible:ring-offset-orange-300 sm:text-sm">
                    <span class="block truncate text-sm font-medium text-gray-700">{{ selectedBackend ? selectedBackend.name : '' }}</span>
                    <span
                      class="pointer-events-none absolute inset-y-0 right-0 flex items-center pr-2"
                    >
                      <ChevronUpDownIcon
                        class="h-5 w-5 text-gray-400"
                        aria-hidden="true"
                      />
                    </span>
                  </ListboxButton>

                  <transition
                    leave-active-class="transition duration-100 ease-in"
                    leave-from-class="opacity-100"
                    leave-to-class="opacity-0"
                  >
                    <div class="col-span-6 sm:col-span-4">
                    <ListboxOptions
                      class="relative mt-1 max-h-60 w-full overflow-auto rounded-md bg-white py-1 text-base shadow-lg ring-1 ring-black ring-opacity-5 focus:outline-none sm:text-sm"
                    >
                      <ListboxOption
                        v-slot="{ active, selected }"
                        v-for="backend in backends"
                        :key="backend.name"
                        :value="backend"
                        as="template"
                      >
                        <li
                          :class="[
                            active ? 'bg-emerald-100 text-emerald-900' : 'text-gray-900',
                            'relative cursor-default select-none py-2 pl-10 pr-4',
                          ]"
                        >
                          <span
                            :class="[
                              selected ? 'font-medium' : 'font-normal',
                              'block truncate col-span-6 sm:col-span-4',
                            ]">{{ backend.name }}</span>
                          <span
                            v-if="selected"
                            class="absolute inset-y-0 left-0 flex items-center pl-3 text-amber-600"
                          >
                            <CheckIcon class="h-5 w-5" aria-hidden="true" />
                          </span>
                        </li>
                      </ListboxOption>
                    </ListboxOptions>
                    </div>
                  </transition>
                </Listbox>
            </div>
            <br/>
            <div v-if="!isPermanent" class="col-span-6 sm:col-span-3">
                <label for="vaultName" class="block text-sm font-medium text-gray-700">{{ t('CreateVaultS3.enterVaultDetails.region') }}</label>
                <Listbox as="div" class="mt-1 focus:ring-primary focus:border-primary block w-full shadow-sm sm:text-sm border-gray-300 rounded-md disabled:bg-gray-200" v-model="selectedRegion">
                  <ListboxButton class="relative w-full cursor-default rounded-lg bg-white py-2 pl-3 pr-10 text-left shadow-md focus:outline-none focus-visible:border-indigo-500 focus-visible:ring-2 focus-visible:ring-white focus-visible:ring-opacity-75 focus-visible:ring-offset-2 focus-visible:ring-offset-orange-300 sm:text-sm">
                    <span class="block truncate text-sm font-medium text-gray-700">{{ selectedRegion }}</span>
                    <span
                      class="pointer-events-none absolute inset-y-0 right-0 flex items-center pr-2"
                    >
                      <ChevronUpDownIcon
                        class="h-5 w-5 text-gray-400"
                        aria-hidden="true"
                      />
                    </span>
                  </ListboxButton>

                  <transition
                    leave-active-class="transition duration-100 ease-in"
                    leave-from-class="opacity-100"
                    leave-to-class="opacity-0"
                  >
                    <div class="col-span-6 sm:col-span-4">
                    <ListboxOptions
                      class="relative mt-1 max-h-60 w-full overflow-auto rounded-md bg-white py-1 text-base shadow-lg ring-1 ring-black ring-opacity-5 focus:outline-none sm:text-sm"
                    >
                      <ListboxOption
                        v-slot="{ active, selected }"
                        v-for="region in regions"
                        :key="region"
                        :value="region"
                        as="template"
                      >
                        <li
                          :class="[
                            active ? 'bg-emerald-100 text-emerald-900' : 'text-gray-900',
                            'relative cursor-default select-none py-2 pl-10 pr-4',
                          ]"
                        >
                          <span
                            :class="[
                              selected ? 'font-medium' : 'font-normal',
                              'block truncate col-span-6 sm:col-span-4',
                            ]">{{ region }}</span>
                          <span
                            v-if="selected"
                            class="absolute inset-y-0 left-0 flex items-center pl-3 text-amber-600"
                          >
                            <CheckIcon class="h-5 w-5" aria-hidden="true" />
                          </span>
                        </li>
                      </ListboxOption>
                    </ListboxOptions>
                    </div>
                  </transition>
                </Listbox>
            </div>
            <div v-if="isPermanent" class="col-span-6 sm:col-span-4">
                <label for="vaultAccessKeyId" class="block text-sm font-medium text-gray-700">
                  {{ t('CreateVaultS3.enterVaultDetails.vaultPermanentAccessKeyId') }}
                </label>
                <input id="vaultAccessKeyId" v-model="vaultAccessKeyId" :disabled="processing" type="text" class="mt-1 focus:ring-primary focus:border-primary block w-full shadow-sm sm:text-sm border-gray-300 rounded-md disabled:bg-gray-200" :class="{ 'invalid:border-red-300 invalid:text-red-900 focus:invalid:ring-red-500 focus:invalid:border-red-500': onCreateError instanceof FormValidationFailedError }" required/>
            </div>
            <div v-if="isPermanent" class="col-span-6 sm:col-span-4">
                <label for="vaultSecretKey" class="block text-sm font-medium text-gray-700">
                  {{ t('CreateVaultS3.enterVaultDetails.vaultPermanentSecretKey') }}
                </label>
                <input id="vaultSecretKey" v-model="vaultSecretKey" :disabled="processing" type="text" class="mt-1 focus:ring-primary focus:border-primary block w-full shadow-sm sm:text-sm border-gray-300 rounded-md disabled:bg-gray-200" :class="{ 'invalid:border-red-300 invalid:text-red-900 focus:invalid:ring-red-500 focus:invalid:border-red-500': onCreateError instanceof FormValidationFailedError }" required/>
            </div>
            <div v-if="isPermanent" class="col-span-6 sm:col-span-4">
                <label for="vaultBucketName" class="block text-sm font-medium text-gray-700">
                  {{ t('CreateVaultS3.enterVaultDetails.vaultPermanentBucketName') }}
                </label>
                <input id="vaultBucketName" v-model="vaultBucketName" :disabled="processing" type="text" class="mt-1 focus:ring-primary focus:border-primary block w-full shadow-sm sm:text-sm border-gray-300 rounded-md disabled:bg-gray-200" :class="{ 'invalid:border-red-300 invalid:text-red-900 focus:invalid:ring-red-500 focus:invalid:border-red-500': onCreateError instanceof FormValidationFailedError }" required/>
            </div>
            <br/>
            <div class="col-span-6 sm:col-span-3">
                <label for="vaultName" class="block text-sm font-medium text-gray-700">{{ t('CreateVaultS3.enterVaultDetails.automaticAccessGrant') }}</label>
                <input id="automaticAccessGrant" v-model="automaticAccessGrant" name="automaticAccessGrant" type="checkbox" class="h-4 w-4 rounded border-gray-300 text-primary focus:ring-primary" required>
            </div>
            <!-- \ end katta extension -->


          </div>

          <div class="bg-gray-50 mt-4 px-4 py-3 sm:px-6">
            <div class="flex flex-col sm:flex-row sm:justify-between sm:items-center sm:space-x-4">
              <div class="text-sm text-red-900 text-right sm:flex-1 sm:min-w-0">
                <template v-if="onCreateError">
                  <p v-if="(onCreateError instanceof FormValidationFailedError)">
                    {{ t('createVault.error.formValidationFailed','') }}
                  </p>
                  <!-- // / start katta extension -->
                  <p v-else-if="(onCreateError instanceof StorageProfileError )">
                    {{ t('CreateVaultS3.error.invalidStorageProfileConfiguration', '') }}: {{ onCreateError.message }}
                  </p>
                  <!-- // \ end katta extension -->
                  <!-- // / start katta modification -->
                  <p v-else-if="(onRecoverError instanceof DecodeUvfRecoveryKeyError || onRecoverError instanceof DecodeVf8RecoveryKeyError)">
                  <!-- // \  end katta modification -->
                    {{ t('createVault.error.invalidRecoveryKey','') }}
                  </p>
                  <p v-else>
                    {{ t('common.unexpectedError', [onCreateError.message]) }}
                  </p>
                </template>
              </div>
              <div class="flex flex-col-reverse sm:flex-row-reverse sm:space-x-reverse sm:space-x-3 flex-shrink-0 mt-4 sm:mt-0">
                <button
                  type="submit"
                  :disabled="processing"
                  class="inline-flex justify-center rounded-md border border-transparent bg-primary px-4 py-2 text-base font-medium text-white shadow-sm hover:bg-primary-d1 focus:outline-hidden focus:ring-2 focus:ring-offset-2 focus:ring-primary sm:text-sm disabled:opacity-50 disabled:hover:bg-primary disabled:cursor-not-allowed"
                >
                  {{ t('common.next') }}
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </form>
  </div>
  <div v-else-if="state == State.DefineEmergencyAccess">
    <BreadcrumbNav :crumbs="[ { label: t('vaultList.title'), to: '/app/vaults' }, { label: t('createVault.enterVaultDetails.title') } ]"/>
    <VaultCreationProgress :state="state" :steps="getCurrentStates" class="flex justify-center mb-4" />
    <form @submit.prevent="validateVaultEmergencyAccess()">
      <div class="flex justify-center">
        <div class="bg-white shadow-sm rounded-lg sm:w-full sm:max-w-lg">
          <div class="mx-auto mt-5 flex items-center justify-center h-12 w-12 rounded-full bg-emerald-100">
            <ArrowPathIcon class="h-6 w-6 text-emerald-600" aria-hidden="true" />
          </div>
          <div class="mt-3 mb-3 px-4 sm:mt-5">
            <h3 class="text-lg leading-6 font-medium text-gray-900 text-center">
              {{ t('createVault.emergencyAccessDetails.title') }}
            </h3>
            <div class="mt-2">
              <p class="text-sm text-gray-500 text-center">
                {{ allowChangingDefaults
                  ? t('createVault.emergencyAccessDetails.description')
                  : t('createVault.emergencyAccessDetails.description.adminDefined') }}
              </p>
            </div>
            <div class="relative">
              <div class="sm:grid sm:grid-cols-2 sm:items-center sm:gap-2 pt-2 pb-2">
                <label for="coundcilMembers" class="text-sm font-medium text-gray-700 flex items-center">
                  {{ t('emergencyAccess.label.councilMembers') }}
                </label>
              </div>
              <MultiUserSelectInputGroup
                :selected-users="emergencyCouncilMembers"
                :on-search="searchCouncilMembers"
                :input-visible="allowChangingDefaults"
                @action="addCouncilMember"
                @remove="removeCouncilMember"
              />
              <div v-if="allowChangingDefaults && minMembers - emergencyCouncilMembers.length > 0" class="flex items-start gap-2 rounded-md border border-yellow-200 bg-yellow-50 p-1 text-sm text-gray-900 mt-1">
                <span class="leading-5">
                  <span class="text-gray-600">
                    {{ t('emergencyAccess.validation.selectMoreCouncilMembers', [minMembers - emergencyCouncilMembers.length]) }}
                  </span>
                </span>
              </div>
            </div>
            <label class="block text-sm font-medium text-gray-700 pt-4">
              {{ t('emergencyAccess.label.exampleRecovery') }}
            </label>
            <EmergencyScenarioVisualization
              :selected-users="emergencyCouncilMembers"
              :required-key-shares="requiredKeyShares"
              :min-members="0"
            />
            <div v-if="needsRedundancy()" class="mt-4 mr-3">
              <span
                class="inline-flex items-center gap-2 rounded-full bg-yellow-50 ring-1 ring-yellow-300/70 px-2.5 py-1 text-xs font-medium text-yellow-800"
              >
                <ExclamationTriangleIcon class="h-4 w-4" aria-hidden="true" />
                {{ t('emergencyAccess.noRedundancy') }}
              </span>
            </div>
          </div>
          <div class="bg-gray-50 mt-4 px-4 py-3 sm:px-6 rounded-b-lg">
            <div class="flex flex-col sm:flex-row sm:justify-between sm:items-center sm:space-x-4">
              <div class="text-sm text-red-900 sm:flex-1 sm:min-w-0">
                <template v-if="onCreateError">
                  <p v-if="!(onCreateError instanceof PaymentRequiredError)">
                    {{ t('common.unexpectedError', [onCreateError.message]) }}
                  </p>
                </template>
              </div>
              <div class="flex flex-col-reverse sm:flex-row-reverse sm:space-x-reverse sm:space-x-3 flex-shrink-0 mt-4 sm:mt-0">
                <button
                  type="submit"
                  :disabled="isGrantButtonDisabled"
                  class="inline-flex justify-center rounded-md border border-transparent bg-primary px-4 py-2 text-base font-medium text-white shadow-sm hover:bg-primary-d1 focus:outline-hidden focus:ring-2 focus:ring-offset-2 focus:ring-primary sm:text-sm disabled:opacity-50 disabled:hover:bg-primary disabled:cursor-not-allowed"
                >
                  {{ t('common.next') }}
                </button>
                <button
                  type="button"
                  class="mt-3 sm:mt-0 inline-flex justify-center rounded-md border border-gray-300 bg-white px-4 py-2 text-base font-medium text-gray-700 shadow-sm hover:bg-gray-50 focus:outline-hidden focus:ring-2 focus:ring-offset-2 focus:ring-primary sm:text-sm"
                  @click="backToEnterVaultDetails()"
                >
                  {{ t('common.previous') }}
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </form>
  </div>
  <div v-else-if="state == State.ShowRecoveryKey">
    <BreadcrumbNav :crumbs="[ { label: t('vaultList.title'), to: '/app/vaults' }, { label: t('createVault.enterVaultDetails.title') } ]"/>
    <VaultCreationProgress :state="state" :steps="getCurrentStates" class="flex justify-center mb-4" />
    <form @submit.prevent="createVault()">
      <div class="flex justify-center text-center">
        <div class="bg-white shadow-sm rounded-lg overflow-hidden sm:max-w-lg">
          <div class="mx-auto mt-5 flex items-center justify-center h-12 w-12 rounded-full bg-emerald-100">
            <KeyIcon class="h-6 w-6 text-emerald-600" aria-hidden="true" />
          </div>
          <div class="mt-3 sm:mt-5 px-4">
            <h3 class="text-lg leading-6 font-medium text-gray-900">
              {{ t('createVault.showRecoveryKey.title') }}
            </h3>
            <div class="mt-2">
              <p class="text-sm text-gray-500">
                {{ t('createVault.showRecoveryKey.description') }}
              </p>
            </div>
            <div class="relative mt-5 sm:mt-6">
              <div class="overflow-hidden rounded-lg border border-gray-300 shadow-xs focus-within:border-primary focus-within:ring-1 focus-within:ring-primary">
                <label for="recoveryKey" class="sr-only">{{ t('createVault.showRecoveryKey.recoveryKey') }}</label>
                <textarea
                  id="recoveryKey" v-model="recoveryKeyStr" rows="6" name="recoveryKey"
                  class="block w-full resize-none border-0 py-3 focus:ring-0 sm:text-sm" readonly
                />
                <!-- Spacer element to match the height of the toolbar -->
                <div class="py-2" aria-hidden="true">
                  <div class="h-9" />
                </div>
              </div>

              <div class="absolute inset-x-0 bottom-0">
                <div class="flex flex-nowrap justify-end space-x-2 py-2 px-2 sm:px-3">
                  <div class="flex-shrink-0">
                    <button
                      type="button" class="relative inline-flex items-center whitespace-nowrap rounded-full bg-gray-50 py-2 px-2 text-sm font-medium text-gray-500 hover:bg-gray-100 sm:px-3"
                      @click="copyRecoveryKey()"
                    >
                      <ClipboardIcon class="h-5 w-5 shrink-0 text-gray-300 sm:-ml-1" aria-hidden="true" />
                      <span v-if="!copiedRecoveryKey" class="hidden truncate sm:ml-2 sm:block text-gray-900">{{ t('common.copy') }}</span>
                      <span v-else class="hidden truncate sm:ml-2 sm:block text-gray-900">{{ t('common.copied') }}</span>
                    </button>
                  </div>
                </div>
              </div>
            </div>
            <div class="relative flex items-start text-left mt-5 sm:mt-6">
              <div class="flex h-5 items-center">
                <input
                  id="confirmRecoveryKey" v-model="confirmRecoveryKey" name="confirmRecoveryKey" type="checkbox"
                  class="h-4 w-4 rounded-sm border-gray-300 text-primary focus:ring-primary" required
                >
              </div>
              <div class="ml-3 text-sm">
                <label for="confirmRecoveryKey" class="font-medium text-gray-700">{{ t('createVault.showRecoveryKey.confirmRecoveryKey') }}</label>
              </div>
            </div>
          </div>
          <div class="bg-gray-50 mt-4 px-4 py-3 sm:px-6">
            <div class="flex flex-col sm:flex-row sm:justify-between sm:items-center sm:space-x-4">
              <div class="text-sm text-red-900 sm:flex-1 sm:min-w-0">
                <template v-if="onCreateError">
                  <p v-if="!(onCreateError instanceof PaymentRequiredError)">
                    {{ t('common.unexpectedError', [onCreateError.message]) }}
                  </p>
                  <p v-else>
                    {{ t('createVault.error.paymentRequired') }}
                  </p>
                </template>
              </div>
              <div class="flex flex-col-reverse sm:flex-row-reverse sm:space-x-reverse sm:space-x-3 flex-shrink-0 mt-4 sm:mt-0">
                <button
                  type="submit"
                  :disabled="!confirmRecoveryKey || processing"
                  class="inline-flex justify-center rounded-md border border-transparent bg-primary px-4 py-2 text-base font-medium text-white shadow-sm hover:bg-primary-d1 focus:outline-hidden focus:ring-2 focus:ring-offset-2 focus:ring-primary sm:text-sm disabled:opacity-50 disabled:hover:bg-primary disabled:cursor-not-allowed"
                >
                  {{ t('createVault.showRecoveryKey.submit') }}
                </button>
                <button
                  type="button"
                  class="mt-3 sm:mt-0 inline-flex justify-center rounded-md border border-gray-300 bg-white px-4 py-2 text-base font-medium text-gray-700 shadow-sm hover:bg-gray-50 focus:outline-hidden focus:ring-2 focus:ring-offset-2 focus:ring-primary sm:text-sm"
                  @click="backToDefineEmergencyAccess()"
                >
                  {{ t('common.previous') }}
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </form>
  </div>

  <div v-else-if="state == State.Finished">
    <BreadcrumbNav :crumbs="[ { label: t('vaultList.title'), to: '/app/vaults' }, { label: t('createVault.enterVaultDetails.title') } ]"/>
    <VaultCreationProgress :state="state" :steps="getCurrentStates" class="flex justify-center mb-4" />
    <div class="flex justify-center">
      <div class="bg-white px-4 py-5 shadow-sm sm:rounded-lg sm:p-6 text-center sm:w-full sm:max-w-lg">
        <div class="mx-auto flex items-center justify-center h-12 w-12 rounded-full bg-emerald-100">
          <CheckIcon class="h-6 w-6 text-emerald-600" aria-hidden="true" />
        </div>
        <div class="mt-3 sm:mt-5">
          <h3 class="text-lg leading-6 font-medium text-gray-900">
            {{ t('createVault.success.title') }}
          </h3>
          <div class="mt-2">
            <p class="text-sm text-gray-500">
              <!-- / start katta modification -->
              {{ t('CreateVaultS3.success.description') }}
              <!-- \ end katta modification -->
            </p>
          </div>
        </div>
        <div class="mt-5 sm:mt-6">
          <button
            type="button"
            class="inline-flex items-center px-4 py-2 border border-transparent shadow-xs text-sm font-medium rounded-md text-white bg-primary hover:bg-primary-d1 focus:outline-hidden focus:ring-2 focus:ring-offset-2 focus:ring-primary"
            @click="downloadVaultTemplate()"
          >
            <ArrowDownTrayIcon class="-ml-1 mr-2 h-5 w-5" aria-hidden="true" />
            {{ t('createVault.success.download') }}
          </button>
          <p v-if="onDownloadTemplateError" class="text-sm text-red-900 mr-4">
            {{ t('createVault.error.downloadTemplateFailed', [onDownloadTemplateError.message]) }}
          </p>
          <!-- TODO: not beautiful-->
        </div>
        <!-- / start katta modification -->
        <div class="mt-5 sm:mt-6">
          <button type="button" class="inline-flex items-center px-4 py-2 border border-transparent shadow-sm text-sm font-medium rounded-md text-white bg-primary hover:bg-primary-d1 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary" @click="openBookmark()">
            <ArrowTopRightOnSquareIcon class="-ml-1 mr-2 h-5 w-5" aria-hidden="true" />
            {{ t('CreateVaultS3.success.open') }}
          </button>
          <p v-if="onOpenBookmarkError != null " class="text-sm text-red-900 mr-4">{{ t('CreateVaultS3.error.openBookmarkFailed', [onOpenBookmarkError.message]) }}</p> <!-- TODO: not beautiful-->
        </div>
        <div class="mt-5 sm:mt-6">
          <p v-if="onUploadTemplateError != null " class="text-sm text-red-900 mr-4">{{ t('CreateVaultS3.error.uploadTemplateFailed') }}{{ onUploadTemplateError.message == null ? '' : ': ' + onUploadTemplateError.message }}</p> <!-- TODO: not beautiful-->
        </div>
        <!-- \ end katta modification -->
        <div class="mt-2">
          <router-link to="/app/vaults" class="text-sm text-gray-500">
            {{ t('createVault.success.return') }}
          </router-link>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ClipboardIcon, XCircleIcon } from '@heroicons/vue/20/solid';
import { ArrowPathIcon, ArrowUpOnSquareIcon, CheckIcon, DocumentCheckIcon, KeyIcon, PlusIcon } from '@heroicons/vue/24/outline';
import { ArrowDownTrayIcon, ExclamationTriangleIcon } from '@heroicons/vue/24/solid';
import { saveAs } from 'file-saver';
import { computed, onMounted, ref } from 'vue';
import { useI18n } from 'vue-i18n';
import backend, { AccessGrant, ActivatedUser, didCompleteSetup, LicenseUserInfoDto, PaymentRequiredError, SettingsDto, VaultDto } from '../common/backend';
import { absBackendBaseURL } from '../common/config';
import { VaultTemplateProducing } from '../common/crypto';
import { EmergencyAccess } from '../common/emergencyaccess';
import { DecodeUvfRecoveryKeyError, UniversalVaultFormat } from '../common/universalVaultFormat';
import userdata from '../common/userdata';
import { debounce, wordEncoder } from '../common/util';
import BreadcrumbNav from './BreadcrumbNav.vue';
import EmergencyScenarioVisualization from './emergencyaccess/EmergencyScenarioVisualization.vue';
import MultiUserSelectInputGroup from './MultiUserSelectInputGroup.vue';
import VaultCreationProgress from './VaultCreationProgress.vue';
import { DecodeVf8RecoveryKeyError, VaultFormat8 } from '../common/vaultFormat8';
// / start katta extension
import { StorageProfileDto, VaultMetadataJWEBackendDto } from '../common/backend';
import {
     Listbox,
     ListboxButton,
     ListboxOptions,
     ListboxOption,
   } from '@headlessui/vue';
import { ChevronUpDownIcon } from '@heroicons/vue/24/outline';
import { ArrowTopRightOnSquareIcon } from '@heroicons/vue/24/solid';
import { STSClient,AssumeRoleWithWebIdentityCommand } from "@aws-sdk/client-sts";
import { S3Client, PutObjectCommand, ListObjectsV2Command, GetBucketLocationCommand, HeadBucketCommand } from "@aws-sdk/client-s3";
import authPromise from '../common/auth';
import {AxiosError} from 'axios';
import { base64urlnopad } from '@scure/base';
import { isAwsHostname } from '../../src/common/katta';
// \ end katta extension

enum State {
  Initial,
  EnterRecoveryKey,
  EnterVaultDetails,
  DefineEmergencyAccess,
  ShowRecoveryKey,
  Finished
}

enum VaultType {
  VaultFormat8,
  UniversalVaultFormat
}

const vaultType = ref(VaultType.UniversalVaultFormat);

class FormValidationFailedError extends Error {
  constructor() {
    super('The form is invalid.');
  }
}

class EmptyVaultTemplateError extends Error {
  constructor() {
    super('Vault template is empty.');
  }
}


class NoFileError extends Error {
  constructor() {
    super('Drag and drop operation has no file.');
  }
}

class WrongFileNameError extends Error {
  constructor() {
    super('Dropped file is not named "vault.cryptomator" or "vault.uvf"');
  }
}

class FileTooBigError extends Error {
  constructor() {
    super('Dropped file exceeds size limit of 8KB');
  }
}

const { t } = useI18n({ useScope: 'global' });

const form = ref<HTMLFormElement>();
const fileUpload = ref<HTMLInputElement>();

const onCreateError = ref<Error>();
const onRecoverError = ref<Error>();
const onDownloadTemplateError = ref<Error>();
const onUploadError = ref<Error>();

const state = ref(State.Initial);
const processing = ref(false);
const settings = ref<SettingsDto>();
const vaultName = ref('');
const vaultDescription = ref<string | undefined>();
const vault = ref<VaultDto>({
  id: crypto.randomUUID(),
  name: '',
  description: '',
  archived: false,
  creationTime: new Date(),
  requiredEmergencyKeyShares: 0,
  emergencyKeyShares: {}
});
const copiedRecoveryKey = ref(false);
const debouncedCopyFinish = debounce(() => copiedRecoveryKey.value = false, 2000);
const confirmRecoveryKey = ref(false);
const vaultFormat8 = ref<VaultFormat8>();
const recoveryKeyStr = ref<string>('');
const uvfVault = ref<UniversalVaultFormat>();

const metadataFilename = computed(() => vaultType.value == VaultType.VaultFormat8 ? 'vault.cryptomator' : 'vault.uvf');
const vaultMetadata = ref<string>('');
const isDraggingOver = ref<boolean>(false);

const props = defineProps<{
  recover: boolean
}>();

const emergencyCouncilMembers = computed(() =>
  [...initialEmergencyCouncilMembers.value, ...addedEmergencyCouncilMembers.value]
);
const defaultEmergencyCouncilMembers = ref<ActivatedUser[]>([]);
const defaultRequiredEmergencyKeyShares = ref<number>(0);
const allowChangingDefaults = ref<boolean>(false);
const requiredKeyShares = ref<number>(0);
const minMembers = ref<number>(0);
const initialEmergencyCouncilMembers = ref<ActivatedUser[]>([]);
const addedEmergencyCouncilMembers = ref<ActivatedUser[]>([]);

async function searchCouncilMembers(query: string): Promise<ActivatedUser[]> {
  const existingIds = new Set(emergencyCouncilMembers.value.map(m => m.id));
  const authorities = await backend.authorities.search(query, true);
  return authorities
    .filter(a => a.type === 'USER')
    .filter(a => didCompleteSetup(a))
    .filter(a => !existingIds.has(a.id))
    .sort((a, b) => a.name.localeCompare(b.name));
}

async function addCouncilMember(authority: ActivatedUser) {
  const alreadyExists =
    initialEmergencyCouncilMembers.value.some(u => u.id === authority.id) ||
    addedEmergencyCouncilMembers.value.some(u => u.id === authority.id);
  if (!alreadyExists) {
    addedEmergencyCouncilMembers.value = [...addedEmergencyCouncilMembers.value, authority];
  }
}

function removeCouncilMember(user: ActivatedUser) {
  initialEmergencyCouncilMembers.value = initialEmergencyCouncilMembers.value.filter(u => u.id !== user.id);
  addedEmergencyCouncilMembers.value = addedEmergencyCouncilMembers.value.filter(u => u.id !== user.id);
}

async function loadDefaultEmergencyAccessSettings() {
  try {
    settings.value = await backend.settings.get();
    const authorities = await backend.authorities.listSome(settings.value.emergencyCouncilMemberIds);
    const activatedUsers = authorities
      .filter((a): a is ActivatedUser => a.type === 'USER' && didCompleteSetup(a))
      .sort((a, b) => a.name.localeCompare(b.name));

    defaultEmergencyCouncilMembers.value = activatedUsers;
    initialEmergencyCouncilMembers.value = [...activatedUsers];
    allowChangingDefaults.value = settings.value.allowChoosingEmergencyCouncil;
    defaultRequiredEmergencyKeyShares.value = settings.value.defaultRequiredEmergencyKeyShares;
    requiredKeyShares.value = settings.value.defaultRequiredEmergencyKeyShares;
    minMembers.value = settings.value.defaultMinMembers;
  } catch (error) {
    console.error('Loading emergency council members failed:', error);
    defaultRequiredEmergencyKeyShares.value = 0;
    allowChangingDefaults.value = false;
  }
}

const isInvalidKeyShares = computed(() => {
  return requiredKeyShares.value < 1;
});

const isInvaildCouncilMembers = computed(() => {
  return emergencyCouncilMembers.value.length < 1;
});

const hasTooFewCouncilMembers = computed(() => {
  return emergencyCouncilMembers.value.length < requiredKeyShares.value;
});

const isGrantButtonDisabled = computed(() => {
  return isInvalidKeyShares.value || isInvaildCouncilMembers.value || hasTooFewCouncilMembers.value;
});

// / start katta extension
const selectedBackend = ref<StorageProfileDto | null >(null);
const selectedRegion = ref<string | undefined>();
const isPermanent = ref(false);
const regions = ref<string[] | undefined>();
const backends = ref<StorageProfileDto[] | null>(null);
const vaultAccessKeyId = ref('');
const vaultSecretKey = ref('');
const vaultBucketName = ref('');
const automaticAccessGrant = ref<boolean>(true);
const onOpenBookmarkError = ref<Error | null>(null);
const onUploadTemplateError = ref<Error | null>(null);

class ErrorWithCodeHint extends Error {
  constructor(public message: string, public codehint: string) {
    super(message);
    this.codehint = codehint;
  }
}
// \ end katta extension
onMounted(initialize);
const licenseStatus = ref<LicenseUserInfoDto>();

const isCommunityLicense = computed(() => {
  return !licenseStatus.value?.expiresAt;
});

async function initialize() {
  if (props.recover) {
    state.value = State.EnterRecoveryKey;
  } else {
    switch (vaultType.value) {
      case VaultType.VaultFormat8:
        vaultFormat8.value = await VaultFormat8.create();
        recoveryKeyStr.value = await vaultFormat8.value.createRecoveryKey();
        break;
      case VaultType.UniversalVaultFormat:
        uvfVault.value = await UniversalVaultFormat.create({ enabled: false, maxWotDepth: 0 }, { provider: '', defaultPath: '', nickname: '', region: ''});
        recoveryKeyStr.value = await uvfVault.value.recoveryKey.createRecoveryKey();
        break;
    }
    await loadDefaultEmergencyAccessSettings();
    state.value = State.EnterVaultDetails;
  }
  licenseStatus.value = await backend.license.getUserInfo();
  // / start katta extension
  // get only non-archived storage profiles for new vaults
  backends.value = await backend.storageprofiles.get(false);
  selectedBackend.value = backends.value[0];
  setRegionsOnSelectStorage(selectedBackend.value);
  selectedRegion.value = selectedBackend.value.region;
  // \ end katta extension

}

async function handleDragEnterAndOver (event: DragEvent){
  isDraggingOver.value = true;
  if (event.dataTransfer) {
    event.dataTransfer.effectAllowed = 'copy';
    event.dataTransfer.dropEffect = 'copy';
  }
}

async function handleDragLeave() {
  isDraggingOver.value = false;
}

async function handleDrop(event: DragEvent) {
  onUploadError.value = undefined;
  isDraggingOver.value = false;
  let file: File | null = null;
  if (event.dataTransfer?.items && event.dataTransfer.items.length >= 1) {
    //new DataTransferItemList API
    const item = event.dataTransfer.items[0];
    if (item.kind == 'file') {
      file = item.getAsFile();
    }
  } else {
    file = event.dataTransfer?.files[0] ?? null;
  }
  validateAndSetMetadataFile(file);
}

async function handleUpload(event: Event) {
  onUploadError.value = undefined;
  validateAndSetMetadataFile(fileUpload.value?.files?.item(0) ?? null);
}

async function validateAndSetMetadataFile(file: File | null) {
  try {
    if (!file) {
      throw new NoFileError();
    } else if (!file.name.match(/vault\.(cryptomator|uvf)/)) {
      throw new WrongFileNameError();
    } else if (file.size > 8000) {
      throw new FileTooBigError();
    }

    vaultType.value = file.name.endsWith('.uvf') ? VaultType.UniversalVaultFormat : VaultType.VaultFormat8;
    vaultMetadata.value = await file.text();
  } catch (error) {
    onUploadError.value = error instanceof Error ? error : new Error('Error reading file as UTF-8 encoded text.');
  }
}

async function validateRecoveryKey() {
  onRecoverError.value = undefined;
  if (!form.value?.checkValidity() || !vaultMetadata.value ) {
    onRecoverError.value = new FormValidationFailedError();
    return;
  }
  await recoverVault();
}

const getCurrentStates = computed(() => {
  return isCommunityLicense.value || !settings.value?.enableEmergencyAccess ? communityCreateStates : allCreateStates;
});

const allCreateStates = [
  State.EnterVaultDetails,
  State.DefineEmergencyAccess,
  State.ShowRecoveryKey,
  State.Finished,
];

const communityCreateStates = [
  State.EnterVaultDetails,
  State.ShowRecoveryKey,
  State.Finished,
];

async function recoverVault() {
  onRecoverError.value = undefined;
  try {
    processing.value = true;
    if (vaultType.value == VaultType.UniversalVaultFormat) {
      uvfVault.value = await UniversalVaultFormat.recover(vaultMetadata.value, recoveryKeyStr.value);
    } else {
      vaultFormat8.value = await VaultFormat8.recoverAndVerify(vaultMetadata.value, recoveryKeyStr.value);
    }
    state.value = State.EnterVaultDetails;
  } catch (error) {
    console.error('Recovering vault failed.', error);
    onRecoverError.value = error instanceof Error ? error : new Error('Unknown reason');
  } finally {
    processing.value = false;
  }
}

async function validateVaultDetails() {
  onCreateError.value = undefined;
  if (!form.value?.checkValidity()) {
    onCreateError.value = new FormValidationFailedError();
    return;
  }
  // / start katta extension
    console.log("validateS3");
    if(!selectedBackend.value){
        // TODO https://github.com/shift7-ch/katta-server/issues/31 localization
        onCreateError.value = new StorageProfileError('Select a vault storage location.');
        return;
    }
    if(!isPermanent.value){
        if(!selectedRegion.value){
            // TODO https://github.com/shift7-ch/katta-server/issues/31 localization
            onCreateError.value = new StorageProfileError('Select a region.');
            return
        }
    }
    else if(isPermanent.value){
        if(!vaultAccessKeyId.value){
            // TODO https://github.com/shift7-ch/katta-server/issues/31 localization
            onCreateError.value = new StorageProfileError('Enter the username and re-try.');
            return;
        }
        if(!vaultSecretKey.value){
            // TODO https://github.com/shift7-ch/katta-server/issues/31 localization
            onCreateError.value = new StorageProfileError('Enter the password and re-try.');
            return;
        }
        if(!vaultBucketName.value){
            // TODO https://github.com/shift7-ch/katta-server/issues/31 localization
            onCreateError.value = new StorageProfileError('Enter the bucket name and re-try.');
            return;
        }
        const endpoint = (selectedBackend.value.scheme && selectedBackend.value.hostname && selectedBackend.value.port) ? `${selectedBackend.value.scheme}://${selectedBackend.value.hostname}:${selectedBackend.value.port}` : undefined;

    try {
      const headBucketClient = new S3Client({
        // https://github.com/aws/aws-sdk-js/issues/462 us-east-1 seems to have special behaviour
        region: "us-west-2", // must not be empty, despite documentation saying optional (SDK rejects before even sending out request)
        endpoint: `https://s3.amazonaws.com`,
        credentials: {
          accessKeyId: vaultAccessKeyId.value,
          secretAccessKey: vaultSecretKey.value
        }
      });

      const command = new GetBucketLocationCommand({
        Bucket: vaultBucketName.value
      });
      try {
        const response = await headBucketClient.send(command);
        console.log(response)
      }
      catch (error) {
        console.log(error)
        // https://stackoverflow.com/questions/47668509/the-authorization-header-is-malformed-the-region-us-east-1-is-wrong-expectin
        if ((error as any)?.Code == "AuthorizationHeaderMalformed" && (error as any)?.Region != undefined) {
          selectedRegion.value = (error as any).Region
        }
        else {
          if (selectedRegion.value === undefined) { // MinIO returns undefined
            selectedRegion.value = "us-east-1"; // must not be empty, despite documentation saying optional (SDK rejects before even sending out request)
          }
        }
      }
      console.log(`GetBucketLocation returned region ${selectedRegion.value}`);

            const client = new S3Client({
               region: selectedRegion.value,
               endpoint: endpoint,
               forcePathStyle: selectedBackend.value.withPathStyleAccessEnabled,
               credentials:{
                   accessKeyId: vaultAccessKeyId.value,
                   secretAccessKey: vaultSecretKey.value
               }
            });
            // N.B. there seems to be no API to check write permissions without actually writing.
            const commandListObjects = new ListObjectsV2Command({
               Bucket: vaultBucketName.value,
               MaxKeys: 1,
            });
            const responseListObjects = await client.send(commandListObjects);
            console.log(responseListObjects);
            if(responseListObjects.KeyCount != 0){
                // TODO https://github.com/shift7-ch/katta-server/issues/31 localization
                onCreateError.value = new Error('Bucket not empty, cannot upload template. Empty the bucket manually and re-try.');
                return;
            }
        } catch (error) {
            console.log(error);
            // TODO review can we improve whether this is a CORS problem? FF message is "NetworkError when attempting to fetch resource", Safari "Load failed".
            if(error instanceof TypeError){
                // TODO https://github.com/shift7-ch/katta-server/issues/31 localization
                onCreateError.value = new ErrorWithCodeHint(error.message + ". Check your bucket CORS settings.", `
                aws s3api put-bucket-cors --endpoint-url ${endpoint} --bucket ${vaultBucketName.value} --cors-configuration file://cors.json

                cors.json:
                {
                  "CORSRules": [
                           {
                               "AllowedHeaders": [
                                   "*"
                               ],
                               "AllowedMethods": [
                                   "GET",
                                   "PUT"
                               ],
                               "AllowedOrigins": [
                                   "${document.baseURI}"
                               ],
                               "ExposeHeaders": [
                                   "ETag"
                               ],
                               "MaxAgeSeconds": 3600
                           }
                       ]
                }
                `);
            }
            else{
              console.error('Uploading template failed.', error);
              error instanceof Error ? error : new Error('Unknown Error');
            }
            return;
        }
    }
    else{
        // we assume CORS settings are set correctly by admins
    }
    // \ end katta extension
  if (props.recover) {
    await createVault();
  } else {
    if (!isCommunityLicense.value && settings.value?.enableEmergencyAccess)
      state.value = State.DefineEmergencyAccess;
    else
      state.value = State.ShowRecoveryKey;
  }
}

async function validateVaultEmergencyAccess(){
  await splitRecoveryKey();
  state.value = State.ShowRecoveryKey;
}

function needsRedundancy(): boolean {
  return requiredKeyShares.value == emergencyCouncilMembers.value.length;
}

const vaultKeyShares = ref<Record<string, string>>({});

async function splitRecoveryKey() {
  try {
    onCreateError.value = undefined;

    if (requiredKeyShares.value == null || requiredKeyShares.value < 1) {
      throw new Error(t('grantEmergencyAccessDialog.error.keySharesRequired'));
    }

    if (emergencyCouncilMembers.value.length < 1) {
      throw new Error(t('grantEmergencyAccessDialog.error.councilMembersRequired'));
    }

    if (emergencyCouncilMembers.value.length < requiredKeyShares.value) {
      throw new Error(
        t('grantEmergencyAccessDialog.error.tooFewCouncilMembers')
      );
    }

    let recoveryKeyBytes: Uint8Array;
    switch (vaultType.value) {
      case VaultType.VaultFormat8: {
        if (!vaultFormat8.value) {
          throw new Error('Invalid state');
        }
        recoveryKeyBytes = await vaultFormat8.value.createPaddedRecoveryKeyBytes();
        break;
      }
      case VaultType.UniversalVaultFormat: {
        if (!uvfVault.value) {
          throw new Error('Invalid state');
        }
        recoveryKeyBytes = await uvfVault.value.createPaddedRecoveryKeyBytes();
        break;
      }
    }

    vaultKeyShares.value = await EmergencyAccess.split(
      recoveryKeyBytes,
      requiredKeyShares.value,
      ...emergencyCouncilMembers.value
    );
    vault.value.requiredEmergencyKeyShares = requiredKeyShares.value;
    vault.value.emergencyKeyShares = { ...vaultKeyShares.value };
  } catch (error) {
    console.error('Splitting recovery key failed.', error);
    onCreateError.value = error instanceof Error ? error : new Error('Unknown Error');
    throw error;
  }
}

function backToEnterVaultDetails(){
  state.value = State.EnterVaultDetails;
}

function backToDefineEmergencyAccess(){
  if (isCommunityLicense.value || !settings.value?.enableEmergencyAccess)
    state.value = State.EnterVaultDetails;
  else
    state.value = State.DefineEmergencyAccess;
}

async function createVault() {
  onCreateError.value = undefined;
  try {
    processing.value = true;
    const owner = await userdata.me;
    if (!owner.setupCode) {
      throw new Error('User not set up');
    }
    const ownerGrant: AccessGrant = { userId: owner.id, token: '' };

    switch (vaultType.value) {
      case VaultType.VaultFormat8: {
        if (!vaultFormat8.value) {
          throw new Error('Invalid state');
        }
        ownerGrant.token = await vaultFormat8.value.encryptForUser(await userdata.ecdhPublicKey);
        break;
      }
      case VaultType.UniversalVaultFormat: {
        if (!uvfVault.value) {
          throw new Error('Invalid state');
        }
        // / start katta extension
        if (!uvfVault.value) {
          throw new Error('Invalid state');
        }
        if (!selectedBackend.value) {
          throw new Error('Invalid state');
        }
        if (!selectedRegion.value) {
          throw new Error('Invalid state');
        }

        uvfVault.value.metadata.backend.provider = selectedBackend.value.id;
        uvfVault.value.metadata.backend.defaultPath = selectedBackend.value.bucketPrefix + vault.value.id;
        uvfVault.value.metadata.backend.nickname = vault.value.name;
        uvfVault.value.metadata.backend.region = selectedRegion.value;
        uvfVault.value.metadata.automaticAccessGrant.enabled = automaticAccessGrant.value;

        if(isPermanent.value){
            uvfVault.value.metadata.backend.username = vaultAccessKeyId.value;
            uvfVault.value.metadata.backend.password = vaultSecretKey.value;
            uvfVault.value.metadata.backend.defaultPath = vaultBucketName.value;
        }
        // \ end katta extension

        ownerGrant.token = await uvfVault.value.encryptForUser(await userdata.ecdhPublicKey, true);
        const recoveryPublicKey = await uvfVault.value.recoveryKey.serializePublicKey();
        vault.value.uvfMetadataFile = await uvfVault.value.createMetadataFile(absBackendBaseURL, vault.value);
        vault.value.uvfKeySet = `{"keys": [${recoveryPublicKey}]}`;
        break;
      }
    }
    // / start katta extension
    if (!uvfVault.value) {
      throw new Error('Invalid state');
    }
    if (!selectedBackend.value) {
      throw new Error('Invalid state');
    }
    if (!selectedRegion.value) {
      throw new Error('Invalid state');
    }
    // Decision 2024-02-01 upload vault template/create bucket before creating vault in hub and uploading JWE. This is the most delicate operation. No further rollback for now.
    if(isPermanent.value){
       await uploadVaultTemplate();
    }
    else {
        // N.B. the access tokens for cryptomator and cryptomator hub clients do only have realm roles added to them, but not client roles.
        //      We use client roles for vaults shared with a user. So this setup prevents access tokens from growing with new vaults.
        const token = await authPromise.then(auth => auth.bearerToken());

        // https://docs.aws.amazon.com/AWSJavaScriptSDK/v3/latest/clients/client-sts/classes/stsclient.html

        const stsClient = new STSClient({
            region: selectedRegion.value,
            endpoint: selectedBackend.value.stsEndpoint
        });

        // https://docs.aws.amazon.com/AWSJavaScriptSDK/v3/latest/clients/client-sts/classes/assumerolewithwebidentitycommand.html
        // https://docs.aws.amazon.com/STS/latest/APIReference/API_AssumeRoleWithWebIdentity.html
        // N.B. almost zero trust: add inline policy to pass only credentials allowing for creating the specified bucket in the backend
        const assumeRoleWithWebIdentityArgs = {
          // Required. The OAuth 2.0 access token or OpenID Connect ID token that is provided by the
          // identity provider.
          WebIdentityToken: token,
          RoleSessionName: vault.value.id,
          // Valid Range: Minimum value of 900. Maximum value of 43200.
          DurationSeconds: 900,
          Policy: `{
            "Version": "2012-10-17",
            "Statement": [
              {
                "Effect": "Allow",
                "Action": [
                  "s3:CreateBucket",
                  "s3:GetBucketPolicy",
                  "s3:PutBucketVersioning",
                  "s3:GetBucketVersioning"
                ],
                "Resource": "arn:aws:s3:::{}"
              },
              {
                "Effect": "Allow",
                "Action": [
                  "s3:PutObject"
                ],
                "Resource": [
                  "arn:aws:s3:::{}/*.uvf",
                  "arn:aws:s3:::{}/*/"
                ]
              }
            ]
          }`.replaceAll("{}", uvfVault.value.metadata.backend.defaultPath),
          // Required. ARN of the role that the caller is assuming.
          RoleArn: selectedBackend.value.stsRoleCreateBucketHub
        }


        const { Credentials } = await stsClient
            .send(new AssumeRoleWithWebIdentityCommand(assumeRoleWithWebIdentityArgs));

        if (!Credentials) {
            throw new Error('Invalid state: Could not assume role with web identity.');
        }
        if (!Credentials.AccessKeyId) {
            throw new Error('Invalid state: Missing AccessKeyId.');
        }
        if (!Credentials.SecretAccessKey) {
            throw new Error('Invalid state: Missing SecretAccessKey.');
        }
        if (!Credentials.SessionToken) {
            throw new Error('Invalid state: Missing SessionToken.');
        }

        const rootDirId = await uvfVault.value.computeRootDirId();
        const rootDirHash = await uvfVault.value.computeRootDirIdHash(rootDirId);
        if (!rootDirHash) {
            throw new Error('Invalid state: rootDirHash missing.');
        }
        if (!vault.value?.uvfMetadataFile) {
            throw new Error('Invalid state: uvfMetadataFile missing.');
        }
        const dirFile = await uvfVault.value.encryptFile(rootDirId, uvfVault.value.metadata.initialSeedId);
        await backend.storage.put(vault.value.id, {
            vaultId: vault.value.id,
            storageConfigId: selectedBackend.value.id,
            vaultUvf: vault.value.uvfMetadataFile,
            dirUvf: base64urlnopad.encode(dirFile),
            rootDirHash: rootDirHash,
            // https://github.com/awslabs/smithy-typescript/blob/697310da9aec949034f92598f5cefc2cc162ef4d/packages/types/src/identity/awsCredentialIdentity.ts#L24
            awsAccessKey: Credentials.AccessKeyId,
            awsSecretKey: Credentials.SecretAccessKey,
            sessionToken: Credentials.SessionToken,
            region: selectedRegion.value

        });
    }
    // \ end katta extension
    var minio = (!isPermanent.value) && (selectedBackend.value.hostname != null);
    var aws = (!isPermanent.value) && ((selectedBackend.value.hostname == null) || isAwsHostname(selectedBackend.value.hostname ));

    await backend.vaults.createOrUpdateVault(vault.value, aws, minio);
    await backend.vaults.grantAccess(vault.value.id, ownerGrant);
    state.value = State.Finished;
  } catch (error) {
    console.error('Creating vault failed.', error);

    // / start katta extension
    if(typeof(error) === 'string'){
        onCreateError.value = new Error(error);
    }
    else if((error instanceof AxiosError)){
      var msg = error.message;
      if(error.response?.statusText){
        msg += ` (${error.response?.statusText}).`;
      }
      else{
        msg += `.`;
      }
      if(error.response?.status === 409){
        msg += ` Details: Bucket ${uvfVault.value?.metadata.backend.defaultPath} already exists or no permission to list.`;
      }
      else if(error.response?.data.details){
        msg += ` Details: ${error.response.data.details}.`;
      }
      onCreateError.value = new Error(msg);
    }
    else if(error instanceof Error){
        onCreateError.value = error;
    }
    else {
        onCreateError.value = new Error('Unknown reason');
    }
    // \ end katta extension
  } finally {
    processing.value = false;
  }
}

async function copyRecoveryKey() {
  await navigator.clipboard.writeText(recoveryKeyStr.value);
  copiedRecoveryKey.value = true;
  debouncedCopyFinish();
}

async function downloadVaultTemplate() {
  if (!vaultFormat8.value && !uvfVault.value) {
    throw new Error('Invalid state');
  }
  onDownloadTemplateError.value = undefined;
  try {
    const templateProducer: VaultTemplateProducing = vaultFormat8.value || uvfVault.value!;
    const blob = await templateProducer.exportTemplate(absBackendBaseURL, vault.value);
    if (blob != null) {
      saveAs(blob, `${vault.value.name}.zip`);
    } else {
      throw new EmptyVaultTemplateError();
    }
  } catch (error) {
    console.error('Exporting vault template failed.', error);
    onDownloadTemplateError.value = error instanceof Error ? error : new Error('Unknown reason');
  }
}

// / start katta extension
import { baseURL } from '../common/config';
async function openBookmark() {
  onOpenBookmarkError.value = null;
  try {
    window.location.href = `katta://${new URL(location.origin).host}${baseURL}`;
  } catch (error) {
    console.error('Opening bookmark from browser failed.', error);
    onOpenBookmarkError.value = error instanceof Error ? error : new Error('Unknown Error');
  }
}

function setRegionsOnSelectStorage(storage: StorageProfileDto){
    console.log('selected storage ' + storage.name);
    regions.value = storage.regions;
    console.log('   available regions: ' + storage.regions);
    selectedRegion.value = storage.region;
    console.log('   default region: ' + storage.region);
    if (!selectedBackend.value) {
      throw new Error('Invalid state.');
    }
    isPermanent.value = selectedBackend.value['protocol'] === 'S3STATIC';
    console.log('   isPermanent: ' + isPermanent.value);
}

async function uploadVaultTemplate() {
  onUploadTemplateError.value = null;
  try {
    if (!selectedBackend.value) {
        throw new Error('Invalid state.');
    }
    const endpoint = (selectedBackend.value.scheme && selectedBackend.value.hostname && selectedBackend.value.port) ? `${selectedBackend.value.scheme}://${selectedBackend.value.hostname}:${selectedBackend.value.port}` : undefined;
    const client = new S3Client({
        region: selectedRegion.value,
        endpoint: endpoint,
        forcePathStyle: selectedBackend.value.withPathStyleAccessEnabled,
        credentials:{
            accessKeyId: vaultAccessKeyId.value,
            secretAccessKey: vaultSecretKey.value
        }
    });
    const commandListObjects = new ListObjectsV2Command({
        Bucket: vaultBucketName.value,
        MaxKeys: 1,
      });
    const responseListObjects = await client.send(commandListObjects);
    console.log(responseListObjects);
    if(responseListObjects.KeyCount != 0){
        throw new Error('Bucket not empty, cannot upload template. Empty the bucket manually and re-try.');
    }
    if(!uvfVault.value){
       throw new Error('Invalid state');
    }

    const rootDirHash = await uvfVault.value.computeRootDirIdHash(await uvfVault.value.computeRootDirId());
    console.log(rootDirHash);

    if (!rootDirHash) {
        throw new Error('Invalid state: rootDirHash missing.');
    }

    const commandPutVaultCryptomator = new PutObjectCommand({
        Bucket: vaultBucketName.value,
        Key: 'vault.uvf',
        Body: vault.value.uvfMetadataFile
    });
    console.log(commandPutVaultCryptomator);
    const responsePutVaultCryptomator = await client.send(commandPutVaultCryptomator);
    console.log(responsePutVaultCryptomator);

    const commandPutDFolder = new PutObjectCommand({
        Bucket: vaultBucketName.value,
        Key: `d/${rootDirHash.substring(0, 2)}/${rootDirHash.substring(2)}/`,
        Body: '',
    });
    console.log(commandPutDFolder);
    const responsePutDFolder = await client.send(commandPutDFolder);
    console.log(responsePutDFolder);

  } catch (error) {
    console.error('Uploading vault template failed.', error);
    onUploadTemplateError.value = error instanceof Error ? error : new Error('Unknown reason');
  }
}
class StorageProfileError extends Error {
  constructor(s: string) {
    super(s);
  }
}
// \ end katta extension

</script>

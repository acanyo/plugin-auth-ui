<script setup lang="ts">
import { Toast, VButton, VLoading } from '@halo-dev/components'
import { computed, onMounted, ref } from 'vue'
import RiInformationLine from '~icons/ri/information-line'

import SectionHeader from '../workbench/SectionHeader.vue'
import {
  DEFAULT_SETTINGS,
  fetchBasicSettings,
  saveBasicSettings,
  type BasicSettings,
  type DisplayMode,
} from './basic-settings'

const saved = ref<BasicSettings>({ ...DEFAULT_SETTINGS })
const draft = ref<BasicSettings>({ ...DEFAULT_SETTINGS })
const loading = ref(true)
const saving = ref(false)

const dirty = computed(
  () => draft.value.logo !== saved.value.logo || draft.value.displayMode !== saved.value.displayMode,
)

const modes: { value: DisplayMode; title: string; description: string }[] = [
  {
    value: 'modal',
    title: '弹窗',
    description: '站内登录、注册、退出链接和直接访问都在当前页面打开弹窗，无需修改主题。',
  },
  {
    value: 'page',
    title: '独立页面',
    description: '跳转到完整的登录、注册页面。站点不再加载弹窗脚本。',
  },
]

async function load() {
  loading.value = true
  try {
    saved.value = await fetchBasicSettings()
    draft.value = { ...saved.value }
  } catch (error) {
    console.error('Failed to load auth-ui settings', error)
    Toast.error('读取设置失败')
  } finally {
    loading.value = false
  }
}

async function save() {
  saving.value = true
  try {
    await saveBasicSettings(draft.value)
    saved.value = { ...draft.value }
    Toast.success('设置已保存')
  } catch (error) {
    console.error('Failed to save auth-ui settings', error)
    Toast.error('保存失败，请稍后重试')
  } finally {
    saving.value = false
  }
}

function reset() {
  draft.value = { ...saved.value }
}

onMounted(load)
</script>

<template>
  <SectionHeader title="基础设置" description="设置登录界面的 Logo，以及访客登录时看到弹窗还是独立页面。" />

  <VLoading v-if="loading" />
  <section v-else class="au-panel" aria-labelledby="au-basic-heading">
    <div class="au-panel-heading">
      <div>
        <h2 id="au-basic-heading">登录界面</h2>
        <p>修改后点击保存，站点立即生效。</p>
      </div>
      <span v-if="dirty" class="au-unsaved"><span class="au-status-dot" />有未保存的修改</span>
    </div>

    <div class="au-panel-section">
      <header>
        <h3>Logo</h3>
        <p>显示在登录、注册和退出界面顶部。留空时显示站点名称首字。</p>
      </header>
      <FormKit
        v-model="draft.logo"
        type="attachment"
        name="logo"
        label="Logo 图片"
        :accepts="['image/*']"
      />
    </div>

    <div class="au-panel-section">
      <header>
        <h3>登录方式</h3>
        <p>后台登录与退出在这两种方式下都可用，登录成功后回到原来的页面。</p>
      </header>
      <fieldset class="au-choices">
        <legend class="sr-only">登录方式</legend>
        <label v-for="mode in modes" :key="mode.value" class="au-choice">
          <input v-model="draft.displayMode" type="radio" name="displayMode" :value="mode.value" />
          <strong>{{ mode.title }}</strong>
          <span>{{ mode.description }}</span>
        </label>
      </fieldset>
      <p class="au-notice" style="margin-top: 16px">
        <RiInformationLine width="15" height="15" aria-hidden="true" />
        浏览器不支持弹窗时会自动使用独立页面。
      </p>
    </div>

    <div class="au-panel-footer">
      <span>{{ dirty ? '保存后对站点立即生效' : '所有修改已保存' }}</span>
      <div class="au-panel-actions">
        <VButton :disabled="!dirty || saving" @click="reset">撤销</VButton>
        <VButton
          v-permission="['system:plugins:manage']"
          type="secondary"
          :disabled="!dirty"
          :loading="saving"
          @click="save"
        >
          保存
        </VButton>
      </div>
    </div>
  </section>
</template>

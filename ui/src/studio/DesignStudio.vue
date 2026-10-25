<script setup lang="ts">
import { Dialog, Toast, VLoading } from '@halo-dev/components'
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { onBeforeRouteLeave, useRoute, useRouter } from 'vue-router'
import RiArrowLeftLine from '~icons/ri/arrow-left-line'
import RiCheckboxCircleLine from '~icons/ri/checkbox-circle-line'
import RiCodeSSlashLine from '~icons/ri/code-s-slash-line'
import RiLayoutLine from '~icons/ri/layout-line'
import RiPaletteLine from '~icons/ri/palette-line'
import RiSparkling2Line from '~icons/ri/sparkling-2-line'
import RiSparklingLine from '~icons/ri/sparkling-line'

import AssistantPanel from '../assistant/AssistantPanel.vue'
import { compactDesign, normalizeDesign } from '../templates/design'
import { BLANK_TEMPLATE, STARTER_TEMPLATES } from '../templates/starter-templates'
import {
  createTemplate,
  fetchActiveTemplateName,
  getTemplate,
  setActiveTemplateName,
  updateTemplate,
} from '../templates/template-api'
import type { AuthTemplate, TemplateDraft } from '../templates/types'
import { studioLocation, workbenchLocation } from '../workbench/navigation'
import '../workbench/styles/index.css'
import './studio.css'
import AppearancePanel from './panels/AppearancePanel.vue'
import CodePanel from './panels/CodePanel.vue'
import ContentPanel from './panels/ContentPanel.vue'
import MotionPanel from './panels/MotionPanel.vue'
import PreviewPane from './PreviewPane.vue'
import { useDraftPreview } from './useDraftPreview'

const route = useRoute()
const router = useRouter()

const template = ref<AuthTemplate>()
const design = ref<TemplateDraft>()
const savedSnapshot = ref('')
const activeName = ref('')
const loading = ref(true)
const saving = ref(false)
const preview = ref<InstanceType<typeof PreviewPane>>()

const panels = [
  { id: 'ai', label: 'AI', icon: RiSparkling2Line },
  { id: 'appearance', label: '外观', icon: RiPaletteLine },
  { id: 'motion', label: '动效', icon: RiSparklingLine },
  { id: 'content', label: '内容', icon: RiLayoutLine },
  { id: 'code', label: '代码', icon: RiCodeSSlashLine },
] as const
type PanelId = (typeof panels)[number]['id']
// The AI chat opens first so it is easy to find; `?panel=` opens another one.
const panel = ref<PanelId>(panels.find((item) => item.id === route.query.panel)?.id ?? 'ai')

const isNew = computed(() => !template.value)
const dirty = computed(() => !!design.value && JSON.stringify(compactDesign(design.value)) !== savedSnapshot.value)
const isActive = computed(() => !!template.value && template.value.metadata.name === activeName.value)
const { draftId, updating, failed } = useDraftPreview(design)

async function load() {
  loading.value = true
  try {
    const name = String(route.params.name ?? 'new')
    activeName.value = await fetchActiveTemplateName()
    if (name === 'new') {
      template.value = undefined
      const from = String(route.query.from ?? '')
      const starter = from.startsWith('starter-') ? STARTER_TEMPLATES[Number(from.slice(8))] : undefined
      design.value = normalizeDesign(starter ?? BLANK_TEMPLATE)
      savedSnapshot.value = ''
    } else {
      template.value = await getTemplate(name)
      design.value = normalizeDesign(template.value.spec)
      savedSnapshot.value = JSON.stringify(compactDesign(design.value))
    }
  } catch (error) {
    console.error('Failed to load auth-ui design', error)
    Toast.error('读取设计失败')
    void router.replace(workbenchLocation('templates'))
  } finally {
    loading.value = false
  }
}

async function save(activate = false) {
  if (!design.value) return
  if (!design.value.displayName?.trim()) {
    Toast.warning('请填写设计名称')
    return
  }
  saving.value = true
  try {
    const spec = compactDesign(design.value)
    const saved = template.value ? await updateTemplate(template.value, spec) : await createTemplate(spec)
    template.value = saved
    savedSnapshot.value = JSON.stringify(spec)
    if (activate) {
      await setActiveTemplateName(saved.metadata.name)
      activeName.value = saved.metadata.name
    }
    Toast.success(activate ? '已保存并启用' : '设计已保存')
    if (route.params.name === 'new') {
      await router.replace(studioLocation(saved.metadata.name))
    }
  } catch (error) {
    console.error('Failed to save auth-ui design', error)
    Toast.error('保存失败，设计可能已在别处修改，请刷新后重试')
  } finally {
    saving.value = false
  }
}

// Set once the user chose to discard, so the repeated navigation passes the guard.
let discarding = false

onBeforeRouteLeave((to) => {
  if (discarding || !dirty.value || saving.value) return true
  // A guard settles the navigation once, so the dialog cannot resume it; it starts a new one.
  Dialog.warning({
    title: '放弃未保存的修改？',
    description: '离开后，本次修改不会保存。',
    confirmText: '放弃修改',
    cancelText: '继续编辑',
    onConfirm: () => {
      discarding = true
      void router.push(to.fullPath).finally(() => (discarding = false))
    },
  })
  return false
})

// Ctrl/Cmd + S saves without leaving the editor.
function onKeydown(event: KeyboardEvent) {
  if ((event.metaKey || event.ctrlKey) && event.key.toLowerCase() === 's') {
    event.preventDefault()
    if (dirty.value && !saving.value) void save()
  }
}
function onBeforeUnload(event: BeforeUnloadEvent) {
  if (dirty.value) event.preventDefault()
}

onMounted(() => {
  void load()
  window.addEventListener('keydown', onKeydown)
  window.addEventListener('beforeunload', onBeforeUnload)
})
onBeforeUnmount(() => {
  window.removeEventListener('keydown', onKeydown)
  window.removeEventListener('beforeunload', onBeforeUnload)
})
watch(() => route.params.name, (name, previous) => {
  // Saving a new design swaps "new" for its name; the loaded design stays as it is.
  if (previous === 'new' && template.value?.metadata.name === name) return
  void load()
})
</script>

<template>
  <div class="au-workbench st-studio">
    <header class="st-header">
      <RouterLink :to="workbenchLocation('templates')" class="au-text-button" aria-label="返回设计列表">
        <RiArrowLeftLine width="16" height="16" /><span>设计列表</span>
      </RouterLink>
      <div v-if="design" class="st-title">
        <input v-model="design.displayName" type="text" maxlength="40" aria-label="设计名称" placeholder="设计名称" />
        <span v-if="isActive" class="au-badge"><span class="au-status-dot is-ready" />使用中</span>
        <span v-else-if="isNew" class="au-chip">未保存</span>
      </div>
      <div class="st-header-actions">
        <span class="au-unsaved" :class="{ 'is-clean': !dirty }">
          <span class="au-status-dot" :class="{ 'is-ready': !dirty }" />{{ dirty ? '有未保存的修改' : '已保存' }}
        </span>
        <button type="button" class="au-button" :disabled="saving || (!dirty && !isNew)" @click="save(false)">保存</button>
        <button
          v-if="!isActive"
          type="button"
          class="au-button is-primary"
          :disabled="saving"
          @click="save(true)"
        >
          <RiCheckboxCircleLine width="15" height="15" />保存并启用
        </button>
      </div>
    </header>

    <VLoading v-if="loading || !design" />
    <div v-else class="st-body" :data-panel="panel">
      <aside class="st-sidebar">
        <nav class="st-nav" role="tablist" aria-label="编辑分类">
          <button
            v-for="item in panels"
            :key="item.id"
            type="button"
            role="tab"
            :aria-selected="panel === item.id"
            @click="panel = item.id"
          >
            <component :is="item.icon" width="17" height="17" aria-hidden="true" />
            <span>{{ item.label }}</span>
          </button>
        </nav>
        <div class="st-panel" :class="{ 'is-chat': panel === 'ai' }" role="tabpanel">
          <label v-if="panel === 'appearance'" class="st-field st-section">
            <span class="st-label">描述</span>
            <input v-model="design.description" type="text" maxlength="200" placeholder="一句话介绍这个设计" />
          </label>
          <!-- v-show keeps the conversation and its pending request while switching panels. -->
          <AssistantPanel v-show="panel === 'ai'" v-model="design" />
          <AppearancePanel v-if="panel === 'appearance'" v-model="design" />
          <MotionPanel v-else-if="panel === 'motion'" v-model="design" @replay="preview?.replay()" />
          <ContentPanel v-else-if="panel === 'content'" v-model="design" />
          <CodePanel v-else-if="panel === 'code'" v-model="design" />
        </div>
      </aside>
      <PreviewPane ref="preview" :draft="draftId" :updating="updating" :failed="failed" />
    </div>
  </div>
</template>

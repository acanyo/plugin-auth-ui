<script setup lang="ts">
import { Dialog, Toast, VDropdown, VDropdownItem, VLoading } from '@halo-dev/components'
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import RiAddLine from '~icons/ri/add-line'
import RiMore2Line from '~icons/ri/more-2-line'
import RiPaletteLine from '~icons/ri/palette-line'
import RiSparkling2Line from '~icons/ri/sparkling-2-line'

import SectionHeader from '../workbench/SectionHeader.vue'
import { studioLocation } from '../workbench/navigation'
import { STARTER_TEMPLATES } from './starter-templates'
import {
  createTemplate,
  deleteTemplate,
  fetchActiveTemplateName,
  listTemplates,
  setActiveTemplateName,
} from './template-api'
import DesignThumbnail from './DesignThumbnail.vue'
import type { AuthTemplate } from './types'

const router = useRouter()
const templates = ref<AuthTemplate[]>([])
const activeName = ref('')
const loading = ref(true)
const busy = ref(false)

async function load() {
  loading.value = templates.value.length === 0
  try {
    const [items, active] = await Promise.all([listTemplates(), fetchActiveTemplateName()])
    templates.value = items
    activeName.value = active
  } catch (error) {
    console.error('Failed to load auth-ui designs', error)
    Toast.error('读取登录设计失败')
  } finally {
    loading.value = false
  }
}

async function activate(template: AuthTemplate) {
  busy.value = true
  try {
    await setActiveTemplateName(template.metadata.name)
    activeName.value = template.metadata.name
    Toast.success(`已启用“${template.spec.displayName}”`)
  } catch (error) {
    console.error('Failed to activate auth-ui design', error)
    Toast.error('启用失败，请稍后重试')
  } finally {
    busy.value = false
  }
}

async function useDefaultLook() {
  busy.value = true
  try {
    await setActiveTemplateName('')
    activeName.value = ''
    Toast.success('已恢复插件默认样式')
  } finally {
    busy.value = false
  }
}

async function duplicate(template: AuthTemplate) {
  busy.value = true
  try {
    const copy = await createTemplate({
      ...structuredClone(JSON.parse(JSON.stringify(template.spec))),
      displayName: `${template.spec.displayName} 副本`.slice(0, 40),
    })
    Toast.success('已复制')
    await load()
    void router.push(studioLocation(copy.metadata.name))
  } catch (error) {
    console.error('Failed to copy auth-ui design', error)
    Toast.error('复制失败，请稍后重试')
  } finally {
    busy.value = false
  }
}

function confirmDelete(template: AuthTemplate) {
  const active = template.metadata.name === activeName.value
  Dialog.warning({
    title: `删除设计“${template.spec.displayName}”`,
    description: active
      ? '这是正在使用的设计，删除后登录页会恢复插件默认样式。此操作无法撤销。'
      : '此操作无法撤销。',
    confirmType: 'danger',
    confirmText: '删除',
    cancelText: '取消',
    async onConfirm() {
      try {
        await deleteTemplate(template.metadata.name)
        if (active) await setActiveTemplateName('')
        Toast.success('设计已删除')
        await load()
      } catch (error) {
        console.error('Failed to delete auth-ui design', error)
        Toast.error('删除失败，请稍后重试')
      }
    },
  })
}

onMounted(load)
</script>

<template>
  <SectionHeader
    title="登录设计"
    description="设计保存在数据库中，可以在可视化编辑器里调整外观、动效和内容，也可以直接编写 HTML、CSS 与 JavaScript，或让 AI 助手通过 MCP 创建。"
  >
    <template #actions>
      <button
        v-if="activeName"
        v-permission="['system:plugins:manage']"
        type="button"
        class="au-button"
        :disabled="busy"
        @click="useDefaultLook"
      >
        恢复默认样式
      </button>
      <RouterLink v-permission="['system:plugins:manage']" :to="studioLocation(undefined, undefined, 'ai')" class="au-button" title="在设计工作室中和 AI 对话，生成新的登录设计">
        <RiSparkling2Line width="15" height="15" aria-hidden="true" />用 AI 设计
      </RouterLink>
      <RouterLink v-permission="['system:plugins:manage']" :to="studioLocation()" class="au-button is-primary">
        <RiAddLine width="15" height="15" aria-hidden="true" />新建设计
      </RouterLink>
    </template>
  </SectionHeader>

  <VLoading v-if="loading" />

  <template v-else>
    <div v-if="templates.length" class="au-design-grid">
      <article
        v-for="template in templates"
        :key="template.metadata.name"
        class="au-design-card"
        :class="{ 'is-active': activeName === template.metadata.name }"
      >
        <RouterLink :to="studioLocation(template.metadata.name)" class="au-design-thumb" :aria-label="`编辑 ${template.spec.displayName}`">
          <DesignThumbnail :template="template.metadata.name" />
          <span class="au-design-thumb-hover"><RiPaletteLine width="16" height="16" />打开编辑器</span>
        </RouterLink>
        <div class="au-design-meta">
          <div class="au-design-title">
            <strong>{{ template.spec.displayName }}</strong>
            <span v-if="activeName === template.metadata.name" class="au-badge"><span class="au-status-dot is-ready" />使用中</span>
          </div>
          <p>{{ template.spec.description || '没有描述' }}</p>
        </div>
        <div v-permission="['system:plugins:manage']" class="au-design-actions">
          <button
            v-if="activeName !== template.metadata.name"
            type="button"
            class="au-button is-small"
            :disabled="busy"
            @click="activate(template)"
          >
            启用
          </button>
          <RouterLink :to="studioLocation(template.metadata.name)" class="au-button is-small">编辑</RouterLink>
          <VDropdown>
            <button type="button" class="au-icon-button" aria-label="更多操作"><RiMore2Line width="16" height="16" /></button>
            <template #popper>
              <VDropdownItem @click="duplicate(template)">复制</VDropdownItem>
              <VDropdownItem type="danger" @click="confirmDelete(template)">删除</VDropdownItem>
            </template>
          </VDropdown>
        </div>
      </article>
    </div>

    <div v-else class="au-panel au-state" role="status">
      <RiPaletteLine width="28" height="28" aria-hidden="true" />
      <h2>还没有登录设计</h2>
      <p>当前使用插件默认样式。从下方的起点开始，或新建一个空白设计。</p>
    </div>

    <section v-permission="['system:plugins:manage']" class="au-starters" aria-labelledby="au-starters-title">
      <header>
        <h2 id="au-starters-title">从起点开始</h2>
        <p>选择一个起点进入编辑器，保存后才会写入数据库。</p>
      </header>
      <div class="au-starter-grid">
        <RouterLink
          v-for="(starter, index) in STARTER_TEMPLATES"
          :key="starter.displayName"
          :to="studioLocation(undefined, `starter-${index}`)"
          class="au-starter"
        >
          <span
            class="au-starter-swatch"
            :style="{
              '--a': starter.theme?.background?.color ?? '#eef4f3',
              '--b': starter.theme?.background?.color2 ?? starter.theme?.background?.color ?? '#dcebe8',
              '--p': starter.theme?.primary ?? '#176b62',
            }"
            aria-hidden="true"
          >
            <i :class="[starter.theme?.layout, { glass: starter.displayName === '液态玻璃' }]" />
          </span>
          <strong>{{ starter.displayName }}</strong>
          <span>{{ starter.description }}</span>
        </RouterLink>
        <RouterLink :to="studioLocation()" class="au-starter is-blank">
          <span class="au-starter-swatch" aria-hidden="true"><RiAddLine width="22" height="22" /></span>
          <strong>空白设计</strong>
          <span>从插件默认样式开始。</span>
        </RouterLink>
      </div>
    </section>
  </template>
</template>

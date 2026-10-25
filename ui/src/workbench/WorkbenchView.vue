<script setup lang="ts">
import { computed, defineAsyncComponent, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import RiExternalLinkLine from '~icons/ri/external-link-line'
import RiShieldUserLine from '~icons/ri/shield-user-line'

import { WORKBENCH_TABS, getWorkbenchTab, workbenchLocation, type WorkbenchTabId } from './navigation'
import './styles/index.css'

const route = useRoute()
const router = useRouter()
const activeTab = computed(() => getWorkbenchTab(route.query.tab))

const panels = {
  settings: defineAsyncComponent(() => import('../settings/BasicSettingsPanel.vue')),
  templates: defineAsyncComponent(() => import('../templates/TemplateManager.vue')),
  mcp: defineAsyncComponent(() => import('../mcp/McpPanel.vue')),
}

function navigate(tab: WorkbenchTabId) {
  if (tab !== activeTab.value.id) void router.push(workbenchLocation(tab))
}

// Arrow keys move focus between tabs; Enter or Space opens the focused one.
async function onTabKeydown(event: KeyboardEvent, index: number) {
  const count = WORKBENCH_TABS.length
  const next = { ArrowRight: (index + 1) % count, ArrowLeft: (index - 1 + count) % count, Home: 0, End: count - 1 }[
    event.key
  ]
  if (next === undefined) return
  event.preventDefault()
  await nextTick()
  const tablist = (event.currentTarget as HTMLElement).parentElement
  tablist?.querySelectorAll<HTMLButtonElement>('[role="tab"]')[next]?.focus()
}
</script>

<template>
  <div class="au-workbench">
    <header class="au-app-header">
      <RouterLink :to="workbenchLocation('settings')" class="au-brand" aria-label="认证界面设置">
        <span class="au-brand-mark" aria-hidden="true"><RiShieldUserLine width="19" height="19" /></span>
        <strong>认证界面</strong>
        <span class="au-brand-divider" aria-hidden="true">/</span>
        <span class="au-brand-caption">登录 · 注册 · 退出</span>
      </RouterLink>
      <div class="au-header-links">
        <a href="/login?auth_ui_page=1" target="_blank" rel="noopener" class="au-text-button" title="在新窗口打开站点登录页">
          <RiExternalLinkLine width="15" height="15" /><span>查看登录页</span>
        </a>
      </div>
    </header>

    <nav class="au-tabs" role="tablist" aria-label="认证界面功能">
      <button
        v-for="(tab, index) in WORKBENCH_TABS"
        :id="`au-tab-${tab.id}`"
        :key="tab.id"
        type="button"
        role="tab"
        :class="{ 'is-active': activeTab.id === tab.id }"
        :aria-selected="activeTab.id === tab.id"
        :aria-controls="`au-panel-${tab.id}`"
        :tabindex="activeTab.id === tab.id ? 0 : -1"
        @click="navigate(tab.id)"
        @keydown="onTabKeydown($event, index)"
      >
        <component :is="tab.icon" width="16" height="16" aria-hidden="true" />
        {{ tab.label }}
      </button>
    </nav>

    <main
      :id="`au-panel-${activeTab.id}`"
      class="au-main"
      role="tabpanel"
      :aria-labelledby="`au-tab-${activeTab.id}`"
      tabindex="0"
    >
      <component :is="panels[activeTab.id]" :key="activeTab.id" />
    </main>

    <footer class="au-footer">
      <span>认证界面</span><span>沿用 Halo 原生登录、注册与退出流程</span>
    </footer>
  </div>
</template>

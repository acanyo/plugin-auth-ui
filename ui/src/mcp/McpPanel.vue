<script setup lang="ts">
import { VLoading } from '@halo-dev/components'
import { computed, onMounted, ref } from 'vue'
import RiCheckboxCircleFill from '~icons/ri/checkbox-circle-fill'
import RiDownload2Line from '~icons/ri/download-2-line'
import RiErrorWarningLine from '~icons/ri/error-warning-line'
import RiExternalLinkLine from '~icons/ri/external-link-line'
import RiRefreshLine from '~icons/ri/refresh-line'

import SectionHeader from '../workbench/SectionHeader.vue'
import CopyButton from './CopyButton.vue'
import {
  GUIDE_PATH,
  SKILL_PATH,
  clientConfigs,
  mcpEndpoint,
  tokenExport,
} from './client-config'
import {
  MCP_SERVER_REPO_URL,
  MCP_SERVER_STORE_URL,
  fetchMcpStatus,
  type McpStatus,
} from './mcp-status'
import { AUTH_UI_TOOLS, EXAMPLE_PROMPTS } from './tools'

const status = ref<McpStatus>()
const loading = ref(true)
const refreshing = ref(false)
const origin = window.location.origin
const configs = clientConfigs(origin)
const client = ref(configs[0]!.id)
const activeConfig = computed(() => configs.find((item) => item.id === client.value) ?? configs[0]!)

const ready = computed(() => status.value?.server === 'ready')
const tools = computed(() => status.value?.tools ?? AUTH_UI_TOOLS)
const hasKey = computed(() => (status.value?.keysWithAccess ?? 0) > 0)

const serverText = computed(() => {
  switch (status.value?.server) {
    case 'ready':
      return `已启用 MCP Server ${status.value.version ?? ''}`.trim()
    case 'disabled':
      return '已安装 MCP Server，但尚未启用'
    case 'starting':
      return 'MCP Server 正在启动'
    case 'failed':
      return 'MCP Server 启动失败，请在插件页查看原因'
    case 'unknown':
      return '无法读取 MCP Server 状态'
    default:
      return '尚未安装 MCP Server'
  }
})

const keyText = computed(() => {
  const count = status.value?.keysWithAccess
  if (!ready.value) return '启用 MCP Server 后创建'
  if (count === null || count === undefined) return '仅超级管理员可查看访问密钥'
  return count > 0 ? `${count} 个访问密钥可以使用这些工具` : '还没有访问密钥勾选这些工具'
})

async function load(manual = false) {
  refreshing.value = manual
  try {
    status.value = await fetchMcpStatus()
  } finally {
    loading.value = false
    refreshing.value = false
  }
}

onMounted(() => load())
</script>

<template>
  <SectionHeader
    title="AI 设计"
    description="通过 Halo 官方的 MCP Server，让 Claude Code、Codex、Cursor 等 AI 助手读取、创建、检查、预览和启用登录设计。"
  >
    <template #actions>
      <button type="button" class="au-button" :disabled="refreshing" @click="load(true)">
        <RiRefreshLine width="15" height="15" aria-hidden="true" :class="{ 'au-spin': refreshing }" />刷新状态
      </button>
    </template>
  </SectionHeader>

  <VLoading v-if="loading" />
  <div v-else class="au-mcp">
    <ol class="au-mcp-steps">
      <!-- 1. MCP Server -->
      <li class="au-mcp-step" :data-done="ready">
        <span class="au-mcp-step-mark" aria-hidden="true">
          <RiCheckboxCircleFill v-if="ready" width="20" height="20" /><span v-else>1</span>
        </span>
        <div class="au-mcp-step-body">
          <header>
            <h2>安装并启用 MCP Server</h2>
            <span class="au-mcp-state" :data-state="status?.server" role="status">{{ serverText }}</span>
          </header>
          <p>本插件的 AI 工具由 Halo 官方插件 MCP Server（1.3.0 及以上）对外提供。未安装时插件照常运行，只是没有 AI 工具。</p>
          <div v-if="!ready" class="au-mcp-actions">
            <a :href="MCP_SERVER_STORE_URL" target="_blank" rel="noopener" class="au-button is-primary">
              <RiExternalLinkLine width="15" height="15" aria-hidden="true" />在应用市场查看
            </a>
            <RouterLink to="/plugins" class="au-button">前往插件管理</RouterLink>
            <a :href="MCP_SERVER_REPO_URL" target="_blank" rel="noopener" class="au-text-button">GitHub</a>
          </div>
        </div>
      </li>

      <!-- 2. Access key -->
      <li class="au-mcp-step" :data-done="ready && hasKey">
        <span class="au-mcp-step-mark" aria-hidden="true">
          <RiCheckboxCircleFill v-if="ready && hasKey" width="20" height="20" /><span v-else>2</span>
        </span>
        <div class="au-mcp-step-body">
          <header>
            <h2>创建访问密钥并勾选工具</h2>
            <span class="au-mcp-state" :data-state="hasKey ? 'ready' : 'pending'" role="status">{{ keyText }}</span>
          </header>
          <p>
            在「工具 → MCP 服务」中新建访问密钥，在工具列表的「认证界面 → 登录界面设计」下勾选需要的工具。新工具不会自动加入已有密钥；只读工具适合先体验。密钥只显示一次，请妥善保存。
          </p>
          <div v-if="ready" class="au-mcp-actions">
            <RouterLink to="/mcp" class="au-button" :class="{ 'is-primary': !hasKey }">打开 MCP 服务</RouterLink>
          </div>
        </div>
      </li>

      <!-- 3. Client -->
      <li class="au-mcp-step">
        <span class="au-mcp-step-mark" aria-hidden="true"><span>3</span></span>
        <div class="au-mcp-step-body">
          <header>
            <h2>在 AI 客户端中连接</h2>
          </header>
          <p>先把密钥放进环境变量，不要写进配置文件或提交到代码仓库。生产环境请使用 HTTPS。</p>
          <div class="au-mcp-code">
            <div class="au-mcp-code-bar"><span>终端</span><CopyButton :text="tokenExport()" /></div>
            <pre><code>{{ tokenExport() }}</code></pre>
          </div>
          <div class="au-mcp-code">
            <div class="au-mcp-code-bar">
              <div class="au-segmented" role="group" aria-label="AI 客户端">
                <button
                  v-for="item in configs"
                  :key="item.id"
                  type="button"
                  :aria-pressed="client === item.id"
                  @click="client = item.id"
                >
                  {{ item.label }}
                </button>
              </div>
              <CopyButton :text="activeConfig.snippet" />
            </div>
            <pre><code>{{ activeConfig.snippet }}</code></pre>
            <p class="au-mcp-code-note">写入 {{ activeConfig.file }}。其他客户端填写 MCP 地址 <code>{{ mcpEndpoint(origin) }}</code> 和 Bearer 密钥即可。</p>
          </div>
        </div>
      </li>

      <!-- 4. Start -->
      <li class="au-mcp-step">
        <span class="au-mcp-step-mark" aria-hidden="true"><span>4</span></span>
        <div class="au-mcp-step-body">
          <header>
            <h2>直接告诉 AI 你想要的登录页</h2>
          </header>
          <p>
            无需安装 Skill。连接后 AI 会先调用「获取设计指南」，读取设计格式和工作流程：保存前检查并修正，给你预览链接，启用前一定先征得你的同意。可以从右侧的示例开始。
          </p>
          <div class="au-mcp-actions">
            <a :href="GUIDE_PATH" target="_blank" rel="noopener" class="au-text-button">查看设计指南</a>
            <a :href="SKILL_PATH" download="SKILL.md" class="au-text-button">
              <RiDownload2Line width="15" height="15" aria-hidden="true" />下载 SKILL.md（离线或自定义流程时使用）
            </a>
          </div>
        </div>
      </li>
    </ol>

    <aside class="au-mcp-side">
      <section class="au-panel au-mcp-card" aria-labelledby="au-mcp-tools">
        <header>
          <h2 id="au-mcp-tools">提供的工具</h2>
          <span class="au-chip">{{ tools.length }} 个</span>
        </header>
        <p v-if="ready && status?.tools === null" class="au-notice">
          <RiErrorWarningLine width="15" height="15" aria-hidden="true" />工具目录仅超级管理员可读，以下为插件自带的工具说明。
        </p>
        <ul class="au-mcp-tools">
          <li v-for="tool in tools" :key="tool.name">
            <div>
              <strong>{{ tool.title }}</strong>
              <span v-if="tool.destructive" class="au-mcp-tag is-danger">删除</span>
              <span v-else-if="!tool.readOnly" class="au-mcp-tag">写入</span>
            </div>
            <code>{{ tool.name }}</code>
            <p>{{ tool.description }}</p>
          </li>
        </ul>
      </section>

      <section class="au-panel au-mcp-card" aria-labelledby="au-mcp-prompts">
        <header><h2 id="au-mcp-prompts">试试这样说</h2></header>
        <ul class="au-mcp-prompts">
          <li v-for="prompt in EXAMPLE_PROMPTS" :key="prompt">
            <p>{{ prompt }}</p>
            <CopyButton :text="prompt" />
          </li>
        </ul>
        <p class="au-mcp-code-note">启用设计会立即改变所有访客看到的登录页。AI 创建的设计也会出现在「登录设计」中，可以继续手动编辑。</p>
      </section>
    </aside>
  </div>
</template>

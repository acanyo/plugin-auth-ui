<script setup lang="ts">
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import RiArrowGoBackLine from '~icons/ri/arrow-go-back-line'
import RiArrowUpLine from '~icons/ri/arrow-up-line'
import RiErrorWarningLine from '~icons/ri/error-warning-line'
import RiExternalLinkLine from '~icons/ri/external-link-line'
import RiPlugLine from '~icons/ri/plug-line'
import RiSparkling2Line from '~icons/ri/sparkling-2-line'

import type { TemplateDraft } from '../templates/types'
import { workbenchLocation } from '../workbench/navigation'
import { errorMessage, fetchAssistantStatus, type AssistantStatus } from './assistant-api'
import { changedLabels } from './conversation'
import { loadModelPreference, saveModelPreference } from './model-preference'
import ModelSelector from './ModelSelector.vue'
import { QUICK_PROMPTS } from './prompts'
import { useAssistantConversation } from './useAssistantConversation'
import './assistant.css'

const design = defineModel<TemplateDraft>({ required: true })

const status = ref<AssistantStatus>()
const checking = ref(false)
const modelName = ref(loadModelPreference())
const instruction = ref('')
const allowScript = ref(false)
const log = ref<HTMLElement>()
const input = ref<HTMLTextAreaElement>()
const { turns, elapsed, running, undoable, send, cancel, undo } = useAssistantConversation(design)

const ready = computed(() => !!status.value?.available)
/** AI Foundation is installed: show the composer and model picker even if the chosen model is unusable. */
const installed = computed(() => !!status.value?.canChooseModel)
const sendBlocker = computed(() =>
  running.value
    ? 'AI 正在设计，请稍候'
    : !ready.value
      ? (status.value?.reason ?? '正在检查 AI Foundation…')
      : !instruction.value.trim()
        ? '请先描述想要的效果'
        : '',
)

async function loadStatus() {
  checking.value = true
  try {
    status.value = await fetchAssistantStatus(modelName.value)
  } catch (error) {
    status.value = { available: false, canChooseModel: false, reason: errorMessage(error), setupUrl: '' }
  } finally {
    checking.value = false
  }
}

async function submit() {
  if (sendBlocker.value) return
  const text = instruction.value
  instruction.value = ''
  await send(text, { allowScript: allowScript.value, modelName: modelName.value })
  await nextTick()
  input.value?.focus()
}

/** Enter sends, Shift + Enter adds a line; IME composition is left alone. */
function onKeydown(event: KeyboardEvent) {
  if (event.key !== 'Enter' || event.shiftKey || event.isComposing || event.keyCode === 229) return
  event.preventDefault()
  void submit()
}

function choose(prompt: string) {
  instruction.value = prompt
  input.value?.focus()
}

watch(modelName, (name) => {
  saveModelPreference(name)
  void loadStatus()
})
// Keep the newest message in view.
watch(
  [() => turns.value.length, running],
  async () => {
    await nextTick()
    if (log.value) log.value.scrollTop = log.value.scrollHeight
  },
)
onMounted(loadStatus)
</script>

<template>
  <section class="st-ai" aria-label="AI 设计对话">
    <header class="st-ai-heading">
      <span><RiSparkling2Line width="15" height="15" aria-hidden="true" /><strong>AI 设计</strong></span>
      <span class="st-ai-muted">{{ turns.length ? '继续调整当前设计' : '由 AI Foundation 提供' }}</span>
    </header>

    <div ref="log" class="st-ai-log" :class="{ 'is-empty': !turns.length }" role="log" aria-label="设计对话记录" aria-live="polite">
      <div v-if="!turns.length" class="st-ai-welcome">
        <span class="st-ai-mark" aria-hidden="true"><RiSparkling2Line width="28" height="28" /></span>
        <h2>想要一个怎样的登录页？</h2>
        <p>描述风格、配色和动效，AI 会按设计规范修改当前设计，右侧实时预览。结果不会自动保存，不满意可以撤销。</p>
        <div v-if="installed" class="st-ai-suggestions" role="group" aria-label="设计灵感">
          <button v-for="item in QUICK_PROMPTS" :key="item.label" type="button" :disabled="running" @click="choose(item.prompt)">
            {{ item.label }}
          </button>
        </div>
      </div>

      <template v-for="turn in turns" :key="turn.id">
        <article class="st-ai-message is-user">
          <p>{{ turn.instruction }}</p>
        </article>
        <article class="st-ai-message is-assistant" :data-state="turn.state" :data-undone="turn.undone">
          <span class="st-ai-author"><RiSparkling2Line width="14" height="14" aria-hidden="true" />AI 设计</span>
          <p v-if="turn.state === 'running'" class="st-ai-working">正在设计，已用 {{ elapsed }} 秒</p>
          <p v-else-if="turn.state === 'cancelled'" class="st-ai-muted">已停止，设计没有变化。</p>
          <template v-else-if="turn.state === 'failed'">
            <p class="st-ai-error">{{ turn.error }}</p>
            <button type="button" class="st-ai-action" :disabled="running || !ready" @click="send(turn.instruction, { allowScript, modelName })">
              重试
            </button>
          </template>
          <template v-else-if="turn.suggestion">
            <p>{{ turn.suggestion.summary || '已更新设计。' }}</p>
            <div class="st-ai-meta">
              <span v-for="label in changedLabels(turn.suggestion)" :key="label" class="st-ai-tag">{{ label }}</span>
              <span v-if="turn.suggestion.model">{{ turn.suggestion.model }}</span>
              <span v-if="turn.suggestion.tokens">{{ turn.suggestion.tokens.toLocaleString() }} tokens</span>
              <span v-if="turn.suggestion.turns > 1">已自动修正</span>
            </div>
            <details v-if="turn.suggestion.warnings.length" class="st-ai-warnings">
              <summary>{{ turn.suggestion.warnings.length }} 处内容会被忽略</summary>
              <ul>
                <li v-for="warning in turn.suggestion.warnings" :key="warning">{{ warning }}</li>
              </ul>
            </details>
            <p v-if="turn.undone" class="st-ai-muted">已撤销</p>
            <button v-else-if="turn === undoable" type="button" class="st-ai-action" :disabled="running" @click="undo(turn)">
              <RiArrowGoBackLine width="13" height="13" aria-hidden="true" />撤销这次修改
            </button>
          </template>
        </article>
      </template>
    </div>

    <div class="st-ai-compose">
      <div v-if="status && !ready" class="st-ai-setup" role="status">
        <RiErrorWarningLine width="15" height="15" aria-hidden="true" />
        <div>
          <p>{{ status.reason }}</p>
          <a
            v-if="status.setupUrl"
            :href="status.setupUrl"
            :target="status.setupUrl.startsWith('http') ? '_blank' : undefined"
            rel="noopener"
            class="st-link"
          >
            前往设置 <RiExternalLinkLine width="12" height="12" aria-hidden="true" />
          </a>
          <button type="button" class="st-link" :disabled="checking" @click="loadStatus">重新检测</button>
        </div>
      </div>
      <p v-if="allowScript" class="st-ai-script-note" role="note">
        AI 写的脚本会在正式登录页运行，能接触访客输入。保存前请在「代码 → JavaScript」中逐行审阅。
      </p>

      <form class="st-ai-prompt" @submit.prevent="submit">
        <textarea
          ref="input"
          v-model="instruction"
          rows="2"
          maxlength="2000"
          :disabled="!installed"
          :placeholder="turns.length ? '继续描述想调整的地方…' : '例如：做成液态玻璃风格，主色用玫红，背景缓慢流动'"
          aria-label="描述想要的修改"
          name="instruction"
          autocomplete="off"
          @keydown="onKeydown"
        />
        <div class="st-ai-actions">
          <ModelSelector v-if="installed" v-model="modelName" :disabled="running" />
          <label class="st-ai-script" title="脚本会在正式登录页运行，请审阅后再保存">
            <input v-model="allowScript" type="checkbox" :disabled="running || !installed" />
            <span>允许 JS</span>
          </label>
          <button v-if="running" type="button" class="st-ai-send is-stop" aria-label="停止生成" title="停止生成" @click="cancel">
            <span class="st-ai-stop-icon" aria-hidden="true" />
          </button>
          <button
            v-else
            type="submit"
            class="st-ai-send"
            :disabled="!!sendBlocker"
            :title="sendBlocker || '发送'"
            aria-label="发送设计需求"
          >
            <RiArrowUpLine width="18" height="18" aria-hidden="true" />
          </button>
        </div>
      </form>
      <p class="st-ai-hint">
        Enter 发送 · Shift + Enter 换行 ·
        <RouterLink :to="workbenchLocation('mcp')" class="st-link"><RiPlugLine width="12" height="12" aria-hidden="true" />连接外部 AI</RouterLink>
      </p>
    </div>
  </section>
</template>

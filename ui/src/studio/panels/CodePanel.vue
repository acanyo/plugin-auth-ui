<script setup lang="ts">
import { computed, ref } from 'vue'

import type { TemplateDraft } from '../../templates/types'

const design = defineModel<TemplateDraft>({ required: true })

type Tab = 'css' | 'js' | 'variables'
const tab = ref<Tab>('css')

/** Custom properties edited as `--name: value` lines. */
const variablesText = computed({
  get: () =>
    Object.entries(design.value.theme!.variables ?? {})
      .map(([name, value]) => `${name}: ${value}`)
      .join('\n'),
  set: (text: string) => {
    const variables: Record<string, string> = {}
    for (const line of text.split('\n')) {
      const match = line.match(/^\s*(--[a-zA-Z][\w-]*)\s*:\s*(.+?)\s*;?\s*$/)
      if (match) variables[match[1]!] = match[2]!
    }
    design.value.theme!.variables = variables
  },
})
</script>

<template>
  <section class="st-section st-code">
    <div class="st-segment" role="tablist" aria-label="代码类型">
      <button type="button" role="tab" :aria-selected="tab === 'css'" @click="tab = 'css'">CSS</button>
      <button type="button" role="tab" :aria-selected="tab === 'js'" @click="tab = 'js'">JavaScript</button>
      <button type="button" role="tab" :aria-selected="tab === 'variables'" @click="tab = 'variables'">CSS 变量</button>
    </div>

    <template v-if="tab === 'css'">
      <p class="st-section-note">
        选择器会自动限定在登录容器 <code>#auth-ui</code> 内，<code>:root</code>、<code>body</code> 表示容器本身；支持
        <code>@media</code>、<code>@supports</code>、<code>@keyframes</code>、<code>@font-face</code>。可用
        <code>[data-view=login]</code>、<code>[data-embedded=true]</code> 等属性区分页面与弹窗。
      </p>
      <FormKit v-model="design.code!.css" type="code" language="css" height="460px" />
    </template>

    <template v-else-if="tab === 'js'">
      <p class="st-section-note">
        在登录页就绪后执行，可通过 <code>AuthUI</code> 访问：<code>root</code>、<code>view</code>、<code>embedded</code>、
        <code>on('ready' | 'submit', fn)</code>、<code>replay()</code>、<code>setVar(name, value)</code>。预览中不会运行，保存并启用后在站点生效；代码出错不会影响登录。
      </p>
      <FormKit v-model="design.code!.js" type="code" language="javascript" height="460px" />
    </template>

    <template v-else>
      <p class="st-section-note">每行一个 <code>--名称: 值</code>，会写入登录容器，供 CSS 与内置样式使用。只接受安全的值（颜色、长度、数字等）。</p>
      <FormKit v-model="variablesText" type="code" language="css" height="300px" />
    </template>
  </section>
</template>

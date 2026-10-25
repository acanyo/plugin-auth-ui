<script setup lang="ts">
import { isSplitLayout } from '../../templates/design'
import type { TemplateDraft } from '../../templates/types'
import { SLOT_SNIPPETS } from '../snippets'

const design = defineModel<TemplateDraft>({ required: true })

type SlotName = 'aside' | 'header' | 'footer'

function insert(slot: SlotName, html: string) {
  const current = design.value.slots![slot] ?? ''
  design.value.slots![slot] = current ? `${current.trimEnd()}\n${html}` : html
}
</script>

<template>
  <section class="st-section">
    <h3>文案</h3>
    <p class="st-section-note">左右分栏布局时显示在品牌面板上；填写了“品牌面板 HTML”后以 HTML 为准。</p>
    <label class="st-field">
      <span class="st-label">登录页标语</span>
      <input v-model="design.loginSlogan" type="text" maxlength="80" placeholder="例如：欢迎回来，继续你的阅读。" />
    </label>
    <label class="st-field">
      <span class="st-label">注册页标语</span>
      <input v-model="design.signupSlogan" type="text" maxlength="80" placeholder="例如：加入我们，开始你的创作。" />
    </label>
  </section>

  <section class="st-section">
    <h3>自定义区块</h3>
    <p class="st-section-note">
      可以写任意 HTML，并使用内置样式类（auth-chip、auth-features、auth-quote、auth-stats、auth-big、auth-gradient-text）。给元素加
      <code>data-animate="rise|fade|zoom|slide|float"</code> 和 <code>data-delay="毫秒"</code> 即可添加入场动画。脚本和事件属性会被移除，交互请写在“代码”中的 JavaScript。
    </p>

    <div v-if="isSplitLayout(design.theme!.layout)" class="st-field">
      <span class="st-label st-label-row">
        品牌面板 HTML
        <span class="st-snippets">
          <button v-for="snippet in SLOT_SNIPPETS.aside" :key="snippet.label" type="button" @click="insert('aside', snippet.html)">
            + {{ snippet.label }}
          </button>
        </span>
      </span>
      <FormKit v-model="design.slots!.aside" type="code" language="html" height="220px" />
    </div>

    <div class="st-field">
      <span class="st-label st-label-row">
        表单上方
        <span class="st-snippets">
          <button v-for="snippet in SLOT_SNIPPETS.header" :key="snippet.label" type="button" @click="insert('header', snippet.html)">
            + {{ snippet.label }}
          </button>
        </span>
      </span>
      <FormKit v-model="design.slots!.header" type="code" language="html" height="120px" />
    </div>

    <div class="st-field">
      <span class="st-label st-label-row">
        表单下方
        <span class="st-snippets">
          <button v-for="snippet in SLOT_SNIPPETS.footer" :key="snippet.label" type="button" @click="insert('footer', snippet.html)">
            + {{ snippet.label }}
          </button>
        </span>
      </span>
      <FormKit v-model="design.slots!.footer" type="code" language="html" height="120px" />
    </div>
  </section>
</template>

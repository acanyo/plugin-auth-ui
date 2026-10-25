<script setup lang="ts">
import ChoiceCards from '../controls/ChoiceCards.vue'
import SegmentField from '../controls/SegmentField.vue'
import SwitchField from '../controls/SwitchField.vue'
import type { TemplateDraft } from '../../templates/types'
import { AMBIENTS, ENTRANCES, SPEEDS } from '../options'

const design = defineModel<TemplateDraft>({ required: true })
defineEmits<{ replay: [] }>()
</script>

<template>
  <section class="st-section">
    <header class="st-section-header">
      <h3>入场动画</h3>
      <button type="button" class="st-link" @click="$emit('replay')">重新播放</button>
    </header>
    <ChoiceCards v-model="design.theme!.motion!.entrance" label="卡片出现方式" :options="ENTRANCES" />
    <SwitchField
      v-model="design.theme!.motion!.stagger"
      label="逐项浮现"
      help="标题、输入框和按钮依次出现。"
    />
  </section>

  <section class="st-section">
    <h3>背景动效</h3>
    <ChoiceCards v-model="design.theme!.motion!.ambient" label="背景效果" :options="AMBIENTS" />
  </section>

  <section class="st-section">
    <h3>交互</h3>
    <SwitchField
      v-model="design.theme!.motion!.interactive"
      label="交互反馈"
      help="输入框聚焦上浮、按钮悬停光泽与按压回弹、第三方登录卡片悬停抬起。"
    />
    <SegmentField v-model="design.theme!.motion!.speed" label="动画速度" :options="SPEEDS" />
    <p class="st-section-note">访客在系统中开启“减少动态效果”时，所有动画会自动关闭。</p>
  </section>
</template>

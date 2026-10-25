<script setup lang="ts">
import ChoiceCards from '../controls/ChoiceCards.vue'
import ColorField from '../controls/ColorField.vue'
import RangeField from '../controls/RangeField.vue'
import SegmentField from '../controls/SegmentField.vue'
import { THEME_DEFAULTS, isSplitLayout } from '../../templates/design'
import type { TemplateDraft } from '../../templates/types'
import { BACKGROUNDS, DECORATIONS, LAYOUTS, SCHEMES, SHADOWS } from '../options'

const design = defineModel<TemplateDraft>({ required: true })
</script>

<template>
  <section class="st-section">
    <h3>布局</h3>
    <ChoiceCards v-model="design.theme!.layout" label="页面布局" :options="LAYOUTS" :columns="2" />
  </section>

  <section class="st-section">
    <h3>配色</h3>
    <SegmentField v-model="design.theme!.colorScheme" label="明暗模式" :options="SCHEMES" />
    <div class="st-grid">
      <ColorField v-model="design.theme!.primary" label="主色" :placeholder="THEME_DEFAULTS.primary" />
      <ColorField v-model="design.theme!.onPrimary" label="主色上的文字" :placeholder="THEME_DEFAULTS.onPrimary" />
      <ColorField v-model="design.theme!.surface" label="卡片背景" :placeholder="THEME_DEFAULTS.surface" />
      <ColorField v-model="design.theme!.text" label="文字" :placeholder="THEME_DEFAULTS.text" />
      <ColorField v-model="design.theme!.muted" label="次要文字" :placeholder="THEME_DEFAULTS.muted" />
      <ColorField v-model="design.theme!.border" label="边框" :placeholder="THEME_DEFAULTS.border" />
      <ColorField v-model="design.theme!.field" label="输入框背景" :placeholder="THEME_DEFAULTS.field" />
    </div>
  </section>

  <section class="st-section">
    <h3>页面背景</h3>
    <ChoiceCards v-model="design.theme!.background!.type" label="背景类型" :options="BACKGROUNDS" />
    <div class="st-grid">
      <ColorField v-model="design.theme!.background!.color" label="颜色 1" />
      <ColorField
        v-if="design.theme!.background!.type !== 'solid' && design.theme!.background!.type !== 'image'"
        v-model="design.theme!.background!.color2"
        label="颜色 2"
      />
      <ColorField
        v-if="['gradient', 'mesh', 'aurora'].includes(design.theme!.background!.type ?? '')"
        v-model="design.theme!.background!.color3"
        label="颜色 3"
      />
    </div>
    <RangeField
      v-if="design.theme!.background!.type === 'gradient'"
      v-model="design.theme!.background!.angle"
      label="渐变角度"
      :min="0"
      :max="360"
      unit="°"
      :placeholder="140"
    />
    <template v-if="design.theme!.background!.type === 'image'">
      <FormKit v-model="design.theme!.background!.image" type="attachment" label="背景图片" :accepts="['image/*']" />
      <RangeField v-model="design.theme!.background!.overlay" label="遮罩" :min="0" :max="0.9" :step="0.05" :placeholder="0.25" />
      <RangeField v-model="design.theme!.background!.blur" label="模糊" :min="0" :max="40" unit="px" :placeholder="0" />
    </template>
  </section>

  <section v-if="isSplitLayout(design.theme!.layout)" class="st-section">
    <h3>品牌面板</h3>
    <ColorField v-model="design.theme!.brand!.background" label="面板颜色" placeholder="跟随主色" />
    <FormKit v-model="design.theme!.brand!.image" type="attachment" label="面板图片" :accepts="['image/*']" />
    <ChoiceCards v-model="design.theme!.brand!.decoration" label="装饰动效" :options="DECORATIONS" :columns="4" />
  </section>

  <section class="st-section">
    <h3>形状与排版</h3>
    <RangeField v-model="design.theme!.radius" label="圆角" :min="0" :max="40" unit="px" :placeholder="THEME_DEFAULTS.radius" />
    <RangeField v-model="design.theme!.formWidth" label="表单宽度" :min="300" :max="560" unit="px" :placeholder="THEME_DEFAULTS.formWidth" />
    <SegmentField v-model="design.theme!.shadow" label="阴影" :options="SHADOWS" />
    <label class="st-field">
      <span class="st-label">字体</span>
      <input
        v-model.trim="design.theme!.font"
        type="text"
        placeholder="默认使用系统字体，例如 'LXGW WenKai', serif"
        spellcheck="false"
      />
    </label>
  </section>
</template>

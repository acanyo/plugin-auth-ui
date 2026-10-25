<script setup lang="ts">
/**
 * AI Foundation's own model picker (the `aiModelSelector` FormKit input it registers in the
 * console). Only render it when AI Foundation is enabled, or FormKit has no such input.
 */
defineProps<{ disabled?: boolean }>()
const model = defineModel<string>({ default: '' })
</script>

<template>
  <div class="st-ai-model" role="group" aria-label="生成模型" title="选择 AI Foundation 中的语言模型；清空后使用默认模型。">
    <FormKit
      type="aiModelSelector"
      name="auth-ui-assistant-model"
      model-type="language"
      :delay="0"
      :available="true"
      :clearable="true"
      :model-value="model || undefined"
      :disabled="disabled"
      placeholder="默认模型"
      @update:model-value="model = typeof $event === 'string' ? $event : ''"
    />
  </div>
</template>

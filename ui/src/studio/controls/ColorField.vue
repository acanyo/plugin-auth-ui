<script setup lang="ts">
import { computed } from 'vue'

/** A colour swatch plus a text box that also accepts rgb(), hsl() and transparency. */
const props = defineProps<{ label: string; placeholder?: string; help?: string }>()
const model = defineModel<string | undefined>()

const hex = computed(() => {
  const value = model.value?.trim() ?? ''
  if (/^#[0-9a-f]{6}$/i.test(value)) return value
  if (/^#[0-9a-f]{3}$/i.test(value)) return `#${value.slice(1).replace(/./g, (c) => c + c)}`
  return /^#[0-9a-f]{6}$/i.test(props.placeholder ?? '') ? props.placeholder! : '#000000'
})

function onText(event: Event) {
  const value = (event.target as HTMLInputElement).value.trim()
  model.value = value || undefined
}
</script>

<template>
  <label class="st-field st-color">
    <span class="st-label">{{ label }}</span>
    <span class="st-color-row">
      <input
        type="color"
        :value="hex"
        :aria-label="`${label}颜色选择器`"
        @input="model = ($event.target as HTMLInputElement).value"
      />
      <input type="text" :value="model ?? ''" :placeholder="placeholder" spellcheck="false" @change="onText" />
      <button v-if="model" type="button" class="st-clear" :aria-label="`恢复${label}默认值`" title="恢复默认" @click="model = undefined">
        ×
      </button>
    </span>
    <span v-if="help" class="st-help">{{ help }}</span>
  </label>
</template>

<script setup lang="ts" generic="T extends string">
/** A row of mutually exclusive options. */
defineProps<{ label: string; options: readonly { value: T; label: string; hint?: string }[]; help?: string }>()
const model = defineModel<T | undefined>()
</script>

<template>
  <div class="st-field">
    <span class="st-label">{{ label }}</span>
    <div class="st-segment" role="radiogroup" :aria-label="label">
      <button
        v-for="option in options"
        :key="option.value"
        type="button"
        role="radio"
        :aria-checked="model === option.value"
        :title="option.hint"
        @click="model = option.value"
      >
        {{ option.label }}
      </button>
    </div>
    <span v-if="help" class="st-help">{{ help }}</span>
  </div>
</template>

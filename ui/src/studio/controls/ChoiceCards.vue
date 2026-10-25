<script setup lang="ts" generic="T extends string">
/** Larger option cards with a small visual sample, for layouts, backgrounds and animations. */
defineProps<{
  label: string
  options: readonly { value: T; label: string; sample?: string }[]
  columns?: number
}>()
const model = defineModel<T | undefined>()
</script>

<template>
  <div class="st-field">
    <span class="st-label">{{ label }}</span>
    <div class="st-cards" role="radiogroup" :aria-label="label" :style="{ '--st-columns': columns ?? 3 }">
      <button
        v-for="option in options"
        :key="option.value"
        type="button"
        role="radio"
        class="st-card"
        :aria-checked="model === option.value"
        @click="model = option.value"
      >
        <span class="st-card-sample" :data-sample="option.sample ?? option.value" aria-hidden="true"><i /><i /><i /></span>
        <span>{{ option.label }}</span>
      </button>
    </div>
  </div>
</template>

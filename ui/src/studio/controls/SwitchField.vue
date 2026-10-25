<script setup lang="ts">
/** An on/off setting whose empty value means `fallback`. */
withDefaults(defineProps<{ label: string; help?: string; fallback?: boolean }>(), { help: undefined, fallback: true })
const model = defineModel<boolean | undefined>()
</script>

<template>
  <label class="st-field st-switch">
    <span>
      <span class="st-label">{{ label }}</span>
      <span v-if="help" class="st-help">{{ help }}</span>
    </span>
    <input
      type="checkbox"
      role="switch"
      :checked="model ?? fallback"
      @change="model = ($event.target as HTMLInputElement).checked"
    />
  </label>
</template>

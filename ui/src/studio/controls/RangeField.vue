<script setup lang="ts">
/** A slider with its current value; empty uses the design default shown as `placeholder`. */
withDefaults(
  defineProps<{ label: string; min: number; max: number; step?: number; unit?: string; placeholder?: number }>(),
  { step: 1, unit: '', placeholder: undefined },
)
const model = defineModel<number | undefined>()
</script>

<template>
  <label class="st-field">
    <span class="st-label st-label-row">
      {{ label }}
      <span class="st-value">
        {{ model ?? placeholder ?? '默认' }}{{ model !== undefined || placeholder !== undefined ? unit : '' }}
        <button v-if="model !== undefined" type="button" class="st-clear" title="恢复默认" :aria-label="`恢复${label}默认值`" @click.prevent="model = undefined">×</button>
      </span>
    </span>
    <input
      type="range"
      :min="min"
      :max="max"
      :step="step"
      :value="model ?? placeholder ?? min"
      @input="model = Number(($event.target as HTMLInputElement).value)"
    />
  </label>
</template>

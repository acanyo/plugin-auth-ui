<script setup lang="ts">
defineProps<{ title: string; description?: string }>()
</script>

<template>
  <header class="au-section-header">
    <div class="au-section-heading">
      <h1>{{ title }}</h1>
      <p v-if="description">{{ description }}</p>
    </div>
    <div v-if="$slots.actions" class="au-section-actions"><slot name="actions" /></div>
  </header>
</template>

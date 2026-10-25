<script setup lang="ts">
import { Toast } from '@halo-dev/components'
import { onBeforeUnmount, ref } from 'vue'
import RiCheckLine from '~icons/ri/check-line'
import RiFileCopyLine from '~icons/ri/file-copy-line'

const props = defineProps<{ text: string; label?: string }>()

const copied = ref(false)
let timer: ReturnType<typeof setTimeout> | undefined

async function copy() {
  try {
    await navigator.clipboard.writeText(props.text)
    copied.value = true
    clearTimeout(timer)
    timer = setTimeout(() => (copied.value = false), 1600)
  } catch {
    // Clipboard access needs a secure context; the text stays selectable.
    Toast.warning('无法访问剪贴板，请手动选择复制')
  }
}

onBeforeUnmount(() => clearTimeout(timer))
</script>

<template>
  <button type="button" class="au-text-button au-copy" :aria-label="label ?? '复制'" @click="copy">
    <RiCheckLine v-if="copied" width="14" height="14" aria-hidden="true" />
    <RiFileCopyLine v-else width="14" height="14" aria-hidden="true" />
    <span>{{ copied ? '已复制' : (label ?? '复制') }}</span>
  </button>
</template>

<script setup lang="ts">
import { ref } from 'vue'

import { previewUrl } from '../preview/preview-url'
import { useFitScale } from '../preview/useFitScale'

/** A live, non-interactive thumbnail of a stored design's login page. */
defineProps<{ template: string }>()

const WIDTH = 1440
const HEIGHT = 900
const box = ref<HTMLElement>()
const { scale } = useFitScale(box, ref(WIDTH))
</script>

<template>
  <div ref="box" class="au-design-thumb-canvas" aria-hidden="true">
    <iframe
      :src="previewUrl('login', 'page', { template })"
      tabindex="-1"
      loading="lazy"
      title=""
      :style="{ width: `${WIDTH}px`, height: `${HEIGHT}px`, transform: `scale(${scale})` }"
    />
  </div>
</template>

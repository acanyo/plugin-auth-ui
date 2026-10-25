import { onBeforeUnmount, ref, watch, type Ref } from 'vue'

import { compactDesign } from '../templates/design'
import { createPreviewDraft } from '../templates/template-api'
import type { TemplateDraft } from '../templates/types'

/**
 * Posts the design to the server shortly after each edit and exposes the returned draft id,
 * so the preview shows unsaved changes. Rapid edits are merged into one request.
 */
export function useDraftPreview(design: Ref<TemplateDraft | undefined>, delay = 350) {
  const draftId = ref('')
  const updating = ref(false)
  const failed = ref(false)
  let timer: ReturnType<typeof setTimeout> | undefined
  let sequence = 0

  async function push() {
    if (!design.value) return
    const current = ++sequence
    updating.value = true
    try {
      const id = await createPreviewDraft(compactDesign(design.value))
      // Ignore answers to requests that a newer edit has replaced.
      if (current === sequence) {
        draftId.value = id
        failed.value = false
      }
    } catch (error) {
      console.error('Failed to update auth-ui preview', error)
      if (current === sequence) failed.value = true
    } finally {
      if (current === sequence) updating.value = false
    }
  }

  watch(
    design,
    () => {
      clearTimeout(timer)
      timer = setTimeout(push, delay)
    },
    { deep: true },
  )
  onBeforeUnmount(() => clearTimeout(timer))

  return { draftId, updating, failed, refresh: push }
}

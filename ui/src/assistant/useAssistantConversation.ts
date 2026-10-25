import { computed, onScopeDispose, ref, type Ref } from 'vue'

import { compactDesign, normalizeDesign } from '../templates/design'
import type { TemplateDraft } from '../templates/types'
import { errorMessage, requestSuggestion } from './assistant-api'
import { conversationHistory, latestUndoable, markUndone, type Turn } from './conversation'

export interface SendOptions {
  allowScript: boolean
  modelName: string
}

/**
 * The studio's AI conversation. Each answer replaces the edited design, which stays unsaved
 * until the user saves it; the latest answer can be undone.
 */
export function useAssistantConversation(design: Ref<TemplateDraft>) {
  const turns = ref<Turn[]>([])
  const elapsed = ref(0)
  let controller: AbortController | undefined
  let clock: ReturnType<typeof setInterval> | undefined
  let nextId = 1

  const running = computed(() => turns.value.some((turn) => turn.state === 'running'))
  const undoable = computed(() => latestUndoable(turns.value))

  function find(id: number) {
    return turns.value.find((turn) => turn.id === id)!
  }

  async function send(text: string, options: SendOptions) {
    const instruction = text.trim()
    if (!instruction || running.value) return
    const history = conversationHistory(turns.value)
    const id = nextId++
    turns.value.push({ id, instruction, state: 'running' })
    controller = new AbortController()
    elapsed.value = 0
    clock = setInterval(() => elapsed.value++, 1000)
    const before = normalizeDesign(compactDesign(design.value))
    try {
      const suggestion = await requestSuggestion(
        { design: compactDesign(design.value), instruction, history, ...options },
        controller.signal,
      )
      Object.assign(find(id), { state: 'done', suggestion, before })
      design.value = normalizeDesign(suggestion.design)
    } catch (error) {
      const turn = find(id)
      if (controller?.signal.aborted) turn.state = 'cancelled'
      else Object.assign(turn, { state: 'failed', error: errorMessage(error) })
    } finally {
      clearInterval(clock)
      controller = undefined
    }
  }

  function cancel() {
    controller?.abort()
  }

  /** Restores the design from before a suggestion, including any edits made after it. */
  function undo(turn: Turn) {
    if (!turn.before) return
    design.value = normalizeDesign(compactDesign(turn.before))
    markUndone(turns.value, turn)
  }

  onScopeDispose(() => {
    controller?.abort()
    clearInterval(clock)
  })

  return { turns, elapsed, running, undoable, send, cancel, undo }
}

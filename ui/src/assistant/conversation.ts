import type { TemplateDraft } from '../templates/types'
import type { Suggestion } from './assistant-api'
import { FIELD_LABELS } from './prompts'

/** One request and the assistant's answer to it. */
export interface Turn {
  id: number
  instruction: string
  state: 'running' | 'done' | 'failed' | 'cancelled'
  suggestion?: Suggestion
  error?: string
  /** The design before this suggestion, for undo. */
  before?: TemplateDraft
  undone?: boolean
}

/** Requests whose result is still in the design, oldest first; sent so follow-ups make sense. */
export function conversationHistory(turns: readonly Turn[]): string[] {
  return turns.filter((turn) => turn.state === 'done' && !turn.undone).map((turn) => turn.instruction)
}

/** The newest suggestion that can still be undone. */
export function latestUndoable(turns: readonly Turn[]): Turn | undefined {
  return [...turns].reverse().find((turn) => turn.state === 'done' && !turn.undone)
}

/** Marks a turn and every later suggestion as undone, since undo restores the design before it. */
export function markUndone(turns: Turn[], from: Turn): void {
  for (const turn of turns) {
    if (turn.id >= from.id && turn.state === 'done') turn.undone = true
  }
}

export function changedLabels(suggestion: Suggestion): string[] {
  return suggestion.changed.map((field) => FIELD_LABELS[field] ?? field)
}

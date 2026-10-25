import { describe, expect, it } from 'vitest'

import type { Suggestion } from '../assistant-api'
import { changedLabels, conversationHistory, latestUndoable, markUndone, type Turn } from '../conversation'
import { loadModelPreference, MODEL_PREFERENCE_KEY, saveModelPreference } from '../model-preference'

const suggestion = (changed: string[]): Suggestion => ({
  design: { displayName: 'x' },
  summary: '',
  changed,
  warnings: [],
  model: '',
  tokens: null,
  turns: 1,
})

function turns(): Turn[] {
  return [
    { id: 1, instruction: '深色', state: 'done', suggestion: suggestion(['theme']), before: { displayName: 'a' } },
    { id: 2, instruction: '失败的', state: 'failed', error: 'x' },
    { id: 3, instruction: '青色', state: 'done', suggestion: suggestion(['theme']), before: { displayName: 'b' } },
    { id: 4, instruction: '停了', state: 'cancelled' },
  ]
}

describe('conversation', () => {
  it('sends only requests whose result is still applied', () => {
    const list = turns()
    expect(conversationHistory(list)).toEqual(['深色', '青色'])
    markUndone(list, list[2]!)
    expect(conversationHistory(list)).toEqual(['深色'])
  })

  it('undo of an earlier turn also retires later ones', () => {
    const list = turns()
    expect(latestUndoable(list)?.id).toBe(3)
    markUndone(list, list[0]!)
    expect(list.filter((turn) => turn.undone).map((turn) => turn.id)).toEqual([1, 3])
    expect(latestUndoable(list)).toBeUndefined()
  })

  it('labels changed fields in Chinese and keeps unknown ones', () => {
    expect(changedLabels(suggestion(['theme', 'code', 'other']))).toEqual(['外观与动效', '代码', 'other'])
  })
})

describe('model preference', () => {
  function memory() {
    const data = new Map<string, string>()
    return {
      data,
      getItem: (key: string) => data.get(key) ?? null,
      setItem: (key: string, value: string) => void data.set(key, value),
      removeItem: (key: string) => void data.delete(key),
    }
  }

  it('remembers a chosen model and forgets it when cleared', () => {
    const store = memory()
    saveModelPreference('gpt-x', store)
    expect(loadModelPreference(store)).toBe('gpt-x')
    saveModelPreference('', store)
    expect(store.data.has(MODEL_PREFERENCE_KEY)).toBe(false)
    expect(loadModelPreference(store)).toBe('')
  })

  it('falls back to the default model when storage is blocked or the value is invalid', () => {
    const blocked = {
      getItem: () => {
        throw new Error('blocked')
      },
      setItem: () => {
        throw new Error('blocked')
      },
      removeItem: () => {},
    }
    expect(loadModelPreference(blocked)).toBe('')
    expect(() => saveModelPreference('x', blocked)).not.toThrow()
    const store = memory()
    store.setItem(MODEL_PREFERENCE_KEY, 'x'.repeat(200))
    expect(loadModelPreference(store)).toBe('')
  })
})

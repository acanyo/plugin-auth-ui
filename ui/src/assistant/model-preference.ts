/** The AI Foundation model last chosen in the studio, kept per browser. Empty means the default model. */
export const MODEL_PREFERENCE_KEY = 'auth-ui:assistant-model'
const MAX_NAME = 160

type Store = Pick<Storage, 'getItem' | 'setItem' | 'removeItem'>

export function loadModelPreference(store: Store = localStorage): string {
  try {
    const value = store.getItem(MODEL_PREFERENCE_KEY)?.trim() ?? ''
    return value.length <= MAX_NAME ? value : ''
  } catch {
    // Blocked storage only means the choice is not remembered.
    return ''
  }
}

export function saveModelPreference(name: string, store: Store = localStorage): void {
  try {
    if (name) store.setItem(MODEL_PREFERENCE_KEY, name)
    else store.removeItem(MODEL_PREFERENCE_KEY)
  } catch {
    // See loadModelPreference.
  }
}

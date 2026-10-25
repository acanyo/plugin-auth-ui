import type { AuthTemplateSpec, TemplateDraft, TemplateLayout, TemplateTheme } from './types'

/** Values the studio shows when a design leaves a setting empty; they match auth/page.css. */
export const THEME_DEFAULTS = {
  primary: '#176b62',
  onPrimary: '#ffffff',
  surface: '#ffffff',
  text: '#17201f',
  muted: '#66706e',
  border: '#e2e7e6',
  field: '#f7f9f9',
  radius: 22,
  formWidth: 380,
} as const

/**
 * A complete, editable copy of a design: every nested group exists so the studio can bind to it,
 * and the layout of first-version templates moves into `theme.layout`.
 */
export function normalizeDesign(spec: AuthTemplateSpec): TemplateDraft {
  const copy: TemplateDraft = structuredClone(toPlain(spec))
  const theme: TemplateTheme = copy.theme ?? {}
  theme.layout ??= (copy.layout === 'split' ? 'split' : 'centered') as TemplateLayout
  theme.background ??= {}
  theme.brand ??= {}
  theme.motion ??= {}
  theme.variables ??= {}
  copy.theme = theme
  copy.slots ??= {}
  copy.code ??= {}
  // Keep CSS from the first version editable in one place.
  if (copy.css && !copy.code.css) {
    copy.code.css = copy.css
  }
  delete copy.css
  copy.layout = theme.layout === 'split' || theme.layout === 'split-reverse' ? 'split' : 'centered'
  return copy
}

/** Drops empty strings and empty groups so stored designs stay small and readable. */
export function compactDesign(draft: TemplateDraft): TemplateDraft {
  return prune(structuredClone(toPlain(draft))) as TemplateDraft
}

export function isSplitLayout(layout: TemplateLayout | undefined) {
  return layout === 'split' || layout === 'split-reverse'
}

/** Reactive proxies cannot be cloned directly. */
function toPlain<T>(value: T): T {
  return JSON.parse(JSON.stringify(value ?? {})) as T
}

function prune(value: unknown): unknown {
  if (Array.isArray(value)) return value
  if (value && typeof value === 'object') {
    const result: Record<string, unknown> = {}
    for (const [key, child] of Object.entries(value)) {
      const pruned = prune(child)
      const empty =
        pruned === undefined ||
        pruned === null ||
        pruned === '' ||
        (typeof pruned === 'object' && !Array.isArray(pruned) && Object.keys(pruned).length === 0)
      if (!empty) result[key] = pruned
    }
    return result
  }
  return value
}

import { describe, expect, it } from 'vitest'

import { BLANK_TEMPLATE, STARTER_TEMPLATES } from '../starter-templates'

describe('starter templates', () => {
  it('are valid drafts', () => {
    for (const draft of [...STARTER_TEMPLATES, BLANK_TEMPLATE]) {
      expect(draft.displayName.length).toBeGreaterThan(0)
      expect(draft.displayName.length).toBeLessThanOrEqual(40)
      expect(['centered', 'split', 'split-reverse', 'fullscreen']).toContain(draft.theme?.layout)
    }
  })

  it('have unique names', () => {
    const names = STARTER_TEMPLATES.map((draft) => draft.displayName)
    expect(new Set(names).size).toBe(names.length)
  })

  it('never put scripts in slots', () => {
    for (const draft of STARTER_TEMPLATES) {
      for (const html of Object.values(draft.slots ?? {})) {
        expect(html).not.toMatch(/<script|on\w+=/i)
      }
    }
  })

  it('include the liquid glass case first', () => {
    const glass = STARTER_TEMPLATES[0]!
    expect(glass.displayName).toBe('液态玻璃')
    expect(glass.code?.css).toContain('backdrop-filter')
    expect(glass.code?.css).toContain(':root[data-embedded=true]')
    expect(glass.code?.js).toContain("AuthUI.setVar('--lg-x'")
  })
})

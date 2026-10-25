import { describe, expect, it } from 'vitest'

import { compactDesign, isSplitLayout, normalizeDesign } from '../design'

describe('normalizeDesign', () => {
  it('fills every group the studio binds to', () => {
    const design = normalizeDesign({ displayName: '空白' })
    expect(design.theme?.layout).toBe('centered')
    expect(design.theme?.background).toEqual({})
    expect(design.theme?.motion).toEqual({})
    expect(design.slots).toEqual({})
    expect(design.code).toEqual({})
  })

  it('moves first-version fields into the new structure', () => {
    const design = normalizeDesign({ displayName: '旧模板', layout: 'split', css: ':root{--auth-primary:#111}' })
    expect(design.theme?.layout).toBe('split')
    expect(design.code?.css).toBe(':root{--auth-primary:#111}')
    expect(design.css).toBeUndefined()
  })

  it('does not share objects with the source', () => {
    const source = { displayName: 'a', theme: { primary: '#111' } }
    const design = normalizeDesign(source)
    design.theme!.primary = '#222'
    expect(source.theme.primary).toBe('#111')
  })
})

describe('compactDesign', () => {
  it('drops empty values and groups', () => {
    expect(
      compactDesign({
        displayName: 'a',
        description: '',
        theme: { primary: '#111', background: {}, motion: { entrance: 'rise' } },
        slots: { aside: '' },
        code: {},
      }),
    ).toEqual({ displayName: 'a', theme: { primary: '#111', motion: { entrance: 'rise' } } })
  })

  it('keeps false and zero', () => {
    expect(compactDesign({ displayName: 'a', theme: { radius: 0, motion: { stagger: false } } })).toEqual({
      displayName: 'a',
      theme: { radius: 0, motion: { stagger: false } },
    })
  })
})

describe('isSplitLayout', () => {
  it('recognises both split layouts', () => {
    expect(isSplitLayout('split')).toBe(true)
    expect(isSplitLayout('split-reverse')).toBe(true)
    expect(isSplitLayout('centered')).toBe(false)
  })
})

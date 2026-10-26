import { describe, expect, it } from 'vitest'

import { parseBasicSettings } from '../basic-settings'

describe('parseBasicSettings', () => {
  it('defaults to the modal without a logo', () => {
    expect(parseBasicSettings({})).toEqual({ logo: '', displayMode: 'modal' })
  })

  it('reads the basic group', () => {
    expect(parseBasicSettings({ basic: { logo: '/upload/logo.png', displayMode: 'page' } })).toEqual({
      logo: '/upload/logo.png',
      displayMode: 'page',
    })
  })

  it('ignores unknown display modes', () => {
    expect(parseBasicSettings({ basic: { displayMode: 'popup' } }).displayMode).toBe('modal')
  })
})

import { describe, expect, it } from 'vitest'

import { previewUrl } from '../preview-url'

describe('previewUrl', () => {
  it('points at the server preview route', () => {
    expect(previewUrl('login', 'page')).toBe('/auth-ui/preview/login?auth_ui_display=page')
    expect(previewUrl('two-factor', 'modal')).toBe('/auth-ui/preview/two-factor?auth_ui_display=modal')
  })

  it('previews a stored template', () => {
    expect(previewUrl('login', 'page', { template: 'aurora' })).toBe(
      '/auth-ui/preview/login?auth_ui_display=page&template=aurora',
    )
  })

  it('prefers an unsaved draft over a stored template', () => {
    expect(previewUrl('signup', 'page', { template: 'aurora', draft: 'd1' })).toBe(
      '/auth-ui/preview/signup?auth_ui_display=page&draft=d1',
    )
  })

  it('adds page states and a revision', () => {
    expect(previewUrl('login', 'modal', { state: 'error', revision: 3 })).toBe(
      '/auth-ui/preview/login?auth_ui_display=modal&state=error&r=3',
    )
    expect(previewUrl('login', 'page', { state: 'default' })).toBe('/auth-ui/preview/login?auth_ui_display=page')
  })
})

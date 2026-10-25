import { describe, expect, it } from 'vitest'

import {
  ROUTE_NAME,
  STUDIO_ROUTE_NAME,
  WORKBENCH_TABS,
  getWorkbenchTab,
  studioLocation,
  workbenchLocation,
} from '../navigation'

describe('workbench navigation', () => {
  it('opens the first tab for unknown values', () => {
    expect(getWorkbenchTab(undefined).id).toBe(WORKBENCH_TABS[0]!.id)
    expect(getWorkbenchTab('removed').id).toBe(WORKBENCH_TABS[0]!.id)
  })

  it('finds tabs by id', () => {
    expect(getWorkbenchTab('settings').id).toBe('settings')
    expect(getWorkbenchTab('mcp').id).toBe('mcp')
  })

  it('links tabs through the route query', () => {
    expect(workbenchLocation('templates')).toEqual({ name: ROUTE_NAME, query: { tab: 'templates' } })
  })

  it('links to the studio for new and stored designs', () => {
    expect(studioLocation()).toEqual({ name: STUDIO_ROUTE_NAME, params: { name: 'new' }, query: {} })
    expect(studioLocation('a1', 'starter-2')).toEqual({
      name: STUDIO_ROUTE_NAME,
      params: { name: 'a1' },
      query: { from: 'starter-2' },
    })
  })
})

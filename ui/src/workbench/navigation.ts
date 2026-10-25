import type { Component } from 'vue'
import { markRaw } from 'vue'
import type { RouteLocationRaw } from 'vue-router'
import RiLayoutMasonryLine from '~icons/ri/layout-masonry-line'
import RiSettings3Line from '~icons/ri/settings-3-line'
import RiSparkling2Line from '~icons/ri/sparkling-2-line'

export const ROUTE_NAME = 'AuthUiWorkbench'
export const STUDIO_ROUTE_NAME = 'AuthUiStudio'

export type WorkbenchTabId = 'settings' | 'templates' | 'mcp'

export interface WorkbenchTab {
  id: WorkbenchTabId
  label: string
  icon: Component
}

export const WORKBENCH_TABS: readonly WorkbenchTab[] = [
  { id: 'templates', label: '登录设计', icon: markRaw(RiLayoutMasonryLine) },
  { id: 'mcp', label: 'AI 设计', icon: markRaw(RiSparkling2Line) },
  { id: 'settings', label: '基础设置', icon: markRaw(RiSettings3Line) },
]

/** The tab named in the route query; unknown values open the first tab. */
export function getWorkbenchTab(value: unknown): WorkbenchTab {
  return WORKBENCH_TABS.find((tab) => tab.id === value) ?? WORKBENCH_TABS[0]!
}

export function workbenchLocation(tab: WorkbenchTabId): RouteLocationRaw {
  return { name: ROUTE_NAME, query: { tab } }
}

/**
 * The design studio for a stored template, or for a new one when `name` is omitted.
 * `panel` opens a sidebar panel other than the default AI chat.
 */
export function studioLocation(name?: string, from?: string, panel?: string): RouteLocationRaw {
  const query: Record<string, string> = {}
  if (from) query.from = from
  if (panel) query.panel = panel
  return { name: STUDIO_ROUTE_NAME, params: { name: name ?? 'new' }, query }
}

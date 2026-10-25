import { axiosInstance } from '@halo-dev/api-client'

import { fetchConfig, saveGroup } from '../config/plugin-config'
import {
  TEMPLATE_GROUP,
  TEMPLATE_KIND,
  TEMPLATE_VERSION,
  type AuthTemplate,
  type TemplateDraft,
} from './types'

/**
 * Halo's generic extension REST API for the template table. The same endpoints are available
 * to any authorised client (e.g. an MCP server), so templates never live in plugin code.
 */
const BASE = `/apis/${TEMPLATE_GROUP}/${TEMPLATE_VERSION}/authtemplates`

/** Config group naming the active template; separate from the `basic` settings group. */
const ACTIVE_GROUP = 'template'

export async function listTemplates(): Promise<AuthTemplate[]> {
  const { data } = await axiosInstance.get<{ items: AuthTemplate[] }>(BASE, {
    params: { page: 0, size: 0 },
  })
  // Halo deletes in two steps; hide templates that are already being deleted.
  return (data.items ?? []).filter((t) => !t.metadata.deletionTimestamp).sort(
    (a, b) =>
      (a.spec.priority ?? 0) - (b.spec.priority ?? 0) ||
      a.spec.displayName.localeCompare(b.spec.displayName),
  )
}

export async function createTemplate(draft: TemplateDraft): Promise<AuthTemplate> {
  const { data } = await axiosInstance.post<AuthTemplate>(BASE, {
    apiVersion: `${TEMPLATE_GROUP}/${TEMPLATE_VERSION}`,
    kind: TEMPLATE_KIND,
    metadata: { name: '', generateName: 'auth-template-' },
    spec: draft,
  })
  return data
}

/** Sends the full object back; `metadata.version` makes concurrent edits fail instead of merge. */
export async function updateTemplate(template: AuthTemplate, draft: TemplateDraft): Promise<AuthTemplate> {
  const { data } = await axiosInstance.put<AuthTemplate>(`${BASE}/${template.metadata.name}`, {
    ...template,
    spec: draft,
  })
  return data
}

export async function deleteTemplate(name: string): Promise<void> {
  await axiosInstance.delete(`${BASE}/${name}`)
}

export async function getTemplate(name: string): Promise<AuthTemplate> {
  const { data } = await axiosInstance.get<AuthTemplate>(`${BASE}/${name}`)
  return data
}

/**
 * Stores an unsaved design on the server for a short time and returns an id for
 * `previewUrl(..., { draft })`, so the studio previews edits before they are saved.
 */
export async function createPreviewDraft(draft: TemplateDraft): Promise<string> {
  const { data } = await axiosInstance.post<{ name: string }>(
    '/apis/api.authui.xhhao.com/v1alpha1/previews',
    draft,
  )
  return data.name
}

export async function fetchActiveTemplateName(): Promise<string> {
  const value = (await fetchConfig())[ACTIVE_GROUP]?.name
  return typeof value === 'string' ? value : ''
}

/** Saves only the active template, keeping the Logo and display mode untouched. */
export async function setActiveTemplateName(name: string): Promise<void> {
  await saveGroup(ACTIVE_GROUP, { name })
}

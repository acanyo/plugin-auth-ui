import { axiosInstance } from '@halo-dev/api-client'

/** Halo MCP Server, the official plugin that exposes this plugin's tools to AI clients. */
export const MCP_SERVER_PLUGIN = 'mcp-server'
export const MCP_SERVER_STORE_URL = 'https://www.halo.run/store/apps/app-ybv96zol'
export const MCP_SERVER_REPO_URL = 'https://github.com/halo-dev/plugin-mcp-server'
/** Tools contributed by this plugin carry this source plugin name. */
const OWN_PLUGIN = 'auth-ui'
const MCP_API = '/apis/console.api.mcp.halo.run/v1alpha1'
/**
 * Halo's console shows a toast for failed requests unless they carry `mute`. A missing MCP Server
 * is an expected state here, so these requests stay quiet.
 */
const MUTED = { mute: true } as Record<string, unknown>

export type McpServerState = 'missing' | 'disabled' | 'starting' | 'ready' | 'failed' | 'unknown'

export interface McpTool {
  name: string
  title: string
  description: string
  readOnly: boolean
  destructive: boolean
}

export interface McpStatus {
  server: McpServerState
  version?: string
  /** This plugin's tools in the MCP catalog; `null` when the catalog cannot be read. */
  tools: McpTool[] | null
  /** Access keys that can call at least one of the tools; `null` when keys cannot be read. */
  keysWithAccess: number | null
}

interface PluginLike {
  spec?: { enabled?: boolean; version?: string }
  status?: { phase?: string }
}

interface CatalogTool extends McpTool {
  source?: { pluginName?: string }
}

interface AccessKey {
  enabled?: boolean
  allowedTools?: string[]
}

export function serverState(plugin: PluginLike | undefined): McpServerState {
  if (!plugin) return 'missing'
  const phase = plugin.status?.phase
  if (phase === 'STARTED') return 'ready'
  if (phase === 'FAILED') return 'failed'
  if (!plugin.spec?.enabled) return 'disabled'
  return 'starting'
}

/** Enabled keys that allow every tool (`*`) or at least one of `toolNames`. */
export function countKeysWithAccess(keys: AccessKey[], toolNames: string[]): number {
  return keys.filter(
    (key) =>
      key.enabled !== false &&
      (key.allowedTools ?? []).some((tool) => tool === '*' || toolNames.includes(tool)),
  ).length
}

export function ownTools(catalog: CatalogTool[]): McpTool[] {
  return catalog
    .filter((tool) => tool.source?.pluginName === OWN_PLUGIN)
    .map(({ name, title, description, readOnly, destructive }) => ({
      name,
      title,
      description,
      readOnly,
      destructive,
    }))
}

function status(error: unknown): number | undefined {
  return (error as { response?: { status?: number } })?.response?.status
}

async function fetchServerPlugin(): Promise<{ state: McpServerState; version?: string }> {
  try {
    const { data } = await axiosInstance.get<PluginLike>(
      `/apis/plugin.halo.run/v1alpha1/plugins/${MCP_SERVER_PLUGIN}`,
      MUTED,
    )
    return { state: serverState(data), version: data.spec?.version }
  } catch (error) {
    return { state: status(error) === 404 ? 'missing' : 'unknown' }
  }
}

/**
 * MCP Server's state, this plugin's tools in its catalog and how many access keys can use them.
 * The catalog and keys are only readable by Halo super administrators; others see `null`.
 */
export async function fetchMcpStatus(): Promise<McpStatus> {
  const server = await fetchServerPlugin()
  if (server.state !== 'ready') {
    return { server: server.state, version: server.version, tools: null, keysWithAccess: null }
  }
  const [catalog, keys] = await Promise.allSettled([
    axiosInstance.get<CatalogTool[]>(`${MCP_API}/tools`, MUTED),
    axiosInstance.get<AccessKey[]>(`${MCP_API}/keys`, MUTED),
  ])
  const tools = catalog.status === 'fulfilled' ? ownTools(catalog.value.data ?? []) : null
  const keysWithAccess =
    keys.status === 'fulfilled' && tools
      ? countKeysWithAccess(
          keys.value.data ?? [],
          tools.map((tool) => tool.name),
        )
      : null
  return { server: 'ready', version: server.version, tools, keysWithAccess }
}

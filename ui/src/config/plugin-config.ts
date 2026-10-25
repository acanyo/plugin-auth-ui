import { consoleApiClient } from '@halo-dev/api-client'

export const PLUGIN_NAME = 'auth-ui'

/** The plugin's ConfigMap, grouped like Halo's plugin settings: `{ group: { key: value } }`. */
export type PluginConfig = Record<string, Record<string, unknown> | undefined>

export async function fetchConfig(): Promise<PluginConfig> {
  const { data } = await consoleApiClient.plugin.plugin.fetchPluginJsonConfig({ name: PLUGIN_NAME })
  return (data ?? {}) as PluginConfig
}

/**
 * Replaces one group and keeps every other group as stored, so the settings page and the
 * template manager never overwrite each other's values.
 */
export async function saveGroup(group: string, value: Record<string, unknown>): Promise<void> {
  const config = await fetchConfig()
  await consoleApiClient.plugin.plugin.updatePluginJsonConfig({
    name: PLUGIN_NAME,
    body: { ...config, [group]: value },
  })
}

import { fetchConfig, saveGroup } from '../config/plugin-config'

export type DisplayMode = 'modal' | 'page'

/** Values read by the server's `AuthUiSettings` from the `basic` group. */
export interface BasicSettings {
  logo: string
  displayMode: DisplayMode
}

const GROUP = 'basic'

export const DEFAULT_SETTINGS: BasicSettings = { logo: '', displayMode: 'modal' }

/** Reads the `basic` group; missing or unknown values fall back to the defaults. */
export function parseBasicSettings(config: Record<string, Record<string, unknown> | undefined>): BasicSettings {
  const basic = config[GROUP] ?? {}
  const logo = typeof basic.logo === 'string' ? basic.logo : DEFAULT_SETTINGS.logo
  const displayMode = basic.displayMode === 'page' || basic.displayMode === 'modal'
    ? basic.displayMode : DEFAULT_SETTINGS.displayMode
  return { logo, displayMode }
}

export async function fetchBasicSettings(): Promise<BasicSettings> {
  return parseBasicSettings(await fetchConfig())
}

export async function saveBasicSettings(settings: BasicSettings): Promise<void> {
  await saveGroup(GROUP, { logo: settings.logo, displayMode: settings.displayMode })
}

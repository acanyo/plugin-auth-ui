import { fetchConfig, saveGroup } from '../config/plugin-config'

export type DisplayMode = 'modal' | 'page'

/** Values read by the server's `AuthUiSettings` from the `basic` group. */
export interface BasicSettings {
  logo: string
  displayMode: DisplayMode
}

const GROUP = 'basic'

export const DEFAULT_SETTINGS: BasicSettings = { logo: '', displayMode: 'modal' }

/** Reads the `basic` group, including values written by earlier plugin versions. */
export function parseBasicSettings(config: Record<string, Record<string, unknown> | undefined>): BasicSettings {
  const basic = config[GROUP] ?? {}
  const legacyLogo = config.brand?.logo
  const logo = typeof basic.logo === 'string' && basic.logo ? basic.logo
    : typeof legacyLogo === 'string' ? legacyLogo : ''
  const legacyMode: DisplayMode = basic.modalEnabled === false ? 'page' : 'modal'
  const displayMode = basic.displayMode === 'page' || basic.displayMode === 'modal'
    ? basic.displayMode : legacyMode
  return { logo, displayMode }
}

export async function fetchBasicSettings(): Promise<BasicSettings> {
  return parseBasicSettings(await fetchConfig())
}

export async function saveBasicSettings(settings: BasicSettings): Promise<void> {
  await saveGroup(GROUP, { logo: settings.logo, displayMode: settings.displayMode })
}

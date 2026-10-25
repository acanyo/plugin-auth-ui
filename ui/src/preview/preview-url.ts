/** Pages a design can be previewed on; ids match the server's `AuthView`. */
export type PreviewView = 'login' | 'signup' | 'logout' | 'password-reset' | 'two-factor'
export type PreviewDisplay = 'page' | 'modal'
/** Extra states Halo shows on a page, e.g. a failed password login. */
export type PreviewState = 'default' | 'local' | 'error' | 'logout'

export interface PreviewOptions {
  /** Stored template to preview. */
  template?: string
  /** Unsaved design posted by the studio; takes precedence over `template`. */
  draft?: string
  state?: PreviewState
  /** A changing value forces the iframe to reload. */
  revision?: number
}

export const PREVIEW_VIEWS: readonly { id: PreviewView; label: string }[] = [
  { id: 'login', label: '登录' },
  { id: 'signup', label: '注册' },
  { id: 'password-reset', label: '找回密码' },
  { id: 'two-factor', label: '两步验证' },
  { id: 'logout', label: '退出' },
]

/** Server route that renders a design with the saved Logo and inert forms. */
export function previewUrl(view: PreviewView, display: PreviewDisplay, options: PreviewOptions = {}): string {
  const params = new URLSearchParams({ auth_ui_display: display })
  if (options.draft) params.set('draft', options.draft)
  else if (options.template) params.set('template', options.template)
  if (options.state && options.state !== 'default') params.set('state', options.state)
  if (options.revision && options.revision > 0) params.set('r', String(options.revision))
  return `/auth-ui/preview/${view}?${params}`
}

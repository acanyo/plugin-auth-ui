/** Mirrors the server's `AuthTemplate` custom model (authui.xhhao.com/v1alpha1). */
export const TEMPLATE_GROUP = 'authui.xhhao.com'
export const TEMPLATE_VERSION = 'v1alpha1'
export const TEMPLATE_KIND = 'AuthTemplate'

export type TemplateLayout = 'centered' | 'split' | 'split-reverse' | 'fullscreen'
export type ColorScheme = 'light' | 'dark' | 'system'
export type ShadowLevel = 'none' | 'soft' | 'strong'
export type BackgroundType = 'solid' | 'gradient' | 'image' | 'mesh' | 'aurora'
export type Decoration = 'orbit' | 'grid' | 'waves' | 'none'
export type Entrance = 'none' | 'fade' | 'rise' | 'zoom' | 'slide' | 'flip'
export type Ambient = 'none' | 'gradient' | 'float' | 'particles' | 'aurora'
export type MotionSpeed = 'slow' | 'normal' | 'fast'

export interface TemplateBackground {
  type?: BackgroundType
  color?: string
  color2?: string
  color3?: string
  angle?: number
  image?: string
  overlay?: number
  blur?: number
}

export interface TemplateBrand {
  background?: string
  image?: string
  decoration?: Decoration
}

export interface TemplateMotion {
  entrance?: Entrance
  ambient?: Ambient
  stagger?: boolean
  interactive?: boolean
  speed?: MotionSpeed
}

export interface TemplateTheme {
  primary?: string
  onPrimary?: string
  surface?: string
  text?: string
  muted?: string
  border?: string
  field?: string
  colorScheme?: ColorScheme
  radius?: number
  formWidth?: number
  shadow?: ShadowLevel
  layout?: TemplateLayout
  font?: string
  background?: TemplateBackground
  brand?: TemplateBrand
  motion?: TemplateMotion
  variables?: Record<string, string>
}

export interface TemplateSlots {
  aside?: string
  header?: string
  footer?: string
}

export interface TemplateCode {
  css?: string
  js?: string
}

export interface AuthTemplateSpec {
  displayName: string
  description?: string
  loginSlogan?: string
  signupSlogan?: string
  theme?: TemplateTheme
  slots?: TemplateSlots
  code?: TemplateCode
  priority?: number
}

export interface AuthTemplate {
  apiVersion: string
  kind: string
  metadata: {
    name: string
    generateName?: string
    version?: number
    creationTimestamp?: string
    deletionTimestamp?: string
  }
  spec: AuthTemplateSpec
}

/** The editable part of a template, before it is saved. */
export type TemplateDraft = AuthTemplateSpec

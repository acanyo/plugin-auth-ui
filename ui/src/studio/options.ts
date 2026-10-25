import type {
  Ambient,
  BackgroundType,
  ColorScheme,
  Decoration,
  Entrance,
  MotionSpeed,
  ShadowLevel,
  TemplateLayout,
} from '../templates/types'

/** Choices offered by the studio; values match the server's `ThemeCompiler`. */
export const LAYOUTS: readonly { value: TemplateLayout; label: string }[] = [
  { value: 'centered', label: '居中卡片' },
  { value: 'split', label: '左图右表' },
  { value: 'split-reverse', label: '左表右图' },
  { value: 'fullscreen', label: '全屏侧栏' },
]

export const SCHEMES: readonly { value: ColorScheme; label: string }[] = [
  { value: 'light', label: '浅色' },
  { value: 'dark', label: '深色' },
  { value: 'system', label: '跟随系统' },
]

export const SHADOWS: readonly { value: ShadowLevel; label: string }[] = [
  { value: 'none', label: '无' },
  { value: 'soft', label: '柔和' },
  { value: 'strong', label: '明显' },
]

export const BACKGROUNDS: readonly { value: BackgroundType; label: string }[] = [
  { value: 'solid', label: '纯色' },
  { value: 'gradient', label: '渐变' },
  { value: 'mesh', label: '弥散' },
  { value: 'aurora', label: '极光' },
  { value: 'image', label: '图片' },
]

export const DECORATIONS: readonly { value: Decoration; label: string }[] = [
  { value: 'orbit', label: '环形' },
  { value: 'grid', label: '网格' },
  { value: 'waves', label: '波浪' },
  { value: 'none', label: '无' },
]

export const ENTRANCES: readonly { value: Entrance; label: string; sample: string }[] = [
  { value: 'rise', label: '上浮', sample: 'motion-rise' },
  { value: 'fade', label: '淡入', sample: 'motion-fade' },
  { value: 'zoom', label: '缩放', sample: 'motion-zoom' },
  { value: 'slide', label: '侧滑', sample: 'motion-slide' },
  { value: 'flip', label: '翻转', sample: 'motion-flip' },
  { value: 'none', label: '无', sample: 'motion-none' },
]

export const AMBIENTS: readonly { value: Ambient; label: string; sample: string }[] = [
  { value: 'none', label: '静止', sample: 'ambient-none' },
  { value: 'gradient', label: '流动渐变', sample: 'ambient-gradient' },
  { value: 'float', label: '漂浮光斑', sample: 'ambient-float' },
  { value: 'aurora', label: '极光', sample: 'ambient-aurora' },
  { value: 'particles', label: '粒子', sample: 'ambient-particles' },
]

export const SPEEDS: readonly { value: MotionSpeed; label: string }[] = [
  { value: 'slow', label: '舒缓' },
  { value: 'normal', label: '适中' },
  { value: 'fast', label: '轻快' },
]

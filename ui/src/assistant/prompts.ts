/** One-click requests in the studio's AI panel. */
export const QUICK_PROMPTS: readonly { label: string; prompt: string }[] = [
  { label: '液态玻璃', prompt: '做成 Apple 液态玻璃风格：半透明毛玻璃卡片、高光边缘、彩色光斑背景，跟随系统深浅色。' },
  { label: '深色科技', prompt: '改成深色科技风：深蓝黑背景、青色主色、粒子背景动效，文字清晰可读。' },
  { label: '左右分栏', prompt: '改成左右分栏布局，左侧品牌面板放欢迎语和三条亮点，配合逐项浮现动画。' },
  { label: '极简', prompt: '改成极简风格：纯白背景、无阴影、细边框输入框，只保留轻微的淡入动画。' },
  { label: '提高对比度', prompt: '检查并提高所有文字与按钮的对比度，保持现在的风格。' },
  { label: '更多动效', prompt: '在不影响可读性的前提下增加一些细腻的动效：更有弹性的入场、按钮悬停光泽、背景缓慢流动。' },
]

/** Labels for the top-level fields a suggestion can change. */
export const FIELD_LABELS: Record<string, string> = {
  displayName: '名称',
  description: '描述',
  loginSlogan: '登录标语',
  signupSlogan: '注册标语',
  theme: '外观与动效',
  slots: '内容',
  code: '代码',
}

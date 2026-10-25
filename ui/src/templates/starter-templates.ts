import type { TemplateDraft } from './types'

/**
 * Starting points offered in the console. They are copied into the template table when the
 * user adds one, and can then be edited or deleted like any other design.
 */
export const STARTER_TEMPLATES: readonly TemplateDraft[] = [
  {
    displayName: '液态玻璃',
    description: '仿 Apple Liquid Glass：半透明毛玻璃卡片与高光边缘，光泽随指针流动，背景彩色光斑缓慢漂浮，跟随系统深浅色。',
    priority: 5,
    theme: {
      background: {
        color: '#f3f5ff',
        color2: '#ff8ac4',
        color3: '#6ec3ff',
        type: 'mesh',
      },
      border: 'rgb(60 60 67 / 0.14)',
      colorScheme: 'system',
      field: 'rgb(255 255 255 / 0.5)',
      font: '-apple-system, BlinkMacSystemFont, "SF Pro Display", "SF Pro Text", "PingFang SC", "Helvetica Neue", sans-serif',
      formWidth: 380,
      layout: 'centered',
      motion: {
        ambient: 'float',
        entrance: 'zoom',
        interactive: true,
        speed: 'normal',
        stagger: true,
      },
      muted: '#48484f',
      onPrimary: '#ffffff',
      primary: '#0058d0',
      radius: 32,
      shadow: 'none',
      surface: 'rgb(255 255 255 / 0.5)',
      text: '#1d1d1f',
    },
    code: {
      css: `/* Liquid Glass. Glass needs a colourful wallpaper behind it, so the page gets soft colour fields. */
:root {
  --lg-link: #0058d0;
  --lg-glass: rgb(255 255 255 / .46);
  --lg-glass-strong: rgb(255 255 255 / .66);
  --lg-rim: rgb(255 255 255 / .7);
  --lg-edge: rgb(255 255 255 / .9);
  --lg-shade: rgb(40 48 110 / .28);
  --lg-field: rgb(255 255 255 / .5);
  --lg-field-focus: rgb(255 255 255 / .78);
  --lg-divider: rgb(60 60 67 / .14);
  --auth-ring: rgb(0 88 208 / .2);
  --auth-page:
    radial-gradient(42% 52% at 10% 14%, #ffc6e4 0%, transparent 70%),
    radial-gradient(46% 56% at 92% 8%, #bfe0ff 0%, transparent 70%),
    radial-gradient(60% 60% at 72% 100%, #d8c8ff 0%, transparent 72%),
    linear-gradient(160deg, #f3f5ff, #fdf3fa);
}
@media (prefers-color-scheme: dark) {
  :root {
    --auth-text: #f5f5f7; --auth-muted: #b7b7bf; --auth-border: rgb(255 255 255 / .12);
    --auth-card: rgb(30 30 36 / .5); --auth-field: rgb(255 255 255 / .07);
    --lg-link: #6cb6ff;
    --lg-glass: rgb(30 30 36 / .5);
    --lg-glass-strong: rgb(44 44 52 / .7);
    --lg-rim: rgb(255 255 255 / .16);
    --lg-edge: rgb(255 255 255 / .45);
    --lg-shade: rgb(0 0 0 / .55);
    --lg-field: rgb(255 255 255 / .07);
    --lg-field-focus: rgb(255 255 255 / .12);
    --lg-divider: rgb(255 255 255 / .12);
    --auth-ring: rgb(108 182 255 / .28);
    --auth-bg-2: #b4005a; --auth-bg-3: #0060a0;
    --auth-page:
      radial-gradient(42% 52% at 10% 14%, #4a1240 0%, transparent 70%),
      radial-gradient(46% 56% at 92% 8%, #0d2f5c 0%, transparent 70%),
      radial-gradient(60% 60% at 72% 100%, #2c1b6b 0%, transparent 72%),
      linear-gradient(160deg, #0b0c1a, #14101f);
  }
}

/* Extra colour drops that drift behind the glass. */
.auth-backdrop::before, .auth-backdrop::after {
  content: ""; position: absolute; width: 30vmax; height: 30vmax; border-radius: 42% 58% 63% 37% / 45% 40% 60% 55%;
  filter: blur(48px); opacity: .6; will-change: transform, border-radius;
  animation: lg-drift calc(26s * var(--auth-speed)) ease-in-out infinite alternate;
}
.auth-backdrop::before { left: 8vw; bottom: 6vh; background: #ffd27a; }
.auth-backdrop::after { right: 10vw; top: 38vh; background: #7ef0d8; animation-duration: calc(32s * var(--auth-speed)); animation-direction: alternate-reverse; }
:root[data-ambient=float] .auth-blob {
  opacity: .7; border-radius: 46% 54% 58% 42% / 52% 44% 56% 48%;
  animation: auth-float calc(16s * var(--auth-speed)) ease-in-out infinite alternate,
             lg-morph calc(12s * var(--auth-speed)) ease-in-out infinite alternate;
}
@media (prefers-color-scheme: dark) {
  .auth-backdrop::before { background: #7a2cff; opacity: .45; }
  .auth-backdrop::after { background: #00a3a3; opacity: .35; }
  :root[data-ambient=float] .auth-blob { opacity: .55; }
}

/* The glass card: frosted fill, bright rim, soft depth and a highlight that follows the pointer. */
.auth-shell {
  isolation: isolate;
  background: linear-gradient(140deg, var(--lg-glass-strong), var(--lg-glass) 45%, var(--lg-glass));
  -webkit-backdrop-filter: blur(30px) saturate(190%);
  backdrop-filter: blur(30px) saturate(190%);
  box-shadow: inset 0 1px 0 var(--lg-edge), inset 0 -1px 0 var(--lg-rim), 0 40px 90px -30px var(--lg-shade), 0 2px 10px rgb(0 0 0 / .05);
}
.auth-shell::before {
  content: ""; position: absolute; inset: 0; z-index: 2; border-radius: inherit; padding: 1.5px; pointer-events: none;
  background: linear-gradient(145deg, var(--lg-edge), transparent 32%, transparent 62%, var(--lg-rim));
  -webkit-mask: linear-gradient(#000 0 0) content-box, linear-gradient(#000 0 0);
  -webkit-mask-composite: xor; mask-composite: exclude;
}
.auth-shell::after {
  content: ""; position: absolute; inset: 0; z-index: 1; border-radius: inherit; pointer-events: none;
  background: radial-gradient(520px circle at var(--lg-x, 22%) var(--lg-y, -8%), rgb(255 255 255 / .5), transparent 42%);
  mix-blend-mode: soft-light;
  transition: opacity .4s;
}
.auth-card { position: relative; z-index: 3; background: transparent; }
@supports not ((backdrop-filter: blur(1px)) or (-webkit-backdrop-filter: blur(1px))) {
  .auth-shell { background: var(--lg-glass-strong); }
}

/* Liquid entrance: the card condenses into place. */
:root:not([data-entrance=none]) .auth-shell { animation-name: lg-emerge; animation-duration: calc(.9s * var(--auth-speed)); animation-timing-function: cubic-bezier(.2, .9, .25, 1.15); }

/* Type and brand. */
h1 { font-weight: 650; letter-spacing: -.035em; }
.auth-brand img, .auth-monogram { border-radius: 12px; box-shadow: inset 0 0 0 1px var(--lg-rim), 0 4px 12px -4px var(--lg-shade); }
a, .auth-switch a { color: var(--lg-link); }
.divider-wrapper { color: var(--auth-muted); }
hr { border-top-color: var(--lg-divider); }

/* Glass fields. */
input:not([type=hidden]):not([type=checkbox]):not([type=radio]), select {
  height: 48px; border-radius: 14px; border: 1px solid var(--lg-rim); background: var(--lg-field);
  box-shadow: inset 0 1px 2px rgb(0 0 0 / .06), inset 0 -1px 0 var(--lg-rim);
  -webkit-backdrop-filter: blur(10px); backdrop-filter: blur(10px);
}
input:not([type=hidden]):not([type=checkbox]):not([type=radio]):hover:not(:focus) { border-color: var(--lg-edge); }
input:focus { background: var(--lg-field-focus); border-color: color-mix(in srgb, var(--lg-link) 55%, var(--lg-rim)); box-shadow: 0 0 0 4px var(--auth-ring), inset 0 1px 2px rgb(0 0 0 / .04); }
.form-input-group button {
  min-height: 48px; border-radius: 14px; border: 1px solid var(--lg-rim); background: var(--lg-field); color: var(--lg-link);
  -webkit-backdrop-filter: blur(10px); backdrop-filter: blur(10px);
}

/* Capsule button with a glossy top light; it squishes like liquid when pressed. */
button[type=submit] {
  min-height: 50px; border-radius: 999px;
  background: linear-gradient(180deg, color-mix(in srgb, var(--auth-primary) 78%, #fff), var(--auth-primary) 55%);
  box-shadow: inset 0 1px 0 rgb(255 255 255 / .55), inset 0 -3px 8px rgb(0 0 0 / .14), 0 14px 30px -14px var(--auth-primary);
}
:root[data-interactive=true] button[type=submit]:active:not(:disabled) { animation: lg-squish .5s cubic-bezier(.3, 1.6, .5, 1); }

/* Other sign-in methods as glass capsules. */
.pill-items :is(a, button) {
  border-radius: 999px; border: 1px solid var(--lg-rim); background: var(--lg-field);
  box-shadow: inset 0 1px 0 var(--lg-rim);
  -webkit-backdrop-filter: blur(12px); backdrop-filter: blur(12px);
}
.pill-items :is(a, button):hover { border-color: var(--lg-edge); background: var(--lg-field-focus); }

/* Messages and the logout card. */
.alert { border-radius: 14px; background: var(--lg-field); box-shadow: inset 0 0 0 1px var(--lg-rim); -webkit-backdrop-filter: blur(10px); backdrop-filter: blur(10px); }
.alert-error { background: rgb(255 59 48 / .1); }
.auth-closed, .auth-logout .user-info { border: 1px solid var(--lg-rim); border-radius: 18px; background: var(--lg-field); }

/* Inside the site modal the frame shows this card colour; the glass keeps a soft tint. */
:root[data-embedded=true] {
  --auth-card: rgb(255 255 255 / .55);
  /* On the modal's white base, white glass would vanish; tint controls and draw their rim. */
  --lg-field: rgb(118 118 140 / .08);
  --lg-field-focus: rgb(255 255 255 / .92);
  --lg-rim: rgb(60 60 67 / .12);
  --lg-edge: rgb(60 60 67 / .2);
  background:
    radial-gradient(70% 50% at 0% 0%, rgb(255 198 228 / .55) 0%, transparent 70%),
    radial-gradient(70% 60% at 100% 10%, rgb(191 224 255 / .6) 0%, transparent 70%);
}
:root[data-embedded=true] .auth-shell { background: transparent; box-shadow: none; -webkit-backdrop-filter: none; backdrop-filter: none; }
:root[data-embedded=true] .auth-shell::before, :root[data-embedded=true] .auth-shell::after { display: none; }
@media (prefers-color-scheme: dark) {
  :root[data-embedded=true] {
    --auth-card: rgb(30 30 36 / .6);
    --lg-field: rgb(255 255 255 / .07);
    --lg-field-focus: rgb(255 255 255 / .12);
    --lg-rim: rgb(255 255 255 / .14);
    --lg-edge: rgb(255 255 255 / .3);
    background:
      radial-gradient(70% 50% at 0% 0%, rgb(74 18 64 / .6) 0%, transparent 70%),
      radial-gradient(70% 60% at 100% 10%, rgb(13 47 92 / .6) 0%, transparent 70%);
  }
}

@keyframes lg-emerge {
  0% { opacity: 0; transform: translateY(18px) scale(.94); border-radius: 48px; }
  60% { opacity: 1; }
}
@keyframes lg-squish {
  30% { transform: scale(.96, .9); }
  60% { transform: scale(1.02, 1.04); }
  100% { transform: scale(1); }
}
@keyframes lg-morph {
  to { border-radius: 58% 42% 40% 60% / 42% 58% 44% 56%; }
}
@keyframes lg-drift {
  to { transform: translate3d(-8vmax, -6vmax, 0) rotate(40deg) scale(1.15); border-radius: 60% 40% 38% 62% / 58% 46% 54% 42%; }
}
@media (prefers-reduced-motion: reduce) {
  .auth-backdrop::before, .auth-backdrop::after, .auth-blob { animation: none; }
}
`,
      js: `// Liquid Glass: the card's highlight follows the pointer, like light moving across glass.
const shell = AuthUI.root.querySelector('.auth-shell');
const canHover = window.matchMedia('(hover: hover) and (pointer: fine)').matches;
const calm = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
if (shell && canHover && !calm) {
  let frame = 0;
  let last = null;
  shell.addEventListener('pointermove', (event) => {
    last = event;
    if (frame) return;
    frame = requestAnimationFrame(() => {
      frame = 0;
      const box = shell.getBoundingClientRect();
      AuthUI.setVar('--lg-x', \`\${(((last.clientX - box.left) / box.width) * 100).toFixed(1)}%\`);
      AuthUI.setVar('--lg-y', \`\${(((last.clientY - box.top) / box.height) * 100).toFixed(1)}%\`);
    });
  });
  shell.addEventListener('pointerleave', () => {
    AuthUI.setVar('--lg-x', '22%');
    AuthUI.setVar('--lg-y', '-8%');
  });
}
`,
    },
  },
  {
    displayName: '森林分栏',
    description: '品牌面板配环形动效，表单逐项浮现。',
    priority: 10,
    theme: {
      primary: '#176b62',
      layout: 'split',
      radius: 24,
      shadow: 'soft',
      background: { type: 'gradient', color: '#eef4f3', color2: '#d7e9e4', angle: 140 },
      brand: { background: '#145c54', decoration: 'orbit' },
      motion: { entrance: 'rise', ambient: 'none', stagger: true, interactive: true },
    },
    slots: {
      aside: `<span class="auth-chip" data-animate="fade">欢迎回来</span>
<div class="auth-spacer"></div>
<p class="auth-big" data-animate="rise" data-delay="120">继续你的<br>阅读与创作</p>
<ul class="auth-features" data-animate="rise" data-delay="260">
  <li>一个账号，跨设备同步</li>
  <li>评论、收藏与订阅随时可用</li>
</ul>`,
    },
  },
  {
    displayName: '极光夜色',
    description: '深色毛玻璃卡片，背景极光缓慢流动。',
    priority: 20,
    theme: {
      primary: '#8b7cff',
      onPrimary: '#ffffff',
      surface: 'rgb(20 22 36 / 0.78)',
      text: '#eef0ff',
      muted: '#a5a9c8',
      border: 'rgb(255 255 255 / 0.1)',
      field: 'rgb(255 255 255 / 0.05)',
      colorScheme: 'dark',
      layout: 'centered',
      radius: 26,
      shadow: 'strong',
      background: { type: 'aurora', color: '#090b16', color2: '#5b4bff', color3: '#00c2ff' },
      motion: { entrance: 'zoom', ambient: 'aurora', stagger: true, interactive: true, speed: 'normal' },
    },
    code: {
      css: `.auth-shell { backdrop-filter: blur(22px) saturate(1.4); border: 1px solid rgb(255 255 255 / .08); }
h1 { background: linear-gradient(120deg, #fff, #b9b0ff); -webkit-background-clip: text; background-clip: text; color: transparent; }`,
    },
  },
  {
    displayName: '晨光渐变',
    description: '柔和流动的渐变背景，居中卡片。',
    priority: 30,
    theme: {
      primary: '#e0578c',
      layout: 'centered',
      radius: 28,
      shadow: 'soft',
      background: { type: 'gradient', color: '#ffe3ec', color2: '#e5e9ff', color3: '#fff4dd', angle: 120 },
      motion: { entrance: 'rise', ambient: 'gradient', stagger: true, interactive: true, speed: 'slow' },
    },
    slots: {
      header: `<span class="auth-chip" style="color:#e0578c" data-animate="fade">✨ 新的一天</span>`,
    },
  },
  {
    displayName: '极简',
    description: '纯白页面，没有卡片和装饰，只保留细节动效。',
    priority: 40,
    theme: {
      primary: '#111111',
      surface: '#ffffff',
      text: '#111111',
      muted: '#6b6b6b',
      border: '#e5e5e5',
      field: '#ffffff',
      layout: 'centered',
      radius: 0,
      shadow: 'none',
      background: { type: 'solid', color: '#ffffff' },
      motion: { entrance: 'fade', ambient: 'none', stagger: true, interactive: true, speed: 'fast' },
    },
    code: {
      css: `input:not([type=hidden]):not([type=checkbox]), button[type=submit] { border-radius: 8px; }`,
    },
  },
  {
    displayName: '星点科技',
    description: '深色全屏侧栏，粒子缓缓上升。',
    priority: 50,
    theme: {
      primary: '#22d3ee',
      onPrimary: '#04212a',
      text: '#e6f6fb',
      muted: '#8fb3bf',
      border: 'rgb(255 255 255 / 0.1)',
      field: 'rgb(255 255 255 / 0.04)',
      surface: 'rgb(6 18 28 / 0.82)',
      colorScheme: 'dark',
      layout: 'fullscreen',
      shadow: 'none',
      background: { type: 'mesh', color: '#0b3b4a', color2: '#1d2b64', color3: '#0f172a' },
      motion: { entrance: 'slide', ambient: 'particles', stagger: true, interactive: true },
    },
  },
]

/** A blank design for users who want to start from the base look. */
export const BLANK_TEMPLATE: TemplateDraft = {
  displayName: '新设计',
  description: '',
  priority: 100,
  theme: {
    layout: 'centered',
    background: {},
    brand: {},
    motion: { entrance: 'rise', stagger: true, interactive: true },
  },
  slots: {},
  code: {
    css: `/* 选择器会自动限定在登录容器内，:root / body 表示容器本身。
   常用变量：--auth-primary --auth-card --auth-text --auth-muted --auth-border
   --auth-field --auth-radius --auth-width --auth-shadow --auth-page --auth-story */
`,
    js: `// 页面就绪后执行。可用 AuthUI：root、view、on('ready' | 'submit', fn)、replay()、setVar(name, value)
// AuthUI.on('submit', ({ view }) => console.log('提交', view))
`,
  },
}

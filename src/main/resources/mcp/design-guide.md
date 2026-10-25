# Auth UI design guide

A design is an `AuthTemplate` record (`authui.xhhao.com/v1alpha1`). It restyles Halo's own
login, signup, logout, password reset and two-factor pages. Halo still renders and handles every
form, so a design can never break sign-in. The site uses one active design at a time.

## Layers

Use the lowest layer that does the job.

1. `theme`: colours, layout, background, brand panel and motion. Every value is validated;
   invalid values are dropped silently (`auth-ui__lint_design` reports them).
2. `slots`: sanitised HTML around the form. `aside` (brand panel, split layouts only),
   `header` (above the title), `footer` (below the form).
3. `code.css`: free CSS, scoped to `#auth-ui`. `code.js`: JavaScript on the live page only.

## Page structure

```
main#auth-ui[data-view][data-layout][data-scheme][data-bg][data-entrance][data-ambient][data-embedded]
  .auth-backdrop            blobs and particles for ambient motion
  .auth-shell               the card (or split container)
    aside.auth-story        brand panel, split layouts only
      .auth-story-art       decoration (orbit / grid / waves)
      .auth-story-content   slots.aside, or .auth-site + .auth-story-copy (slogan)
    section.auth-card
      .auth-slot-header     slots.header
      header.auth-heading   .auth-brand (logo + site title), h1, .auth-lead
      .auth-forms           Halo's form fragments
      .auth-switch          "还没有账号？创建账号" and similar links
      .auth-slot-footer     slots.footer
      footer.auth-footer    "返回首页"
```

`data-view` is one of `login`, `signup`, `logout`, `password-reset-send`,
`password-reset-reset`, `two-factor`.

`data-embedded="true"` means the page is inside the site modal. In the modal, the backdrop,
brand panel and footer are hidden and the card fills the dialog. Most visitors see the modal,
so check designs there too.

Halo's form markup: `.form-item`, `label`, `input`, `.form-input-group` (input + "send code"
button), `button[type=submit]`, `.alert` / `.alert-error`, `.divider-wrapper`, `.pill-items`
(other login methods and social providers).

## CSS custom properties

Set them through `theme`, `theme.variables` or `code.css` (`:root { --auth-primary: … }`).

| Property | Meaning |
| --- | --- |
| `--auth-primary`, `--auth-on-primary` | Accent colour and text on it |
| `--auth-card`, `--auth-text`, `--auth-muted`, `--auth-border`, `--auth-field` | Card, text and inputs |
| `--auth-radius`, `--auth-width`, `--auth-shadow`, `--auth-font` | Shape and type |
| `--auth-page` | Page background (any CSS background value) |
| `--auth-bg-1`, `--auth-bg-2`, `--auth-bg-3`, `--auth-bg-angle` | Background colours used by gradient, mesh and aurora |
| `--auth-story` | Brand panel background |
| `--auth-speed` | Animation duration multiplier (slow 1.6, normal 1, fast 0.6) |
| `--auth-ease`, `--auth-ring` | Easing curve and focus ring colour |

## Slot HTML

Allowed: text formatting, headings, lists, links (http, https, mailto, site paths), images,
`section`/`article`/`figure` and similar containers, inline SVG. Attributes `class`, `id`,
`style`, `role` and `aria-*` are kept. Removed: `script`, `iframe`, `form`, `input`, event
handlers such as `onclick`, and any `style` that contains `url(`.

Utility classes:

- `auth-chip`: a small pill label.
- `auth-big`: a large headline.
- `auth-gradient-text`: gradient-filled text.
- `auth-features`: a `ul` with check marks.
- `auth-stats`: a row of `<div><strong>12k+</strong><span>位读者</span></div>`.
- `auth-quote`: a quotation with a side rule.
- `auth-spacer`: pushes later content to the bottom of the brand panel.

Slot animation: `data-animate="rise|fade|zoom|slide|float"` and `data-delay="120"` (ms).

## Motion

- `motion.entrance`: how the card enters. One of `none`, `fade`, `rise`, `zoom`, `slide`, `flip`.
- `motion.ambient`: an animated background. One of `none`, `gradient`, `float`, `particles`,
  `aurora`. Ambient effects need a visible page background, so they are not seen in the modal.
- `motion.stagger`: reveal fields one by one.
- `motion.interactive`: hover, focus and press feedback.
- `motion.speed`: overall animation speed.

All animations stop under `prefers-reduced-motion`. Custom `@keyframes` in `code.css` should
add their own reduced-motion rule.

## code.css rules

- Selectors are prefixed with `#auth-ui`. `:root`, `html` and `body` mean `#auth-ui` itself.
- Only `@media`, `@supports`, `@keyframes` and `@font-face` are kept. `@import` is dropped.
- Target pages with `#auth-ui[data-view=signup] …` (written as `:root[data-view=signup] …`)
  and the modal with `:root[data-embedded=true] …`.

## code.js

The script runs once on the live page, after it is ready, inside try/catch. It never runs in
previews, so check it on the site. The API:

```js
AuthUI.root                       // the #auth-ui element
AuthUI.view                       // current page id
AuthUI.embedded                   // true inside the site modal
AuthUI.on('ready', ({ view, embedded }) => {})   // returns an unsubscribe function
AuthUI.on('submit', ({ form, view }) => {})      // a form is being submitted
AuthUI.replay()                   // replay entrance and stagger animations
AuthUI.setVar('--auth-primary', '#e11d48')
```

Do not change form fields, names or actions. Halo reads them.

## Example

```json
{
  "displayName": "森林分栏",
  "description": "品牌面板配环形动效，表单逐项浮现。",
  "theme": {
    "primary": "#176b62",
    "layout": "split",
    "radius": 24,
    "shadow": "soft",
    "background": { "type": "gradient", "color": "#eef4f3", "color2": "#d7e9e4", "angle": 140 },
    "brand": { "background": "#145c54", "decoration": "orbit" },
    "motion": { "entrance": "rise", "ambient": "none", "stagger": true, "interactive": true }
  },
  "slots": {
    "aside": "<span class=\"auth-chip\" data-animate=\"fade\">欢迎回来</span><div class=\"auth-spacer\"></div><p class=\"auth-big\" data-animate=\"rise\" data-delay=\"120\">继续你的<br>阅读与创作</p>"
  },
  "code": {
    "css": ".auth-shell { border: 1px solid color-mix(in srgb, var(--auth-primary) 12%, transparent); }"
  }
}
```

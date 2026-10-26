(() => {
  'use strict';
  if (window.HaloAuthUI || window.parent !== window || /^\/(login|signup|logout|console|uc|system|challenges|password-reset)(\/|$)/.test(location.pathname)) return;
  // Pages the modal opens: login, signup, logout and password reset, including the emailed
  // /password-reset/email/{token} link.
  const AUTH_PAGE = /^\/(login|signup|logout|password-reset\/email(\/[^/]+)?)$/;
  // Pages the modal iframe may show; anything else means the flow left the modal. Two-factor
  // follows a password login made in the modal.
  const FLOW_PAGE = /^\/(challenges\/two-factor\/totp|auth-ui\/(complete|logged-out))$/;
  const isAuthPage = path => AUTH_PAGE.test(path);
  const isModalPage = path => isAuthPage(path) || FLOW_PAGE.test(path);
  const isFormPage = path => isAuthPage(path) || path === '/challenges/two-factor/totp';
  let dialog, frame, host, opener, target, scrollStyle;
  let originalUrl;
  // Themes often move body elements while booting, which drops a dialog out of the top layer;
  // put the dialog back on top if the host is moved later.
  const topLayer = new MutationObserver(() => {
    if (dialog?.open && host.isConnected && !dialog.matches(':modal')) {
      dialog.close();
      dialog.showModal();
      dialog.focus({ preventScroll: true });
    }
  });

  function localTarget(value) {
    if (!value) return null;
    try {
      const url = new URL(value, location.origin);
      if (url.origin !== location.origin || !['http:', 'https:'].includes(url.protocol)
        || /^\/(login|signup|logout|password-reset|challenges|auth-ui)(\/|$)/.test(url.pathname)) return null;
      return url.pathname + url.search + url.hash;
    } catch { return null; }
  }

  const here = () => location.pathname + location.search + location.hash;

  function close() {
    if (!dialog?.open || dialog.dataset.closing !== undefined) return;
    const finish = () => {
      delete dialog.dataset.closing;
      dialog.close();
      frame.src = 'about:blank';
      restorePage();
    };
    if (matchMedia('(prefers-reduced-motion: reduce)').matches) return finish();
    dialog.dataset.closing = '';
    dialog.addEventListener('animationend', finish, { once: true });
    // Fallback when the animation does not run (e.g. the tab is hidden).
    setTimeout(() => dialog.dataset.closing !== undefined && finish(), 300);
  }

  function restorePage() {
    document.body.style.overflow = scrollStyle.overflow;
    document.body.style.paddingRight = scrollStyle.paddingRight;
    opener?.focus({ preventScroll: true });
  }

  function mount() {
    host = document.createElement('div');
    host.id = 'auth-ui-modal';
    const shadow = host.attachShadow({ mode: 'closed' });
    const style = document.createElement('style');
    // The frame adopts the design's colours (--m-*), read from the page once it is ready.
    style.textContent = `
      :host { all: initial; }
      dialog {
        --m-card:#fff; --m-base:#fff; --m-text:#17201f; --m-muted:#66706e; --m-accent:#176b62; --m-radius:24px;
        --m-scrim:rgb(12 18 20 / .42); --m-close:rgb(0 0 0 / .05); --m-close-hover:rgb(0 0 0 / .09);
        --max:calc(var(--auth-viewport,100dvh) - 32px);
        position:fixed; inset:0; margin:auto; padding:0; width:min(440px,calc(100vw - 24px)); height:fit-content;
        max-width:none; max-height:var(--max); border:0; border-radius:var(--m-radius);
        /* A translucent design card (e.g. glass) is laid over an opaque base so text stays readable. */
        background:linear-gradient(var(--m-card), var(--m-card)) var(--m-base);
        color:var(--m-text); overflow:hidden; isolation:isolate;
        box-shadow:0 0 0 1px rgb(0 0 0 / .05), 0 2px 6px rgb(0 0 0 / .05), 0 32px 90px -24px rgb(0 0 0 / .45);
        transition:width .25s cubic-bezier(.2,.8,.2,1), background-color .3s;
      }
      dialog[open] { animation:enter .34s cubic-bezier(.2,.9,.25,1.12); }
      dialog[data-closing] { animation:leave .18s ease-in forwards; }
      dialog[data-closing]::backdrop { animation:fade-out .18s ease-in forwards; }
      dialog:focus { outline:none; }
      dialog::backdrop { background:var(--m-scrim); -webkit-backdrop-filter:blur(10px) saturate(1.2); backdrop-filter:blur(10px) saturate(1.2); animation:fade-in .25s ease-out; }
      /* The body scrolls with a hidden scrollbar; soft fades at the edges show there is more. */
      .body {
        position:relative; overflow-y:auto; overflow-x:hidden; max-height:var(--max); overscroll-behavior:contain;
        scrollbar-width:none; border-radius:inherit;
      }
      .body::-webkit-scrollbar { display:none; }
      dialog[data-scroll~=up] .body { -webkit-mask-image:linear-gradient(transparent, #000 36px); mask-image:linear-gradient(transparent, #000 36px); }
      dialog[data-scroll~=down] .body { -webkit-mask-image:linear-gradient(#000 calc(100% - 44px), transparent); mask-image:linear-gradient(#000 calc(100% - 44px), transparent); }
      dialog[data-scroll~=up][data-scroll~=down] .body { -webkit-mask-image:linear-gradient(transparent, #000 36px, #000 calc(100% - 44px), transparent); mask-image:linear-gradient(transparent, #000 36px, #000 calc(100% - 44px), transparent); }
      iframe { display:block; width:100%; height:var(--auth-frame,420px); border:0; background:transparent; color-scheme:normal; transition:height .28s cubic-bezier(.2,.8,.2,1), opacity .2s; }
      dialog[data-state=switching] iframe { opacity:.35; }
      .close {
        position:absolute; z-index:3; right:14px; top:14px; display:grid; place-items:center; width:34px; height:34px; padding:0;
        border:0; border-radius:50%; background:var(--m-close); color:var(--m-muted); cursor:pointer;
        -webkit-backdrop-filter:blur(12px); backdrop-filter:blur(12px); transition:background .15s, color .15s, transform .2s;
      }
      .close svg { width:16px; height:16px; }
      .close:hover { background:var(--m-close-hover); color:var(--m-text); transform:rotate(90deg); }
      .close:active { transform:rotate(90deg) scale(.9); }
      .close:focus-visible { outline:2px solid var(--m-accent); outline-offset:2px; }
      .status {
        position:absolute; inset:0; z-index:2; display:none; flex-direction:column; align-items:center; justify-content:center; gap:14px;
        background:linear-gradient(var(--m-card), var(--m-card)) var(--m-base); color:var(--m-text); font:14px/1.6 -apple-system,BlinkMacSystemFont,"PingFang SC","Microsoft YaHei",sans-serif;
      }
      dialog:is([data-state=loading],[data-state=done]) .status { display:flex; }
      /* While a form posts, keep the form visible but block double submits. */
      dialog[data-state=submitting] iframe { pointer-events:none; }
      .spinner { width:26px; height:26px; border:2.5px solid color-mix(in srgb, var(--m-accent) 18%, transparent); border-top-color:var(--m-accent); border-radius:50%; animation:spin .8s linear infinite; }
      .check { display:none; width:48px; height:48px; border-radius:50%; background:var(--m-accent) url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='white' stroke-width='3' stroke-linecap='round' stroke-linejoin='round'%3E%3Cpath d='m5 12 5 5 9-10'/%3E%3C/svg%3E") center/22px no-repeat; box-shadow:0 10px 30px -8px var(--m-accent); animation:pop .4s cubic-bezier(.3,1.6,.5,1); }
      dialog[data-state=done] .spinner { display:none; }
      dialog[data-state=done] .check { display:block; }
      dialog[data-state=loading] .status-text { color:var(--m-muted); font-size:13px; }
      @keyframes spin { to { transform:rotate(360deg); } }
      @keyframes pop { from { transform:scale(.4); opacity:0; } }
      @keyframes enter { from { transform:translateY(14px) scale(.96); opacity:0; } }
      @keyframes leave { to { transform:translateY(8px) scale(.97); opacity:0; } }
      @keyframes fade-in { from { opacity:0; } }
      @keyframes fade-out { to { opacity:0; } }
      @media (prefers-reduced-motion: reduce) {
        dialog, dialog[open], dialog[data-closing], dialog::backdrop, .check, iframe, .close { animation:none !important; transition:none !important; }
        .spinner { animation-duration:2s; }
      }
      /* Phones: a bottom sheet that rises from the edge. */
      @media (max-width:520px) {
        dialog { --max:calc(var(--auth-viewport,100dvh) - 12px); width:100vw; margin:auto 0 0; border-radius:var(--m-radius) var(--m-radius) 0 0; padding-bottom:env(safe-area-inset-bottom); }
        dialog[open] { animation:sheet .38s cubic-bezier(.2,.9,.25,1); }
        dialog[data-closing] { animation:sheet-out .2s ease-in forwards; }
        dialog::before { content:""; position:absolute; z-index:4; top:8px; left:50%; width:36px; height:5px; margin-left:-18px; border-radius:3px; background:color-mix(in srgb, var(--m-text) 22%, transparent); }
        @keyframes sheet { from { transform:translateY(100%); } }
        @keyframes sheet-out { to { transform:translateY(100%); } }
      }
    `;
    dialog = document.createElement('dialog');
    dialog.setAttribute('aria-label', '账号登录与注册');
    dialog.setAttribute('aria-modal', 'true');
    // Focus the dialog itself on open, so the close button is not highlighted first.
    dialog.tabIndex = -1;
    const button = document.createElement('button');
    button.className = 'close';
    button.type = 'button';
    button.setAttribute('aria-label', '关闭弹窗');
    button.innerHTML = '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" aria-hidden="true"><path d="M6 6l12 12M18 6 6 18"/></svg>';
    button.addEventListener('click', close);
    const body = document.createElement('div');
    body.className = 'body';
    frame = document.createElement('iframe');
    frame.title = '账号登录、注册与退出';
    // Covers the iframe while it loads and after success, so callback pages are never shown.
    const status = document.createElement('div');
    status.className = 'status';
    status.setAttribute('role', 'status');
    status.innerHTML = '<span class="spinner" aria-hidden="true"></span><span class="check" aria-hidden="true"></span><span class="status-text">正在加载…</span>';
    body.append(frame, status);
    const edges = () => {
      const more = [];
      if (body.scrollTop > 2) more.push('up');
      if (body.scrollTop + body.clientHeight < body.scrollHeight - 2) more.push('down');
      dialog.dataset.scroll = more.join(' ');
    };
    body.addEventListener('scroll', edges, { passive: true });
    if (typeof ResizeObserver === 'function') new ResizeObserver(edges).observe(body);
    dialog.append(button, body);
    dialog.addEventListener('cancel', event => { event.preventDefault(); close(); });
    dialog.addEventListener('click', event => {
      const box = dialog.getBoundingClientRect();
      if (event.target === dialog && (event.clientX < box.left || event.clientX > box.right || event.clientY < box.top || event.clientY > box.bottom)) close();
    });
    frame.addEventListener('load', () => {
      if (!dialog.open) return;
      try {
        const url = frame.contentWindow.location;
        if (url.href === 'about:blank') return;
        // Halo sends visitors who are already signed in from /login to /uc; treat it as success.
        if (/^\/uc(\/|$)/.test(url.pathname)) {
          succeed();
        } else if (!isModalPage(url.pathname)) {
          window.location.assign(url.href);
        } else if (isFormPage(url.pathname) && !frame.contentDocument.getElementById('auth-ui')) {
          window.location.assign(fullPage(originalUrl));
        } else if (isFormPage(url.pathname)) {
          dialog.dataset.view = url.pathname.split('/')[1];
          adopt(frame.contentDocument.getElementById('auth-ui'));
          // Dim the form while the next page in the modal loads (e.g. login to signup).
          frame.contentWindow.addEventListener('pagehide', () => {
            if (dialog.dataset.state === 'ready') dialog.dataset.state = 'switching';
          });
          // A form posted back to the same page (e.g. a wrong password) shows it again;
          // older browsers without ResizeObserver never report a height.
          fit(frame.contentDocument.getElementById('auth-ui').getBoundingClientRect().height);
        }
      } catch {
        // Social providers open in the top window (page.js sets target=_top), so a cross-origin
        // page never stays inside the modal.
      }
    });
    shadow.append(style, dialog);
    document.body.append(host);
    topLayer.observe(document.body, { childList: true });
    const viewport = () => dialog.style.setProperty('--auth-viewport', `${window.visualViewport?.height || innerHeight}px`);
    viewport();
    window.visualViewport?.addEventListener('resize', viewport);
  }

  // The server would otherwise turn a top-level visit to /login back into this modal.
  function fullPage(value) {
    const url = new URL(value, location.origin);
    url.searchParams.set('auth_ui_page', '1');
    return url.href;
  }

  function openUrl(value) {
    const url = new URL(value, location.origin);
    if (url.origin !== location.origin || !isAuthPage(url.pathname)) return false;
    if (!document.body || typeof HTMLDialogElement === 'undefined') { location.assign(fullPage(url.href)); return true; }
    // PJAX libraries that swap the whole body (e.g. Turbo) take the modal with it; mount again.
    if (!host?.isConnected) mount();
    if (!dialog.open) {
      opener = document.activeElement;
      scrollStyle = { overflow: document.body.style.overflow, paddingRight: document.body.style.paddingRight };
      const gutter = innerWidth - document.documentElement.clientWidth;
      if (gutter > 0) document.body.style.paddingRight = `${parseFloat(getComputedStyle(document.body).paddingRight) + gutter}px`;
      document.body.style.overflow = 'hidden';
    }
    target = localTarget(url.searchParams.get('redirect_uri'));
    originalUrl = url.href;
    // Halo redirects to redirect_uri when it is done; these pages report back to this window.
    const callback = new URL(url.pathname === '/logout' ? '/auth-ui/logged-out' : '/auth-ui/complete', location.origin);
    callback.searchParams.set('target', target || here());
    url.searchParams.set('redirect_uri', callback.pathname + callback.search);
    dialog.dataset.view = url.pathname.split('/')[1];
    dialog.dataset.state = 'loading';
    frame.src = url.href;
    if (!dialog.open) {
      dialog.showModal();
      dialog.focus({ preventScroll: true });
    }
    return true;
  }

  // Matches the frame to the design inside it: card colour, text, accent and corner radius.
  function adopt(root) {
    if (!root) return;
    const css = getComputedStyle(root);
    const pick = (name, fallback = '') => css.getPropertyValue(name).trim() || fallback;
    // Dark designs get a dark base, a darker scrim and a lighter close button.
    const dark = css.colorScheme.includes('dark') && !css.colorScheme.includes('light');
    const values = {
      '--m-card': pick('--auth-card', '#fff'),
      '--m-base': dark ? '#16171c' : '#fff',
      '--m-text': pick('--auth-text', '#17201f'),
      '--m-muted': pick('--auth-muted', '#66706e'),
      '--m-accent': pick('--auth-primary', '#176b62'),
    };
    const radius = Number.parseFloat(pick('--auth-radius'));
    if (Number.isFinite(radius)) values['--m-radius'] = `${Math.min(Math.max(radius, 14), 32)}px`;
    values['--m-scrim'] = dark ? 'rgb(0 0 0 / .6)' : 'rgb(12 18 20 / .42)';
    values['--m-close'] = dark ? 'rgb(255 255 255 / .1)' : 'rgb(0 0 0 / .05)';
    values['--m-close-hover'] = dark ? 'rgb(255 255 255 / .18)' : 'rgb(0 0 0 / .09)';
    for (const [name, value] of Object.entries(values)) dialog.style.setProperty(name, value);
  }

  // The iframe reports its content height; the dialog grows or shrinks to fit, within the viewport.
  function fit(height) {
    dialog.style.setProperty('--auth-frame', `${Math.max(160, Math.round(height))}px`);
    if (dialog.dataset.state !== 'done') dialog.dataset.state = 'ready';
  }

  // Show a short confirmation in place of the form, then reload the current page or follow an
  // explicit redirect target when it differs.
  function succeed(message = '登录成功') {
    if (dialog) {
      dialog.querySelector('.status-text').textContent = `${message}，正在返回…`;
      dialog.dataset.state = 'done';
    }
    setTimeout(() => {
      if (target && target !== here()) location.assign(target);
      else location.reload();
    }, 450);
  }

  window.addEventListener('message', event => {
    if (!dialog?.open || event.origin !== location.origin || event.source !== frame.contentWindow) return;
    const { type, height } = event.data || {};
    if (type === 'auth-ui:close') close();
    if (type === 'auth-ui:resize' && Number.isFinite(height)) fit(height);
    if (type === 'auth-ui:ready') adopt(frame.contentDocument?.getElementById('auth-ui'));
    // Keep the form visible while it submits; the next page replaces it or reports success.
    if (type === 'auth-ui:submitting') dialog.dataset.state = 'submitting';
    if (type === 'auth-ui:success') succeed('登录成功');
    if (type === 'auth-ui:logged-out') succeed('已退出登录');
  });
  // Listen while capturing on window, so auth links are handled before PJAX navigation (Swup,
  // pjax, jquery-pjax, Turbo) sees the click; Swup ignores defaultPrevented, so stop the event too.
  window.addEventListener('click', event => {
    if (event.defaultPrevented || event.button !== 0 || event.ctrlKey || event.metaKey || event.shiftKey || event.altKey) return;
    const link = event.composedPath().find(node => node instanceof HTMLAnchorElement);
    if (!link || link.hasAttribute('download') || (link.target && link.target.toLowerCase() !== '_self') || link.relList.contains('external')) return;
    const url = new URL(link.href, location.origin);
    if (url.origin !== location.origin || !isAuthPage(url.pathname)) return;
    event.preventDefault();
    event.stopPropagation();
    openUrl(url.href);
  }, true);
  window.HaloAuthUI = Object.freeze({
    openLogin(options = {}) {
      const url = new URL('/login', location.origin);
      if (localTarget(options.redirectTo)) url.searchParams.set('redirect_uri', localTarget(options.redirectTo));
      return openUrl(url.href);
    },
    openSignup(options = {}) {
      const url = new URL('/signup', location.origin);
      if (localTarget(options.redirectTo)) url.searchParams.set('redirect_uri', localTarget(options.redirectTo));
      return openUrl(url.href);
    },
    openLogout(options = {}) {
      const url = new URL('/logout', location.origin);
      if (localTarget(options.redirectTo)) url.searchParams.set('redirect_uri', localTarget(options.redirectTo));
      return openUrl(url.href);
    },
    close,
  });

  // A direct visit to an auth page lands here as #auth-ui=<auth URL>.
  function openFromFragment() {
    if (!location.hash.startsWith('#auth-ui=')) return;
    let requested;
    try { requested = decodeURIComponent(location.hash.slice('#auth-ui='.length)); } catch { return; }
    history.replaceState(history.state, '', location.pathname + location.search);
    openUrl(requested);
  }
  // Open after load, once themes have finished moving body elements around.
  if (document.readyState === 'complete') openFromFragment();
  else window.addEventListener('load', openFromFragment, { once: true });
})();

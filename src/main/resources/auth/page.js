(() => {
  'use strict';
  const root = document.getElementById('auth-ui');
  if (!root) return;
  const query = new URLSearchParams(location.search);
  // The console preview renders on /auth-ui/preview/*; the server sets this flag for that path only.
  const preview = window.authUiPreview === true;
  const display = query.get('auth_ui_display') === 'modal' ? 'modal' : 'page';
  const embedded = window.parent !== window && (!preview || display === 'modal');
  root.dataset.embedded = String(embedded);
  const view = root.dataset.view;
  /** Set once the page is ready, so late 'ready' listeners still run. */
  let readyDetail = null;

  const notify = (type, detail = {}) => window.parent.postMessage({ type, ...detail }, location.origin);

  // Report the content height so the site modal and the console preview fit the page.
  if (window.parent !== window && typeof ResizeObserver === 'function') {
    let last = 0;
    new ResizeObserver(() => {
      const height = Math.ceil(root.getBoundingClientRect().height);
      if (height && height !== last) {
        last = height;
        notify('auth-ui:resize', { height });
      }
    }).observe(root);
  }
  document.addEventListener('keydown', event => {
    if (embedded && event.key === 'Escape') {
      event.preventDefault();
      notify('auth-ui:close');
    }
  });

  const PREVIEW_VIEWS = { '/login': 'login', '/signup': 'signup', '/logout': 'logout', '/password-reset/email': 'password-reset', '/challenges/two-factor/totp': 'two-factor' };
  document.querySelectorAll('#auth-ui a').forEach(link => {
    const url = new URL(link.href, location.origin);
    if (url.origin !== location.origin) {
      if (embedded && !link.target) link.target = '_top';
      return;
    }
    if (preview) {
      // Links inside the preview switch between preview pages and never reach real pages.
      const target = PREVIEW_VIEWS[url.pathname];
      if (target) {
        const next = new URL(`/auth-ui/preview/${target}`, location.origin);
        for (const key of ['auth_ui_display', 'template', 'draft']) {
          if (query.get(key)) next.searchParams.set(key, query.get(key));
        }
        link.href = next.href;
      } else {
        link.addEventListener('click', event => event.preventDefault());
      }
      return;
    }
    if (['/login', '/signup', '/password-reset/email'].includes(url.pathname)) {
      // Keep the return target when switching between login, signup and password reset.
      const redirect = query.get('redirect_uri');
      if (redirect) url.searchParams.set('redirect_uri', redirect);
      // Visitors who chose the full page keep it instead of being sent back to the modal.
      if (query.get('auth_ui_page') === '1') url.searchParams.set('auth_ui_page', '1');
      link.href = url.href;
    } else if (embedded && !link.target && !url.pathname.startsWith('/password-reset')) {
      link.target = '_top';
    }
  });
  // Halo marks this link tabindex=-1 by default. Include it in the accessible keyboard flow.
  document.querySelectorAll('.form-item-extra-link').forEach(link => link.removeAttribute('tabindex'));

  // Number headings, fields and links so CSS can reveal them one after another.
  function prepareMotion() {
    const items = root.querySelectorAll(
      '.auth-slot-header, .auth-heading > *, .auth-forms .form-item, .auth-forms .form-item-compact, .auth-forms .divider-wrapper, .auth-forms .pill-items, .auth-forms .alert, .auth-closed, .auth-switch, .auth-slot-footer');
    items.forEach((item, index) => {
      item.classList.add('auth-stagger');
      item.style.setProperty('--i', String(Math.min(index, 14)));
    });
    root.querySelectorAll('[data-animate][data-delay]').forEach(item => {
      const delay = Number.parseInt(item.dataset.delay, 10);
      if (Number.isFinite(delay)) item.style.setProperty('--auth-delay', `${Math.max(0, Math.min(delay, 5000))}ms`);
    });
  }

  function ready() {
    prepareMotion();
    document.querySelectorAll('#auth-ui form').forEach(form => {
      if (embedded && form.classList.contains('social-auth-provider-form')) form.target = '_top';
      form.addEventListener('submit', event => {
        if (preview || form.dataset.submitting === 'true') {
          event.preventDefault();
          return;
        }
        form.dataset.submitting = 'true';
        queueMicrotask(() => {
          if (event.defaultPrevented) {
            delete form.dataset.submitting;
            return;
          }
          if (embedded && form.target !== '_top') notify('auth-ui:submitting');
          form.setAttribute('aria-busy', 'true');
          form.querySelectorAll('[type=submit]').forEach(button => {
            button.disabled = true;
            button.setAttribute('aria-busy', 'true');
            button.dataset.originalLabel = button.textContent;
            button.textContent = '正在提交…';
          });
          root.dispatchEvent(new CustomEvent('auth-ui:submit', { detail: { form, view } }));
        });
      });
    });
    if (preview) {
      document.querySelectorAll('#auth-ui button').forEach(button => { button.disabled = true; });
    }
    // The console already confirmed logout in its own dialog; finish it without asking again.
    const logoutForm = document.getElementById('logout-form');
    if (window.authUiAutoSubmit === true && logoutForm && !preview) {
      root.dataset.autoSubmit = 'true';
      logoutForm.requestSubmit();
    }
    if (embedded && logoutForm) prepareModalLogout(logoutForm);
    if (embedded) notify('auth-ui:ready');
    readyDetail = { view, embedded, preview };
    root.dispatchEvent(new CustomEvent('auth-ui:ready', { detail: readyDetail }));
  }

  // In the site modal, "cancel" closes the dialog instead of navigating back.
  function prepareModalLogout(form) {
    form.querySelectorAll('.cancel-link').forEach(link => {
      link.href = '#';
      link.addEventListener('click', event => {
        event.preventDefault();
        notify('auth-ui:close');
      });
    });
  }

  /**
   * Small API for design JavaScript, e.g.
   * AuthUI.on('ready', ({ view }) => AuthUI.root.classList.add('is-' + view)).
   */
  window.AuthUI = Object.freeze({
    root,
    view,
    embedded,
    preview,
    /**
     * Listens to 'ready' or 'submit'; returns a function that removes the listener.
     * A 'ready' handler added after the page is ready runs right away.
     */
    on(name, handler) {
      const listener = event => handler(event.detail);
      root.addEventListener(`auth-ui:${name}`, listener);
      if (name === 'ready' && readyDetail) queueMicrotask(() => handler(readyDetail));
      return () => root.removeEventListener(`auth-ui:${name}`, listener);
    },
    /** Replays the entrance and stagger animations. */
    replay() {
      root.querySelectorAll('.auth-shell, .auth-stagger, [data-animate]').forEach(item => {
        item.style.animation = 'none';
        void item.offsetWidth;
        item.style.animation = '';
      });
    },
    /** Sets a CSS custom property on the container, e.g. setVar('--auth-primary', '#e11d48'). */
    setVar(name, value) {
      if (/^--[a-zA-Z][\w-]*$/.test(name)) root.style.setProperty(name, String(value));
    },
  });

  if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', ready);
  else ready();

  // The design's own script waits in a non-executing element until AuthUI exists.
  const designScript = document.getElementById('auth-ui-design-script');
  if (designScript && !preview) {
    const script = document.createElement('script');
    script.textContent = designScript.textContent;
    document.body.append(script);
  }

  window.addEventListener('pageshow', () => {
    document.querySelectorAll('#auth-ui form[aria-busy]').forEach(form => {
      form.removeAttribute('aria-busy');
      delete form.dataset.submitting;
      form.querySelectorAll('[data-original-label]').forEach(button => {
        button.disabled = preview;
        button.removeAttribute('aria-busy');
        button.textContent = button.dataset.originalLabel;
      });
    });
  });
})();

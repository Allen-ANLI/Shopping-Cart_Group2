(() => {
  'use strict';
  try {
    const saved = sessionStorage.getItem('storeCatalogUrl');
    if (saved) {
      const destination = new URL(saved, window.location.origin);
      if (destination.origin === window.location.origin && destination.pathname === '/products' && !destination.searchParams.has('id')) {
        document.querySelectorAll('[data-continue-shopping]').forEach(link => {
          link.href = destination.pathname + destination.search + '#catalog';
        });
      }
    }
  } catch { /* The catalogue link remains usable without storage. */ }
  document.querySelectorAll('[data-language]').forEach(button => {
    button.addEventListener('click', () => {
      const language = button.dataset.language === 'zh' ? 'zh' : 'en';
      document.cookie = 'store_lang=' + language + '; Path=/; Max-Age=31536000; SameSite=Lax';
      window.location.reload();
    });
  });
  document.querySelectorAll('[data-confirm]').forEach(form => {
    form.addEventListener('submit', event => {
      if (!window.confirm(form.dataset.confirm)) event.preventDefault();
    });
  });
  const avatar = document.querySelector('[data-nav-avatar]');
  if (avatar) {
    const refreshAvatar = async () => {
      if (document.hidden) return;
      try {
        const response = await fetch('/api/auth/session', {credentials: 'same-origin', cache: 'no-store'});
        if (!response.ok) return;
        const state = await response.json();
        if (!state.loggedIn || String(state.user.id) !== avatar.dataset.userId) return;
        if (state.avatarUrl) {
          const image = new Image(); image.alt = '';
          image.onload = () => avatar.replaceChildren(image);
          image.src = state.avatarUrl + '?v=' + Date.now();
        } else { avatar.textContent = avatar.dataset.initial || ''; }
      } catch { /* Keep the currently displayed identity when offline. */ }
    };
    document.addEventListener('visibilitychange', refreshAvatar);
    window.addEventListener('pageshow', event => { if (event.persisted) refreshAvatar(); });
  }
})();

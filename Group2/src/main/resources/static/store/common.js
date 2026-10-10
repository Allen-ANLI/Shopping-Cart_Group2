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
})();

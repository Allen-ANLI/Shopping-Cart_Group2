(() => {
  'use strict';
  document.querySelectorAll('[data-localized-form]').forEach(form => {
    const clearError = field => {
      const error = form.querySelector('#' + field.id + '-validation');
      if (error) error.remove();
      field.removeAttribute('aria-invalid');
      const describedBy = (field.getAttribute('aria-describedby') || '').split(' ')
        .filter(id => id && id !== field.id + '-validation');
      if (describedBy.length) field.setAttribute('aria-describedby', describedBy.join(' '));
      else field.removeAttribute('aria-describedby');
    };
    const errorFor = field => {
      const value = field.value;
      if (field.required && !value.trim()) return field.dataset.requiredMessage;
      if (!value && !field.validity.badInput) return null;
      if (field.validity.badInput || field.validity.typeMismatch) return field.dataset.invalidMessage;
      if ((field.minLength > 0 && value.length < field.minLength)
          || (field.maxLength > 0 && value.length > field.maxLength)) return field.dataset.lengthMessage;
      if (field.pattern && !new RegExp('^(?:' + field.pattern + ')$').test(value)) return field.dataset.invalidMessage;
      if (field.type === 'tel' && (!/^[+0-9][0-9 ()-]{5,29}$/.test(value)
          || value.replace(/[^0-9]/g, '').length < 6 || value.replace(/[^0-9]/g, '').length > 15)) return field.dataset.invalidMessage;
      if (field.id === 'birthday' && value) {
        const birthday = new Date(value + 'T00:00:00');
        const today = new Date(); today.setHours(0, 0, 0, 0);
        if (Number.isNaN(birthday.valueOf()) || birthday >= today) return field.dataset.invalidMessage;
      }
      if (field.id === 'postalCode' && !/^[\p{L}\p{N} -]+$/u.test(value)) return field.dataset.invalidMessage;
      if (form.dataset.formKind === 'register') {
        if (field.name === 'password') {
          if (!/[A-Z]/.test(value) || !/[a-z]/.test(value) || !/[\p{P}\p{S}]/u.test(value)) return field.dataset.invalidMessage;
          if (new TextEncoder().encode(value).length > 72) return field.dataset.bytesMessage;
        }
        if (field.name === 'confirmPassword' && value !== form.elements.password.value) return field.dataset.invalidMessage;
      }
      return null;
    };
    form.querySelectorAll('input[id]').forEach(field => field.addEventListener('input', () => clearError(field)));
    form.addEventListener('submit', event => {
      let firstInvalid = null;
      form.querySelectorAll('input[id]:not([type=radio]):not([type=checkbox]):not([type=hidden])').forEach(field => {
        clearError(field);
        const message = errorFor(field);
        if (!message) return;
        const error = document.createElement('p');
        error.id = field.id + '-validation';
        error.className = 'client-field-error';
        error.setAttribute('role', 'alert');
        error.textContent = message;
        (field.closest('.password-input') || field).insertAdjacentElement('afterend', error);
        field.setAttribute('aria-invalid', 'true');
        field.setAttribute('aria-describedby', [field.getAttribute('aria-describedby'), error.id].filter(Boolean).join(' '));
        firstInvalid ||= field;
      });
      if (firstInvalid) { event.preventDefault(); firstInvalid.focus(); }
    });
    form.querySelectorAll('[data-password-target]').forEach(button => button.addEventListener('click', () => {
      const field = document.getElementById(button.dataset.passwordTarget);
      const visible = field.type === 'password'; field.type = visible ? 'text' : 'password';
      button.setAttribute('aria-pressed', String(visible));
      button.setAttribute('aria-label', visible ? button.dataset.hide : button.dataset.show);
    }));
  });
  document.querySelectorAll('[data-login-form]').forEach(form => {
    const field = form.elements.username;
    const label = form.querySelector('[data-identifier-label]');
    const saved = {};
    let active = form.elements.loginMethod.value || 'username';
    const selectMethod = (focus = false) => {
      const selected = form.elements.loginMethod.value || 'username';
      saved[active] = field.value;
      if (selected !== active) field.value = saved[selected] || '';
      active = selected;
      label.textContent = label.dataset[selected] || label.dataset.username;
      field.type = selected === 'email' ? 'email' : selected === 'phone' ? 'tel' : 'text';
      field.inputMode = selected === 'email' ? 'email' : selected === 'phone' ? 'tel' : 'text';
      field.maxLength = selected === 'email' ? 255 : selected === 'phone' ? 30 : 50;
      field.removeAttribute('pattern');
      field.dispatchEvent(new Event('input'));
      if (focus) field.focus();
    };
    form.querySelectorAll('input[name="loginMethod"]').forEach(radio => radio.addEventListener('change', () => selectMethod(true)));
    selectMethod();
  });
})();

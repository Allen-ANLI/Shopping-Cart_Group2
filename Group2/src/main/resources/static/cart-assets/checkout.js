/* 结账提交状态与浏览器返回后的按钮恢复。 */
(function () {
    'use strict';
    var form = document.querySelector('[data-checkout-form]');
    if (!form) { return; }
    var button = form.querySelector('[data-place-order]');
    var status = form.querySelector('[data-checkout-status]');
    var originalText = button.textContent;
    var initiallyDisabled = button.disabled;
    var submitting = false;

    form.addEventListener('submit', function (event) {
        if (submitting || initiallyDisabled) { event.preventDefault(); return; }
        if (!form.checkValidity()) { return; }
        submitting = true;
        button.disabled = true;
        button.textContent = form.dataset.placing || 'Placing order…';
        form.setAttribute('aria-busy', 'true');
        status.textContent = form.dataset.wait || 'Please wait while your order is saved.';
    });

    // 浏览器返回上一页或网络失败后返回时，恢复提交按钮，允许安全重试同一请求标识。
    window.addEventListener('pageshow', function () {
        submitting = false;
        button.disabled = initiallyDisabled;
        button.textContent = originalText;
        form.removeAttribute('aria-busy');
        status.textContent = '';
    });
})();

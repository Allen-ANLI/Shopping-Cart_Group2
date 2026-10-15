/* Save cart quantities without leaving the page. Checkout uses confirmed server values only. */
(() => {
    'use strict';
    const page = document.querySelector('[data-cart-page]');
    const checkout = page?.querySelector('[data-cart-checkout]');
    if (!checkout) return;
    const zh = document.documentElement.lang.startsWith('zh');
    const text = (en, cn) => zh ? cn : en;
    const status = page.querySelector('[data-cart-status]');
    const queue = new Set();
    let saving = false;
    let canCheckout = page.dataset.canCheckout === 'true';
    const rows = Array.from(page.querySelectorAll('[data-product-id]')).map(element => ({
        element, input: element.querySelector('[name="quantity"]'),
        error: element.querySelector('[data-quantity-error]'),
        saved: Number(element.querySelector('[name="quantity"]').value),
        dirty: false, invalid: false, notice: '',
    }));
    const money = value => 'S$' + Number(value).toFixed(2);
    const message = row => {
        const raw = row.input.value;
        if (!/^\d+$/.test(raw) || !Number.isSafeInteger(Number(raw)) || Number(raw) < 1) {
            return text('Enter a positive whole number.', '请输入大于 0 的整数。');
        }
        if (Number(raw) > Number(row.input.max)) {
            return text('Quantity cannot exceed available stock: ', '数量不能超过库存上限：') + row.input.max;
        }
        return '';
    };
    function error(row, detail, invalid = Boolean(detail)) {
        row.invalid = invalid;
        row.error.textContent = detail;
        row.error.hidden = !detail;
        row.input.setAttribute('aria-invalid', String(row.invalid));
    }
    function updateCheckout() {
        checkout.disabled = !canCheckout || saving || queue.size > 0 || rows.some(row => row.dirty || row.invalid);
        page.setAttribute('aria-busy', String(saving));
    }
    function restore(row, detail) {
        row.input.value = String(row.saved);
        row.dirty = false;
        row.notice = '';
        error(row, detail + text(' Cannot change quantity. Restored the previous quantity. Enter a valid quantity before checkout.', ' 无法更改，已恢复原数量，请重新输入有效数量后结账。'));
        updateCheckout();
    }
    function normalize(row) {
        const raw = row.input.value;
        const value = Number(raw);
        const stock = Number(row.input.max);
        if (stock < 1) {
            restore(row, text('This item is out of stock. Remove it from your cart.', '此商品已无库存，请移除商品。'));
            return false;
        }
        if (raw !== '' && Number.isFinite(value)) {
            if (value <= 0) {
                row.input.value = '1';
                row.notice = text('Quantity must be at least 1. Adjusted to 1.', '数量不能小于 1，已自动调整为 1。');
            } else if (value > stock) {
                row.input.value = String(stock);
                row.notice = text('Quantity exceeds available stock. Adjusted to ', '数量超过库存，已自动调整为库存上限：') + stock;
            }
        }
        const invalid = message(row);
        if (invalid) { restore(row, invalid); return false; }
        error(row, row.notice, false);
        return true;
    }
    function applyState(state, active) {
        canCheckout = state.canCheckout;
        for (const row of rows) {
            const item = state.items.find(item => String(item.productId) === row.element.dataset.productId);
            if (!item) throw new Error(text('Your cart changed. Refresh the page.', '购物车已变化，请刷新页面。'));
            row.saved = item.quantity;
            row.input.max = String(item.stockQuantity);
            if (row === active || !row.dirty) row.input.value = String(item.quantity);
            row.element.querySelector('[data-line-unit-price]').textContent = money(item.unitPrice);
            row.element.querySelector('[data-line-subtotal]').textContent = money(item.subtotal);
            row.element.querySelector('[data-stock-label]').textContent = text(item.stockQuantity + ' in stock', '库存 ' + item.stockQuantity + ' 件');
            const problem = row.element.querySelector('[data-item-problem]');
            problem.textContent = item.problem || '';
            problem.hidden = item.available;
        }
        page.querySelector('[data-cart-count]').textContent = state.totalQuantity;
        page.querySelector('[data-cart-count-label]').textContent = text(state.totalQuantity + ' item(s) in your cart', '购物车中共 ' + state.totalQuantity + ' 件商品');
        document.querySelectorAll('.nexus-cart-count').forEach(badge => { badge.textContent = state.totalQuantity; });
        page.querySelectorAll('[data-price]').forEach(element => {
            element.textContent = (element.dataset.price === 'discountAmount' ? '-' : '') + money(state.pricing[element.dataset.price]);
        });
        page.querySelector('[data-cart-unavailable]').hidden = canCheckout;
    }
    async function pump() {
        if (saving || queue.size === 0) return;
        const row = queue.values().next().value;
        queue.delete(row);
        if (!normalize(row)) {
            pump();
            return;
        }
        if (Number(row.input.value) === row.saved) {
            row.dirty = false;
            updateCheckout();
            pump();
            return;
        }
        saving = true;
        // Serialize saves so late responses cannot overwrite newer prices or quantities.
        const controls = Array.from(page.querySelectorAll('input[name="quantity"], form button')).filter(control => !control.disabled);
        controls.forEach(control => { control.disabled = true; });
        status.textContent = text('Saving quantity…', '正在保存数量…');
        updateCheckout();
        try {
            const response = await fetch('/api/cart/items/' + row.element.dataset.productId, {
                method: 'PUT', credentials: 'same-origin', headers: {'Content-Type': 'application/json'},
                body: JSON.stringify({quantity: Number(row.input.value), cartFormToken: row.element.querySelector('[name="cartFormToken"]').value}),
            });
            const state = await response.json();
            if (!response.ok) throw new Error(state.message || text('Could not save quantity. Try again.', '数量保存失败，请重试。'));
            applyState(state, row);
            row.dirty = false;
            error(row, row.notice, false);
            status.textContent = text('Cart updated.', '购物车已更新。');
        } catch (failure) {
            restore(row, failure.message || text('Could not save quantity. Try again.', '数量保存失败，请重试。'));
            status.textContent = '';
        } finally {
            saving = false;
            controls.forEach(control => { control.disabled = false; });
            updateCheckout();
            pump();
        }
    }
    function commit(row) {
        if (!row.dirty) return;
        if (!normalize(row)) return;
        queue.add(row);
        updateCheckout();
        pump();
    }
    rows.forEach(row => {
        row.input.addEventListener('input', () => {
            row.dirty = true;
            row.notice = '';
            error(row, '');
            status.textContent = '';
            updateCheckout();
        });
        row.input.addEventListener('blur', () => commit(row));
        row.input.form.addEventListener('submit', event => { event.preventDefault(); row.input.blur(); });
    });
    checkout.addEventListener('click', event => {
        event.preventDefault();
        updateCheckout();
        if (!checkout.disabled) window.location.assign('/checkout');
    });
    window.addEventListener('pageshow', event => {
        if (event.persisted) window.location.reload();
    });
    updateCheckout();
})();

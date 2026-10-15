import { readFileSync } from 'node:fs';
import assert from 'node:assert/strict';
import { test } from 'node:test';
import { JSDOM } from 'jsdom';

const script = readFileSync(new URL('../../src/main/resources/static/cart-assets/cart.js', import.meta.url), 'utf8');
const settle = () => new Promise(resolve => setImmediate(resolve));
function setup(t, { lang = 'en', stock = 250, quantity = 2, count = 1, available = true } = {}) {
    const dom = new JSDOM(`<html lang="${lang}"><body><span class="nexus-cart-count">${quantity}</span>
      <main data-cart-page data-can-checkout="${available}"><table><tbody>${Array.from({length: count}, (_, index) => `
        <tr data-product-id="${index + 1}"><td><p data-item-problem hidden></p></td><td data-line-unit-price>S$10.00</td><td>
          <form class="qty-form"><input name="cartFormToken" value="server-token" type="hidden">
          <input name="quantity" type="number" min="1" max="${stock}" value="${quantity}" step="1"></form>
          <p data-stock-label></p><p data-quantity-error role="alert" hidden></p></td>
          <td data-line-subtotal>S$20.00</td><td><form><button>Remove</button></form></td></tr>`).join('')}
      </tbody></table><p data-cart-status></p><p data-cart-count-label></p><span data-cart-count>${quantity}</span>
      ${['originalSubtotal', 'discountAmount', 'subtotal', 'gstAmount', 'total'].map(key => `<span data-price="${key}"></span>`).join('')}
      <p data-cart-unavailable hidden></p><button data-cart-checkout disabled>Proceed to checkout</button></main></body></html>`,
        { url: 'http://127.0.0.1/cart', runScripts: 'outside-only' });
    t.after(() => dom.window.close());
    const requests = [];
    dom.window.fetch = (url, options) => new Promise((resolve, reject) => requests.push({url, options, resolve, reject}));
    dom.window.eval(script);
    const document = dom.window.document;
    const inputs = [...document.querySelectorAll('input[name="quantity"]')];
    const input = inputs[0];
    const checkout = document.querySelector('[data-cart-checkout]');
    const type = (value, target = input) => {
        target.value = value;
        target.dispatchEvent(new dom.window.Event('input'));
    };
    const change = (value, target = input) => {
        type(value, target);
        target.dispatchEvent(new dom.window.Event('change'));
        target.dispatchEvent(new dom.window.Event('blur'));
    };
    const state = quantities => {
        const totalQuantity = quantities.reduce((sum, value) => sum + value, 0);
        return {totalQuantity, canCheckout: true, items: quantities.map((value, index) => ({
            productId: index + 1, quantity: value, stockQuantity: stock, available: true, unitPrice: 10, subtotal: value * 10,
        })), pricing: {originalSubtotal: totalQuantity * 12, discountAmount: totalQuantity * 2,
            subtotal: totalQuantity * 10, gstAmount: totalQuantity * .9, total: totalQuantity * 10.9}};
    };
    const respond = async (index, data, ok = true) => {
        requests[index].resolve({ok, json: async () => data});
        await settle();
    };
    return {window: dom.window, document, requests, input, inputs, checkout, type, change, state, respond};
}

test('saves valid changes automatically and updates line amount, counts, discount, GST and total', async t => {
    const ui = setup(t);
    assert.equal(ui.checkout.disabled, false);
    ui.change('200');
    assert.equal(ui.checkout.disabled, true);
    assert.equal(ui.input.disabled, true);
    assert.equal(ui.requests.length, 1);
    assert.equal(ui.requests[0].url, '/api/cart/items/1');
    assert.equal(ui.requests[0].options.method, 'PUT');
    assert.deepEqual(JSON.parse(ui.requests[0].options.body), {quantity: 200, cartFormToken: 'server-token'});
    await ui.respond(0, ui.state([200]));
    assert.equal(ui.input.value, '200');
    assert.equal(ui.document.querySelector('[data-line-subtotal]').textContent, 'S$2000.00');
    assert.equal(ui.document.querySelector('[data-price="discountAmount"]').textContent, '-S$400.00');
    assert.equal(ui.document.querySelector('[data-price="gstAmount"]').textContent, 'S$180.00');
    assert.equal(ui.document.querySelector('[data-price="total"]').textContent, 'S$2180.00');
    assert.equal(ui.document.querySelector('.nexus-cart-count').textContent, '200');
    assert.equal(ui.checkout.disabled, false);
});

for (const value of ['1.5', '', 'abc', '1e2']) {
    test(`rejects ${JSON.stringify(value)}, restores the saved value, and blocks checkout until a valid edit`, async t => {
        const ui = setup(t);
        ui.type(value);
        assert.equal(ui.checkout.disabled, true);
        assert.equal(ui.document.querySelector('[data-quantity-error]').hidden, true);
        ui.input.dispatchEvent(new ui.window.Event('change'));
        assert.equal(ui.document.querySelector('[data-quantity-error]').hidden, true);
        ui.input.dispatchEvent(new ui.window.Event('blur'));
        assert.equal(ui.input.value, '2');
        assert.equal(ui.requests.length, 0);
        assert.equal(ui.document.querySelector('[data-quantity-error]').hidden, false);
        assert.equal(ui.input.getAttribute('aria-invalid'), 'true');
        ui.checkout.click();
        assert.equal(ui.window.location.pathname, '/cart');
        ui.change('2');
        assert.equal(ui.checkout.disabled, false);
        assert.equal(ui.input.getAttribute('aria-invalid'), 'false');
    });
}

for (const value of ['100', '0', '-1', '251', '1.5', '']) {
    test(`typing ${JSON.stringify(value)} waits for blur, even after a pause or change event`, async t => {
        const ui = setup(t);
        ui.type(value);
        ui.input.dispatchEvent(new ui.window.Event('change'));
        await new Promise(resolve => setTimeout(resolve, 600));
        assert.equal(ui.input.value, value);
        assert.equal(ui.checkout.disabled, true);
        assert.equal(ui.requests.length, 0);
        assert.equal(ui.document.querySelector('[data-quantity-error]').hidden, true);
        assert.equal(ui.input.getAttribute('aria-invalid'), 'false');
    });
}

for (const [value, expected] of [['0', 1], ['-1', 1], ['-100', 1], ['251', 250], ['1000', 250], ['9007199254740992', 250]]) {
    test(`clamps ${value} to ${expected} on blur, saves it and enables checkout after confirmation`, async t => {
        const ui = setup(t, {lang: 'zh'});
        ui.change(value);
        assert.equal(ui.input.value, String(expected));
        assert.equal(ui.checkout.disabled, true);
        assert.equal(ui.requests.length, 1);
        assert.equal(JSON.parse(ui.requests[0].options.body).quantity, expected);
        assert.equal(ui.document.querySelector('[data-quantity-error]').hidden, false);
        assert.match(ui.document.querySelector('[data-quantity-error]').textContent, /已自动调整/);
        assert.equal(ui.input.getAttribute('aria-invalid'), 'false');
        await ui.respond(0, ui.state([expected]));
        assert.equal(ui.checkout.disabled, false);
        assert.equal(ui.input.value, String(expected));
        assert.match(ui.document.querySelector('[data-quantity-error]').textContent, /已自动调整/);
    });
}

for (const [quantity, value] of [[1, '0'], [250, '251']]) {
    test(`clamping to an already saved quantity of ${quantity} needs no request or further edit`, t => {
        const ui = setup(t, {quantity});
        ui.change(value);
        assert.equal(ui.input.value, String(quantity));
        assert.equal(ui.checkout.disabled, false);
        assert.equal(ui.requests.length, 0);
        assert.equal(ui.document.querySelector('[data-quantity-error]').hidden, false);
    });
}

test('zero stock never clamps to an unpurchasable quantity or enables checkout', t => {
    const ui = setup(t, {stock: 0, available: false});
    ui.change('0');
    assert.equal(ui.input.value, '2');
    assert.equal(ui.checkout.disabled, true);
    assert.equal(ui.requests.length, 0);
    assert.match(ui.document.querySelector('[data-quantity-error]').textContent, /out of stock/);
});

test('Enter saves without submitting the old form or requiring an update button', async t => {
    const ui = setup(t);
    ui.input.focus();
    ui.type('5');
    const submit = new ui.window.Event('submit', {cancelable: true});
    ui.input.form.dispatchEvent(submit);
    assert.equal(submit.defaultPrevented, true);
    assert.equal(ui.requests.length, 1);
    assert.equal(ui.input.form.querySelector('button'), null);
    await ui.respond(0, ui.state([5]));
    assert.equal(ui.checkout.disabled, false);
});

test('server rejection restores the last confirmed quantity and keeps checkout blocked', async t => {
    const ui = setup(t, {lang: 'zh'});
    ui.change('150'); await ui.respond(0, ui.state([150]));
    ui.change('151'); await ui.respond(1, {message: '库存不足，请减少购买数量。'}, false);
    assert.equal(ui.input.value, '150');
    assert.equal(ui.checkout.disabled, true);
    assert.match(ui.document.querySelector('[data-quantity-error]').textContent, /库存不足.*已恢复原数量/);
    ui.change('149'); await ui.respond(2, ui.state([149]));
    assert.equal(ui.checkout.disabled, false);
});

test('network failures restore the last value and allow retrying a valid edit', async t => {
    const ui = setup(t);
    ui.change('3'); ui.requests[0].reject(new Error('Network unavailable')); await settle();
    assert.equal(ui.input.value, '2');
    assert.equal(ui.checkout.disabled, true);
    assert.equal(ui.input.disabled, false);
    ui.change('3'); await ui.respond(1, ui.state([3]));
    assert.equal(ui.checkout.disabled, false);
});

test('serializes changes to multiple rows and preserves unsaved quantities while applying server totals', async t => {
    const ui = setup(t, {count: 2});
    ui.type('3', ui.inputs[0]); ui.type('4', ui.inputs[1]);
    ui.inputs[0].dispatchEvent(new ui.window.Event('blur'));
    ui.inputs[1].dispatchEvent(new ui.window.Event('blur'));
    assert.equal(ui.requests.length, 1);
    await ui.respond(0, ui.state([3, 2]));
    assert.equal(ui.inputs[1].value, '4');
    assert.equal(ui.requests.length, 2);
    assert.equal(ui.checkout.disabled, true);
    await ui.respond(1, ui.state([3, 4]));
    assert.equal(ui.document.querySelector('[data-cart-count]').textContent, '7');
    assert.equal(ui.checkout.disabled, false);
});

test('unavailable cart items remain blocked until the server confirms a valid correction', async t => {
    const ui = setup(t, {stock: 3, quantity: 5, available: false});
    assert.equal(ui.checkout.disabled, true);
    ui.change('4');
    assert.equal(ui.input.value, '3');
    assert.equal(ui.checkout.disabled, true);
    await ui.respond(0, ui.state([3]));
    assert.equal(ui.input.max, '3');
    assert.equal(ui.checkout.disabled, false);
});

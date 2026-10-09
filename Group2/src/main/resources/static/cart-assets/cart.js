/**
 * 为购物车提供数量增减和变更提示，保存仍通过现有 POST 表单完成。
 * @author 王重一
 */
(function () {
 'use strict';
 document.querySelectorAll('.qty-form').forEach(function (form) {
  var input = form.querySelector('[name="quantity"]');
  var initial = input.value;
  var update = form.querySelector('[type="submit"]');
  var steps = form.querySelectorAll('[data-quantity-step]');
  function render() {
   var value = Number(input.value); update.disabled = input.value === initial;
   steps.forEach(function (button) {
    var delta = Number(button.dataset.quantityStep);
    button.disabled = !input.validity.valid || value + delta < Number(input.min) || value + delta > Number(input.max);
   });
  }
  steps.forEach(function (button) {
   button.hidden = false;
   button.addEventListener('click', function () {
    var next = Number(input.value) + Number(button.dataset.quantityStep);
    if (next < Number(input.min) || next > Number(input.max)) return;
    input.value = String(next); input.dispatchEvent(new Event('input', { bubbles: true }));
   });
  });
  input.addEventListener('input', render); window.addEventListener('pageshow', render); render();
 });
})();

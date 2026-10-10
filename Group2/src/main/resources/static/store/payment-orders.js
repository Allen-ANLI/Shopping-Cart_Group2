(() => {
  'use strict';
  const zh = document.documentElement.lang === 'zh';
  const cardNumber=document.querySelector('#cardNumber');
  if(cardNumber) cardNumber.addEventListener('input',()=>{
    const caret=cardNumber.selectionStart;
    const digitsBefore=cardNumber.value.slice(0,caret).replace(/\D/g,'').length;
    const digits=cardNumber.value.replace(/\D/g,'').slice(0,19);
    cardNumber.value=digits.replace(/(.{4})(?=.)/g,'$1 ');
    let next=digitsBefore+Math.floor(Math.max(0,digitsBefore-1)/4);
    cardNumber.setSelectionRange(next,next);
  });
  const expiry=document.querySelector('#cardExpiry');
  if(expiry) expiry.addEventListener('input',()=>{
    const digits=expiry.value.replace(/\D/g,'').slice(0,4);
    expiry.value=digits.length>2 ? digits.slice(0,2)+'/'+digits.slice(2) : digits;
  });
  document.querySelector('[data-payment-form]')?.addEventListener('submit',event=>{
    const form=event.currentTarget;
    form.querySelector('button[type="submit"]').disabled=true;
    form.querySelector('[data-payment-status]').textContent=zh?'正在处理支付…':'Processing payment…';
  });
  window.addEventListener('pageshow',()=>{const form=document.querySelector('[data-payment-form]');if(form){form.querySelector('button[type=submit]').disabled=false;form.querySelector('[data-payment-status]').textContent='';}});
  const stars=document.querySelector('.rating-stars');
  if(stars){
    const paint=rating=>stars.querySelectorAll('[data-star]').forEach(label=>label.classList.toggle('filled',Number(label.dataset.star)<=rating));
    paint(Number(stars.dataset.rating));
    stars.addEventListener('change',event=>{stars.dataset.rating=event.target.value;paint(Number(event.target.value));document.querySelector('[data-rating-hint]').textContent=event.target.value+(zh?' 星':' / 5 stars');});
    stars.querySelectorAll('[data-star]').forEach(label=>label.addEventListener('mouseenter',()=>paint(Number(label.dataset.star))));
    stars.addEventListener('mouseleave',()=>paint(Number(stars.dataset.rating)));
  }

  document.addEventListener('submit', async event => {
    const form = event.target;
    if (!form.matches('[data-order-action]')) return;
    event.preventDefault();
    const button = form.querySelector('button[type="submit"]');
    if (button.disabled) return;
    button.disabled = true;
    form.setAttribute('aria-busy', 'true');
    try {
      const response = await fetch(form.action, { method: 'POST', body: new URLSearchParams(new FormData(form)), credentials: 'same-origin' });
      const html = new DOMParser().parseFromString(await response.text(), 'text/html');
      const content = html.querySelector('#order-content');
      if (!response.ok || !content) throw new Error('Request failed');
      document.querySelector('#order-content').replaceWith(content);
      const message = content.querySelector('[role="status"], [role="alert"]');
      if (message) { message.tabIndex = -1; message.focus({ preventScroll: true }); message.scrollIntoView({ behavior: 'smooth', block: 'center' }); }
    } catch {
      let error = form.querySelector('[data-request-error]');
      if (!error) { error = document.createElement('p'); error.dataset.requestError = ''; error.className = 'form-error'; error.setAttribute('role', 'alert'); form.append(error); }
      error.textContent = zh ? '操作未完成，请刷新页面后重试。' : 'We could not complete this action. Refresh the page and try again.';
      button.disabled = false;
    } finally { form.removeAttribute('aria-busy'); }
  });
})();

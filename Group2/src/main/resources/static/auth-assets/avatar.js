(() => {
  'use strict';
  const editor = document.querySelector('[data-avatar-editor]');
  if (!editor) return;
  const select = selector => editor.querySelector(selector);
  const fileInput = select('[data-avatar-file]'), choose = select('[data-avatar-choose]');
  const dialog = select('dialog'), canvas = select('canvas'), zoom = select('[data-avatar-zoom]');
  const status = select('[data-avatar-status]'), error = select('[data-avatar-error]');
  const save = select('[data-avatar-save]'), cancel = select('[data-avatar-cancel]');
  let photo, objectUrl, offsetX = 0, offsetY = 0, drag, saving = false;
  const size = 300;
  const scale = () => Math.max(size / photo.naturalWidth, size / photo.naturalHeight) * Number(zoom.value);
  const clamp = () => {
    const limitX = (photo.naturalWidth * scale() - size) / 2;
    const limitY = (photo.naturalHeight * scale() - size) / 2;
    offsetX = Math.max(-limitX, Math.min(limitX, offsetX));
    offsetY = Math.max(-limitY, Math.min(limitY, offsetY));
  };
  const drawPhoto = context => {
    const width = photo.naturalWidth * scale(), height = photo.naturalHeight * scale();
    context.fillStyle = '#fff'; context.fillRect(0, 0, size, size);
    context.drawImage(photo, (size - width) / 2 + offsetX, (size - height) / 2 + offsetY, width, height);
  };
  const render = () => {
    if (!photo) return;
    clamp();
    const context = canvas.getContext('2d'); context.setTransform(2, 0, 0, 2, 0, 0);
    drawPhoto(context);
    context.beginPath(); context.rect(0, 0, size, size); context.arc(size / 2, size / 2, size / 2 - 2, 0, Math.PI * 2);
    context.fillStyle = 'rgba(12, 30, 22, .65)'; context.fill('evenodd');
    context.beginPath(); context.arc(size / 2, size / 2, size / 2 - 2, 0, Math.PI * 2);
    context.strokeStyle = '#fff'; context.lineWidth = 2; context.stroke();
  };
  const release = () => {
    if (objectUrl) URL.revokeObjectURL(objectUrl);
    objectUrl = null; photo = null; drag = null; fileInput.value = '';
  };
  choose.addEventListener('click', () => fileInput.click());
  fileInput.addEventListener('change', () => {
    const file = fileInput.files[0]; if (!file) return;
    status.textContent = ''; error.textContent = '';
    if (!['image/jpeg', 'image/png', 'image/webp'].includes(file.type) || file.size > 8 * 1024 * 1024) {
      status.textContent = editor.dataset.invalid; fileInput.value = ''; return;
    }
    release(); choose.disabled = true;
    const image = new Image(); objectUrl = URL.createObjectURL(file);
    image.onload = () => {
      choose.disabled = false;
      if (image.naturalWidth < 64 || image.naturalHeight < 64) { status.textContent = editor.dataset.invalid; release(); return; }
      photo = image; offsetX = 0; offsetY = 0; zoom.value = '1'; render(); dialog.showModal();
      canvas.focus();
    };
    image.onerror = () => { choose.disabled = false; status.textContent = editor.dataset.invalid; release(); };
    image.src = objectUrl;
  });
  canvas.addEventListener('pointerdown', event => {
    if (saving || !photo) return;
    drag = {x: event.clientX, y: event.clientY}; canvas.setPointerCapture(event.pointerId); canvas.focus();
  });
  canvas.addEventListener('pointermove', event => {
    if (!drag) return;
    const ratio = size / canvas.getBoundingClientRect().width;
    offsetX += (event.clientX - drag.x) * ratio; offsetY += (event.clientY - drag.y) * ratio;
    drag = {x: event.clientX, y: event.clientY}; render();
  });
  ['pointerup', 'pointercancel', 'lostpointercapture'].forEach(name => canvas.addEventListener(name, () => { drag = null; }));
  canvas.addEventListener('keydown', event => {
    if (saving || !photo || !['ArrowLeft', 'ArrowRight', 'ArrowUp', 'ArrowDown'].includes(event.key)) return;
    event.preventDefault(); const step = event.shiftKey ? 15 : 5;
    offsetX += event.key === 'ArrowLeft' ? -step : event.key === 'ArrowRight' ? step : 0;
    offsetY += event.key === 'ArrowUp' ? -step : event.key === 'ArrowDown' ? step : 0; render();
  });
  zoom.addEventListener('input', render);
  select('[data-avatar-reset]').addEventListener('click', () => { if (!saving) { offsetX = offsetY = 0; zoom.value = '1'; render(); } });
  cancel.addEventListener('click', () => { if (!saving) dialog.close(); });
  dialog.addEventListener('cancel', event => { if (saving) event.preventDefault(); });
  dialog.addEventListener('close', () => { release(); choose.focus(); });
  save.addEventListener('click', async () => {
    if (!photo || saving) return;
    saving = true; drag = null; save.disabled = cancel.disabled = zoom.disabled = true;
    save.textContent = editor.dataset.saving; error.textContent = '';
    try {
      const output = document.createElement('canvas'); output.width = output.height = 512;
      const context = output.getContext('2d'); context.scale(512 / size, 512 / size); drawPhoto(context);
      const blob = await new Promise(resolve => output.toBlob(resolve, 'image/jpeg', .9));
      if (!blob) throw new Error(editor.dataset.failed);
      const body = new FormData(); body.append('avatar', blob, 'avatar.jpg'); body.append('accountFormToken', editor.dataset.token);
      const response = await fetch('/api/account/avatar', {method: 'POST', body, credentials: 'same-origin'});
      const result = await response.json().catch(() => ({}));
      if (!response.ok) throw new Error(result.message || editor.dataset.failed);
      const current = select('[data-avatar-image]'); current.src = result.url + '?v=' + Date.now(); current.hidden = false;
      document.querySelectorAll('[data-nav-avatar]').forEach(container => {
        const image = new Image(); image.alt = ''; image.src = current.src; container.replaceChildren(image);
      });
      select('[data-avatar-initial]').hidden = true; status.textContent = result.message; dialog.close();
    } catch (failure) { error.textContent = failure.message || editor.dataset.failed; }
    finally { saving = false; save.disabled = cancel.disabled = zoom.disabled = false; save.textContent = editor.dataset.save; }
  });
})();

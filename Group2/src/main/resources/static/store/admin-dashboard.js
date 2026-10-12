(() => {
  'use strict';
  const dashboard = document.querySelector('[data-dashboard]');
  if (!dashboard) return;
  const locale = document.documentElement.lang === 'zh' ? 'zh-SG' : 'en-SG';
  const number = new Intl.NumberFormat(locale);
  const money = value => 'S$' + new Intl.NumberFormat(locale, {minimumFractionDigits: 2, maximumFractionDigits: 2}).format(value);
  const status = dashboard.querySelector('[data-refresh-status]');
  const button = dashboard.querySelector('[data-refresh]');
  let busy = false;
  let previous = null;
  async function refresh() {
    if (busy || document.hidden) return;
    busy = true; button.disabled = true;
    try {
      const response = await fetch(dashboard.dataset.endpoint, {credentials: 'same-origin', cache: 'no-store'});
      if (!response.ok || !response.headers.get('content-type')?.includes('application/json')) throw new Error('Unable to refresh');
      const data = await response.json();
      const snapshot = JSON.stringify(data);
      if (snapshot !== previous) {
        const rows = document.createDocumentFragment();
        for (const sale of data.productSales) {
          const row = document.createElement('tr'); const name = document.createElement('td'); const link = document.createElement('a');
          link.href = dashboard.dataset.productUrl + '?id=' + encodeURIComponent(sale.productId); link.textContent = document.documentElement.lang === 'zh' && sale.nameZh ? sale.nameZh : sale.name; name.append(link); row.append(name);
          for (const value of [money(sale.unitPrice), number.format(sale.quantity), money(sale.revenue)]) { const cell = document.createElement('td'); cell.className = 'col-num'; cell.textContent = value; row.append(cell); }
          rows.append(row);
        }
        if (!data.productSales.length) { const row = document.createElement('tr'); const cell = document.createElement('td'); cell.colSpan = 4; cell.textContent = dashboard.dataset.empty; row.append(cell); rows.append(row); }
        dashboard.querySelector('[data-sales-rows]').replaceChildren(rows); dashboard.querySelector('[data-total-sales]').textContent = money(data.totalSales); dashboard.querySelector('[data-item-count]').textContent = number.format(data.itemCount); dashboard.querySelector('[data-order-count]').textContent = number.format(data.orderCount); previous = snapshot;
      }
      status.textContent = dashboard.dataset.live + ' ' + dashboard.dataset.updated + ' ' + new Date().toLocaleTimeString(locale);
    } catch { status.textContent = dashboard.dataset.offline; }
    finally { busy = false; button.disabled = false; }
  }
  button.addEventListener('click', refresh); document.addEventListener('visibilitychange', refresh); window.addEventListener('focus', refresh); window.addEventListener('pageshow', refresh); setInterval(refresh, 5000); refresh();
})();

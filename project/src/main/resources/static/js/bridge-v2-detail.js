(() => {
  'use strict';
  const tabs = [...document.querySelectorAll('.bv-tab[role="tab"]')];
  function activate(tab) {
    for (const item of tabs) {
      const selected = item === tab;
      item.classList.toggle('is-active', selected);
      item.setAttribute('aria-selected', String(selected));
      item.tabIndex = selected ? 0 : -1;
      document.getElementById(item.dataset.panel).hidden = !selected;
    }
  }
  tabs.forEach((tab, i) => {
    tab.tabIndex = i === 0 ? 0 : -1;
    tab.addEventListener('click', () => activate(tab));
    tab.addEventListener('keydown', e => {
      if (e.key !== 'ArrowLeft' && e.key !== 'ArrowRight') return;
      e.preventDefault();
      const offset = e.key === 'ArrowRight' ? 1 : -1;
      const next = tabs[(i + offset + tabs.length) % tabs.length];
      activate(next); next.focus();
    });
  });
  const data = document.getElementById('bv-viewed-data');
  if (data && /^\d+$/.test(data.dataset.no || '')) {
    try {
      sessionStorage.setItem('bv-recent-bridge', JSON.stringify({
        no: data.dataset.no, name: data.dataset.name, code: data.dataset.code,
        region: data.dataset.region, type: data.dataset.type, date: data.dataset.date,
        status: data.dataset.status
      }));
    } catch (_) { /* 저장소가 비활성화된 경우 무시 */ }
  }
})();

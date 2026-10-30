(() => {
  'use strict';
  const tabs = [...document.querySelectorAll('.bv-tab[role="tab"]')];
  function activate(tab) {
    for (const item of tabs) {
      const selected = item === tab;
      item.classList.toggle('is-active', selected);
      item.setAttribute('aria-selected', String(selected));
      item.tabIndex = selected ? 0 : -1;
      const panel = document.getElementById(item.dataset.panel);
      if (panel) panel.hidden = !selected;
    }
  }
  tabs.forEach((tab, index) => {
    tab.tabIndex = index === 0 ? 0 : -1;
    tab.addEventListener('click', () => activate(tab));
    tab.addEventListener('keydown', event => {
      if (!['ArrowLeft', 'ArrowRight'].includes(event.key)) return;
      event.preventDefault();
      const next = tabs[(index + (event.key === 'ArrowRight' ? 1 : -1) + tabs.length) % tabs.length];
      activate(next); next.focus();
    });
  });
})();

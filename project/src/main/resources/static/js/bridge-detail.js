/* 4단계 교량 상세 화면: 서버 저장/API 요청 없이 업무 기록 탭만 전환합니다. */
(() => {
    'use strict';
    const tabs = [...document.querySelectorAll('[data-detail-tab]')];
    const panels = [...document.querySelectorAll('[data-detail-panel]')];
    if (!tabs.length || !panels.length) return;

    function activate(tab, focus = false) {
        const selected = tab.dataset.detailTab;
        for (const item of tabs) {
            const active = item === tab;
            item.setAttribute('aria-selected', String(active));
            item.tabIndex = active ? 0 : -1;
            item.classList.toggle('is-active', active);
        }
        for (const panel of panels) {
            panel.hidden = panel.dataset.detailPanel !== selected;
        }
        if (focus) tab.focus();
    }

    tabs.forEach((tab, index) => {
        tab.addEventListener('click', () => activate(tab));
        tab.addEventListener('keydown', (event) => {
            let nextIndex = -1;
            if (event.key === 'ArrowRight') nextIndex = (index + 1) % tabs.length;
            if (event.key === 'ArrowLeft') nextIndex = (index - 1 + tabs.length) % tabs.length;
            if (event.key === 'Home') nextIndex = 0;
            if (event.key === 'End') nextIndex = tabs.length - 1;
            if (nextIndex < 0) return;
            event.preventDefault();
            activate(tabs[nextIndex], true);
        });
    });
    activate(tabs.find(tab => tab.getAttribute('aria-selected') === 'true') || tabs[0]);
})();
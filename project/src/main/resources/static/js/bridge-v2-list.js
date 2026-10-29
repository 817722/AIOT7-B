(() => {
  'use strict';
  const province = document.getElementById('bv-province');
  const city = document.getElementById('bv-city');
  if (province && city) {
    const options = [...city.options].filter(opt => opt.value);
    const update = () => {
      for (const opt of options) {
        opt.hidden = !!province.value && opt.dataset.province !== province.value;
        opt.disabled = opt.hidden;
      }
      if (city.selectedOptions[0]?.disabled) city.value = '';
    };
    province.addEventListener('change', () => { city.value = ''; update(); });
    update();
  }
  const recent = document.getElementById('bv-recent-text');
  const link = document.getElementById('bv-recent-link');
  if (recent && link) {
    try {
      const item = JSON.parse(sessionStorage.getItem('bv-recent-bridge') || 'null');
      if (item && /^\d+$/.test(String(item.no)) && typeof item.name === 'string') {
        recent.textContent = [item.name, item.code, item.region, item.type, item.date, item.status].filter(Boolean).join(' / ');
        link.href = `/bridges/${encodeURIComponent(item.no)}`;
        link.hidden = false;
      }
    } catch (_) { /* 브라우저 저장소를 사용할 수 없으면 안내문 유지 */ }
  }
})();

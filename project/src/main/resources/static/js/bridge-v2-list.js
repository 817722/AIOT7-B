(() => {
  'use strict';
  const province = document.getElementById('bv-province');
  const city = document.getElementById('bv-city');
  if (!province || !city) return;
  const options = [...city.options].filter(opt => opt.value);
  const update = () => {
    options.forEach(opt => {
      opt.hidden = !!province.value && opt.dataset.province !== province.value;
      opt.disabled = opt.hidden;
    });
    if (city.selectedOptions[0]?.disabled) city.value = '';
  };
  province.addEventListener('change', () => { city.value = ''; update(); });
  update();
  // 최근 조회 이력: 다른 계정의 브라우저 세션 데이터가 노출되지 않도록
  // 기존 전역 sessionStorage 예시 기능은 실제 계정별 저장 방식 확정 전 비활성화.
})();

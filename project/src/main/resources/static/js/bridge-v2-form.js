(() => {
  'use strict';
  const query = document.getElementById('bv-csv-query');
  const button = document.getElementById('bv-csv-search');
  const rows = [...document.querySelectorAll('.bv-candidate-row')];
  const empty = document.getElementById('bv-csv-empty');
  const status = document.getElementById('bv-selection-message');
  function search() {
    const term = (query?.value || '').trim().toLocaleLowerCase('ko');
    let count = 0;
    for (const row of rows) {
      const match = !term || [row.dataset.name, row.dataset.code, row.dataset.province, row.dataset.city]
        .some(v => (v || '').toLocaleLowerCase('ko').includes(term));
      row.hidden = !match;
      if (match) count++;
    }
    if (empty) empty.hidden = count !== 0;
  }
  button?.addEventListener('click', search);
  query?.addEventListener('keydown', e => { if (e.key === 'Enter') { e.preventDefault(); search(); } });
  query?.addEventListener('input', search);
  const fields = { name: 'bv-name', code: 'bv-code', facility: 'bv-facility' };
  function choose() {
    const checked = document.querySelector('input[name="csvCandidate"]:checked');
    if (!checked) { if (status) status.textContent = '검색 결과에서 교량을 선택해 주세요.'; return; }
    const row = checked.closest('.bv-candidate-row');
    if (!row || row.hidden) { if (status) status.textContent = '현재 검색된 교량 중에서 선택해 주세요.'; return; }
    for (const [key, id] of Object.entries(fields)) document.getElementById(id).value = row.dataset[key] || '';
    document.getElementById('bv-region').value = [row.dataset.province, row.dataset.city].filter(Boolean).join(' ');
    if (status) status.textContent = `${row.dataset.name} 정보를 입력란에 반영했습니다. (화면 시연)`;
  }
  document.getElementById('bv-choose-bridge')?.addEventListener('click', choose);
  const file = document.getElementById('bv-image');
  const preview = document.getElementById('bv-preview');
  const placeholder = document.getElementById('bv-preview-placeholder');
  const message = document.getElementById('bv-image-message');
  let objectUrl = null;
  file?.addEventListener('change', () => {
    if (objectUrl) URL.revokeObjectURL(objectUrl);
    objectUrl = null; preview.hidden = true; preview.removeAttribute('src'); placeholder.hidden = false;
    const image = file.files?.[0];
    if (!image) { message.textContent = '대표 이미지를 선택해 미리볼 수 있습니다.'; return; }
    if (!['image/jpeg', 'image/png', 'image/webp'].includes(image.type) || image.size > 5 * 1024 * 1024) {
      message.textContent = 'JPG·PNG·WebP 형식 및 5MB 이하의 파일만 선택할 수 있습니다.';
      file.value = ''; return;
    }
    objectUrl = URL.createObjectURL(image);
    preview.src = objectUrl; preview.hidden = false; placeholder.hidden = true;
    message.textContent = `${image.name} — 미리보기만 제공됩니다. 서버에 저장되지는 않습니다.`;
  });
  window.addEventListener('pagehide', () => { if (objectUrl) URL.revokeObjectURL(objectUrl); });
})();

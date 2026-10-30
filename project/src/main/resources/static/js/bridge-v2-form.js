(() => {
  'use strict';
  const query = document.getElementById('bv-csv-query');
  const searchButton = document.getElementById('bv-csv-search');
  const tbody = document.getElementById('bv-csv-results');
  const empty = document.getElementById('bv-csv-empty');
  const status = document.getElementById('bv-selection-message');
  const selectedId = document.createElement('input');
  selectedId.type = 'hidden';
  selectedId.id = 'bv-catalog-id';
  document.getElementById('bv-choose-bridge')?.insertAdjacentElement('beforebegin', selectedId);
  const message = text => { if (status) status.textContent = text; };
  const emptyRow = text => {
    tbody.replaceChildren();
    const tr = document.createElement('tr');
    const td = document.createElement('td'); td.colSpan = 5; td.textContent = text;
    tr.appendChild(td); tbody.appendChild(tr);
  };
  async function search() {
    const keyword = query.value.trim();
    if (!keyword) { emptyRow('교량명을 입력해 주세요.'); message('교량명을 입력해 주세요.'); return; }
    selectedId.value = '';
    searchButton.disabled = true; message('전국교량 데이터를 검색하는 중입니다.');
    try {
      const response = await fetch(`/api/bridge-ui/catalog?keyword=${encodeURIComponent(keyword)}`,
        { credentials: 'same-origin', headers: { Accept: 'application/json' } });
      if (!response.ok) throw new Error(`HTTP ${response.status}`);
      const bridges = await response.json();
      if (!Array.isArray(bridges)) throw new Error('검색 응답 형식 오류');
      tbody.replaceChildren();
      for (const item of bridges.slice(0, 50)) {
        const tr = document.createElement('tr');
        tr.className = 'bv-candidate-row';
        tr.dataset.catalogId = item.catalogId || '';
        tr.dataset.name = item.name || '';
        tr.dataset.province = item.province || '';
        tr.dataset.city = item.city || '';
        tr.dataset.facility = item.facilityType || '';
        tr.dataset.address = item.address || '';
        tr.dataset.road = item.roadName || '';
        const radioCell = document.createElement('td');
        const radio = document.createElement('input');
        radio.type = 'radio'; radio.name = 'csvCandidate'; radio.value = item.catalogId || '';
        radio.setAttribute('aria-label', `${item.name || '교량'} 선택`);
        radioCell.appendChild(radio); tr.appendChild(radioCell);
        for (const val of [item.name, item.catalogId,
          [item.province, item.city].filter(Boolean).join(' '), item.facilityType]) {
          const td = document.createElement('td'); td.textContent = val || '-'; tr.appendChild(td);
        }
        tbody.appendChild(tr);
      }
      if (!bridges.length) emptyRow('검색 결과가 없습니다.');
      if (empty) empty.hidden = bridges.length > 0;
      message(`${bridges.length}건 검색됨${bridges.length > 50 ? ' (처음 50건만 표시)' : ''}. 선택해 주세요.`);
    } catch (err) {
      emptyRow('교량 검색 요청에 실패했습니다.');
      message(`검색 실패: ${err.message}`);
    } finally {
      searchButton.disabled = false;
    }
  }
  searchButton?.addEventListener('click', search);
  query?.addEventListener('keydown', event => {
    if (event.key === 'Enter') { event.preventDefault(); search(); }
  });
  document.getElementById('bv-choose-bridge')?.addEventListener('click', () => {
    const checked = tbody.querySelector('input[name="csvCandidate"]:checked');
    if (!checked) { message('검색 결과에서 교량을 선택해 주세요.'); return; }
    const row = checked.closest('.bv-candidate-row');
    selectedId.value = row.dataset.catalogId;
    document.getElementById('bv-name').value = row.dataset.name;
    document.getElementById('bv-code').value = row.dataset.catalogId;
    document.getElementById('bv-region').value = [row.dataset.province, row.dataset.city].filter(Boolean).join(' ');
    const raw = row.dataset.facility;
    document.getElementById('bv-facility').value = ({'01':'제1종','02':'제2종','03':'제3종','99':'미분류'})[raw] || raw;
    message(`${row.dataset.name} 원본을 선택했습니다. 등록 버튼으로 저장할 수 있습니다.`);
  });
  const file = document.getElementById('bv-image');
  const preview = document.getElementById('bv-preview');
  const placeholder = document.getElementById('bv-preview-placeholder');
  const imageMessage = document.getElementById('bv-image-message');
  let objectUrl = null;
  file?.addEventListener('change', () => {
    if (objectUrl) URL.revokeObjectURL(objectUrl);
    objectUrl = null;
    preview.hidden = true; preview.removeAttribute('src'); placeholder.hidden = false;
    const image = file.files?.[0];
    if (!image) { imageMessage.textContent = '대표 이미지를 선택해 미리볼 수 있습니다.'; return; }
    if (!['image/jpeg','image/png','image/webp'].includes(image.type) || image.size > 5 * 1024 * 1024) {
      imageMessage.textContent = 'JPG·PNG·WebP 형식, 5MB 이하의 파일만 선택할 수 있습니다.';
      file.value = ''; return;
    }
    objectUrl = URL.createObjectURL(image);
    preview.src = objectUrl; preview.hidden = false; placeholder.hidden = true;
    imageMessage.textContent = `${image.name} — 저장 버튼을 누르면 서버에 업로드됩니다.`;
  });

  const main = document.getElementById('main-content');
  const isEdit = main?.dataset.editMode === 'true';
  const bridgeNo = Number(main?.dataset.bridgeNo);
  const canManage = main?.dataset.canManage === 'true';
  const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content;
  const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
  const requestHeaders = { 'Content-Type': 'application/json', Accept: 'application/json' };
  if (csrfHeader && csrfToken) requestHeaders[csrfHeader] = csrfToken;

  const managerSelect = document.getElementById('bv-manager-select');
  if (isEdit && canManage && managerSelect) {
    fetch('/api/bridge-ui/managers', { credentials:'same-origin', headers:{ Accept:'application/json' } })
      .then(r => { if (!r.ok) throw new Error(`HTTP ${r.status}`); return r.json(); })
      .then(rows => {
        managerSelect.replaceChildren();
        const placeholder = document.createElement('option');
        placeholder.value = '';
        placeholder.textContent = '담당자를 선택하세요';
        managerSelect.appendChild(placeholder);
        for (const row of rows) {
          const option = document.createElement('option');
          option.value = String(row.no); option.textContent = row.name;
          option.selected = String(row.no) === String(main.dataset.managerNo);
          managerSelect.append(option);
        }
      })
      .catch(() => { managerSelect.replaceChildren(); message('담당자 목록을 읽지 못했습니다.'); });
  }

  async function writeJson(path, method, body) {
    const response = await fetch(path, {
      method, credentials: 'same-origin', headers: requestHeaders,
      ...(body === undefined ? {} : { body: JSON.stringify(body) })
    });
    if (!response.ok) {
      let detail = '';
      try { const error = await response.json(); detail = error.detail || error.message || ''; } catch (_) {}
      throw new Error(`${detail || `HTTP ${response.status}`}`);
    }
    return response.status === 204 ? null : response.json();
  }

  async function uploadRepresentativeImage(no) {
    const image = file?.files?.[0];
    if (!image) return;
    const formData = new FormData();
    formData.append('file', image, image.name);
    const headers = {};
    if (csrfHeader && csrfToken) headers[csrfHeader] = csrfToken;
    const response = await fetch(`/api/bridge-ui/${no}/image`, {
      method: 'POST', credentials: 'same-origin', headers, body: formData
    });
    if (!response.ok) throw new Error(`이미지 업로드 HTTP ${response.status}`);
  }

  document.getElementById('bv-save')?.addEventListener('click', async e => {
    const button = e.currentTarget;
    button.disabled = true;
    try {
      if (!isEdit) {
        const masterNo = Number(selectedId.value);
        if (!Number.isSafeInteger(masterNo) || masterNo <= 0) {
          throw new Error('원본 교량을 검색하고 선택해 주세요.');
        }
        const saved = await writeJson('/api/bridge-ui', 'POST', { masterNo });
        try { await uploadRepresentativeImage(saved.no); }
        catch (imageError) {
          window.alert(`교량은 등록됐지만 대표 이미지 업로드에 실패했습니다: ${imageError.message}\n교량 상세에서 수정 화면으로 이동해 다시 등록할 수 있습니다.`);
        }
        window.location.assign(`/bridges/${encodeURIComponent(saved.no)}`);
      } else {
        if (!Number.isSafeInteger(bridgeNo) || bridgeNo <= 0) throw new Error('교량 번호가 올바르지 않습니다.');
        const managerNo = Number(managerSelect?.value);
        const haveManager = Number.isSafeInteger(managerNo) && managerNo > 0;
        const haveImage = Boolean(file?.files?.[0]);
        if (!haveManager && !haveImage) throw new Error('담당자를 선택하거나 대표 이미지를 첨부해 주세요.');
        if (haveManager) await writeJson(`/api/bridge-ui/${bridgeNo}/manager`, 'PUT', { managerNo });
        if (haveImage) await uploadRepresentativeImage(bridgeNo);
        window.location.assign(`/bridges/${bridgeNo}`);
      }
    } catch (err) { message(`저장 실패: ${err.message}`); button.disabled = false; }
  });

  document.getElementById('bv-delete')?.addEventListener('click', async e => {
    if (!isEdit || !canManage || !Number.isSafeInteger(bridgeNo)) return;
    if (!window.confirm('이 교량을 삭제하시겠습니까? 연결된 기록이나 이미지가 있다면 삭제할 수 없습니다.')) return;
    const button = e.currentTarget; button.disabled = true;
    try {
      await writeJson(`/api/bridge-ui/${bridgeNo}`, 'DELETE');
      window.location.assign('/bridges');
    } catch (err) { message(`삭제 실패: ${err.message}`); button.disabled = false; }
  });

  window.addEventListener('pagehide', () => { if (objectUrl) URL.revokeObjectURL(objectUrl); });
})();

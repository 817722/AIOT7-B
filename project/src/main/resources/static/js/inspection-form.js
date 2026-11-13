(() => {
  'use strict';
  const main = document.getElementById('main-content');
  const form = document.getElementById('inspection-form');
  if (!main || !form) return;
  const editMode = main.dataset.editMode === 'true';
  const inspectionId = main.dataset.inspectionId || new URLSearchParams(location.search).get('id') || '';
  let inspectionNo = ''; // 점검 PK: 실제 조회 응답을 통해서만 확정
  const bridgeNo = document.getElementById('in-bridge-no');
  const bridgeQuery = document.getElementById('in-bridge-query');
  const bridgeSearch = document.getElementById('in-bridge-search');
  const bridgeResults = document.getElementById('in-bridge-results');
  const bridgeSelected = document.getElementById('in-bridge-selected');
  const message = document.getElementById('in-form-message');
  const submit = document.getElementById('in-submit');
  const single = document.getElementById('in-video-single');
  const multiple = document.getElementById('in-video-multiple');
  const photos = document.getElementById('in-photo-files');
  const previewList = document.getElementById('in-video-preview-list');
  const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content;
  const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;

  function notify(value, error = true) {
    message.textContent = value;
    message.hidden = false;
    message.classList.toggle('is-error', error);
  }
  async function jsonRequest(url, options = {}) {
    const headers = { Accept: 'application/json', ...(options.headers || {}) };
    if (csrfHeader && csrfToken && options.method && options.method !== 'GET') headers[csrfHeader] = csrfToken;
    const response = await fetch(url, { credentials: 'same-origin', ...options, headers });
    if (!response.ok) {
      let detail = '';
      try { const body = await response.json(); detail = body.message || body.detail || body.error || ''; } catch (_) {}
      throw new Error(detail || `요청 실패 (${response.status})`);
    }
    if (response.status === 204) return null;
    return response;
  }
  async function findBridges() {
    const keyword = bridgeQuery.value.trim();
    if (!keyword) { notify('교량명 또는 교량 번호를 입력해 주세요.'); return; }
    bridgeResults.hidden = false;
    bridgeResults.textContent = '검색 중...';
    try {
      const response = await jsonRequest(`/api/maintenance-ui/bridges?keyword=${encodeURIComponent(keyword)}`);
      const rows = await response.json();
      bridgeResults.replaceChildren();
      if (!rows.length) { bridgeResults.textContent = '검색된 교량이 없습니다.'; return; }
      rows.forEach(row => {
        const button = document.createElement('button');
        button.type = 'button'; button.className = 'in-bridge-option';
        const title = document.createElement('strong'); title.textContent = row.name;
        const meta = document.createElement('small'); meta.textContent = `${row.province || ''} ${row.city || ''} · ${row.facilityType || ''} · #${row.no}`;
        button.append(title, meta);
        button.addEventListener('click', () => {
          bridgeNo.value = row.no;
          bridgeQuery.value = row.name;
          bridgeSelected.textContent = `선택된 교량: ${row.name} · ${row.province || ''} ${row.city || ''} · #${row.no}`;
          bridgeResults.hidden = true;
        });
        bridgeResults.append(button);
      });
    } catch (error) { bridgeResults.textContent = error.message; }
  }
  bridgeSearch?.addEventListener('click', findBridges);
  bridgeQuery?.addEventListener('keydown', event => { if (event.key === 'Enter') { event.preventDefault(); findBridges(); } });
  async function loadExistingRecord() {
    if (!editMode || !inspectionId) return;
    try {
      const response = await jsonRequest(`/api/inspections/${encodeURIComponent(inspectionId)}`);
      const record = await response.json();
      inspectionNo = record.no;
      bridgeNo.value = record.bridgeNo || '';
      bridgeQuery.value = record.bridgeName || `교량 #${record.bridgeNo}`;
      bridgeQuery.readOnly = true;
      if (bridgeSearch) bridgeSearch.disabled = true;
      bridgeSelected.textContent = `수정 대상 교량: ${bridgeQuery.value} · #${record.bridgeNo}`;
      document.getElementById('in-title').value = record.title || '';
      document.getElementById('in-type').value = record.inspectionType || '';
      document.getElementById('in-status').value = record.status || '진행 대기';
      document.getElementById('in-date').value = record.inspectionDate || '';
      document.getElementById('in-memo').value = record.memo || '';
      document.getElementById('in-overall-result').value = record.overallResult || '';
      document.getElementById('in-review-opinion').value = record.reviewOpinion || '';
      const inspector = document.getElementById('in-inspector-no') || document.getElementById('in-inspector-no-fallback');
      if (inspector) inspector.value = record.inspectorNo || '';
    } catch (error) { notify(`기존 점검 정보를 불러오지 못했습니다: ${error.message}`); }
  }
  if (editMode && !inspectionId) notify('수정할 점검 UUID(id)가 필요합니다. 현재 페이지 컨트롤러에는 이 값이 전달되지 않습니다. 점검 조회는 목록에서 가능합니다.');
  if (editMode && inspectionId) loadExistingRecord();

  function previewFiles() {
    previewList.replaceChildren();
    const files = [...(single.files || []), ...(multiple.files || []), ...(photos?.files || [])];
    if (!files.length) { previewList.textContent = '선택한 영상이 없습니다.'; return; }
    files.forEach(file => {
      const item = document.createElement('div'); item.className = 'in-file-item';
      item.textContent = `${file.name} · ${(file.size / 1024 / 1024).toFixed(1)} MB`;
      previewList.append(item);
    });
  }
  single?.addEventListener('change', previewFiles);
  multiple?.addEventListener('change', previewFiles);
  photos?.addEventListener('change', previewFiles);

  function formValue(id) { return document.getElementById(id)?.value?.trim() || ''; }
  async function uploadPhotos(recordNo) {
    const files = [...(photos?.files || [])];
    for (const file of files) {
      const data = new FormData(); data.append('file', file);
      await jsonRequest(`/api/media/INSPECTION/${encodeURIComponent(recordNo)}`, { method: 'POST', body: data });
    }
  }

  form.addEventListener('submit', async event => {
    event.preventDefault();
    if (!bridgeNo.value) { notify('대상 교량을 검색한 후 선택해 주세요.'); return; }
    if (!formValue('in-title')) { notify('점검 기록명을 입력해 주세요.'); return; }
    if (!formValue('in-type')) { notify('점검 유형을 선택해 주세요.'); return; }
    if (!formValue('in-date')) { notify('점검 날짜를 선택해 주세요.'); return; }
    const inspectorNo = formValue('in-inspector-no') || formValue('in-inspector-no-fallback');
    if (!inspectorNo) { notify('점검 담당자를 선택해 주세요.'); return; }
    submit.disabled = true;
    notify('저장 요청을 처리하고 있습니다.', false);
    try {
      if (editMode) {
        if (!inspectionId) throw new Error('수정에 필요한 점검 ID가 Thymeleaf 모델에 연결되지 않았습니다.');
        const payload = {
          bridgeNo: Number(bridgeNo.value), title: formValue('in-title'),
          inspectionType: formValue('in-type'), status: formValue('in-status'),
          inspectionDate: formValue('in-date'), inspectorNo: Number(inspectorNo),
          memo: formValue('in-memo'), overallResult: formValue('in-overall-result'),
          reviewOpinion: formValue('in-review-opinion')
        };
        await jsonRequest(`/api/inspections/${encodeURIComponent(inspectionId)}`, {
          method: 'PUT', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(payload)
        });
        // 수정 API는 JSON 전용이므로 파일은 기존 영상 업로드 API로 별도 추가합니다.
        await uploadPhotos(inspectionNo);
        const files = [...(single.files || []), ...(multiple.files || [])];
        if (files.length) {
          if (!inspectionNo) throw new Error('영상 추가 업로드에는 점검 번호(inspectionNo)의 Thymeleaf 매핑이 필요합니다.');
          const data = new FormData(); files.forEach(file => data.append('files', file));
          await jsonRequest(`/api/inspections/videos/${encodeURIComponent(inspectionNo)}`, { method: 'POST', body: data });
        }
        notify('점검 기록을 수정했습니다.', false);
        location.href = `/inspections?inspectionId=${encodeURIComponent(inspectionId)}`;
      } else {
        const data = new FormData(form);
        data.set('bridgeNo', bridgeNo.value);
        data.set('inspectorNo', inspectorNo);
        // DTO에 명시된 이름을 사용해 그대로 등록 요청합니다.
        const response = await jsonRequest('/api/inspections', { method: 'POST', body: data });
        const id = (await response.text()).replace(/^"|"$/g, '').trim();
        if (!id) throw new Error('저장은 완료됐지만 API 응답에서 점검 ID를 확인하지 못했습니다.');
        const detailResponse = await jsonRequest(`/api/inspections/${encodeURIComponent(id)}`);
        const detail = await detailResponse.json();
        try { await uploadPhotos(detail.no); } catch (uploadError) { notify(`점검 기록은 저장됐지만 사진 업로드에 실패했습니다: ${uploadError.message}`); submit.disabled = false; return; }
        notify('점검 기록을 저장했습니다.', false);
        location.href = `/inspections?inspectionId=${encodeURIComponent(id)}`;
      }
    } catch (error) { notify(error.message || '저장 중 오류가 발생했습니다.'); submit.disabled = false; }
  });
})();
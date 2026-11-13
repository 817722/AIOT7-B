(() => {
  'use strict';
  const query = document.getElementById('inspection-bridge-query');
  const search = document.getElementById('inspection-bridge-search');
  const results = document.getElementById('inspection-bridge-results');
  const selected = document.getElementById('inspection-selected-bridge');
  const message = document.getElementById('inspection-list-message');
  const list = document.getElementById('inspection-record-list');
  const count = document.getElementById('inspection-result-count');
  if (!query || !search || !list) return;

  async function getJson(url) {
    const response = await fetch(url, { credentials: 'same-origin', headers: { Accept: 'application/json' } });
    if (!response.ok) throw new Error(response.status === 403 ? '점검 조회 권한이 없습니다.' : '데이터를 불러오지 못했습니다.');
    return response.json();
  }
  function text(tag, className, value) {
    const node = document.createElement(tag);
    if (className) node.className = className;
    node.textContent = value == null || value === '' ? '-' : String(value);
    return node;
  }
  function showMessage(value, error = false) {
    message.textContent = value;
    message.classList.toggle('is-error', error);
  }
  async function searchBridges() {
    const keyword = query.value.trim();
    if (!keyword) { showMessage('교량명 또는 교량 번호를 입력해 주세요.', true); return; }
    search.disabled = true;
    results.hidden = false;
    results.textContent = '교량을 검색하고 있습니다.';
    try {
      const bridges = await getJson(`/api/maintenance-ui/bridges?keyword=${encodeURIComponent(keyword)}`);
      results.replaceChildren();
      if (!bridges.length) { results.textContent = '검색된 교량이 없습니다.'; return; }
      bridges.forEach(bridge => {
        const button = document.createElement('button');
        button.type = 'button';
        button.className = 'in-bridge-option';
        button.append(text('strong', '', bridge.name));
        button.append(text('small', '', `${bridge.province || ''} ${bridge.city || ''} · ${bridge.facilityType || ''} · #${bridge.no}`));
        button.addEventListener('click', () => loadInspections(bridge));
        results.append(button);
      });
      showMessage(`${bridges.length}개의 교량을 찾았습니다. 조회할 교량을 선택해 주세요.`);
    } catch (error) { results.textContent = error.message; showMessage(error.message, true); }
    finally { search.disabled = false; }
  }
  async function loadInspections(bridge) {
    results.hidden = true;
    selected.hidden = false;
    selected.replaceChildren(text('strong', '', bridge.name),
      text('span', '', `${bridge.province || ''} ${bridge.city || ''} · ${bridge.facilityType || ''} · 교량 번호 ${bridge.no}`));
    showMessage('점검 기록을 조회하고 있습니다.');
    list.replaceChildren();
    try {
      const rows = await getJson(`/api/inspections/bridge/${encodeURIComponent(bridge.no)}`);
      count.textContent = `조회 결과 ${rows.length}건`;
      if (!rows.length) {
        const empty = document.createElement('div'); empty.className = 'bv-blank';
        empty.append(text('strong', '', '등록된 점검 기록이 없습니다.'));
        empty.append(text('p', '', '이 교량의 점검 기록을 새로 등록할 수 있습니다.'));
        list.append(empty);
      } else {
        rows.forEach((row, index) => {
          const card = document.createElement('a');
          card.className = 'in-record-card';
          card.href = `/inspections?inspectionId=${encodeURIComponent(row.id)}`;
          card.addEventListener('click', event => { event.preventDefault(); history.pushState({}, '', card.href); renderDetail(row.id); });
          const head = document.createElement('div'); head.className = 'in-record-card-head';
          head.append(text('strong', '', row.title || '점검 기록'));
          const state = text('em', 'bv-state', row.status || '상태 미지정');
          if (row.status === '완료') state.classList.add('bv-state--done');
          head.append(state);
          card.append(head);
          card.append(text('span', 'in-record-type', row.inspectionType));
          card.append(text('time', 'in-record-date', row.inspectionDate));
          card.append(text('small', 'in-record-summary', row.overallResult || row.memo || '등록된 점검 메모가 없습니다.'));
          list.append(card);
        });
      }
      showMessage(`${bridge.name}의 점검 기록을 조회했습니다.`);
    } catch (error) { showMessage(error.message, true); list.append(text('p', 'bv-help', error.message)); }
  }

  const inline = document.getElementById('in-inline-detail');
  function setDetail(id, value) { const node = document.getElementById(id); if (node) node.textContent = value == null || value === '' ? '-' : String(value); }
  function createMediaLink(media) {
    const a = document.createElement('a'); a.href = `/api/media/file/${encodeURIComponent(media.no)}`; a.target = '_blank'; a.rel = 'noopener';
    return a;
  }
  async function renderDetail(id) {
    if (!id) return;
    inline.hidden = false;
    inline.scrollIntoView({ behavior: 'smooth', block: 'start' });
    setDetail('in-inline-detail-message', '점검 기록을 불러오는 중입니다.');
    const photos = document.getElementById('in-detail-photo-list');
    const videos = document.getElementById('in-detail-video-list');
    photos.replaceChildren(); videos.replaceChildren();
    const player = document.getElementById('in-inline-player');
    player.pause(); player.removeAttribute('src'); player.load(); player.hidden = true;
    document.getElementById('in-inline-player-message').hidden = false;
    try {
      const r = await getJson(`/api/inspections/${encodeURIComponent(id)}`);
      setDetail('in-detail-bridge', `${r.bridgeName || '교량'} (#${r.bridgeNo})`);
      setDetail('in-detail-title', r.title); setDetail('in-detail-type', r.inspectionType);
      setDetail('in-detail-date', r.inspectionDate); setDetail('in-detail-status', r.status);
      setDetail('in-detail-inspector', r.inspectorNo); setDetail('in-detail-overall', r.overallResult);
      setDetail('in-detail-memo', r.memo);
      const [media, videoMedia] = await Promise.all([
        getJson(`/api/inspections/photos/${encodeURIComponent(r.no)}`).catch(() => []),
        getJson(`/api/inspections/videos/${encodeURIComponent(r.no)}`).catch(() => [])
      ]);
      const pictures = media.filter(item => item.type === 'IMAGE' || (item.mimeType || '').startsWith('image/'));
      if (!pictures.length) photos.append(text('p','bv-help','등록된 사진이 없습니다.'));
      pictures.forEach(item => {
        const a = createMediaLink(item); a.className = 'in-photo-card';
        const img = document.createElement('img'); img.src = a.href; img.alt = item.originName || '점검 사진'; img.loading = 'lazy';
        a.append(img, text('span','',item.originName || `사진 #${item.no}`)); photos.append(a);
      });
      if (!videoMedia.length) videos.append(text('li','bv-help','등록된 영상이 없습니다.'));
      videoMedia.forEach(item => {
        const li = document.createElement('li'); const btn = document.createElement('button');
        btn.type = 'button'; btn.className = 'in-video-choice'; btn.textContent = item.originName || `영상 #${item.no}`;
        btn.addEventListener('click', () => {
          player.src = `/api/media/file/${encodeURIComponent(item.no)}`;
          player.hidden = false; document.getElementById('in-inline-player-message').hidden = true;
          player.load();
        });
        li.append(btn); videos.append(li);
      });
      setDetail('in-inline-detail-message','점검 기록을 조회했습니다.');
    } catch(error) { setDetail('in-inline-detail-message', error.message); }
  }
  function handleRoute() {
    const id = new URLSearchParams(location.search).get('inspectionId');
    if (id) renderDetail(id); else inline.hidden = true;
  }
  document.getElementById('in-close-detail')?.addEventListener('click', () => {
    history.pushState({}, '', '/inspections'); inline.hidden = true;
    document.getElementById('inspection-search-title')?.scrollIntoView({behavior:'smooth'});
  });
  window.addEventListener('popstate',handleRoute);
  handleRoute();

  search.addEventListener('click', searchBridges);
  query.addEventListener('keydown', event => { if (event.key === 'Enter') { event.preventDefault(); searchBridges(); } });
})();
(() => {
  'use strict';
  const main = document.getElementById('main-content');
  if (!main) return;
  const params = new URLSearchParams(location.search);
  const id = main.dataset.inspectionId || params.get('id') || '';
  const no = main.dataset.inspectionNo || location.pathname.split('/').filter(Boolean).pop();
  const message = document.getElementById('in-detail-message');
  const photoWrap = document.getElementById('in-detail-photos');
  const videoList = document.getElementById('in-detail-videos');
  const player = document.getElementById('in-detail-player');
  const playerEmpty = document.getElementById('in-player-empty');
  const editLink = document.getElementById('in-detail-edit');
  function setText(id, value) { const node = document.getElementById(id); if (node) node.textContent = value == null || value === '' ? '-' : String(value); }
  async function getJson(url) {
    const response = await fetch(url, { credentials: 'same-origin', headers: { Accept: 'application/json' } });
    if (!response.ok) throw new Error(`조회 실패 (${response.status})`);
    return response.json();
  }
  function mediaUrl(item) { return `/api/media/file/${encodeURIComponent(item.no)}`; }
  async function load() {
    if (!id) {
      message.textContent = '점검 UUID가 연결되지 않았습니다. 상세 페이지 컨트롤러에서 inspectionId를 Thymeleaf 모델로 전달해야 합니다.';
      message.classList.add('is-error'); return;
    }
    try {
      const record = await getJson(`/api/inspections/${encodeURIComponent(id)}`);
      setText('detail-bridge-name', record.bridgeName);
      setText('detail-bridge-no', record.bridgeNo);
      setText('detail-title', record.title);
      setText('detail-type', record.inspectionType);
      setText('detail-date', record.inspectionDate);
      setText('detail-status', record.status);
      setText('detail-inspector', record.inspectorNo);
      setText('detail-overall-result', record.overallResult);
      setText('detail-memo', record.memo);
      if (editLink) editLink.href = `/inspections?inspectionId=${encodeURIComponent(id)}`;
      message.textContent = '점검 상세 정보를 불러왔습니다.';
      const [photos, videos] = await Promise.all([
        getJson(`/api/inspections/photos/${encodeURIComponent(record.no)}`).catch(() => []),
        getJson(`/api/inspections/videos/${encodeURIComponent(record.no)}`).catch(() => [])
      ]);
      photoWrap.replaceChildren();
      if (!photos.length) photoWrap.textContent = '등록된 증거 사진이 없습니다.';
      photos.filter(photo => photo.type === 'IMAGE' || (photo.mimeType || '').startsWith('image/')).forEach(photo => {
        const link = document.createElement('a'); link.className = 'in-photo-card'; link.href = mediaUrl(photo); link.target = '_blank'; link.rel = 'noopener';
        const img = document.createElement('img'); img.src = mediaUrl(photo); img.alt = photo.originName || '점검 증거 사진';
        const caption = document.createElement('span'); caption.textContent = photo.originName || `사진 #${photo.no}`;
        link.append(img, caption); photoWrap.append(link);
      });
      videoList.replaceChildren();
      if (!videos.length) videoList.append(Object.assign(document.createElement('li'), { textContent: '등록된 영상이 없습니다.' }));
      videos.forEach(video => {
        const li = document.createElement('li');
        const button = document.createElement('button'); button.type = 'button'; button.className = 'in-video-choice';
        button.textContent = video.originName || `영상 #${video.no}`;
        button.addEventListener('click', () => {
          player.src = mediaUrl(video); player.hidden = false; playerEmpty.hidden = true;
          player.load(); player.play().catch(() => {});
        });
        li.append(button); videoList.append(li);
      });
    } catch (error) {
      message.textContent = `${error.message}. 점검 페이지 URL의 id 파라미터와 API 응답을 확인해 주세요.`;
      message.classList.add('is-error');
    }
  }
  load();
})();
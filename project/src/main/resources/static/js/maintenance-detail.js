/* 유지보수 상세 v5: Figma형 영상 UI 유지 + 기존 메모 부가정보 복원 오류 보정 */
(() => {
  'use strict';
  const memo = document.getElementById('mv-stored-memo');
  const marker = '[유지보수 화면 부가정보 v1]';
  if (memo) {
    const raw = (memo.textContent || '').replace(/\r\n?/g, '\n');
    const index = raw.lastIndexOf(marker);
    let notes = raw;
    let extras = null;
    if (index >= 0 && (index === 0 || raw[index - 1] === '\n')) {
      try {
        const obj = JSON.parse(raw.slice(index + marker.length).trim());
        if (obj && typeof obj === 'object' && !Array.isArray(obj) &&
            ['recordCategory', 'safetyEducation', 'educationMemo'].some(k =>
              Object.prototype.hasOwnProperty.call(obj, k))) {
          extras = obj;
          notes = raw.slice(0, index).replace(/\n+$/, '');
        }
      } catch (_) { /* 손상된 메모는 원문을 그대로 보존합니다. */ }
    }
    const set = (id, value) => {
      const element = document.getElementById(id);
      if (element) element.textContent = value;
    };
    set('mv-field-notes', notes.trim() || '기록된 현장 일지가 없습니다.');
    if (extras) {
      set('mv-field-category', extras.recordCategory || '미기록');
      set('mv-field-education', extras.safetyEducation || '미기록');
      set('mv-field-education-memo', extras.educationMemo || '등록된 교육 메모 없음');
    }
  }

  const panel = document.getElementById('mv-detail-video-panel');
  if (!panel) return;
  const maintenanceNo = Number(panel.dataset.maintenanceNo);
  const canEdit = panel.dataset.canEdit === 'true';
  const playerPanel = document.getElementById('mv-video-player-panel');
  const player = document.getElementById('mv-video-player');
  const placeholder = document.getElementById('mv-detail-video-placeholder');
  const message = document.getElementById('mv-video-detail-message');
  const list = document.getElementById('mv-detail-video-list');
  const confirmPanel = document.getElementById('mv-detail-delete-confirm');
  const cancelDelete = document.getElementById('mv-detail-delete-cancel');
  const acceptDelete = document.getElementById('mv-detail-delete-accept');
  const token = document.querySelector('meta[name="_csrf"]')?.content;
  const header = document.querySelector('meta[name="_csrf_header"]')?.content;
  let deleteVideoNo = null;

  function show(text, isError = false) {
    if (!message) return;
    message.textContent = text;
    message.classList.toggle('is-error', isError);
    message.hidden = false;
  }
  function streamUrl(no) {
    return `/api/maintenance-ui/records/${maintenanceNo}/videos/${no}/stream`;
  }
  function hideConfirmation() {
    deleteVideoNo = null;
    if (confirmPanel) confirmPanel.hidden = true;
  }
  cancelDelete?.addEventListener('click', hideConfirmation);

  list?.addEventListener('click', event => {
    const play = event.target.closest('.mv-play-video');
    if (play) {
      const no = Number(play.dataset.videoNo);
      if (!Number.isSafeInteger(no) || no < 1) return;
      player.src = streamUrl(no);
      player.dataset.videoNo = String(no);
      playerPanel.hidden = false;
      if (placeholder) placeholder.hidden = true;
      list.querySelectorAll('.mv-play-video').forEach(button => {
        button.classList.toggle('is-selected', button === play);
      });
      player.load();
      return;
    }
    const remove = event.target.closest('.mv-remove-video');
    if (!remove || !canEdit || !confirmPanel) return;
    const no = Number(remove.dataset.videoNo);
    if (!Number.isSafeInteger(no) || no < 1) return;
    deleteVideoNo = no;
    const title = remove.closest('li')?.querySelector('strong')?.textContent || '선택한 영상';
    document.getElementById('mv-detail-delete-filename').textContent = `${title} · 삭제 후 복구할 수 없습니다.`;
    confirmPanel.hidden = false;
    confirmPanel.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
  });

  // 기업 관리자만 사용하는 유지보수 기록 전체 삭제 UI (영상 한 건 삭제와 별개).
  const recordOpen = document.getElementById('mv-record-delete-open');
  const recordConfirm = document.getElementById('mv-record-delete-confirm');
  const recordCancel = document.getElementById('mv-record-delete-cancel');
  const recordAccept = document.getElementById('mv-record-delete-accept');
  const recordMessage = document.getElementById('mv-record-delete-message');
  const showRecordMessage = (content, error = false) => {
    if (!recordMessage) return;
    recordMessage.textContent = content;
    recordMessage.classList.toggle('is-error', error);
    recordMessage.hidden = false;
  };
  recordOpen?.addEventListener('click', () => {
    if (!recordConfirm) return;
    recordConfirm.hidden = false;
    recordOpen.setAttribute('aria-expanded', 'true');
    recordAccept?.focus();
  });
  recordCancel?.addEventListener('click', () => {
    if (recordConfirm) recordConfirm.hidden = true;
    recordOpen?.setAttribute('aria-expanded', 'false');
    recordOpen?.focus();
  });
  recordAccept?.addEventListener('click', async () => {
    if (!recordConfirm || recordConfirm.hidden || recordAccept.disabled ||
        !Number.isSafeInteger(maintenanceNo) || maintenanceNo < 1) return;
    recordAccept.disabled = true;
    recordCancel.disabled = true;
    const headers = { Accept: 'application/json' };
    if (header && token) headers[header] = token;
    try {
      const response = await fetch(`/api/maintenance-ui/records/${maintenanceNo}`, {
        method: 'DELETE', credentials: 'same-origin', headers
      });
      if (!response.ok) {
        let errorMessage = '';
        try {
          const body = await response.json();
          errorMessage = body.message || body.detail || '';
        } catch (_) { /* 응답 본문이 없는 경우 HTTP 코드 표시 */ }
        throw new Error(errorMessage || `유지보수 기록 삭제 실패 (HTTP ${response.status})`);
      }
      window.location.assign('/maintenances?deleted=1');
    } catch (error) {
      showRecordMessage(error.message || '삭제에 실패했습니다.', true);
      recordAccept.disabled = false;
      recordCancel.disabled = false;
    }
  });

  acceptDelete?.addEventListener('click', async () => {
    if (!canEdit || !deleteVideoNo) return;
    const no = deleteVideoNo;
    acceptDelete.disabled = true;
    const headers = { Accept: 'application/json' };
    if (header && token) headers[header] = token;
    try {
      const response = await fetch(`/api/maintenance-ui/records/${maintenanceNo}/videos/${no}`, {
        method: 'DELETE', credentials: 'same-origin', headers
      });
      if (!response.ok) {
        let error = '';
        try { const data = await response.json(); error = data.message || data.detail || ''; } catch (_) {}
        throw new Error(error || `삭제 실패 (HTTP ${response.status})`);
      }
      if (player.dataset.videoNo === String(no)) {
        player.pause(); player.removeAttribute('src'); player.load();
        delete player.dataset.videoNo;
        playerPanel.hidden = true;
        if (placeholder) placeholder.hidden = false;
      }
      list?.querySelector(`li[data-video-no="${no}"]`)?.remove();
      if (list && !list.children.length) {
        list.hidden = true;
        const empty = document.getElementById('mv-detail-video-empty') || document.createElement('p');
        empty.id = 'mv-detail-video-empty';
        empty.className = 'bv-help';
        empty.textContent = '연결된 영상이 없습니다.';
        list.before(empty);
      }
      hideConfirmation();
      show('영상 삭제를 완료했습니다. 유지보수 기록은 그대로 유지됩니다.');
    } catch (error) { show(error.message, true); }
    finally { acceptDelete.disabled = false; }
  });

  // 증거 사진: 상세 조회, 확대, 삭제 확인 (기존 영상·기록 삭제와 독립).
  const photoGallery = document.getElementById('mv-detail-photo-gallery');
  const photoConfirm = document.getElementById('mv-detail-photo-confirm');
  const photoCancel = document.getElementById('mv-detail-photo-cancel');
  const photoAccept = document.getElementById('mv-detail-photo-accept');
  const photoMessage = document.getElementById('mv-detail-photo-message');
  let pendingPhotoNo = null;
  const photoEndpoint = `/api/maintenance-ui/records/${maintenanceNo}/photos`;
  function photoNotice(text, error = false) {
    if (!photoMessage) return;
    photoMessage.textContent = text;
    photoMessage.hidden = false;
    photoMessage.classList.toggle('is-error', error);
  }
  async function loadPhotos() {
    if (!photoGallery) return;
    try {
      const res = await fetch(photoEndpoint, { credentials: 'same-origin', headers: {Accept:'application/json'} });
      if (!res.ok) throw new Error(`사진 조회 실패 (HTTP ${res.status})`);
      const photos = (await res.json()).filter(p => p.type === 'IMAGE');
      photoGallery.replaceChildren();
      if (!photos.length) {
        const empty = document.createElement('p');
        empty.className = 'bv-help';
        empty.textContent = '등록된 증거 사진이 없습니다.';
        photoGallery.append(empty);
      }
      for (const photo of photos) {
        const card = document.createElement('div');
        card.className = 'mv-evidence-card';
        const link = document.createElement('a');
        link.href = `/api/media/file/${encodeURIComponent(photo.no)}`;
        link.target = '_blank';
        link.rel = 'noopener noreferrer';
        link.title = '새 창에서 사진 크게 보기';
        const image = document.createElement('img');
        image.src = link.href;
        image.loading = 'lazy';
        image.alt = photo.originName || '증거 사진';
        link.append(image);
        const name = document.createElement('span');
        name.textContent = photo.originName || `증거 사진 #${photo.no}`;
        card.append(link, name);
        if (canEdit && photoConfirm) {
          const btn = document.createElement('button');
          btn.type = 'button';
          btn.className = 'bv-btn bv-btn-light mv-evidence-remove';
          btn.textContent = '사진 삭제';
          btn.addEventListener('click', () => {
            pendingPhotoNo = photo.no;
            document.getElementById('mv-detail-photo-filename').textContent =
              `${photo.originName || '선택한 사진'} · 삭제 후 복구할 수 없습니다.`;
            photoConfirm.hidden = false;
          });
          card.append(btn);
        }
        photoGallery.append(card);
      }
    } catch (error) {
      photoGallery.textContent = error.message || '증거 사진을 표시하지 못했습니다.';
    }
  }
  photoCancel?.addEventListener('click', () => {
    pendingPhotoNo = null;
    photoConfirm.hidden = true;
  });
  photoAccept?.addEventListener('click', async () => {
    if (!canEdit || !pendingPhotoNo) return;
    photoAccept.disabled = true;
    const headers = { Accept: 'application/json' };
    if (header && token) headers[header] = token;
    try {
      const response = await fetch(`${photoEndpoint}/${encodeURIComponent(pendingPhotoNo)}`, {
        method: 'DELETE', credentials: 'same-origin', headers
      });
      if (!response.ok) {
        let reason = '';
        try { const body = await response.json(); reason = body.message || body.detail || ''; } catch (_) {}
        throw new Error(reason || `사진 삭제 실패 (HTTP ${response.status})`);
      }
      pendingPhotoNo = null;
      photoConfirm.hidden = true;
      photoNotice('증거 사진을 삭제했습니다.');
      await loadPhotos();
    } catch (error) { photoNotice(error.message, true); }
    finally { photoAccept.disabled = false; }
  });
  loadPhotos();

})();

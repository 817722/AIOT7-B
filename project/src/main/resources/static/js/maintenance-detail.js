/* 유지보수 상세 v4: Figma형 좌우 영상 미리보기, 삭제 확인, 메모 부가정보 표시 */
(() => {
  'use strict';
  const memo = document.getElementById('mv-stored-memo');
  const marker = '\n\n[유지보수 화면 부가정보 v1]\n';
  if (memo) {
    const raw = memo.textContent || '';
    const index = raw.lastIndexOf(marker);
    let notes = raw;
    let extras = null;
    if (index >= 0) {
      try {
        const obj = JSON.parse(raw.slice(index + marker.length));
        if (obj && typeof obj === 'object' && !Array.isArray(obj)) {
          extras = obj;
          notes = raw.slice(0, index);
        }
      } catch (_) { /* 이전 버전의 일반 메모는 변경하지 않습니다. */ }
    }
    const set = (id, text) => {
      const element = document.getElementById(id);
      if (element) element.textContent = text;
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
})();

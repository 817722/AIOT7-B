/* 유지보수 신규 등록·수정 — maintenance-ui 보안 API 전용 */
(() => {
  'use strict';
  const main = document.getElementById('main-content');
  const form = document.getElementById('mv-maintenance-form');
  if (!main || !form) return;
  const editMode = main.dataset.editMode === 'true';
  const recordNo = Number(main.dataset.recordNo);
  const bridgeNoInput = document.getElementById('mv-bridge-no');
  const query = document.getElementById('mv-bridge-query');
  const results = document.getElementById('mv-bridge-results');
  const messageBox = document.getElementById('mv-form-message');
  const submit = document.getElementById('mv-save');
  const videoInput = document.getElementById('mv-video-file');
  const videoPreview = document.getElementById('mv-video-preview');
  const videoElement = document.getElementById('mv-video-local');
  let videoObjectUrl = null;
  let selectedVideoFile = null;
  let pendingDeleteVideoNo = null;
  const DROP_ZONE = document.getElementById('mv-video-dropzone');
  const VIDEO_IDLE = document.getElementById('mv-video-idle');
  const UPLOAD_WRAP = document.getElementById('mv-upload-progress-wrap');
  const UPLOAD_BAR = document.getElementById('mv-upload-progress');
  const UPLOAD_STATUS = document.getElementById('mv-upload-status');
  const DELETE_CONFIRM = document.getElementById('mv-form-delete-confirm');
  // 기록 분류/안전 교육/교육 메모는 DB memo의 구분된 마지막 블록에 저장합니다.
  // v4는 빈 메모에 두 개의 선행 줄바꿈을 붙였으나, <textarea>는 첫 줄바꿈을
  // 제거할 수 있습니다. 구 버전(0/1/2개 줄바꿈) 모두 읽어 데이터 노출을 막습니다.
  const EXTRA_TAG = '[유지보수 화면 부가정보 v1]';
  const DEFAULT_CATEGORY = '사후 보수 기록';
  const DEFAULT_SAFETY = '미기록';

  function readMemo(value) {
    const raw = String(value ?? '').replace(/\r\n?/g, '\n');
    const idx = raw.lastIndexOf(EXTRA_TAG);
    if (idx < 0) return { notes: raw, extras: null };
    // 일반 사용자가 본문에 우연히 같은 문자열을 쓴 경우를 보호합니다.
    if (idx > 0 && raw[idx - 1] !== '\n') return { notes: raw, extras: null };
    try {
      const extras = JSON.parse(raw.slice(idx + EXTRA_TAG.length).trim());
      if (!extras || typeof extras !== 'object' || Array.isArray(extras) ||
          !['recordCategory', 'safetyEducation', 'educationMemo'].some(k =>
            Object.prototype.hasOwnProperty.call(extras, k))) {
        throw new Error('부가정보 형식이 아닙니다.');
      }
      return { notes: raw.slice(0, idx).replace(/\n+$/, ''), extras };
    } catch (_) {
      // 손상된 메모를 임의로 지우거나 변형하지 않습니다.
      return { notes: raw, extras: null };
    }
  }

  const memoElement = document.getElementById('mv-memo');
  if (memoElement && editMode) {
    const parsed = readMemo(memoElement.value);
    memoElement.value = parsed.notes;
    if (parsed.extras) {
      const category = document.getElementById('mv-category');
      const safety = document.getElementById('mv-safety');
      const eduMemo = document.getElementById('mv-education-memo');
      if (category && [...category.options].some(o => o.value === parsed.extras.recordCategory)) category.value = parsed.extras.recordCategory;
      if (safety && [...safety.options].some(o => o.value === parsed.extras.safetyEducation)) safety.value = parsed.extras.safetyEducation;
      if (eduMemo) eduMemo.value = typeof parsed.extras.educationMemo === 'string' ? parsed.extras.educationMemo : '';
    }
  }

  function packMemo() {
    const notes = memoElement?.value.trim() || '';
    const extras = {
      recordCategory: document.getElementById('mv-category')?.value || DEFAULT_CATEGORY,
      safetyEducation: document.getElementById('mv-safety')?.value || DEFAULT_SAFETY,
      educationMemo: document.getElementById('mv-education-memo')?.value.trim() || ''
    };
    // 사용자가 현장 일지/교육 정보에 아무것도 기록하지 않았다면 빈 메모로 저장.
    // 기본 선택값만으로 '부가정보' JSON이 생기는 버그를 방지합니다.
    if (!notes && extras.recordCategory === DEFAULT_CATEGORY &&
        extras.safetyEducation === DEFAULT_SAFETY && !extras.educationMemo) return '';
    const prefix = notes ? `${notes}\n\n` : '';
    return `${prefix}${EXTRA_TAG}\n${JSON.stringify(extras)}`;
  }
  const reference = document.getElementById('mv-inspection-list');
  const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content;
  const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;

  function showMessage(text, error = true) {
    if (!messageBox) return;
    messageBox.textContent = text;
    messageBox.hidden = false;
    messageBox.classList.toggle('is-error', error);
  }

  async function request(path, method = 'GET', body) {
    const headers = { Accept: 'application/json' };
    if (body !== undefined) headers['Content-Type'] = 'application/json';
    if (csrfHeader && csrfToken && method !== 'GET') headers[csrfHeader] = csrfToken;
    const response = await fetch(path, {
      method, credentials: 'same-origin', headers,
      ...(body === undefined ? {} : { body: JSON.stringify(body) })
    });
    if (!response.ok) {
      let msg = '';
      try {
        const err = await response.json();
        msg = err.message || err.detail || err.error || '';
      } catch (_) {}
      throw new Error(msg || `요청 실패 (HTTP ${response.status})`);
    }
    return response.status === 204 ? null : response.json();
  }

  function bytesLabel(bytes) {
    return bytes > 1024 * 1024 ? `${(bytes / (1024 * 1024)).toFixed(1)}MB` : `${Math.ceil(bytes / 1024)}KB`;
  }

  function previewVideo(file) {
    if (videoObjectUrl) URL.revokeObjectURL(videoObjectUrl);
    videoObjectUrl = null;
    selectedVideoFile = null;
    if (videoPreview) videoPreview.hidden = true;
    if (!file) {
      videoElement?.removeAttribute('src');
      if (VIDEO_IDLE) VIDEO_IDLE.hidden = false;
      return;
    }
    if (!/\.(mp4|webm)$/i.test(file.name) || file.size > 200 * 1024 * 1024 || file.size === 0) {
      showMessage('MP4 또는 WebM 영상만 선택할 수 있으며 최대 200MB입니다.');
      if (videoInput) videoInput.value = '';
      if (VIDEO_IDLE) VIDEO_IDLE.hidden = false;
      return;
    }
    selectedVideoFile = file;
    document.getElementById('mv-video-filename').textContent = `${file.name} · ${bytesLabel(file.size)}`;
    videoObjectUrl = URL.createObjectURL(file);
    videoElement.src = videoObjectUrl;
    videoPreview.hidden = false;
    if (VIDEO_IDLE) VIDEO_IDLE.hidden = true;
    if (UPLOAD_WRAP) UPLOAD_WRAP.hidden = false;
    if (UPLOAD_BAR) UPLOAD_BAR.value = 0;
    if (UPLOAD_STATUS) UPLOAD_STATUS.textContent = `선택됨: ${file.name} (저장 시 업로드)`;
  }
  videoInput?.addEventListener('change', () => previewVideo(videoInput.files?.[0]));
  if (DROP_ZONE) {
    for (const eventName of ['dragenter', 'dragover']) DROP_ZONE.addEventListener(eventName, event => {
      event.preventDefault(); DROP_ZONE.classList.add('is-dragover');
    });
    DROP_ZONE.addEventListener('dragleave', event => {
      event.preventDefault(); DROP_ZONE.classList.remove('is-dragover');
    });
    DROP_ZONE.addEventListener('drop', event => {
      event.preventDefault(); DROP_ZONE.classList.remove('is-dragover');
      previewVideo(event.dataTransfer?.files?.[0]);
    });
  }
  window.addEventListener('beforeunload', () => {
    if (videoObjectUrl) URL.revokeObjectURL(videoObjectUrl);
    const connected = document.getElementById('mv-edit-video-player');
    if (connected) { connected.pause(); connected.removeAttribute('src'); }
  });

  // XHR은 fetch와 달리 업로드 진행률(upload progress)을 표시할 수 있습니다.
  function uploadVideo(no) {
    if (!selectedVideoFile) return Promise.resolve();
    const data = new FormData();
    data.append('file', selectedVideoFile);
    if (UPLOAD_WRAP) UPLOAD_WRAP.hidden = false;
    if (UPLOAD_STATUS) UPLOAD_STATUS.textContent = '영상 업로드 중 · 0%';
    return new Promise((resolve, reject) => {
      const xhr = new XMLHttpRequest();
      xhr.open('POST', `/api/maintenance-ui/records/${no}/videos`);
      xhr.withCredentials = true;
      xhr.setRequestHeader('Accept', 'application/json');
      if (csrfHeader && csrfToken) xhr.setRequestHeader(csrfHeader, csrfToken);
      xhr.upload.onprogress = event => {
        if (!event.lengthComputable) return;
        const percent = Math.min(100, Math.round(event.loaded * 100 / event.total));
        if (UPLOAD_BAR) UPLOAD_BAR.value = percent;
        if (UPLOAD_STATUS) UPLOAD_STATUS.textContent = `영상 전송 중 · ${percent}%`;
      };
      xhr.onload = () => {
        if (xhr.status >= 200 && xhr.status < 300) {
          if (UPLOAD_BAR) UPLOAD_BAR.value = 100;
          if (UPLOAD_STATUS) UPLOAD_STATUS.textContent = '업로드 완료';
          resolve();
        } else {
          let text = '';
          try { const obj = JSON.parse(xhr.responseText); text = obj.message || obj.detail || ''; } catch (_) {}
          reject(new Error(text || `영상 업로드 실패 (HTTP ${xhr.status})`));
        }
      };
      xhr.onerror = () => reject(new Error('영상 전송 중 네트워크 오류가 발생했습니다.'));
      xhr.onabort = () => reject(new Error('영상 업로드가 취소되었습니다.'));
      xhr.send(data);
    });
  }

  const deleteCancel = document.getElementById('mv-cancel-video-delete');
  const deleteAccept = document.getElementById('mv-confirm-video-delete');
  deleteCancel?.addEventListener('click', () => { pendingDeleteVideoNo = null; DELETE_CONFIRM.hidden = true; });
  deleteAccept?.addEventListener('click', async () => {
    if (!pendingDeleteVideoNo) return;
    deleteAccept.disabled = true;
    try {
      await request(`/api/maintenance-ui/records/${recordNo}/videos/${pendingDeleteVideoNo}`, 'DELETE');
      const connected = document.getElementById('mv-edit-video-player');
      if (connected?.dataset.videoNo === String(pendingDeleteVideoNo)) {
        connected.pause(); connected.removeAttribute('src'); connected.load();
        document.getElementById('mv-edit-video-player-panel').hidden = true;
        if (!selectedVideoFile && VIDEO_IDLE) VIDEO_IDLE.hidden = false;
      }
      showMessage('영상을 삭제했습니다.', false);
      pendingDeleteVideoNo = null;
      DELETE_CONFIRM.hidden = true;
      await loadEditVideos();
    } catch (error) { showMessage(error.message); }
    finally { deleteAccept.disabled = false; }
  });

  async function loadEditVideos() {
    const list = document.getElementById('mv-edit-video-list');
    if (!editMode || !list) return;
    try {
      const videos = await request(`/api/maintenance-ui/records/${recordNo}/videos`);
      list.replaceChildren();
      if (!videos.length) {
        const empty = document.createElement('li');
        empty.textContent = '연결된 영상이 없습니다.';
        list.appendChild(empty);
      }
      for (const video of videos) {
        const li = document.createElement('li');
        const view = document.createElement('button');
        view.className = 'mv-figma-video-item';
        view.type = 'button';
        const title = document.createElement('strong');
        title.textContent = video.name;
        const desc = document.createElement('small');
        desc.textContent = `${bytesLabel(video.size || 0)} · 클릭하여 재생`;
        view.append(title, desc);
        view.addEventListener('click', () => {
          const player = document.getElementById('mv-edit-video-player');
          const playerPanel = document.getElementById('mv-edit-video-player-panel');
          if (!player || !playerPanel) return;
          player.src = `/api/maintenance-ui/records/${recordNo}/videos/${video.no}/stream`;
          player.dataset.videoNo = String(video.no);
          playerPanel.hidden = false;
          if (VIDEO_IDLE) VIDEO_IDLE.hidden = true;
          player.load();
        });
        const remove = document.createElement('button');
        remove.type = 'button';
        remove.className = 'bv-btn bv-btn-danger';
        remove.textContent = '영상 삭제';
        remove.addEventListener('click', () => {
          pendingDeleteVideoNo = video.no;
          document.getElementById('mv-form-delete-file').textContent = `${video.name} · 삭제 후 복구할 수 없습니다.`;
          DELETE_CONFIRM.hidden = false;
          DELETE_CONFIRM.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
        });
        li.append(view, remove);
        list.appendChild(li);
      }
    } catch (error) { list.textContent = `영상 목록을 불러오지 못했습니다. ${error.message}`; }
  }

  loadEditVideos();
  if (new URLSearchParams(window.location.search).get('videoUploadFailed') === '1') {
    showMessage('유지보수 기록은 저장되었지만 영상 업로드에 실패했습니다. 파일을 다시 선택하고 변경사항을 저장해 주세요.');
  }

  function setBridge(bridge) {
    bridgeNoInput.value = String(bridge.no);
    document.getElementById('mv-selected-name').textContent = bridge.name || '교량';
    document.getElementById('mv-selected-location').textContent =
      [bridge.province, bridge.city].filter(Boolean).join(' ') || '위치 정보 없음';
    document.getElementById('mv-selected-number').textContent = `업무용 등록번호 #${bridge.no}`;
    loadInspections(bridge.no);
  }

  async function loadInspections(bridgeNo) {
    if (!reference || !Number.isSafeInteger(Number(bridgeNo))) return;
    reference.replaceChildren();
    const loading = document.createElement('li');
    loading.textContent = '점검 기록을 조회하고 있습니다.';
    reference.appendChild(loading);
    try {
      const data = await request(`/api/maintenance-ui/inspections?bridgeNo=${encodeURIComponent(bridgeNo)}`);
      reference.replaceChildren();
      if (!data.length) {
        const empty = document.createElement('li');
        empty.textContent = '연결할 참고 점검 기록이 없습니다.';
        reference.appendChild(empty);
      } else {
        for (const item of data) {
          const li = document.createElement('li');
          li.textContent = `#${item.no} · ${item.title} · ${item.date || '일자 미정'} · ${item.status}`;
          reference.appendChild(li);
        }
      }
    } catch (error) {
      reference.replaceChildren();
      const li = document.createElement('li');
      li.textContent = `점검 참고 정보를 불러오지 못했습니다. ${error.message}`;
      reference.appendChild(li);
    }
  }

  async function findBridges() {
    if (!results || !query) return;
    const button = document.getElementById('mv-search-bridge');
    button.disabled = true;
    results.textContent = '교량을 검색하고 있습니다.';
    try {
      const bridges = await request(`/api/maintenance-ui/bridges?keyword=${encodeURIComponent(query.value.trim())}`);
      results.replaceChildren();
      if (!bridges.length) {
        const notice = document.createElement('p');
        notice.className = 'bv-help';
        notice.textContent = '등록된 교량이 없습니다. 먼저 교량 관리에서 등록해 주세요.';
        results.appendChild(notice);
      }
      for (const bridge of bridges) {
        const entry = document.createElement('button');
        entry.type = 'button';
        entry.className = 'mv-bridge-choice';
        const name = document.createElement('strong');
        name.textContent = `${bridge.name || '교량'} (#${bridge.no})`;
        const sub = document.createElement('span');
        sub.textContent = [bridge.province, bridge.city, bridge.facilityType].filter(Boolean).join(' · ');
        entry.append(name, sub);
        entry.addEventListener('click', () => {
          setBridge(bridge);
          results.querySelectorAll('.mv-bridge-choice').forEach(button => button.classList.remove('is-selected'));
          entry.classList.add('is-selected');
        });
        results.appendChild(entry);
      }
    } catch (error) {
      results.textContent = `교량 검색 실패: ${error.message}`;
    } finally {
      button.disabled = false;
    }
  }
  document.getElementById('mv-search-bridge')?.addEventListener('click', findBridges);
  query?.addEventListener('keydown', (event) => {
    if (event.key === 'Enter') { event.preventDefault(); findBridges(); }
  });
  // 새 기록의 초기 교량 후보를 제공하고, 기존 기록은 읽기 전용으로 표시한다.
  if (!editMode && !bridgeNoInput.value) findBridges();
  if (bridgeNoInput.value && !editMode) loadInspections(bridgeNoInput.value);

  form.addEventListener('submit', async event => {
    event.preventDefault();
    if (!form.checkValidity()) { form.reportValidity(); return; }
    const bridgeNo = Number(bridgeNoInput.value);
    const managerNo = Number(document.getElementById('mv-manager').value);
    if (!Number.isSafeInteger(bridgeNo) || bridgeNo <= 0) {
      showMessage('대상 교량을 검색하고 선택해 주세요.'); return;
    }
    if (!Number.isSafeInteger(managerNo) || managerNo <= 0) {
      showMessage('담당자를 선택해 주세요.'); return;
    }
    const compiledMemo = packMemo();
    if (compiledMemo.length > 20000) { showMessage('현장 일지와 추가정보는 합쳐서 20,000자 이하여야 합니다.'); return; }
    const payload = {
      bridgeNo,
      title: document.getElementById('mv-title').value.trim(),
      maintenanceType: document.getElementById('mv-type').value,
      maintenanceDate: document.getElementById('mv-date').value,
      managerNo,
      status: document.getElementById('mv-status').value,
      memo: compiledMemo,
      result: document.getElementById('mv-result').value.trim()
    };
    submit.disabled = true;
    showMessage('유지보수 기록을 저장하는 중입니다.', false);
    try {
      const path = editMode ? `/api/maintenance-ui/records/${recordNo}` : '/api/maintenance-ui/records';
      if (editMode && (!Number.isSafeInteger(recordNo) || recordNo <= 0)) {
        throw new Error('수정할 유지보수 번호가 올바르지 않습니다.');
      }
      const saved = await request(path, editMode ? 'PUT' : 'POST', payload);
      try {
        await uploadVideo(saved.no);
      } catch (videoError) {
        // 기록 등록은 성공했습니다. 재등록 시 중복 생성되지 않도록 수정 화면으로 이동합니다.
        window.location.assign(`/maintenances/${encodeURIComponent(saved.no)}/edit?videoUploadFailed=1`);
        return;
      }
      window.location.assign(`/maintenances/${encodeURIComponent(saved.no)}`);
    } catch (error) {
      showMessage(`저장 실패: ${error.message}`);
      submit.disabled = false;
    }
  });
})();

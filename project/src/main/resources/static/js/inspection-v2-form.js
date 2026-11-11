/* 교량 점검 등록·수정 전용 스크립트 (현재 담당자 자동 출력 및 백엔드 DTO 완벽 호환) */
(() => {
  'use strict';

  document.addEventListener('DOMContentLoaded', () => {
    const form = document.getElementById('inspectionForm') || document.querySelector('form');
    const checklistTable = document.getElementById('checklist-table');
    const bridgeSelect = document.getElementById('mv-selected-bridge');
    const bridgeNoHidden = document.getElementById('mv-bridge-no');
    const inspectionList = document.getElementById('mv-inspection-list');
    const messageBox = document.getElementById('mv-form-message');
    const managerField = document.getElementById('mv-manager') || document.getElementById('bv-inspector');

    // 상세 검색 입력창 자동 탐색
    let queryInput = document.getElementById('mv-bridge-query');
    if (!queryInput && bridgeSelect) {
      const parentGrid = bridgeSelect.closest('.bv-filter-grid') || document;
      queryInput = parentGrid.querySelector('input[type="search"], input[placeholder*="교량"], input[placeholder*="검색"]');
    }
    const searchBtn = document.getElementById('mv-search-bridge');

    let cachedBridges = [];

    const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content;
    const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;

    function showMessage(text, isError = true) {
      if (messageBox) {
        messageBox.textContent = text;
        messageBox.hidden = false;
        messageBox.classList.toggle('is-error', isError);
      } else {
        alert(text);
      }
    }

    async function requestJson(path) {
      const response = await fetch(path, {
        method: 'GET',
        credentials: 'same-origin',
        headers: { Accept: 'application/json' }
      });
      if (!response.ok) {
        throw new Error(`HTTP ${response.status}`);
      }
      return response.json();
    }

    // -----------------------------------------------------------------------
    // 1. 현재 로그인한 담당자 자동 출력 (기존 API 목록 조회 제거)
    // -----------------------------------------------------------------------
async function initCurrentInspector() {
  if (!managerField) return;

  // 1. HTML(Thymeleaf)에서 이미 선택된 담당자 값이 있으면 그대로 유지
  if (managerField.value && managerField.value.trim() !== '') {
    return;
  }

  // 2. 서버의 현재 로그인 사용자 정보 API 호출 시도
  const meEndpoints = ['/api/auth/me', '/api/members/me', '/api/users/me', '/api/me'];
  let currentUserName = '';

  for (const url of meEndpoints) {
    try {
      const data = await requestJson(url);
      currentUserName = data?.name || data?.userName || data?.username || data?.memberName || '';
      if (currentUserName) break;
    } catch (_) {
      // 다음 경로 시도
    }
  }

  // 3. 이름을 찾았다면 select 옵션에 추가 후 자동 선택
  if (currentUserName) {
    if (managerField.tagName === 'SELECT') {
      const option = document.createElement('option');
      option.value = currentUserName;
      option.textContent = currentUserName;
      option.selected = true;
      managerField.appendChild(option);
    } else {
      managerField.value = currentUserName;
    }
  }
}

    // -----------------------------------------------------------------------
    // 2. 교량 셀렉트 박스 옵션 렌더링 및 상세 검색 실시간 필터링
    // -----------------------------------------------------------------------
    function renderBridgeOptions(bridges, keyword = '') {
      if (!bridgeSelect) return;
      bridgeSelect.innerHTML = '';

      const kw = keyword.trim().toLowerCase();
      const filtered = kw
        ? bridges.filter(b => {
            const name = (b.name || '').toLowerCase();
            const no = String(b.no || '');
            const loc = `${b.province || ''} ${b.city || ''}`.toLowerCase();
            return name.includes(kw) || no.includes(kw) || loc.includes(kw);
          })
        : bridges;

      if (!filtered || filtered.length === 0) {
        const emptyOpt = document.createElement('option');
        emptyOpt.value = '';
        emptyOpt.textContent = kw ? `----- '${keyword}' 검색 결과 없음 -----` : '----- 등록된 교량이 없습니다 -----';
        bridgeSelect.appendChild(emptyOpt);
        return;
      }

      const defaultOpt = document.createElement('option');
      defaultOpt.value = '';
      defaultOpt.textContent = kw
        ? `----- 검색 결과 (${filtered.length}건) - 클릭하여 선택 -----`
        : `----- 등록된 교량 (총 ${filtered.length}건) -----`;
      bridgeSelect.appendChild(defaultOpt);

      filtered.forEach(bridge => {
        const option = document.createElement('option');
        option.value = bridge.name || '';
        option.dataset.no = bridge.no;

        const location = [bridge.province, bridge.city].filter(Boolean).join(' ');
        option.textContent = `${bridge.name} ${location ? `[${location}]` : ''} (등록번호 #${bridge.no})`;

        if (bridgeNoHidden && bridgeNoHidden.value === String(bridge.no)) {
          option.selected = true;
        }
        bridgeSelect.appendChild(option);
      });

      if (kw && filtered.length === 1) {
        bridgeSelect.selectedIndex = 1;
        bridgeSelect.dispatchEvent(new Event('change'));
      }
    }

    // 3. 초기 교량 목록 서버 조회
    async function initBridges() {
      if (!bridgeSelect) return;
      try {
        const bridges = await requestJson('/api/maintenance-ui/bridges?keyword=');
        cachedBridges = Array.isArray(bridges) ? bridges : [];
        renderBridgeOptions(cachedBridges, queryInput ? queryInput.value : '');
      } catch (error) {
        console.error('교량 목록 조회 오류:', error);
        bridgeSelect.innerHTML = `<option value="">----- 교량 로딩 실패 (${error.message}) -----</option>`;
      }
    }

    // 4. 교량 선택 시 하단 '최근 점검 기록' 자동 조회
    async function loadReferenceInspections(bridgeNo) {
      if (!inspectionList) return;
      if (!bridgeNo) {
        inspectionList.innerHTML = '<li class="bv-muted" style="color: #adb5bd;">교량을 선택하면 최근 점검 기록이 표시됩니다.</li>';
        return;
      }

      inspectionList.innerHTML = '<li style="color: #6c757d;">점검 기록을 조회하고 있습니다...</li>';
      try {
        const data = await requestJson(`/api/maintenance-ui/inspections?bridgeNo=${encodeURIComponent(bridgeNo)}`);
        inspectionList.innerHTML = '';

        if (!data || data.length === 0) {
          inspectionList.innerHTML = '<li class="bv-muted" style="color: #adb5bd;">최근 점검 기록이 없습니다.</li>';
          return;
        }

        data.forEach(item => {
          const li = document.createElement('li');
          li.style.padding = '4px 0';
          li.textContent = `#${item.no} · ${item.title} · ${item.date || '일자 미정'} · ${item.status}`;
          inspectionList.appendChild(li);
        });
      } catch (error) {
        inspectionList.innerHTML = `<li style="color: #dc3545;">참고 점검 기록 조회 실패: ${error.message}</li>`;
      }
    }

    // 초기화 실행
    initCurrentInspector();
    initBridges();
    if (bridgeNoHidden && bridgeNoHidden.value) {
      loadReferenceInspections(bridgeNoHidden.value);
    }

    if (queryInput) {
      queryInput.addEventListener('input', () => {
        renderBridgeOptions(cachedBridges, queryInput.value);
      });
      queryInput.addEventListener('keydown', e => {
        if (e.key === 'Enter') {
          e.preventDefault();
          renderBridgeOptions(cachedBridges, queryInput.value);
        }
      });
    }

    if (searchBtn && queryInput) {
      searchBtn.addEventListener('click', e => {
        e.preventDefault();
        renderBridgeOptions(cachedBridges, queryInput.value);
      });
    }

    if (bridgeSelect) {
      bridgeSelect.addEventListener('change', () => {
        const selectedOption = bridgeSelect.options[bridgeSelect.selectedIndex];
        const bridgeNo = selectedOption ? selectedOption.dataset.no : '';
        if (bridgeNoHidden) bridgeNoHidden.value = bridgeNo || '';
        loadReferenceInspections(bridgeNo);
      });
    }

    // -----------------------------------------------------------------------
    // 5. 비동기(Fetch) 폼 전송 및 응답 알림 처리
    // -----------------------------------------------------------------------
    if (form) {
      form.addEventListener('submit', async event => {
        event.preventDefault();

        if (!form.checkValidity()) {
          form.reportValidity();
          return;
        }
        if (bridgeSelect && !bridgeSelect.value) {
          showMessage('대상 교량을 선택해 주세요.');
          bridgeSelect.focus();
          return;
        }
        if (managerField && !managerField.value.trim()) {
          showMessage('담당자를 선택 또는 입력해 주세요.');
          managerField.focus();
          return;
        }

        if (checklistTable) {
          form.querySelectorAll('input[name="safetyChecklist"]').forEach(el => el.remove());
          const rows = checklistTable.querySelectorAll('tbody tr');
          rows.forEach(row => {
            const itemId = row.getAttribute('data-item-id') || 'UNKNOWN';
            const statusSelect = row.querySelector('select');
            const statusValue = statusSelect ? statusSelect.value : 'OK';
            const memoInput = row.querySelector('input[type="text"]');
            const memoValue = memoInput ? memoInput.value.trim() : '';

            const hiddenInput = document.createElement('input');
            hiddenInput.type = 'hidden';
            hiddenInput.name = 'safetyChecklist';
            hiddenInput.value = `${itemId}::${statusValue}::${memoValue}`;
            form.appendChild(hiddenInput);
          });
        }

        const formData = new FormData(form);

        formData.delete('photoInputMultiple');
        (window.selectedPhotoFiles || []).forEach(file => formData.append('photoInputMultiple', file));

        // 담당자가 <select>인 경우 표시 텍스트에서 '(현재 담당자)' 안내 문구를 제거한 순수 이름만 DTO(inspector)로 전송
        if (managerField && managerField.tagName === 'SELECT' && managerField.selectedIndex > 0) {
          const selectedManagerOption = managerField.options[managerField.selectedIndex];
          const managerName = selectedManagerOption.textContent.replace(/\s*\(.*\)$/, '').trim();
          formData.set('inspector', managerName);
        }

        const submitBtn = form.querySelector('button[type="submit"]');
        const originalBtnText = submitBtn ? submitBtn.textContent : '저장';

        if (submitBtn) {
          submitBtn.disabled = true;
          submitBtn.textContent = '점검 기록 저장 중...';
        }
        showMessage('점검 기록과 파일을 서버로 전송하고 있습니다.', false);

        const headers = {};
        if (csrfHeader && csrfToken) {
          headers[csrfHeader] = csrfToken;
        }

        try {
          const actionUrl = form.getAttribute('action') || '/api/bridge-inspections';
          const response = await fetch(actionUrl, {
            method: 'POST',
            credentials: 'same-origin',
            headers,
            body: formData
          });

          const responseText = await response.text();
          if (!response.ok) {
            throw new Error(responseText || `저장 실패 (HTTP ${response.status})`);
          }

          alert(responseText || '점검 기록이 성공적으로 등록되었습니다.');
          form.reset();
          initCurrentInspector();
          if (bridgeNoHidden) bridgeNoHidden.value = '';
          renderBridgeOptions(cachedBridges, '');
          loadReferenceInspections('');
          if (messageBox) messageBox.hidden = true;

          // 별도 영상 JS 파일의 테이블/플레이어 초기화
          const videoTableBody = document.getElementById('videoTableBody');
          if (videoTableBody) {
            videoTableBody.innerHTML = '<tr id="emptyRow"><td colspan="3" style="text-align: center;">추가된 파일이 없습니다.</td></tr>';
          }
          const videoPlayer = document.getElementById('videoPlayer');
          if (videoPlayer) {
            videoPlayer.pause();
            videoPlayer.removeAttribute('src');
            videoPlayer.load();
          }

        } catch (error) {
          console.error('점검 기록 저장 오류:', error);
          showMessage(`저장 실패: ${error.message}`, true);
        } finally {
          if (submitBtn) {
            submitBtn.disabled = false;
            submitBtn.textContent = originalBtnText;
          }
        }
      });
    }
  });
})();

/* 3단계 교량 등록·수정 화면: 브라우저 내 후보 검색, 폼 채우기, 이미지 미리보기, 유효성 검사.
 * 주의: 서버로 데이터를 저장/수정/삭제하지 않습니다. */
(() => {
  'use strict';

  const form = document.querySelector('#bridge-editor-form');
  if (!form) return;

  const $ = (selector) => document.querySelector(selector);
  const keywordInput = $('#csv-keyword');
  const allRows = Array.from(document.querySelectorAll('.bridge-csv-row'));
  const searchStatus = $('#csv-search-status');
  const emptyNotice = $('#csv-empty');
  const feedback = $('#bridge-form-status');
  const imageInput = $('#bridge-image-file');
  const image = $('#bridge-preview-img');
  const placeholder = $('#bridge-image-placeholder');
  const removeImage = $('#bridge-image-remove');
  let previewUrl = null;

  function selectedRow() {
    const input = document.querySelector('input[name="csvSelection"]:checked');
    return input ? input.closest('.bridge-csv-row') : null;
  }

  function refreshSelection() {
    allRows.forEach(row => row.classList.toggle('is-selected', row === selectedRow()));
  }

  function searchCandidates() {
    const term = keywordInput.value.trim().toLocaleLowerCase('ko');
    let count = 0;
    allRows.forEach(row => {
      const d = row.dataset;
      const searchable = [d.name, d.code, d.province, d.city, d.facility]
        .join(' ').toLocaleLowerCase('ko');
      const matches = !term || searchable.includes(term);
      row.hidden = !matches;
      if (!matches) {
        const radio = row.querySelector('input[type="radio"]');
        if (radio) radio.checked = false;
      }
      if (matches) count++;
    });
    searchStatus.textContent = `${count}건의 후보 교량이 표시됩니다. (샘플 데이터)`;
    emptyNotice.hidden = count !== 0;
    refreshSelection();
  }

  function setField(id, value) {
    const element = document.getElementById(id);
    if (element) {
      element.value = value || '';
      element.removeAttribute('aria-invalid');
    }
  }

  $('#csv-search-button').addEventListener('click', searchCandidates);
  keywordInput.addEventListener('keydown', event => {
    if (event.key === 'Enter') {
      event.preventDefault();
      searchCandidates();
    }
  });
  allRows.forEach(row => {
    row.addEventListener('click', event => {
      if (event.target instanceof HTMLInputElement && event.target.type === 'radio') {
        event.target.checked = true;
      } else {
        row.querySelector('input[type="radio"]').checked = true;
      }
      refreshSelection();
    });
  });

  $('#csv-apply-button').addEventListener('click', () => {
    const row = selectedRow();
    if (!row || row.hidden) {
      searchStatus.textContent = '먼저 후보 교량 하나를 선택해 주세요.';
      return;
    }
    const d = row.dataset;
    setField('bridge-name', d.name);
    setField('bridge-display-code', d.code);
    setField('bridge-province', d.province);
    setField('bridge-city', d.city);
    setField('bridge-address', d.address);
    setField('bridge-facility-type', d.facility);
    setField('bridge-road', d.road);
    setField('bridge-manager', ''); // CSV에는 현재 담당자 정보가 없음
    feedback.classList.remove('is-error');
    feedback.textContent = '샘플 교량 정보를 입력란에 불러왔습니다. 아직 저장되지 않았습니다.';
    $('#bridge-name').focus();
  });

  $('#direct-entry-button').addEventListener('click', () => {
    const checked = document.querySelector('input[name="csvSelection"]:checked');
    if (checked) checked.checked = false;
    refreshSelection();
    feedback.classList.remove('is-error');
    feedback.textContent = '직접 입력 모드입니다. 기존 입력값은 지우지 않았습니다.';
    $('#bridge-name').focus();
  });

  function resetPreview() {
    if (previewUrl) URL.revokeObjectURL(previewUrl);
    previewUrl = null;
    image.hidden = true;
    image.removeAttribute('src');
    placeholder.hidden = false;
    removeImage.hidden = true;
  }

  imageInput.addEventListener('change', () => {
    resetPreview();
    const file = imageInput.files && imageInput.files[0];
    if (!file) return;
    const allowedTypes = ['image/jpeg', 'image/png', 'image/webp'];
    if (!allowedTypes.includes(file.type) || file.size > 5 * 1024 * 1024) {
      imageInput.value = '';
      feedback.classList.add('is-error');
      feedback.textContent = 'JPG, PNG, WebP 형식 및 5MB 이하 이미지만 선택해 주세요.';
      return;
    }
    previewUrl = URL.createObjectURL(file);
    image.src = previewUrl;
    image.hidden = false;
    placeholder.hidden = true;
    removeImage.hidden = false;
    feedback.classList.remove('is-error');
    feedback.textContent = '대표 이미지 미리보기만 생성했습니다. 아직 업로드되지 않았습니다.';
  });
  removeImage.addEventListener('click', () => {
    imageInput.value = '';
    resetPreview();
  });
  window.addEventListener('pagehide', resetPreview);

  form.addEventListener('submit', event => {
    event.preventDefault(); // API 미연결: 절대로 submit 하지 않음
    const fields = form.querySelectorAll('[required]');
    fields.forEach(field => field.removeAttribute('aria-invalid'));
    const firstInvalid = Array.from(fields).find(field => !field.checkValidity());
    if (firstInvalid) {
      firstInvalid.setAttribute('aria-invalid', 'true');
      firstInvalid.focus();
      feedback.classList.add('is-error');
      feedback.textContent = '필수 항목(교량명, 시·도, 시·군·구, 시설물 종류)을 확인해 주세요.';
      $('#bridge-summary-section').hidden = true;
      return;
    }
    const imageName = imageInput.files && imageInput.files[0]
      ? imageInput.files[0].name : '없음';
    const entries = [
      ['교량명', $('#bridge-name').value.trim()],
      ['교량 번호', $('#bridge-display-code').value.trim() || 'DB 관리번호 미확정'],
      ['지역', `${$('#bridge-province').value.trim()} ${$('#bridge-city').value.trim()}`],
      ['소재지', $('#bridge-address').value.trim() || '미입력'],
      ['시설물 종류', $('#bridge-facility-type').value],
      ['도로노선명', $('#bridge-road').value.trim() || '미입력'],
      ['대표 이미지', imageName]
    ];
    const summary = $('#bridge-summary');
    summary.replaceChildren();
    entries.forEach(([label, value]) => {
      const dt = document.createElement('dt');
      const dd = document.createElement('dd');
      dt.textContent = label;
      dd.textContent = value;
      summary.append(dt, dd);
    });
    $('#bridge-summary-section').hidden = false;
    feedback.classList.remove('is-error');
    feedback.textContent = '입력값 검증 완료. 아직 서버로 저장하지 않았습니다.';
    $('#bridge-summary-section').scrollIntoView({behavior:'smooth', block:'nearest'});
  });
})();

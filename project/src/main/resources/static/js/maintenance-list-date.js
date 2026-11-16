/* 유지보수 목록: 달력 전용 날짜 선택 + 잘못된 검색 기간 방지 */
(() => {
  'use strict';

  const form = document.getElementById('mv-maintenance-search-form');
  const start = document.getElementById('mv-filter-start-date');
  const end = document.getElementById('mv-filter-end-date');
  const errorBox = document.getElementById('mv-filter-date-error');
  if (!form || !start || !end) return;

  const minDate = '1900-01-01';
  const maxDate = '9999-12-31';

  function showError(text, field) {
    if (errorBox) {
      errorBox.textContent = text;
      errorBox.hidden = false;
    }
    field?.focus();
  }

  function clearError() {
    if (errorBox) {
      errorBox.textContent = '';
      errorBox.hidden = true;
    }
  }

  function openPicker(input) {
    // Chromium(Chrome/Edge)에서는 입력창 전체를 클릭해 달력을 열 수 있습니다.
    // Safari 등 미지원 환경에서는 브라우저 기본 달력 아이콘을 사용합니다.
    if (typeof input.showPicker !== 'function') return;
    try { input.showPicker(); } catch (_) { /* 기본 달력 동작 유지 */ }
  }

  function calendarOnly(input) {
    input.setAttribute('inputmode', 'none');
    input.setAttribute('autocomplete', 'off');
    input.setAttribute('title', '달력에서 날짜를 선택해 주세요.');

    // readonly를 쓰면 일부 브라우저에서 달력도 열리지 않으므로 사용하지 않습니다.
    // 키보드로 날짜의 연도/월/일 값을 편집하는 행동만 막습니다.
    input.addEventListener('keydown', event => {
      // 브라우저 단축키(Ctrl+R, Ctrl+C 등)는 유지하되 붙여넣기/잘라내기는 막습니다.
      if (event.ctrlKey || event.metaKey || event.altKey) {
        if (['v', 'x', 'backspace', 'delete'].includes(event.key.toLowerCase()))
          event.preventDefault();
        return;
      }
      if (event.key === 'Tab' || event.key === 'Escape' ||
          event.key === 'Shift' || event.key === 'Control' ||
          event.key === 'Alt' || event.key === 'Meta' ||
          event.key === 'ArrowLeft' || event.key === 'ArrowRight' ||
          /^F\d{1,2}$/.test(event.key)) return;

      if (event.key === 'Enter' || event.key === ' ') {
        event.preventDefault();
        openPicker(input);
        return;
      }

      // 숫자 직접 입력, Delete/Backspace, 화살표로 연도 변경 등을 차단합니다.
      event.preventDefault();
    });

    input.addEventListener('beforeinput', event => {
      // 텍스트 직접 입력 또는 삭제에 한해서 차단합니다.
      if (event.inputType && (
          event.inputType === 'insertText' ||
          event.inputType === 'insertFromPaste' ||
          event.inputType.startsWith('delete'))) {
        event.preventDefault();
      }
    });

    input.addEventListener('paste', event => event.preventDefault());
    input.addEventListener('drop', event => event.preventDefault());

    input.addEventListener('click', () => openPicker(input));
    input.addEventListener('input', clearError);
    input.addEventListener('change', clearError);
  }

  // 비어 있는 검색 조건은 허용합니다. 입력한 경우에만 엄격하게 검증합니다.
  function validDate(input) {
    if (input.validity.badInput) return false;
    const value = input.value;
    if (!value) return true;
    if (!/^\d{4}-\d{2}-\d{2}$/.test(value)) return false;
    if (value < minDate || value > maxDate) return false;

    const [year, month, day] = value.split('-').map(Number);
    const date = new Date(Date.UTC(year, month - 1, day));
    return date.getUTCFullYear() === year &&
           date.getUTCMonth() === month - 1 &&
           date.getUTCDate() === day &&
           input.validity.valid;
  }

  calendarOnly(start);
  calendarOnly(end);

  form.addEventListener('submit', event => {
    clearError();
    if (!validDate(start)) {
      event.preventDefault();
      showError('진행 시작일은 달력에서 올바른 날짜를 선택해 주세요.', start);
      return;
    }
    if (!validDate(end)) {
      event.preventDefault();
      showError('진행 종료일은 달력에서 올바른 날짜를 선택해 주세요.', end);
      return;
    }
    if (start.value && end.value && start.value > end.value) {
      event.preventDefault();
      showError('진행 시작일은 종료일보다 늦을 수 없습니다.', start);
    }
  });
})();

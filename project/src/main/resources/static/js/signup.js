/* Thymeleaf 회원가입 UI: 실제 기업 인증 결과만 인정. 서버 저장 연동 전 최종 가입 버튼 비활성화. */
(() => {
  'use strict';
  const root = document.querySelector('#joinRoot');
  if (!root) return;
  const byId = (id) => document.getElementById(id);
  const step1 = byId('step1');
  const step2 = byId('step2');
  const indicator1 = byId('indicatorStep1');
  const indicator2 = byId('indicatorStep2');
  const verifyBtn = byId('verifyBtn');
  const verifyStatus = byId('verifyStatus');
  const nextBtn = byId('step1Next');
  const form = byId('joinForm');
  const staticPreview = document.body.dataset.staticPreview === 'true';
  const previewMode = new URLSearchParams(location.search).get('preview') === '1' || staticPreview;
  let verifiedBusiness = null;

  function alertStatus(kind, message) {
    verifyStatus.className = 'auth-alert auth-alert--' + kind;
    verifyStatus.textContent = message;
  }
  function readBusiness() {
    return {
      businessNo: byId('businessNo').value.replace(/\D/g, ''),
      representativeName: byId('representativeName').value.trim(),
      openDate: byId('openDate').value
    };
  }
  function validBusiness(data) {
    if (!/^\d{10}$/.test(data.businessNo)) return '사업자등록번호는 숫자 10자리여야 합니다.';
    if (!data.representativeName) return '대표자명을 입력해 주세요.';
    if (!/^\d{4}-\d{2}-\d{2}$/.test(data.openDate)) return '개업일자를 선택해 주세요.';
    return '';
  }
  function renderStep(num) {
    step1.hidden = num !== 1;
    step2.hidden = num !== 2;
    indicator1.classList.toggle('is-current', num === 1);
    indicator1.classList.toggle('is-done', num === 2);
    indicator2.classList.toggle('is-current', num === 2);
    if (num === 1) {
      indicator1.setAttribute('aria-current','step');
      indicator2.removeAttribute('aria-current');
      byId('businessNo').focus({preventScroll:true});
    } else {
      indicator1.removeAttribute('aria-current');
      indicator2.setAttribute('aria-current','step');
      byId('signupUsername').focus({preventScroll:true});
    }
    root.scrollIntoView({behavior:'auto', block:'start'});
  }
  function showSummary(data) {
    byId('summaryBusinessNo').textContent = data.businessNo.replace(/^(\d{3})(\d{2})(\d{5})$/, '$1-$2-$3');
    byId('summaryRepresentative').textContent = data.representativeName;
    byId('summaryOpenDate').textContent = data.openDate;
  }
  function resetVerification() {
    verifiedBusiness = null;
    nextBtn.disabled = !staticPreview;
    alertStatus('neutral','기업 정보가 변경되었습니다. 다시 인증해 주세요.');
  }
  ['businessNo','openDate','representativeName'].forEach(id => byId(id).addEventListener('input', resetVerification));

  verifyBtn.addEventListener('click', async () => {
    if (previewMode) {
      alertStatus('info','화면 미리보기에서는 실제 기업 인증 요청을 보내지 않습니다.');
      return;
    }
    const payload = readBusiness();
    const validation = validBusiness(payload);
    if (validation) { alertStatus('error', validation); return; }
    verifyBtn.disabled = true;
    verifyBtn.textContent = '인증 확인 중…';
    alertStatus('neutral','국세청 사업자 정보 일치 여부를 조회 중입니다.');
    try {
      const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
      const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content || 'X-CSRF-TOKEN';
      const headers = {'Content-Type':'application/json','Accept':'application/json'};
      if (csrfToken) headers[csrfHeader] = csrfToken;
      const response = await fetch('/api/business/verify', {
        method:'POST', headers, body:JSON.stringify(payload), credentials:'same-origin'
      });
      if (!response.ok) throw new Error('인증 요청이 실패했습니다 (HTTP ' + response.status + ').');
      const result = await response.json();
      // 국세청 진위확인 응답: status_code=OK 자체는 사업자 검증 성공이 아님.
      // 반드시 데이터 안의 valid=01을 확인한다.
      const record = Array.isArray(result?.data) ? result.data.find(item => String(item?.b_no).replace(/\D/g,'') === payload.businessNo) : null;
      if (result?.status_code === 'OK' && record && String(record.valid) === '01') {
        verifiedBusiness = payload; // 편의상 UI 상태만 보관. 서버 세션 인증의 대체가 될 수 없음.
        nextBtn.disabled = false;
        alertStatus('success','사업자 정보 일치가 확인되었습니다. 다음 단계에서 작업자 정보를 설정하세요.');
      } else {
        resetVerification();
        alertStatus('error', record?.valid_msg || '입력한 사업자 정보와 국세청 등록 정보가 일치하지 않습니다.');
      }
    } catch (error) {
      resetVerification();
      alertStatus('error', error instanceof Error ? error.message : '기업 인증 중 오류가 발생했습니다.');
    } finally {
      verifyBtn.disabled = false;
      verifyBtn.textContent = '기업 인증';
    }
  });

  nextBtn.addEventListener('click', () => {
    if (!verifiedBusiness && !staticPreview) return;
    showSummary(verifiedBusiness || {businessNo:'0000000000', representativeName:'미리보기 예시', openDate:'2026-01-01'});
    renderStep(2);
  });
  byId('step2Prev').addEventListener('click', () => renderStep(1));

  function passwordState() {
    const password = byId('signupPassword').value;
    const repeat = byId('signupPasswordConfirm').value;
    const element = byId('passwordMatchStatus');
    if (!password && !repeat) { element.className='auth-alert auth-alert--neutral'; element.textContent='비밀번호를 입력하고 일치 여부를 확인하세요.'; return false; }
    if (password.length < 6) { element.className='auth-alert auth-alert--error'; element.textContent='비밀번호는 6자 이상이어야 합니다.'; return false; }
    if (password !== repeat) { element.className='auth-alert auth-alert--error'; element.textContent='비밀번호가 일치하지 않습니다.'; return false; }
    element.className='auth-alert auth-alert--success';
    element.textContent='비밀번호가 일치합니다.';
    return true;
  }
  ['signupPassword','signupPasswordConfirm'].forEach(id => byId(id).addEventListener('input', passwordState));

  byId('checkWorkerBtn').addEventListener('click', () => {
    const required = ['signupUsername','signupPassword','signupPasswordConfirm','signupName','signupPhone'];
    let allGood = true;
    for (const id of required) {
      const field = byId(id);
      const valid = field.checkValidity() && !!field.value.trim();
      field.setAttribute('aria-invalid', valid ? 'false' : 'true');
      if (!valid) allGood = false;
    }
    if (!passwordState()) allGood = false;
    if (byId('corpEmail').value && !byId('corpEmail').checkValidity()) allGood = false;
    byId('workerValidationMessage').textContent = allGood
      ? '입력 항목 확인 완료. 실제 회원가입 저장은 백엔드의 기업 연결 기능 구현 후 사용할 수 있습니다.'
      : '필수 항목과 비밀번호 일치 여부를 확인해 주세요.';
  });

  // 안전 가드: 향후 버튼 disabled를 해제하더라도 서버 검증 통합 전에 submit 금지.
  form.addEventListener('submit', event => {
    event.preventDefault();
    byId('workerValidationMessage').textContent = '현재는 화면 구현 단계입니다. 기업 검증 결과를 서버가 재확인하고 Corp 연결까지 완료한 후 제출을 활성화하세요.';
  });

  if (previewMode) {
    byId('previewBanner').hidden = false;
    if (staticPreview) {
      nextBtn.disabled = false;
      alertStatus('info', '정적 미리보기: 실제 API 호출 없이 화면만 확인합니다. 다음 단계 버튼으로 이동할 수 있습니다.');
    } else {
      showSummary({businessNo:'0000000000', representativeName:'미리보기 예시', openDate:'2026-01-01'});
      renderStep(2);
    }
  }
})();

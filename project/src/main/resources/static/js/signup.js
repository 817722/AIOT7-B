/** 기업 인증(서버 세션) + Thymeleaf 작업자 회원가입 */
(() => {
  'use strict';
  const root = document.getElementById('joinRoot');
  if (!root) return;
  const byId = (id) => document.getElementById(id);
  const step1 = byId('step1');
  const step2 = byId('step2');
  const nextBtn = byId('step1Next');
  const verifyBtn = byId('verifyBtn');
  const verifyStatus = byId('verifyStatus');
  const form = byId('joinForm');
  const staticPreview = document.body.dataset.staticPreview === 'true';
  const previewMode = new URLSearchParams(location.search).get('preview') === '1' || staticPreview;
  const serverResume = root.dataset.resumeStepTwo === 'true';
  let verifiedBusiness = null;
  let submitting = false;

  function message(kind, text) {
    verifyStatus.className = `auth-alert auth-alert--${kind}`;
    verifyStatus.textContent = text;
  }
  function readBusiness() {
    return {
      businessNo: byId('businessNo').value.replace(/\D/g, ''),
      representativeName: byId('representativeName').value.trim(),
      corpName: byId('corpName').value.trim(),
      openDate: byId('openDate').value
    };
  }
  function validateBusiness(b) {
    if (!/^\d{10}$/.test(b.businessNo)) return '사업자등록번호는 숫자 10자리여야 합니다.';
    if (!b.representativeName) return '대표자명을 입력해 주세요.';
    if (!b.corpName) return '기업명을 입력해 주세요.';
    if (!/^\d{4}-\d{2}-\d{2}$/.test(b.openDate)) return '개업일자를 선택해 주세요.';
    return '';
  }
  function renderStep(stage) {
    step1.hidden = stage !== 1;
    step2.hidden = stage !== 2;
    root.classList.toggle('auth-show-worker', stage === 2);
    byId('indicatorStep1').classList.toggle('is-current', stage === 1);
    byId('indicatorStep1').classList.toggle('is-done', stage === 2);
    byId('indicatorStep2').classList.toggle('is-current', stage === 2);
    if (stage === 1) {
      byId('indicatorStep1').setAttribute('aria-current', 'step');
      byId('indicatorStep2').removeAttribute('aria-current');
    } else {
      byId('indicatorStep1').removeAttribute('aria-current');
      byId('indicatorStep2').setAttribute('aria-current', 'step');
    }
  }
  function showSummary(b) {
    byId('summaryCompanyName').textContent = b.corpName || '기업명';
    byId('summaryBusinessNo').textContent = b.businessNo.replace(/^(\d{3})(\d{2})(\d{5})$/, '$1-$2-$3');
    byId('summaryRepresentative').textContent = b.representativeName;
    byId('summaryOpenDate').textContent = b.openDate;
  }
  function invalidateVerification() {
    verifiedBusiness = null;
    nextBtn.disabled = true;
    message('neutral', '기업 정보가 변경되었습니다. 다시 인증해 주세요.');
  }
  ['businessNo', 'representativeName', 'corpName', 'openDate']
      .forEach(id => byId(id).addEventListener('input', invalidateVerification));

  verifyBtn.addEventListener('click', async () => {
    if (previewMode) {
      message('info', '미리보기 모드에서는 실제 기업 인증이 진행되지 않습니다.');
      return;
    }
    const data = readBusiness();
    const issue = validateBusiness(data);
    if (issue) { message('error', issue); return; }
    verifyBtn.disabled = true;
    verifyBtn.textContent = '인증 확인 중…';
    nextBtn.disabled = true;
    verifiedBusiness = null;
    message('neutral', '국세청에 사업자 정보를 확인하고 있습니다.');
    try {
      const token = document.querySelector('meta[name="_csrf"]')?.content;
      const headerName = document.querySelector('meta[name="_csrf_header"]')?.content || 'X-CSRF-TOKEN';
      const headers = {'Content-Type': 'application/json', 'Accept': 'application/json'};
      if (token) headers[headerName] = token;
      const response = await fetch('/api/business/verify', {
        method: 'POST', credentials: 'same-origin', headers, body: JSON.stringify(data)
      });
      const body = await response.json();
      if (response.ok && body.verified === true) {
        verifiedBusiness = data;
        nextBtn.disabled = false;
        showSummary(data);
        message('success', body.message || '기업 인증이 완료되었습니다.');
      } else {
        message('error', body.message || `기업 인증에 실패했습니다 (HTTP ${response.status}).`);
      }
    } catch (e) {
      message('error', '기업 인증 요청에 실패했습니다. 서버 상태를 확인해 주세요.');
    } finally {
      verifyBtn.disabled = false;
      verifyBtn.textContent = '기업 인증';
    }
  });

  nextBtn.addEventListener('click', () => {
    if (!verifiedBusiness && !staticPreview) return;
    showSummary(verifiedBusiness || {businessNo: '0000000000', representativeName: '미리보기', corpName: '미리보기 기업', openDate: '2026-01-01'});
    renderStep(2);
    byId('signupUsername').focus();
  });
  byId('step2Prev').addEventListener('click', () => renderStep(1));

  function passwordState() {
    const a = byId('signupPassword').value;
    const b = byId('signupPasswordConfirm').value;
    const status = byId('passwordMatchStatus');
    if (!a && !b) {
      status.className = 'auth-alert auth-alert--neutral';
      status.textContent = '비밀번호를 입력하고 일치 여부를 확인하세요.';
      return false;
    }
    if (a.length < 6) {
      status.className = 'auth-alert auth-alert--error';
      status.textContent = '비밀번호는 6자 이상 입력해 주세요.';
      return false;
    }
    if (a !== b) {
      status.className = 'auth-alert auth-alert--error';
      status.textContent = '비밀번호가 일치하지 않습니다.';
      return false;
    }
    status.className = 'auth-alert auth-alert--success';
    status.textContent = '비밀번호가 일치합니다.';
    return true;
  }
  ['signupPassword','signupPasswordConfirm']
      .forEach(id => byId(id).addEventListener('input', passwordState));

  function formMessage(text) {
    const target = byId('workerValidationMessage');
    target.textContent = text;
    target.classList.add('is-error');
  }
  form.addEventListener('submit', e => {
    if (previewMode || !verifiedBusiness || submitting) {
      e.preventDefault();
      formMessage(previewMode ? '미리보기 모드에서는 회원가입이 저장되지 않습니다.' : '기업 인증을 먼저 완료해 주세요.');
      return;
    }
    const fields = ['signupUsername','signupPassword','signupPasswordConfirm','signupName','signupPhone'];
    let invalid = null;
    for (const id of fields) {
      const item = byId(id);
      const valid = item.checkValidity() && item.value.trim().length > 0;
      item.setAttribute('aria-invalid', String(!valid));
      if (!valid && !invalid) invalid = item;
    }
    if (!passwordState() && !invalid) invalid = byId('signupPasswordConfirm');
    if (invalid) {
      e.preventDefault();
      formMessage('필수 정보와 비밀번호를 확인해 주세요.');
      invalid.focus();
      return;
    }
    submitting = true;
    byId('joinSubmit').disabled = true;
    byId('workerValidationMessage').textContent = '회원가입을 처리하고 있습니다.';
    byId('workerValidationMessage').classList.remove('is-error');
    // 브라우저 기본 POST → /join, CSRF hidden input 포함. 서버가 인증 세션과 기업 정보를 다시 검증.
  });

  if (previewMode) {
    byId('previewBanner').hidden = false;
    if (staticPreview) nextBtn.disabled = false;
    if (!staticPreview) {
      showSummary({businessNo: '0000000000', representativeName: '미리보기', corpName:'미리보기 기업', openDate:'2026-01-01'});
      renderStep(2);
    }
  } else if (serverResume) {
    // 서버가 유효한 기업 인증 세션을 확인한 후 가입 폼을 다시 표시하는 경우.
    verifiedBusiness = readBusiness();
    nextBtn.disabled = false;
    showSummary(verifiedBusiness);
    renderStep(2);
  }
})();

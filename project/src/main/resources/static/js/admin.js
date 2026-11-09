// 관리자 화면(/admin, /corps) 보조 스크립트
// - 상태 처리 폼(data-confirm)을 제출하기 전에 확인창을 띄운다.
// - 폼 자체는 일반 HTML POST(+CSRF hidden 필드)로 동작하므로 스크립트가 없어도 처리 가능하다.
// - 인라인 스크립트를 사용하지 않는다(외부 파일로만 로드).
document.addEventListener('DOMContentLoaded', () => {
  document.querySelectorAll('form[data-confirm]').forEach((form) => {
    form.addEventListener('submit', (event) => {
      if (!window.confirm(form.dataset.confirm)) {
        event.preventDefault();
      }
    });
  });

  // 사유 입력 영역을 열면 입력란에 바로 포커스
  document.querySelectorAll('details.ad-reason').forEach((details) => {
    details.addEventListener('toggle', () => {
      if (details.open) {
        const field = details.querySelector('textarea');
        if (field) field.focus();
      }
    });
  });
});

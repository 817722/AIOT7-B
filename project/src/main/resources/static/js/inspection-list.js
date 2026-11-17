(() => { 'use strict'; const form=document.querySelector('#in-filter-form'); if(!form)return; form.addEventListener('submit',()=>{const y=form.elements.year;if(y&&y.value && (+y.value<1900||+y.value>2100)){y.setCustomValidity('올바른 연도를 입력하세요.');}else if(y)y.setCustomValidity('');}); })();


/* 기존 /bridges 페이지에 서버가 렌더링한 사용자별 최근 열람 5건을 읽기 전용으로 표시.
 * 새 API·DB·세션 저장을 만들지 않고, 교량/유지보수 목록과 동일한 조회 이력을 사용한다. */
(() => {
  'use strict';
  const panel = document.getElementById('in-list-recent-lines');
  const message = document.getElementById('in-list-recent-message');
  if (!panel || !message) return;

  const showMessage = (value) => {
    panel.replaceChildren();
    panel.hidden = true;
    message.hidden = false;
    message.textContent = value;
  };

  async function loadRecent() {
    try {
      // 상대 경로를 사용해 배포 컨텍스트 경로도 지원한다.
      // /inspections 페이지에서 실행하므로 같은 컨텍스트의 /bridges로 이동.
      const sourceUrl = new URL('bridges', new URL('.', window.location.href));
      const response = await fetch(sourceUrl, {credentials: 'same-origin', headers: {'Accept': 'text/html'}, cache: 'no-store'});
      if (!response.ok) throw new Error('HTTP ' + response.status);
      const documentHtml = new DOMParser().parseFromString(await response.text(), 'text/html');
      const source = documentHtml.querySelector('.bridge-v2 .bv-recent');
      if (!source) throw new Error('최근 교량 목록을 찾을 수 없음');
      const entries = [...source.querySelectorAll('.bv-recent-line')].slice(0, 5);
      if (entries.length === 0) {
        showMessage('최근 확인한 교량이 없습니다. 교량 상세 화면을 확인해 보세요.');
        return;
      }
      const fragment = document.createDocumentFragment();
      for (const entry of entries) {
        const rawHref = entry.getAttribute('href');
        if (!rawHref) continue;
        const destination = new URL(rawHref, sourceUrl);
        // 링크는 같은 출처의 교량 상세 URL만 허용한다.
        if (destination.origin !== location.origin ||
            !/\/bridges\/\d+\/?$/.test(destination.pathname)) continue;
        const link = document.createElement('a');
        link.className = 'in-list-recent-line';
        link.href = destination.pathname + destination.search;
        const name = entry.querySelector('.bv-recent-name');
        if (name) {
          const strong = document.createElement('strong');
          strong.className = 'in-list-recent-name';
          strong.textContent = name.textContent.trim();
          link.append(strong);
        }
        for (const field of entry.querySelectorAll('.bv-recent-segment')) {
          const span = document.createElement('span');
          span.className = 'in-list-recent-segment';
          span.textContent = field.textContent.trim();
          link.append(span);
        }
        fragment.append(link);
      }
      if (!fragment.childNodes.length) throw new Error('조회 가능한 교량 링크가 없음');
      panel.replaceChildren(fragment);
      panel.hidden = false;
      message.hidden = true;
    } catch (error) {
      console.warn('최근 교량 이력 조회 실패:', error);
      showMessage('최근 확인한 교량을 불러오지 못했습니다. 교량 목록에서 확인해 주세요.');
    }
  }
  loadRecent();
})();

function previewVideo(input) {
    const file = input.files[0];
    if (file) {
        const videoPlayer = document.getElementById('videoPlayer');
        
        // 1. 파일 객체로부터 임시 브라우저 URL 생성
        const fileURL = URL.createObjectURL(file);
        
        // 2. 비디오 플레이어의 src에 적용 후 재생
        videoPlayer.src = fileURL;
        videoPlayer.style.display = 'block'; // 혹시 숨겨져 있다면 표시
        videoPlayer.load(); // 새로운 소스 로드
        
        // 메모리 누수 방지를 위한 해제 처리 (선택)
        videoPlayer.onloadeddata = function() {
            URL.revokeObjectURL(fileURL); 
        };
    }
}

// 2. [+ 파일 추가] 클릭 시 다중 영상들을 오른쪽 테이블 목록에 한 줄씩 동적으로 붙여주는 함수
function appendVideoToTable(input) {
  const tbody = document.getElementById('videoTableBody');
  const emptyRow = document.getElementById('emptyRow');

  if (input.files && input.files.length > 0) {
    if (emptyRow) {
      emptyRow.remove(); // '최근 기록이 없습니다' 문구 제거
    }

    Array.from(input.files).forEach(file => {
      const fileURL = URL.createObjectURL(file);
      
      const tr = document.createElement('tr');
      
      // 1열: 영상 이름
      const tdName = document.createElement('td');
      tdName.innerText = file.name;
      
      // 2열: 대기 상태 표시 및 개별 재생 링크 제공
      const tdStatus = document.createElement('td');
      const playLink = document.createElement('span');
      playLink.innerText = "추가됨 (클릭시 재생)";
      playLink.style.cssText = "color: #07868d; cursor: pointer; font-size: 13px; text-decoration: underline;";
      
      // 글씨를 누르면 왼쪽 비디오 플레이어 인터페이스로 영상 교체 연동
      playLink.onclick = function() {
        const videoPlayer = document.getElementById('videoPlayer');
        videoPlayer.src = fileURL;
        videoPlayer.play();
      };
      tdStatus.appendChild(playLink);
      
      // ❌ [추가] 3열: 동적으로 추가된 파일의 '삭제' 버튼 기능 생성
      const tdDelete = document.createElement('td');
      tdDelete.innerText = "x";
      tdDelete.style.cssText = "cursor: pointer; color: #dc3545; font-weight: bold;";
      
      // 동적 삭제 클릭 이벤트 처리 (deleteRow 함수 호출)
      tdDelete.onclick = function() {
        if (confirm("목록에서 이 영상을 제외하시겠습니까?")) {
          tr.remove(); // 자바스크립트로 생성한 이 행(tr)을 즉시 화면에서 제거
          
          // 만약 모든 영상이 지워졌다면 "추가된 파일이 없습니다" 문구 다시 노출
          const dataRows = tbody.querySelectorAll('tr:not(#emptyRow)');
          if (dataRows.length === 0) {
            const newEmptyRow = document.createElement('tr');
            newEmptyRow.id = 'emptyRow';
            newEmptyRow.innerHTML = '<td colspan="3" style="text-align: center;">추가된 파일이 없습니다.</td>';
            tbody.appendChild(newEmptyRow);
          }
        }
      };

      // 순서대로 테이블 행(tr)에 열(td)들을 붙여줌
      tr.appendChild(tdName);
      tr.appendChild(tdStatus);
      tr.appendChild(tdDelete); // 👈 3열 삽입
      tbody.appendChild(tr);
    });
  }
}

// 3. 기존 Thymeleaf로 출력된 데이터들을 위해 필요한 기존 삭제 함수 (그대로 유지)
function deleteRow(element) {
    if (!confirm("목록에서 이 영상을 제외하시겠습니까?")) {
        return;
    }
    const row = element.closest('tr');
    if (row) { row.remove(); }

    const tableBody = document.getElementById('videoTableBody');
    const dataRows = tableBody.querySelectorAll('tr:not(#emptyRow)');
    if (dataRows.length === 0) {
        let emptyRow = document.getElementById('emptyRow');
        if (!emptyRow) {
            emptyRow = document.createElement('tr');
            emptyRow.id = 'emptyRow';
            emptyRow.innerHTML = '<td colspan="3" style="text-align: center;">추가된 파일이 없습니다.</td>';
            tableBody.appendChild(emptyRow);
        } else {
            emptyRow.style.display = '';
        }
    }
}
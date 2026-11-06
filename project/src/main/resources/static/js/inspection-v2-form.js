// 1. [+ 파일 추가] 클릭 시 단일 영상을 재생창에 띄우는 함수
function previewVideo(input) {
  const videoPlayer = document.getElementById('videoPlayer');
  if (input.files && input.files[0]) {
    const fileURL = URL.createObjectURL(input.files[0]);
    videoPlayer.src = fileURL;
    videoPlayer.play(); // 즉시 미리보기 재생
  }
}

// 2. [+ 파일 등록] 클릭 시 다중 영상들을 오른쪽 테이블 목록에 한 줄씩 동적으로 붙여주는 함수
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
      tr.appendChild(tdName);
      tr.appendChild(tdStatus);
      tbody.appendChild(tr);
    });
  }
}
document.addEventListener('DOMContentLoaded', function () {
    const inspectionForm = document.getElementById('inspectionForm');
    const checklistTable = document.getElementById('checklist-table');

    if (inspectionForm && checklistTable) {
        inspectionForm.addEventListener('submit', function (event) {
            // 1. 기존에 생성된 동적 hidden 필드가 있다면 중복 방지를 위해 삭제
            const oldHiddenInputs = inspectionForm.querySelectorAll('input[name="safetyChecklist"]');
            oldHiddenInputs.forEach(input => input.remove());

            // 2. 체크리스트 테이블의 모든 행(tbody tr)을 탐색
            const rows = checklistTable.querySelectorAll('tbody tr');
            
            rows.forEach(row => {
                // 각 행의 식별자 데이터 가져오기 (예: CHK_001, CHK_002 등)
                const itemId = row.getAttribute('data-item-id') || 'UNKNOWN';
                
                // 확인 결과 값 (Select)
                const statusSelect = row.querySelector('select');
                const statusValue = statusSelect ? statusSelect.value : 'OK';
                
                // 관찰 내용 값 (Input text)
                const memoInput = row.querySelector('input[type="text"]');
                const memoValue = memoInput ? memoInput.value.trim() : '';

                // 3. 백엔드에서 구분하기 쉽도록 포맷팅 규칙 정의 (구분자: ::)
                // 예시 결과물: "CHK_001::NEED_CHECK::교각 하부 미세 균열 발견"
                const combinedValue = `${itemId}::${statusValue}::${memoValue}`;

                // 4. 폼 내부에 hidden input 생성하여 데이터 주입
                const hiddenInput = document.createElement('input');
                hiddenInput.type = 'hidden';
                hiddenInput.name = 'safetyChecklist'; // DTO의 필드명과 정확히 매칭
                hiddenInput.value = combinedValue;

                inspectionForm.appendChild(hiddenInput);
            });

            console.log('>>> 체크리스트 데이터 가공 및 폼 전송 준비 완료');
            // 별도의 event.preventDefault()가 없으므로 form의 원래 목적지인 백엔드로 파일과 함께 정상 전송됩니다.
        });
    }
});
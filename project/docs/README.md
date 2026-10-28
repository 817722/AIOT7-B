# GitHub 협업 가이드
## 초기 프로젝트 받기
1. Git Clone
2. Github URL 지정
  - https://github.com/817722/AIOT7-B.git
3. 폴더 선택

## 브랜치 생성 방법
GitHub 프로젝트 ➡ 좌측 상단 Branches 옆에 main 클릭 ➡ View all branches ➡ New branches

## Pull Request
Pull request ➡ 원하는 base 선택 ➡ 내가 방금 push한 브랜치 선택 ➡ create pull request
===============================
예시:
### PR 제목
feat: 로그인/회원가입 기능 구현

### 작업 내용
* 국세청 API 연동 완료
* 작업자의 회원가입 기능 구현 완료
* 로그인 기능 구현 완료
===============================

## 나의 작업 브랜치에 머지
1. 브랜치 이동 : VSCODE 좌측 하단 브랜치 버튼 선택
2. 작업할 브랜치 선택 ex) feature/1-project-setup
3. Merge 
  - 소스 제어 > ... > 분기 > 병합 > main
  - ⭐ main -> feature/1-project-setup

## Mocking 사용 방법
### VS Code에서 실행하기
1. `project` 폴더를 엽니다.
2. `project` 폴더에서 통합 터미널을 엽니다.
3. 다음 명령어를 실행합니다.
.\gradlew.bat bootRun --args="--spring.profiles.active=demo"
4. 터미널 마지막에 > :bootRun 이라는 문장이 뜨면 localhost:8080으로 접속합니다.
5. 샘플 사업자등록번호를 입력하고 인증 테스트를 합니다.

## !!!!!!!!! 주의 !!!!!!!!!
### - Spring Boot Dashboard 에서 web을 누르는게 아니라 주소창에 localhost:8080 들어가서 테스트 하는겁니다!!!!




### 테스트 데이터
- 샘플 사업자등록번호: `1234567890`
- 샘플 번호 입력 시 인증 성공 응답을 반환합니다.
- 샘플 번호 외의 번호 입력 시 불일치 응답을 반환합니다.
- Mock 모드에서는 실제 국세청 API를 호출하지 않습니다.

# 참조할 수업 내용
- https://github.com/ALOHA-CLASS/ALOHA-API/tree/main/Project
1. Spirng Security(security-layout, shop)
2. Spring Boot
3. API

# 프로젝트 산출물
* 깃허브 소스코드
1. 요구사항 정의서
2. 기능정의서
3. 화면 설계서
4. ERD (DB 설계서)
5. 테이블 정의서
6. 프로젝트 일정 2026/10/16~2026/11/24

# 발표 자료 구성 (목차)
✅ 프로젝트 개요
  * 프로젝트 주제
  * 핵심 기능
  * 프로젝트 설계
    - 요구사항 정의서
    - 기능정의서
    - FLOW CHART
    - ERD
    - 테이블 정의서
    - 화면 설계서
    - 개발환경
    - 활용방안 및 기대효과
✅ 수행경과
  * 실제 구현 화면
  * 프로젝트 일정
✅ 팀 구성 및 역할
✅ 수행 절차 및 결과
  * 시연 시나리오
    - 사용자 시연 시나리오
    - 관리자 시연 시나리오
  * 시연
✅ 평가
  * 개별평가
  * 종합평가
  * 향후계획

  ## 프로젝트 주요 일정
  - 프로젝트 계획서 : WBS, 프로젝트 수행 일지(회의록, 양식 존재함) <- 2026.10.26(월)까지 제출
  - 프로젝트 산출물 : 요구 사항 정의서, 기능 정의서... <- 2026.10.30(금)까지 제출
  - 프로젝트 발표물 : 프로젝트 결과 보고서(PPT) <- 2026.11.16(월)까지 제출
  - 프로젝트 발표회 : 2026.11.20(금)

-- =========================================================
-- 기업/사용자 승인 구조 도입 후 데이터 준비 스크립트
-- (DDL-ALOHA.sql 주석에서 참조하는 파일)
--
-- ※ 테이블 구조는 변경하지 않습니다. (DROP / TRUNCATE / ALTER 없음)
-- ※ 반드시 각 블록을 읽고 필요한 부분만 직접 실행하세요.
-- ※ 로그인 허용 조건: users.status = 'ACTIVE' AND corps.status = 'ACTIVE'
-- =========================================================

USE `project`;

-- ---------------------------------------------------------
-- 0. 현재 상태 확인 (읽기 전용)
-- ---------------------------------------------------------
SELECT no, bis_no, corp_name, bis_verified, status FROM corps;
SELECT u.no, u.username, u.manager_name, u.corp_no, u.status,
       GROUP_CONCAT(a.auth) AS auths
  FROM users u LEFT JOIN user_auth a ON a.user_no = u.no
 GROUP BY u.no;

-- ---------------------------------------------------------
-- 1. 운영사 기업 + SUPER_ADMIN 계정 생성
--    users.corp_no 가 NOT NULL 이므로 SUPER_ADMIN 도 '운영사 기업'에 소속시킵니다.
--    초기 계정: superadmin / admin1234  (발표 전 반드시 비밀번호 변경 권장)
-- ---------------------------------------------------------
START TRANSACTION;

INSERT INTO corps (bis_no, ceo_name, corp_name, opened_at, bis_verified, verified_at, status, status_at)
SELECT '0000000000', '운영사', '교량점검 시스템 운영사', CURRENT_DATE, 1, NOW(), 'ACTIVE', NOW()
 WHERE NOT EXISTS (SELECT 1 FROM corps WHERE bis_no = '0000000000');

INSERT INTO users (id, corp_no, username, password, manager_name, phone, status, status_at)
SELECT UUID(), c.no, 'superadmin',
       '$2b$10$4REp867rhHr8tf1vzFEuq.ljNNCRlM6.8J8kYcZNh4uHM9M.Swwn2', -- BCrypt('admin1234')
       '웹 관리자', '000-0000-0000', 'ACTIVE', NOW()
  FROM corps c
 WHERE c.bis_no = '0000000000'
   AND NOT EXISTS (SELECT 1 FROM users WHERE username = 'superadmin');

INSERT INTO user_auth (user_no, auth)
SELECT u.no, 'SUPER_ADMIN' FROM users u
 WHERE u.username = 'superadmin'
   AND NOT EXISTS (SELECT 1 FROM user_auth a WHERE a.user_no = u.no AND a.auth = 'SUPER_ADMIN');

COMMIT;

-- ---------------------------------------------------------
-- 2. (선택) 기존 개발/시연 데이터 활성화
--    승인 구조 도입 전에 가입한 계정은 status 기본값(PENDING) 때문에 로그인할 수 없습니다.
--    0번 조회 결과를 확인한 뒤, 활성화할 기업/사용자 번호를 직접 지정하여 실행하세요.
-- ---------------------------------------------------------
-- UPDATE corps SET status = 'ACTIVE', status_at = NOW()
--  WHERE no IN (/* 기업 번호 */) AND bis_verified = 1;
--
-- UPDATE users SET status = 'ACTIVE', status_at = NOW()
--  WHERE no IN (/* 사용자 번호 */);
--
-- 기존 기업의 최초 가입자를 기업 관리자로 지정해야 하는 경우:
-- INSERT INTO user_auth (user_no, auth) VALUES (/* 사용자 번호 */, 'ADMIN');

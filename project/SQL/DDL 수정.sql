-- Active: 1788828728652@@127.0.0.1@3306@project
-- =========================================================
-- 드론 영상 기반 교량 점검 업무 지원 시스템
-- MySQL 8.0.46 / 초기 구축용 DDL
-- =========================================================

CREATE DATABASE IF NOT EXISTS `project`
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE project;

SET FOREIGN_KEY_CHECKS = 0;

-- =========================================================
-- 기존 테이블 삭제
-- 자식 테이블부터 삭제하여 외래키 충돌을 방지합니다.
-- ※ 중요 ※
-- 반드시 DROP TABLE IF EXISTS `defect_history`는 
-- DROP TABLE IF EXISTS `defect_actions`; 위에 있어야 합니다.
-- =========================================================

DROP TABLE IF EXISTS `defect_history`;
DROP TABLE IF EXISTS `defect_actions`;
DROP TABLE IF EXISTS `maintenance_media`;
DROP TABLE IF EXISTS `inspection_media`;
DROP TABLE IF EXISTS `bridge_media`;
DROP TABLE IF EXISTS `defects`;
DROP TABLE IF EXISTS `maintenance_records`;
DROP TABLE IF EXISTS `inspections`;
DROP TABLE IF EXISTS `media`;
DROP TABLE IF EXISTS `bridges`;
DROP TABLE IF EXISTS `user_auth`;
DROP TABLE IF EXISTS `users`;
DROP TABLE IF EXISTS `bridge_master`;
DROP TABLE IF EXISTS `corps`;

SET FOREIGN_KEY_CHECKS = 1;

-- =========================================================
-- 1. 기업
-- =========================================================
CREATE TABLE `corps` (
    `no` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'PK',
    `bis_no` VARCHAR(20) NOT NULL COMMENT '사업자등록번호',
    `ceo_name` VARCHAR(100) NOT NULL COMMENT '대표자명',
    `corp_name` VARCHAR(100) NOT NULL COMMENT '기업명',
    `opened_at` DATE NOT NULL COMMENT '개업일자',
    `phone` VARCHAR(30) DEFAULT NULL COMMENT '기업 전화번호',
    `email` VARCHAR(254) DEFAULT NULL COMMENT '기업 이메일',
    `bis_verified` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '기업 인증 완료 여부',
    `verified_at` DATETIME DEFAULT NULL COMMENT '기업 인증 일시',
    `email_verified` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '기업 이메일 인증 여부',
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '등록일자',
    `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일자',

    PRIMARY KEY (`no`),
    UNIQUE KEY `uk_corps_bis_no` (`bis_no`),
    KEY `idx_corps_ceo` (`ceo_name`),

    CONSTRAINT `chk_corps_bis_verified`
        CHECK (`bis_verified` IN (0, 1)),
    CONSTRAINT `chk_corps_email_verified`
        CHECK (`email_verified` IN (0, 1))
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='기업 정보 및 인증 정보';


-- =========================================================
-- 2. 사용자
-- 작업자의 로그인 ID는 username 컬럼으로 관리합니다.
-- 외부 참조 및 식별에 사용할 수 있는 고유 식별자는 id 컬럼으로 관리합니다.

-- 복합 FK에서 사용자와 기업의 소속 일치를 검증하기 위한 키
    -- UNIQUE KEY `uk_users_no_corp` (`no`, `corp_no`),
-- =========================================================
CREATE TABLE `users` (
    `no` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'PK',
    `id` VARCHAR(64) NOT NULL COMMENT '고유 식별자',
    `corp_no` BIGINT NOT NULL COMMENT '소속 기업 번호',
    `username` VARCHAR(100) NOT NULL COMMENT '로그인 ID',
    `password` VARCHAR(255) NOT NULL COMMENT '비밀번호 해시',
    `manager_name` VARCHAR(100) NOT NULL COMMENT '담당자명',
    `phone` VARCHAR(100) NOT NULL COMMENT '담당자 연락망',
    `enabled` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '사용 여부',
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '등록일자',
    `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일자',

    PRIMARY KEY (`no`),
    UNIQUE KEY `uk_users_id` (`id`),
    UNIQUE KEY `uk_users_username` (`username`),

    UNIQUE KEY `uk_users_no_corp` (`no`, `corp_no`),

    KEY `idx_users_corp` (`corp_no`),
    KEY `idx_users_name` (`manager_name`),

    CONSTRAINT `fk_users_corp`
        FOREIGN KEY (`corp_no`) REFERENCES `corps` (`no`)
        ON DELETE RESTRICT ON UPDATE CASCADE,

    CONSTRAINT `chk_users_enabled`
        CHECK (`enabled` IN (0, 1))
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='시스템 사용자';


-- =========================================================
-- 3. 사용자 권한
-- =========================================================
CREATE TABLE `user_auth` (
    `no` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'PK',
    `user_no` BIGINT NOT NULL COMMENT '사용자 번호',
    `auth` ENUM('SUPER_ADMIN', 'ADMIN', 'INSPECTOR')
        NOT NULL COMMENT '권한: 최고관리자/관리자/점검 담당자',
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '등록일자',
    `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일자',

    PRIMARY KEY (`no`),
    UNIQUE KEY `uk_user_auth_user_role` (`user_no`, `auth`),
    KEY `idx_user_auth_role` (`auth`),

    CONSTRAINT `fk_user_auth_user`
        FOREIGN KEY (`user_no`) REFERENCES `users` (`no`)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='사용자별 권한';


-- =========================================================
-- 4. 전국교량표준데이터 원본
-- CSV 데이터를 최초 1회 적재하여 보관
-- 웹에서 직접 신규 교량을 생성하는 용도로 사용하지 않음
-- =========================================================

CREATE TABLE `bridge_master` (
    `no` BIGINT NOT NULL AUTO_INCREMENT COMMENT '내부 PK',

    `name` VARCHAR(255) NOT NULL COMMENT '교량명',
    `facility_type` VARCHAR(50) DEFAULT NULL COMMENT '시설물종별등급구분',
    `road_type` VARCHAR(100) DEFAULT NULL COMMENT '도로종류',
    `road_name` VARCHAR(255) DEFAULT NULL COMMENT '도로노선명',
    `road_number` VARCHAR(50) DEFAULT NULL COMMENT '도로노선번호',
    `road_direction` VARCHAR(20) DEFAULT NULL COMMENT '도로노선방향',

    `province` VARCHAR(100) DEFAULT NULL COMMENT '시도명',
    `city` VARCHAR(100) DEFAULT NULL COMMENT '시군구명',
    `city_code` VARCHAR(20) DEFAULT NULL COMMENT '시군구코드',
    `address` VARCHAR(500) DEFAULT NULL COMMENT '소재지지번주소',

    `start_latitude` DECIMAL(10,7) DEFAULT NULL COMMENT '교량 시작점 위도',
    `start_longitude` DECIMAL(10,7) DEFAULT NULL COMMENT '교량 시작점 경도',
    `end_latitude` DECIMAL(10,7) DEFAULT NULL COMMENT '교량 종료점 위도',
    `end_longitude` DECIMAL(10,7) DEFAULT NULL COMMENT '교량 종료점 경도',

    `bridge_length` DECIMAL(12,2) DEFAULT NULL COMMENT '교량연장(m)',
    `bridge_width` DECIMAL(10,2) DEFAULT NULL COMMENT '교량폭(m)',
    `sidewalk_width` DECIMAL(10,2) DEFAULT NULL COMMENT '교량보도폭(m)',
    `bridge_height` DECIMAL(10,2) DEFAULT NULL COMMENT '교량높이(m)',
    `lane_count` INT DEFAULT NULL COMMENT '차로수',

    `divided_traffic` VARCHAR(10) DEFAULT NULL COMMENT '상하행선분리여부',
    `superstructure_type` VARCHAR(100) DEFAULT NULL COMMENT '상부구조형식',
    `completion_year` VARCHAR(10) DEFAULT NULL COMMENT '교량준공연도',

    `last_inspection_date` DATE DEFAULT NULL COMMENT '최종안전점검일자',
    `last_inspection_result` VARCHAR(50) DEFAULT NULL COMMENT '최종안전점검결과',
    `last_inspection_type` VARCHAR(50) DEFAULT NULL COMMENT '최종안전점검유형',

    `seismic_design_applied` VARCHAR(50) DEFAULT NULL COMMENT '내진설계적용여부',
    `seismic_performance_secured` VARCHAR(10) DEFAULT NULL COMMENT '내진성능확보여부',

    `repair_history` TEXT DEFAULT NULL COMMENT '교량보수보강내역',
    `repair_cost` DECIMAL(15,2) DEFAULT NULL COMMENT '교량보수보강비용(천원)',

    `design_live_load` VARCHAR(100) DEFAULT NULL COMMENT '설계활하중',
    `allowable_traffic_load` VARCHAR(50) DEFAULT NULL COMMENT '허용통행하중',
    `underpass_height_limit` VARCHAR(50) DEFAULT NULL COMMENT '하부통과제한높이(m)',

    `management_agency` VARCHAR(255) DEFAULT NULL COMMENT '관리기관명',
    `management_phone` VARCHAR(50) DEFAULT NULL COMMENT '관리기관전화번호',
    `source_data_date` DATE DEFAULT NULL COMMENT '데이터기준일자',

    PRIMARY KEY (`no`),

    KEY `idx_bridge_master_name` (`name`),
    KEY `idx_bridge_master_address` (`address`(191)),
    KEY `idx_bridge_master_region` (`province`, `city`),
    KEY `idx_bridge_master_city_code` (`city_code`),
    KEY `idx_bridge_master_facility_type` (`facility_type`),

    CONSTRAINT `chk_master_start_latitude`
        CHECK (`start_latitude` IS NULL OR `start_latitude` BETWEEN -90 AND 90),
    CONSTRAINT `chk_master_start_longitude`
        CHECK (`start_longitude` IS NULL OR `start_longitude` BETWEEN -180 AND 180),
    CONSTRAINT `chk_master_end_latitude`
        CHECK (`end_latitude` IS NULL OR `end_latitude` BETWEEN -90 AND 90),
    CONSTRAINT `chk_master_end_longitude`
        CHECK (`end_longitude` IS NULL OR `end_longitude` BETWEEN -180 AND 180),

    CONSTRAINT `chk_master_length`
        CHECK (`bridge_length` IS NULL OR `bridge_length` >= 0),
    CONSTRAINT `chk_master_width`
        CHECK (`bridge_width` IS NULL OR `bridge_width` >= 0),
    CONSTRAINT `chk_master_sidewalk_width`
        CHECK (`sidewalk_width` IS NULL OR `sidewalk_width` >= 0),
    CONSTRAINT `chk_master_height`
        CHECK (`bridge_height` IS NULL OR `bridge_height` >= 0),
    CONSTRAINT `chk_master_lane_count`
        CHECK (`lane_count` IS NULL OR `lane_count` >= 0),
    CONSTRAINT `chk_master_repair_cost`
        CHECK (`repair_cost` IS NULL OR `repair_cost` >= 0)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='전국교량표준데이터 CSV 원본';


-- =========================================================
-- 5. 업무용 교량
-- CSV 원본을 선택한 경우에만 등록
-- 기업별로 별도의 업무용 교량 레코드를 생성

 -- 같은 기업에서 같은 CSV 교량을 중복 등록하지 못하도록 제한
    -- UNIQUE KEY `uk_bridges_corp_master` (`corp_no`, `bridge_master_no`),
    -- 기업 소속까지 검증하는 복합 FK용 키
    -- UNIQUE KEY `uk_bridges_no_corp` (`no`, `corp_no`),

    -- 현재 담당자는 반드시 같은 기업 소속 사용자여야 함
    -- CONSTRAINT `fk_bridges_manager_corp`
    --     FOREIGN KEY (`manager_no`, `corp_no`)
    --     REFERENCES `users` (`no`, `corp_no`)
    --     ON DELETE RESTRICT ON UPDATE CASCADE,

    -- 최초 등록자는 반드시 같은 기업 소속 사용자여야 함
    -- CONSTRAINT `fk_bridges_created_by_corp`
    --     FOREIGN KEY (`created_by`, `corp_no`)
    --     REFERENCES `users` (`no`, `corp_no`)
    --     ON DELETE RESTRICT ON UPDATE CASCADE,

    -- 최종 수정자도 반드시 같은 기업 소속 사용자여야 함
    -- CONSTRAINT `fk_bridges_updated_by_corp`
    --     FOREIGN KEY (`updated_by`, `corp_no`)
    --     REFERENCES `users` (`no`, `corp_no`)
    --     ON DELETE RESTRICT ON UPDATE CASCADE,
-- =========================================================

CREATE TABLE `bridges` (
    `no` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'PK',
    `id` VARCHAR(64) NOT NULL COMMENT '시스템 고유 식별자',

    `bridge_master_no` BIGINT NOT NULL COMMENT '선택한 CSV 원본 교량 PK',
    `corp_no` BIGINT NOT NULL COMMENT '업무용 교량을 등록한 기업 PK',

    `source_data_date` DATE DEFAULT NULL COMMENT 'CSV 데이터 기준일자',

    `bridge_name` VARCHAR(255) NOT NULL COMMENT '교량명',
    `facility_type` VARCHAR(50) DEFAULT NULL COMMENT '시설물종별등급구분',
    `road_type` VARCHAR(100) DEFAULT NULL COMMENT '도로종류',
    `road_name` VARCHAR(255) DEFAULT NULL COMMENT '도로노선명',
    `road_number` VARCHAR(50) DEFAULT NULL COMMENT '도로노선번호',
    `road_direction` VARCHAR(20) DEFAULT NULL COMMENT '도로노선방향',

    `province` VARCHAR(100) DEFAULT NULL COMMENT '시도명',
    `city` VARCHAR(100) DEFAULT NULL COMMENT '시군구명',
    `city_code` VARCHAR(20) DEFAULT NULL COMMENT '시군구코드',
    `address` VARCHAR(500) DEFAULT NULL COMMENT '소재지지번주소',

    `start_latitude` DECIMAL(10,7) DEFAULT NULL COMMENT '교량 시작점 위도',
    `start_longitude` DECIMAL(10,7) DEFAULT NULL COMMENT '교량 시작점 경도',
    `end_latitude` DECIMAL(10,7) DEFAULT NULL COMMENT '교량 종료점 위도',
    `end_longitude` DECIMAL(10,7) DEFAULT NULL COMMENT '교량 종료점 경도',

    `bridge_length` DECIMAL(12,2) DEFAULT NULL COMMENT '교량연장(m)',
    `bridge_width` DECIMAL(10,2) DEFAULT NULL COMMENT '교량폭(m)',
    `sidewalk_width` DECIMAL(10,2) DEFAULT NULL COMMENT '교량보도폭(m)',
    `bridge_height` DECIMAL(10,2) DEFAULT NULL COMMENT '교량높이(m)',
    `lane_count` INT DEFAULT NULL COMMENT '차로수',

    `divided_traffic` VARCHAR(10) DEFAULT NULL COMMENT '상하행선분리여부',
    `superstructure_type` VARCHAR(100) DEFAULT NULL COMMENT '상부구조형식',
    `completion_year` VARCHAR(10) DEFAULT NULL COMMENT '교량준공연도',

    `last_inspection_date` DATE DEFAULT NULL COMMENT '최종안전점검일자',
    `last_inspection_result` VARCHAR(50) DEFAULT NULL COMMENT '최종안전점검결과',
    `last_inspection_type` VARCHAR(50) DEFAULT NULL COMMENT '최종안전점검유형',

    `seismic_design_applied` VARCHAR(50) DEFAULT NULL COMMENT '내진설계적용여부',
    `seismic_performance_secured` VARCHAR(10) DEFAULT NULL COMMENT '내진성능확보여부',

    `repair_history` TEXT DEFAULT NULL COMMENT '교량보수보강내역',
    `repair_cost` DECIMAL(15,2) DEFAULT NULL COMMENT '교량보수보강비용(천원)',

    `design_live_load` VARCHAR(100) DEFAULT NULL COMMENT '설계활하중',
    `allowable_traffic_load` VARCHAR(50) DEFAULT NULL COMMENT '허용통행하중',
    `underpass_height_limit` VARCHAR(50) DEFAULT NULL COMMENT '하부통과제한높이(m)',

    `management_agency` VARCHAR(255) DEFAULT NULL COMMENT '관리기관명',
    `management_phone` VARCHAR(50) DEFAULT NULL COMMENT '관리기관전화번호',

    `manager_no` BIGINT DEFAULT NULL COMMENT '현재 담당자 번호',
    `created_by` BIGINT NOT NULL COMMENT '최초 등록자 번호',
    `updated_by` BIGINT DEFAULT NULL COMMENT '최종 수정 사용자 번호',

    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '등록일시',
    `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시',

    PRIMARY KEY (`no`),
    UNIQUE KEY `uk_bridges_id` (`id`),

    UNIQUE KEY `uk_bridges_corp_master` (`corp_no`, `bridge_master_no`),

    UNIQUE KEY `uk_bridges_no_corp` (`no`, `corp_no`),

    KEY `idx_bridges_master` (`bridge_master_no`),
    KEY `idx_bridges_corp` (`corp_no`),
    KEY `idx_bridges_name` (`bridge_name`),
    KEY `idx_bridges_region` (`province`, `city`),
    KEY `idx_bridges_city_code` (`city_code`),
    KEY `idx_bridges_facility_type` (`facility_type`),
    KEY `idx_bridges_manager` (`manager_no`),
    KEY `idx_bridges_created_by` (`created_by`),
    KEY `idx_bridges_updated_by` (`updated_by`),
    KEY `idx_bridges_last_inspection` (`last_inspection_date`),

    CONSTRAINT `fk_bridges_master`
        FOREIGN KEY (`bridge_master_no`) REFERENCES `bridge_master` (`no`)
        ON DELETE RESTRICT ON UPDATE CASCADE,

    CONSTRAINT `fk_bridges_corp`
        FOREIGN KEY (`corp_no`) REFERENCES `corps` (`no`)
        ON DELETE RESTRICT ON UPDATE CASCADE,

    CONSTRAINT `fk_bridges_manager_corp`
        FOREIGN KEY (`manager_no`, `corp_no`)
        REFERENCES `users` (`no`, `corp_no`)
        ON DELETE RESTRICT ON UPDATE CASCADE,

    CONSTRAINT `fk_bridges_created_by_corp`
        FOREIGN KEY (`created_by`, `corp_no`)
        REFERENCES `users` (`no`, `corp_no`)
        ON DELETE RESTRICT ON UPDATE CASCADE,

    CONSTRAINT `fk_bridges_updated_by_corp`
        FOREIGN KEY (`updated_by`, `corp_no`)
        REFERENCES `users` (`no`, `corp_no`)
        ON DELETE RESTRICT ON UPDATE CASCADE,

    CONSTRAINT `chk_bridges_start_latitude`
        CHECK (`start_latitude` IS NULL OR `start_latitude` BETWEEN -90 AND 90),
    CONSTRAINT `chk_bridges_start_longitude`
        CHECK (`start_longitude` IS NULL OR `start_longitude` BETWEEN -180 AND 180),
    CONSTRAINT `chk_bridges_end_latitude`
        CHECK (`end_latitude` IS NULL OR `end_latitude` BETWEEN -90 AND 90),
    CONSTRAINT `chk_bridges_end_longitude`
        CHECK (`end_longitude` IS NULL OR `end_longitude` BETWEEN -180 AND 180),

    CONSTRAINT `chk_bridges_length`
        CHECK (`bridge_length` IS NULL OR `bridge_length` >= 0),
    CONSTRAINT `chk_bridges_width`
        CHECK (`bridge_width` IS NULL OR `bridge_width` >= 0),
    CONSTRAINT `chk_bridges_sidewalk_width`
        CHECK (`sidewalk_width` IS NULL OR `sidewalk_width` >= 0),
    CONSTRAINT `chk_bridges_height`
        CHECK (`bridge_height` IS NULL OR `bridge_height` >= 0),
    CONSTRAINT `chk_bridges_lane_count`
        CHECK (`lane_count` IS NULL OR `lane_count` >= 0),
    CONSTRAINT `chk_bridges_repair_cost`
        CHECK (`repair_cost` IS NULL OR `repair_cost` >= 0)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='기업별 업무용 교량';


-- =========================================================
-- 6. 공통 미디어
-- 파일 경로와 원본 파일명을 저장합니다.
-- 교량 이미지, 점검 영상, 유지보수 영상 등은
-- 업무별 연결 테이블을 통해 연결합니다.
-- 기업 소속 미디어는 업로더와 기업 소속이 일치해야 합니다.
-- corp_no IS NULL인 공통 미디어는 업무 기록에 연결할 수 없습니다.

 -- 기업 소속 일치 검증을 위한 복합 FK 참조 키
    -- UNIQUE KEY `uk_media_no_corp` (`no`, `corp_no`),

-- 업로더가 실제 존재하는 사용자여야 함
    -- CONSTRAINT `fk_media_uploader`
    --     FOREIGN KEY (`uploaded_by`) REFERENCES `users` (`no`)
    --     ON DELETE RESTRICT ON UPDATE CASCADE,

    -- 기업 소속 미디어라면 업로더도 반드시 같은 기업 소속이어야 함
    -- corp_no가 NULL이면 이 복합 FK는 검사되지 않지만,
    -- 위의 fk_media_uploader가 사용자 존재 여부를 검증함
    -- CONSTRAINT `fk_media_uploader_corp`
    --     FOREIGN KEY (`uploaded_by`, `corp_no`)
    --     REFERENCES `users` (`no`, `corp_no`)
    --     ON DELETE RESTRICT ON UPDATE CASCADE,
-- =========================================================
-- 방법 1 corp에 폐업여부를 표시하도록? 
CREATE TABLE `media` (
    `no` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'PK',
    `id` VARCHAR(64) NOT NULL COMMENT '시스템 고유 식별자',
    `corp_no` BIGINT DEFAULT NULL COMMENT '업로드 기업 번호(NULL=공통 미디어)',

    `type` ENUM('IMAGE', 'VIDEO') NOT NULL COMMENT '미디어 유형',
    `path` VARCHAR(1000) NOT NULL COMMENT '저장 파일 경로',
    `origin_name` VARCHAR(255) NOT NULL COMMENT '업로드 원본 파일명',
    `mime_type` VARCHAR(100) DEFAULT NULL COMMENT '파일 MIME 유형',
    `file_size` BIGINT DEFAULT NULL COMMENT '파일 크기(Byte)',
    `uploaded_by` BIGINT NOT NULL COMMENT '업로드 사용자 번호',

    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '업로드 일시',

    PRIMARY KEY (`no`),
    UNIQUE KEY `uk_media_id` (`id`),

    UNIQUE KEY `uk_media_no_corp` (`no`, `corp_no`),

    KEY `idx_media_type` (`type`),
    KEY `idx_media_uploader` (`uploaded_by`),
    KEY `idx_media_corp` (`corp_no`),

    CONSTRAINT `fk_media_corp`
        FOREIGN KEY (`corp_no`) REFERENCES `corps` (`no`)
        ON DELETE RESTRICT ON UPDATE CASCADE,

    CONSTRAINT `fk_media_uploader`
        FOREIGN KEY (`uploaded_by`) REFERENCES `users` (`no`)
        ON DELETE RESTRICT ON UPDATE CASCADE,

    CONSTRAINT `fk_media_uploader_corp`
        FOREIGN KEY (`uploaded_by`, `corp_no`)
        REFERENCES `users` (`no`, `corp_no`)
        ON DELETE RESTRICT ON UPDATE CASCADE,


    CONSTRAINT `chk_media_file_size`
        CHECK (`file_size` IS NULL OR `file_size` >= 0)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='업로드 이미지 및 영상 파일 정보';


-- =========================================================
-- 7. 교량 대표 이미지 연결
-- 교량과 미디어의 기업 소속 일치를 DB에서 강제합니다.
-- 교량별 대표 이미지는 최대 1개만 허용합니다.

-- 대표 이미지 중복 방지용 생성 컬럼
    -- `main_bridge_no` BIGINT
    --     GENERATED ALWAYS AS (
    --         CASE
    --             WHEN `is_main` = 1 THEN `bridge_no`
    --             ELSE NULL
    --         END
    --     ) STORED COMMENT '대표 이미지 중복 방지용 교량 번호',

-- 교량당 대표 이미지 최대 1개
    -- UNIQUE KEY `uk_bridge_media_single_main` (`main_bridge_no`),
    -- KEY `idx_bridge_media_media_corp` (`media_no`, `corp_no`),

    -- 교량과 연결 레코드의 기업 소속 일치 검증
    -- CONSTRAINT `fk_bridge_media_bridge_corp`
    --     FOREIGN KEY (`bridge_no`, `corp_no`)
    --     REFERENCES `bridges` (`no`, `corp_no`)
    --     ON DELETE CASCADE ON UPDATE CASCADE,

    -- 미디어와 연결 레코드의 기업 소속 일치 검증
    -- CONSTRAINT `fk_bridge_media_media_corp`
    --     FOREIGN KEY (`media_no`, `corp_no`)
    --     REFERENCES `media` (`no`, `corp_no`)
    --     ON DELETE RESTRICT ON UPDATE CASCADE,

    -- 교량 이미지가 다른 기업의 기록에 연결 되는걸 방지하는 제약 조건
    -- 근데 이게 작동을 안함 왜 안 하는지 이유를 찾을 수가
    -- CONSTRAINT `fk_bridge_media_bridge_corp`
    --     FOREIGN KEY (`bridge_no`, `corp_no`)
    --     REFERENCES `bridges` (`no`, `corp_no`)
    --     ON DELETE CASCADE ON UPDATE CASCADE,
    -- CONSTRAINT `fk_bridge_media_media_corp`
    --     FOREIGN KEY (`media_no`, `corp_no`)
    --     REFERENCES `media` (`no`, `corp_no`)
    --     ON DELETE RESTRICT ON UPDATE CASCADE,
-- =========================================================

CREATE TABLE `bridge_media` (
    `no` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'PK',
    -- `bridge_no` BIGINT NOT NULL COMMENT '교량 번호',
    -- `corp_no` BIGINT NOT NULL COMMENT '교량 소속 기업 번호',
    `media_no` BIGINT NOT NULL COMMENT '이미지 미디어 번호',
    `is_main` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '대표 이미지 여부',

    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '등록일자',

    PRIMARY KEY (`no`),

        CONSTRAINT `fk_bridge_media_media_corp`
        FOREIGN KEY (`media_no`)
        REFERENCES `media` (`no`)
        ON DELETE RESTRICT ON UPDATE CASCADE,

    CONSTRAINT `chk_bridge_media_main`
        CHECK (`is_main` IN (0, 1))
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='교량 대표 이미지 연결';





-- =========================================================
-- 8. 교량 점검 기록
-- 점검 기록과 기업의 일치 여부를 복합 FK로 검증
    -- KEY `idx_inspections_bridge_corp` (`bridge_no`, `corp_no`),
    -- KEY `idx_inspections_corp` (`corp_no`),
    -- KEY `idx_inspections_status` (`status`),
    -- KEY `idx_inspections_date` (`inspection_date`),
    -- KEY `idx_inspections_inspector` (`inspector_no`),
    -- KEY `idx_inspections_created_by` (`created_by`),
    -- KEY `idx_inspections_updated_by` (`updated_by`),
    -- KEY `idx_inspections_bridge_date` (`bridge_no`, `inspection_date`),
-- =========================================================

CREATE TABLE `inspections` (
    `no` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'PK',
    `id` VARCHAR(64) NOT NULL COMMENT '고유 식별자',

    `bridge_no` BIGINT NOT NULL COMMENT '점검 대상 교량 번호',
    `corp_no` BIGINT NOT NULL COMMENT '점검 기록 소유 기업 번호',

    `inspection_type` ENUM(
        '자체 안전점검',
        '정기 안전점검',
        '정밀 안전진단',
        '긴급 안전점검'
    ) NOT NULL COMMENT '점검 유형',

    `status` ENUM('진행 대기', '진행 중', '완료')
        NOT NULL DEFAULT '진행 대기' COMMENT '점검 진행 상태',

    `inspection_date` DATE NOT NULL COMMENT '점검 날짜',
    `inspector_no` BIGINT NOT NULL COMMENT '점검 담당자 번호',

    `overall_result` TEXT DEFAULT NULL COMMENT '종합 점검 결과',
    `review_opinion` TEXT DEFAULT NULL COMMENT '검토 의견',
    `completed_at` DATETIME DEFAULT NULL COMMENT '점검 완료 일시',

    `created_by` BIGINT NOT NULL COMMENT '기록 생성 사용자 번호',
    `updated_by` BIGINT DEFAULT NULL COMMENT '최종 수정 사용자 번호',

    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '등록일시',
    `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시',

    PRIMARY KEY (`no`),
    UNIQUE KEY `uk_inspections_no_corp` (`no`, `corp_no`),
    UNIQUE KEY `uk_inspections_id` (`id`),

    KEY `idx_inspections_bridge_corp` (`bridge_no`, `corp_no`),
    KEY `idx_inspections_corp` (`corp_no`),
    KEY `idx_inspections_status` (`status`),
    KEY `idx_inspections_date` (`inspection_date`),
    KEY `idx_inspections_inspector` (`inspector_no`),
    KEY `idx_inspections_created_by` (`created_by`),
    KEY `idx_inspections_updated_by` (`updated_by`),
    KEY `idx_inspections_bridge_date` (`bridge_no`, `inspection_date`),

    CONSTRAINT `fk_inspections_bridge_corp`
        FOREIGN KEY (`bridge_no`, `corp_no`)
        REFERENCES `bridges` (`no`, `corp_no`)
        ON DELETE RESTRICT ON UPDATE CASCADE,

    CONSTRAINT `fk_inspections_corp`
        FOREIGN KEY (`corp_no`) REFERENCES `corps` (`no`)
        ON DELETE RESTRICT ON UPDATE CASCADE,

    CONSTRAINT `fk_inspections_inspector_corp`
        FOREIGN KEY (`inspector_no`, `corp_no`)
        REFERENCES `users` (`no`, `corp_no`)
        ON DELETE RESTRICT ON UPDATE CASCADE,

    CONSTRAINT `fk_inspections_created_by_corp`
        FOREIGN KEY (`created_by`, `corp_no`)
        REFERENCES `users` (`no`, `corp_no`)
        ON DELETE RESTRICT ON UPDATE CASCADE,

    CONSTRAINT `fk_inspections_updated_by_corp`
        FOREIGN KEY (`updated_by`, `corp_no`)
        REFERENCES `users` (`no`, `corp_no`)
        ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='교량 점검 기록';


-- =========================================================
-- 9. 점검 영상 연결
-- 점검 기록과 미디어의 기업 소속 일치를 DB에서 강제합니다.


    -- -- 점검 기록과 기업 소속 일치 검증
    -- CONSTRAINT `fk_inspection_media_inspection_corp`
    --     FOREIGN KEY (`inspection_no`, `corp_no`)
    --     REFERENCES `inspections` (`no`, `corp_no`)
    --     ON DELETE CASCADE ON UPDATE CASCADE,

    -- 미디어와 기업 소속 일치 검증
    -- CONSTRAINT `fk_inspection_media_media_corp`
        -- FOREIGN KEY (`media_no`, `corp_no`)
        -- REFERENCES `media` (`no`, `corp_no`)
-- =========================================================
CREATE TABLE `inspection_media` (
    `no` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'PK',
    `inspection_no` BIGINT NOT NULL COMMENT '점검 기록 번호',
    `corp_no` BIGINT NOT NULL COMMENT '점검 기록 소유 기업 번호',
    `media_no` BIGINT NOT NULL COMMENT '영상 미디어 번호',

    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '등록일자',

    PRIMARY KEY (`no`),

    UNIQUE KEY `uk_inspection_media` (`inspection_no`, `media_no`),

    KEY `idx_inspection_media_media_corp` (`media_no`, `corp_no`),

    CONSTRAINT `fk_inspection_media_inspection_corp`
        FOREIGN KEY (`inspection_no`, `corp_no`)
        REFERENCES `inspections` (`no`, `corp_no`)
        ON DELETE CASCADE ON UPDATE CASCADE,

    CONSTRAINT `fk_inspection_media_media_corp`
        FOREIGN KEY (`media_no`, `corp_no`)
        REFERENCES `media` (`no`, `corp_no`)
        ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='점검 기록별 영상 연결';


-- =========================================================
-- 10. 결함 정보
-- =========================================================
CREATE TABLE `defects` (
    `no` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'PK',
    `id` VARCHAR(64) NOT NULL COMMENT '시스템 고유 식별자',

    `inspection_no` BIGINT NOT NULL COMMENT '결함이 발견된 점검 기록 번호',
    `corp_no` BIGINT NOT NULL COMMENT '결함 기록 소유 기업 번호',

    `defect_type` VARCHAR(100) NOT NULL COMMENT '결함 유형',
    `location` VARCHAR(500) NOT NULL COMMENT '교량 내 결함 위치',
    `severity` ENUM('낮음', '보통', '높음', '긴급')
        DEFAULT NULL COMMENT '결함 심각도(분류 기준 확인 필요)',
    `description` TEXT DEFAULT NULL COMMENT '결함 상세 설명',

    `created_by` BIGINT NOT NULL COMMENT '등록 사용자 번호',
    `updated_by` BIGINT DEFAULT NULL COMMENT '최종 수정 사용자 번호',

    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '등록일시',
    `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시',

    PRIMARY KEY (`no`),
    UNIQUE KEY `uk_defects_id` (`id`),
    UNIQUE KEY `uk_defects_no_corp` (`no`, `corp_no`),

    KEY `idx_defects_inspection` (`inspection_no`),
    KEY `idx_defects_corp` (`corp_no`),
    KEY `idx_defects_type` (`defect_type`),
    KEY `idx_defects_severity` (`severity`),

    CONSTRAINT `fk_defects_inspection_corp`
        FOREIGN KEY (`inspection_no`, `corp_no`)
        REFERENCES `inspections` (`no`, `corp_no`)
        ON DELETE RESTRICT ON UPDATE CASCADE,

    CONSTRAINT `fk_defects_created_by_corp`
        FOREIGN KEY (`created_by`, `corp_no`)
        REFERENCES `users` (`no`, `corp_no`)
        ON DELETE RESTRICT ON UPDATE CASCADE,

    CONSTRAINT `fk_defects_updated_by_corp`
        FOREIGN KEY (`updated_by`, `corp_no`)
        REFERENCES `users` (`no`, `corp_no`)
        ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='교량 점검에서 발견된 결함 정보';


-- =========================================================
-- 11. 유지보수 기록
-- =========================================================
CREATE TABLE `maintenance_records` (
    `no` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'PK',
    `id` VARCHAR(64) NOT NULL COMMENT '시스템 고유 식별자',

    `bridge_no` BIGINT NOT NULL COMMENT '유지보수 대상 교량 번호',
    `corp_no` BIGINT NOT NULL COMMENT '유지보수 기록 소유 기업 번호',

    `maintenance_type` VARCHAR(50) DEFAULT NULL COMMENT '유지보수 유형(분류 기준 확정 후 적용)',
    `status` ENUM('진행 대기', '진행 중', '완료')
        NOT NULL DEFAULT '진행 대기' COMMENT '유지보수 진행 상태',

    `maintenance_date` DATE DEFAULT NULL COMMENT '실제 유지보수 작업일',
    `manager_no` BIGINT NOT NULL COMMENT '유지보수 담당자 번호',

    `result` TEXT DEFAULT NULL COMMENT '유지보수 결과',
    `completed_at` DATETIME DEFAULT NULL COMMENT '완료 일시',

    `created_by` BIGINT NOT NULL COMMENT '기록 생성 사용자 번호',
    `updated_by` BIGINT DEFAULT NULL COMMENT '최종 수정 사용자 번호',

    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '등록일시',
    `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시',

    PRIMARY KEY (`no`),
    UNIQUE KEY `uk_maintenance_no_corp` (`no`, `corp_no`),
    UNIQUE KEY `uk_maintenance_records_id` (`id`),

    KEY `idx_maintenance_bridge_corp` (`bridge_no`, `corp_no`),
    KEY `idx_maintenance_corp` (`corp_no`),
    KEY `idx_maintenance_status` (`status`),
    KEY `idx_maintenance_date` (`maintenance_date`),
    KEY `idx_maintenance_manager` (`manager_no`),
    KEY `idx_maintenance_created_by` (`created_by`),
    KEY `idx_maintenance_updated_by` (`updated_by`),
    KEY `idx_maintenance_bridge_date` (`bridge_no`, `maintenance_date`),

    CONSTRAINT `fk_maintenance_bridge_corp`
        FOREIGN KEY (`bridge_no`, `corp_no`)
        REFERENCES `bridges` (`no`, `corp_no`)
        ON DELETE RESTRICT ON UPDATE CASCADE,

    CONSTRAINT `fk_maintenance_corp`
        FOREIGN KEY (`corp_no`) REFERENCES `corps` (`no`)
        ON DELETE RESTRICT ON UPDATE CASCADE,

    CONSTRAINT `fk_maintenance_manager_corp`
        FOREIGN KEY (`manager_no`, `corp_no`)
        REFERENCES `users` (`no`, `corp_no`)
        ON DELETE RESTRICT ON UPDATE CASCADE,

    CONSTRAINT `fk_maintenance_created_by_corp`
        FOREIGN KEY (`created_by`, `corp_no`)
        REFERENCES `users` (`no`, `corp_no`)
        ON DELETE RESTRICT ON UPDATE CASCADE,

    CONSTRAINT `fk_maintenance_updated_by_corp`
        FOREIGN KEY (`updated_by`, `corp_no`)
        REFERENCES `users` (`no`, `corp_no`)
        ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='교량 유지보수 기록';


-- =========================================================
-- 12. 유지보수 영상 연결
-- 하나의 유지보수 기록에 여러 영상 파일을 연결할 수 있습니다.
-- 유지보수 기록과 미디어의 기업 소속 일치를 DB에서 강제합니다.

    -- 유지보수 기록과 기업 소속 일치 검증
    -- CONSTRAINT `fk_maintenance_media_record_corp`
    --     FOREIGN KEY (`maintenance_no`, `corp_no`)
    --     REFERENCES `maintenance_records` (`no`, `corp_no`)
    --     ON DELETE CASCADE ON UPDATE CASCADE,

    -- 미디어와 기업 소속 일치 검증
    -- CONSTRAINT `fk_maintenance_media_media_corp`
    --     FOREIGN KEY (`media_no`, `corp_no`)
    --     REFERENCES `media` (`no`, `corp_no`)
    --     ON DELETE RESTRICT ON UPDATE CASCADE
-- =========================================================
CREATE TABLE `maintenance_media` (
    `no` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'PK',
    `maintenance_no` BIGINT NOT NULL COMMENT '유지보수 기록 번호',
    `corp_no` BIGINT NOT NULL COMMENT '유지보수 기록 소유 기업 번호',
    `media_no` BIGINT NOT NULL COMMENT '영상 미디어 번호',

    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '등록일자',

    PRIMARY KEY (`no`),

    UNIQUE KEY `uk_maintenance_media` (`maintenance_no`, `media_no`),

    KEY `idx_maintenance_media_media_corp` (`media_no`, `corp_no`),

    CONSTRAINT `fk_maintenance_media_record_corp`
        FOREIGN KEY (`maintenance_no`, `corp_no`)
        REFERENCES `maintenance_records` (`no`, `corp_no`)
        ON DELETE CASCADE ON UPDATE CASCADE,

    CONSTRAINT `fk_maintenance_media_media_corp`
        FOREIGN KEY (`media_no`, `corp_no`)
        REFERENCES `media` (`no`, `corp_no`)
        ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='유지보수 기록별 영상 연결';


-- =========================================================
-- 13. 결함 조치 및 후속 조치 이력
-- 결함별로 조치 이력을 여러 건 저장할 수 있습니다.
-- 상태값은 발견, 조치 필요, 조치 중, 조치 완료를 포함합니다.
-- 후속 조치의 날짜, 담당자, 내용 및 결과를 관리합니다.
-- =========================================================

CREATE TABLE `defect_actions` (
    `no` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'PK',
    `id` VARCHAR(64) NOT NULL COMMENT '고유 식별자',

    `defect_no` BIGINT NOT NULL COMMENT '대상 결함 번호',
    `corp_no` BIGINT NOT NULL COMMENT '조치 기록 소유 기업 번호',
    `maintenance_no` BIGINT DEFAULT NULL COMMENT '연결된 유지보수 기록 번호',

    `status` ENUM('발견', '조치 필요', '조치 중', '조치 완료')
        NOT NULL DEFAULT '발견' COMMENT '결함 처리 상태',

    `action_date` DATE DEFAULT NULL COMMENT '조치일',
    `manager_no` BIGINT NOT NULL COMMENT '조치 담당자 번호',
    `action_description` TEXT DEFAULT NULL COMMENT '조치 내용',
    `result` TEXT DEFAULT NULL COMMENT '조치 결과',
    `completed_at` DATETIME DEFAULT NULL COMMENT '조치 완료 일시',

    `created_by` BIGINT NOT NULL COMMENT '이력 등록 사용자 번호',
    `updated_by` BIGINT DEFAULT NULL COMMENT '최종 수정 사용자 번호',

    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '등록일자',
    `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일자',

    PRIMARY KEY (`no`),
    UNIQUE KEY `uk_defect_actions_id` (`id`),
    UNIQUE KEY `uk_defect_actions_no_corp` (`no`, `corp_no`),

    KEY `idx_defect_actions_defect` (`defect_no`),
    KEY `idx_defect_actions_maintenance` (`maintenance_no`),
    KEY `idx_defect_actions_corp` (`corp_no`),
    KEY `idx_defect_actions_status` (`status`),
    KEY `idx_defect_actions_date` (`action_date`),
    KEY `idx_defect_actions_manager` (`manager_no`),

    CONSTRAINT `fk_defect_actions_defect_corp`
        FOREIGN KEY (`defect_no`, `corp_no`)
        REFERENCES `defects` (`no`, `corp_no`)
        ON DELETE RESTRICT ON UPDATE CASCADE,

    CONSTRAINT `fk_defect_actions_maintenance_corp`
        FOREIGN KEY (`maintenance_no`, `corp_no`)
        REFERENCES `maintenance_records` (`no`, `corp_no`)
        ON DELETE RESTRICT ON UPDATE CASCADE,

    CONSTRAINT `fk_defect_actions_manager_corp`
        FOREIGN KEY (`manager_no`, `corp_no`)
        REFERENCES `users` (`no`, `corp_no`)
        ON DELETE RESTRICT ON UPDATE CASCADE,

    CONSTRAINT `fk_defect_actions_created_by_corp`
        FOREIGN KEY (`created_by`, `corp_no`)
        REFERENCES `users` (`no`, `corp_no`)
        ON DELETE RESTRICT ON UPDATE CASCADE,

    CONSTRAINT `fk_defect_actions_updated_by_corp`
        FOREIGN KEY (`updated_by`, `corp_no`)
        REFERENCES `users` (`no`, `corp_no`)
        ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='결함 조치 및 후속 조치 이력';


-- =========================================================
-- 14. 결함 조치 상태 변경 이력
-- 목적: 결함 조치 상태의 변경 전후 값과 변경 담당자 기록
-- =========================================================
CREATE TABLE `defect_history` (
    `no` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'PK',

    `defect_action_no` BIGINT NOT NULL COMMENT '결함 조치 기록 번호',
    `corp_no` BIGINT NOT NULL COMMENT '상태 변경 이력 소유 기업 번호',

    `previous_status` ENUM('발견', '조치 필요', '조치 중', '조치 완료')
        DEFAULT NULL COMMENT '변경 전 상태',

    `new_status` ENUM('발견', '조치 필요', '조치 중', '조치 완료')
        NOT NULL COMMENT '변경 후 상태',

    `changed_by` BIGINT NOT NULL COMMENT '상태 변경 사용자 번호',
    `change_reason` TEXT DEFAULT NULL COMMENT '상태 변경 사유',
    `changed_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '상태 변경 일시',

    PRIMARY KEY (`no`),
    KEY `idx_action_history_action_date` (`defect_action_no`, `changed_at`),
    KEY `idx_action_history_changed_by` (`changed_by`),

    CONSTRAINT `fk_action_history_action_corp`
        FOREIGN KEY (`defect_action_no`, `corp_no`)
        REFERENCES `defect_actions` (`no`, `corp_no`)
        ON DELETE RESTRICT ON UPDATE CASCADE,

    CONSTRAINT `fk_action_history_user_corp`
        FOREIGN KEY (`changed_by`, `corp_no`)
        REFERENCES `users` (`no`, `corp_no`)
        ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='결함 조치 상태 변경 이력';


-- =========================================================
-- 15. 외래키 검사 복원
-- =========================================================

SET FOREIGN_KEY_CHECKS = 1;

-- =========================================================
-- DDL 종료
--
-- 구현 시 주의사항
-- 1. 대표 이미지 지정은 한 교량에 is_main=1인 이미지가
--    하나만 존재하도록 애플리케이션에서 검증해야 합니다.
-- 2. 영상 삭제 시 DB 레코드와 실제 파일 삭제를 함께
--    처리하는 트랜잭션/파일 정리 절차가 필요합니다.
-- 3. 관리자 권한 검수 및 위험도/보수 완료 태그는
--    해당 점검, 결함, 조치 기록의 상태와 결과를 바탕으로
--    관리합니다.
-- 4. 기업 인증 성공 여부는 국세청 API 응답을 검증한 뒤
--    bis_verified에 반영합니다.
-- 5. 업무 기록 조회 및 수정 시 로그인 사용자의 corp_no를
--    기준으로 권한을 검사해야 합니다.
-- 6. 교량 신규 등록은 bridge_master에 존재하는 교량을
--    선택하는 방식으로만 허용해야 합니다.
-- 7. created_by는 최초 등록 이후 변경하지 않으며,
--    updated_by는 수정할 때마다 실제 로그인 사용자의
--    users.no로 갱신해야 합니다.
-- =========================================================
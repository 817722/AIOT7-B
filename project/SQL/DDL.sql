-- Active: 1792462748325@@127.0.0.1@3306@project
-- =========================================================
-- 드론 영상 기반 교량 지원 업무 시스템
-- =========================================================

CREATE DATABASE IF NOT EXISTS `project`
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE `project`;

SET FOREIGN_KEY_CHECKS = 0;

-- ---------------------------------------------------------
-- 기존 테이블 삭제
-- 자식 테이블부터 삭제하여 외래키 충돌을 방지합니다.
-- ※ 중요 ※
-- 반드시 DROP TABLE IF EXISTS `defect_history`는 
-- DROP TABLE IF EXISTS `defect_actions`; 위에 있어야 합니다.
-- ---------------------------------------------------------

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
DROP TABLE IF EXISTS `corps`;

-- =========================================================
-- 1. 기업 정보
-- =========================================================

CREATE TABLE `corps` (
    `no` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'PK',
    `bis_no` VARCHAR(20) NOT NULL COMMENT '사업자등록번호',
    `ceo_name` VARCHAR(100) NOT NULL COMMENT '대표자명',
    `opened_at` DATE NOT NULL COMMENT '개업일자',
    `phone` VARCHAR(30) DEFAULT NULL COMMENT '기업 전화번호',
    `email` VARCHAR(254) DEFAULT NULL COMMENT '기업 이메일',
    `bis_verified` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '기업 인증 완료 여부',
    `verified_at` DATETIME DEFAULT NULL COMMENT '기업 인증 일시',
    `email_verified` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '기업 이메일 인증 여부',

    `created_at` TIMESTAMP 
        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '등록일자',
    `updated_at` TIMESTAMP 
        NOT NULL DEFAULT CURRENT_TIMESTAMP 
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
-- =========================================================

CREATE TABLE `users` (
    `no` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'PK',
    `id` VARCHAR(64) NOT NULL COMMENT '고유 식별자',
    `corp_no` BIGINT NOT NULL COMMENT '소속 기업 번호',
    `username` VARCHAR(100) NOT NULL COMMENT '로그인 ID',
    `password` VARCHAR(255) NOT NULL COMMENT '비밀번호 해시',
    `name` VARCHAR(100) NOT NULL COMMENT '작업자명',
    `phone` VARCHAR(100) NOT NULL COMMENT '작업자 연락망',
    `email` VARCHAR(254) DEFAULT NULL COMMENT '이메일',
    `enabled` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '사용 여부',

    `created_at` TIMESTAMP 
    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '등록일자',
    `updated_at` TIMESTAMP 
    NOT NULL DEFAULT CURRENT_TIMESTAMP 
    ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일자',

    PRIMARY KEY (`no`),
    UNIQUE KEY `uk_users_id` (`id`),
    UNIQUE KEY `uk_users_username` (`username`),
    KEY `idx_users_corp` (`corp_no`),
    KEY `idx_users_name` (`name`),

    CONSTRAINT `fk_users_corp` FOREIGN KEY (`corp_no`) REFERENCES `corps` (`no`)
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

    `created_at` TIMESTAMP NOT NULL
        DEFAULT CURRENT_TIMESTAMP COMMENT '등록일자',
    `updated_at` TIMESTAMP NOT NULL
        DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일자',

    PRIMARY KEY (`no`),
    UNIQUE KEY `uk_user_auth_user_role` (`user_no`, `auth`),
    KEY `idx_user_auth_role` (`auth`),

    CONSTRAINT `fk_user_auth_user`
        FOREIGN KEY (`user_no`) REFERENCES `users` (`no`)
        ON DELETE CASCADE
        ON UPDATE CASCADE
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='사용자별 권한';


-- =========================================================
-- 4. 공통 미디어
-- 파일 경로와 원본 파일명을 저장합니다.
-- 교량 이미지, 점검 영상, 유지보수 영상 등은
-- 업무별 연결 테이블을 통해 연결합니다.
-- =========================================================

CREATE TABLE `media` (
    `no` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'PK',
    `id` VARCHAR(64) NOT NULL COMMENT '시스템 고유 식별자',
    `corp_no` BIGINT DEFAULT NULL
        COMMENT '업로드 기업 번호(NULL=공통 미디어)',

    `type` ENUM('IMAGE', 'VIDEO') NOT NULL
        COMMENT '미디어 유형',

    `path` VARCHAR(1000) NOT NULL
        COMMENT '저장 파일 경로',

    `origin_name` VARCHAR(255) NOT NULL
        COMMENT '업로드 원본 파일명',

    `mime_type` VARCHAR(100) DEFAULT NULL
        COMMENT '파일 MIME 유형',

    `file_size` BIGINT DEFAULT NULL
        COMMENT '파일 크기(Byte)',

    `uploaded_by` BIGINT NOT NULL
        COMMENT '업로드 사용자 번호',

    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        COMMENT '업로드 일시',

    PRIMARY KEY (`no`),
    UNIQUE KEY `uk_media_id` (`id`),

    KEY `idx_media_type` (`type`),
    KEY `idx_media_uploader` (`uploaded_by`),
    KEY `idx_media_corp` (`corp_no`),

    CONSTRAINT `fk_media_corp`
        FOREIGN KEY (`corp_no`) REFERENCES `corps` (`no`)
        ON DELETE RESTRICT ON UPDATE CASCADE,

    CONSTRAINT `fk_media_uploader`
        FOREIGN KEY (`uploaded_by`) REFERENCES `users` (`no`)
        ON DELETE RESTRICT ON UPDATE CASCADE,

    CONSTRAINT `chk_media_file_size`
        CHECK (`file_size` IS NULL OR `file_size` >= 0)

) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='업로드 이미지 및 영상 파일 정보';
  

-- =========================================================
-- 5. 교량 정보
-- 전국교량표준데이터 CSV에서 가져온 교량 정보와
-- 시스템에서 추가/수정한 정보를 함께 관리합니다.
-- 지역 필터를 위해 행정구역을 구분합니다.
-- =========================================================

CREATE TABLE `bridges` (
    `no` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'PK',
    `id` VARCHAR(64) NOT NULL COMMENT '시스템 고유 식별자',
    `source_data_date` DATE DEFAULT NULL COMMENT 'CSV 데이터 기준일자',

    `name` VARCHAR(48) NOT NULL COMMENT '교량명',
    `facility_type` ENUM('제1종', '제2종', '제3종') DEFAULT NULL COMMENT '시설물종별등급구분',

    `road_type` VARCHAR(14) DEFAULT NULL COMMENT '도로종류',
    `road_name` VARCHAR(21) DEFAULT NULL COMMENT '도로노선명',
    `road_number` VARCHAR(4) DEFAULT NULL COMMENT '도로노선번호',
    `road_direction` CHAR(2) DEFAULT NULL COMMENT '도로노선방향',

    `province` VARCHAR(14) DEFAULT NULL COMMENT '시도명',
    `city` VARCHAR(17) DEFAULT NULL COMMENT '시군구명',
    `city_code` CHAR(5) DEFAULT NULL COMMENT '시군구코드',
    `address` VARCHAR(500) DEFAULT NULL COMMENT '소재지지번주소',

    `start_latitude` DECIMAL(9,6) DEFAULT NULL
        COMMENT '교량 시작점 위도',
    `start_longitude` DECIMAL(10,6) DEFAULT NULL
        COMMENT '교량 시작점 경도',
    `end_latitude` DECIMAL(9,6) DEFAULT NULL
        COMMENT '교량 종료점 위도',
    `end_longitude` DECIMAL(10,6) DEFAULT NULL
        COMMENT '교량 종료점 경도',

    `bridge_length` DECIMAL(10,2) DEFAULT NULL
        COMMENT '교량연장(m)',
    `bridge_width` DECIMAL(8,2) DEFAULT NULL
        COMMENT '교량폭(m)',
    `sidewalk_width` DECIMAL(8,2) DEFAULT NULL
        COMMENT '교량보도폭(m)',
    `bridge_height` DECIMAL(8,2) DEFAULT NULL
        COMMENT '교량높이(m)',
    `lane_count` TINYINT UNSIGNED DEFAULT NULL
        COMMENT '차로수',
    `divided_traffic` CHAR(1) DEFAULT NULL
        COMMENT '상하행선분리여부',
    `superstructure_type` VARCHAR(16) DEFAULT NULL
        COMMENT '상부구조형식',
    `completion_year` CHAR(4) DEFAULT NULL
        COMMENT '교량준공연도(YYYY)',

    `last_inspection_date` DATE DEFAULT NULL
        COMMENT '최종안전점검일자',
    `last_inspection_result` VARCHAR(6) DEFAULT NULL
        COMMENT '최종안전점검결과',
    `last_inspection_type` CHAR(8) DEFAULT NULL
        COMMENT '최종안전점검유형',

    `seismic_design_applied` VARCHAR(6) DEFAULT NULL
        COMMENT '내진설계적용여부',
    `seismic_performance_secured` CHAR(1) DEFAULT NULL
        COMMENT '내진성능확보여부',

    `repair_history` VARCHAR(328) DEFAULT NULL
        COMMENT 'CSV 교량보수보강내역',
    `repair_cost` DECIMAL(11,2) DEFAULT NULL
        COMMENT '교량보수보강비용(천원)',

    `design_live_load` VARCHAR(15) DEFAULT NULL
        COMMENT '설계활하중',
    `allowable_traffic_load` CHAR(4) DEFAULT NULL
        COMMENT '허용통행하중(톤)',
    `underpass_height_limit` VARCHAR(4) DEFAULT NULL
        COMMENT '하부통과제한높이(m)',

    `management_agency` VARCHAR(35) DEFAULT NULL
        COMMENT '관리기관명',
    `management_phone` VARCHAR(13) DEFAULT NULL
        COMMENT '관리기관전화번호',

    `manager_no` BIGINT DEFAULT NULL COMMENT '현재 담당자 번호',
    `created_by` BIGINT NOT NULL COMMENT '등록 사용자 번호',
    `updated_by` BIGINT DEFAULT NULL COMMENT '최종 수정 사용자 번호',

    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        COMMENT '시스템 등록일시',
    `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP
        COMMENT '시스템 수정일시',

    PRIMARY KEY (`no`),
    UNIQUE KEY `uk_bridges_id` (`id`),

    KEY `idx_bridges_name` (`name`),
    KEY `idx_bridges_region`
        (`province`, `city`),
    KEY `idx_bridges_city_code` (`city_code`),
    KEY `idx_bridges_facility_type` (`facility_type`),
    KEY `idx_bridges_manager` (`manager_no`),
    KEY `idx_bridges_last_inspection`
        (`last_inspection_date`),

    CONSTRAINT `fk_bridges_manager`
        FOREIGN KEY (`manager_no`) REFERENCES `users` (`no`)
        ON DELETE SET NULL ON UPDATE CASCADE,

    CONSTRAINT `fk_bridges_created_by`
        FOREIGN KEY (`created_by`) REFERENCES `users` (`no`)
        ON DELETE RESTRICT ON UPDATE CASCADE,

    CONSTRAINT `fk_bridges_updated_by`
        FOREIGN KEY (`updated_by`) REFERENCES `users` (`no`)
        ON DELETE SET NULL ON UPDATE CASCADE,

    CONSTRAINT `chk_bridges_start_latitude`
        CHECK (`start_latitude` IS NULL
               OR `start_latitude` BETWEEN -90 AND 90),
    CONSTRAINT `chk_bridges_start_longitude`
        CHECK (`start_longitude` IS NULL
               OR `start_longitude` BETWEEN -180 AND 180),
    CONSTRAINT `chk_bridges_end_latitude`
        CHECK (`end_latitude` IS NULL
               OR `end_latitude` BETWEEN -90 AND 90),
    CONSTRAINT `chk_bridges_end_longitude`
        CHECK (`end_longitude` IS NULL
               OR `end_longitude` BETWEEN -180 AND 180),

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
  COMMENT='CSV 기반 교량 기본정보 및 제원';


-- =========================================================
-- 6. 교량 대표 이미지 연결
-- 교량과 미디어의 관계를 별도 관리합니다.
-- is_main=1인 이미지가 대표 이미지입니다.
-- =========================================================

CREATE TABLE `bridge_media` (
    `no` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'PK',
    `bridge_no` BIGINT NOT NULL COMMENT '교량 번호',
    `media_no` BIGINT NOT NULL COMMENT '이미지 미디어 번호',
    `is_main` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '대표 이미지 여부',
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        COMMENT '등록일자',

    PRIMARY KEY (`no`),
    UNIQUE KEY `uk_bridge_media` (`bridge_no`, `media_no`),
    KEY `idx_bridge_media_media` (`media_no`),

    CONSTRAINT `fk_bridge_media_bridge`
        FOREIGN KEY (`bridge_no`) REFERENCES `bridges` (`no`)
        ON DELETE CASCADE ON UPDATE CASCADE,

    CONSTRAINT `fk_bridge_media_media`
        FOREIGN KEY (`media_no`) REFERENCES `media` (`no`)
        ON DELETE RESTRICT ON UPDATE CASCADE,

    CONSTRAINT `chk_bridge_media_main`
        CHECK (`is_main` IN (0, 1))
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='교량 대표 이미지 연결';

-- =========================================================
-- 7. 교량 점검 기록
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
        NOT NULL DEFAULT '진행 대기'
        COMMENT '점검 진행 상태',

    `inspection_date` DATE NOT NULL
        COMMENT '점검 날짜',

    `inspector_no` BIGINT NOT NULL
        COMMENT '점검 담당자 번호',

    `overall_result` TEXT DEFAULT NULL
        COMMENT '종합 점검 결과',

    `review_opinion` TEXT DEFAULT NULL
        COMMENT '검토 의견',

    `completed_at` DATETIME DEFAULT NULL
        COMMENT '점검 완료 일시',

    `created_by` BIGINT NOT NULL
        COMMENT '기록 생성 사용자 번호',

    `created_at` TIMESTAMP NOT NULL
        DEFAULT CURRENT_TIMESTAMP COMMENT '등록일시',

    `updated_at` TIMESTAMP NOT NULL
        DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시',

    PRIMARY KEY (`no`),
    UNIQUE KEY `uk_inspections_id` (`id`),

    KEY `idx_inspections_bridge` (`bridge_no`),
    KEY `idx_inspections_corp` (`corp_no`),
    KEY `idx_inspections_status` (`status`),
    KEY `idx_inspections_date` (`inspection_date`),
    KEY `idx_inspections_inspector` (`inspector_no`),
    KEY `idx_inspections_bridge_date`
        (`bridge_no`, `inspection_date`),

    CONSTRAINT `fk_inspections_bridge`
        FOREIGN KEY (`bridge_no`) REFERENCES `bridges` (`no`)
        ON DELETE RESTRICT ON UPDATE CASCADE,

    CONSTRAINT `fk_inspections_corp`
        FOREIGN KEY (`corp_no`) REFERENCES `corps` (`no`)
        ON DELETE RESTRICT ON UPDATE CASCADE,

    CONSTRAINT `fk_inspections_inspector`
        FOREIGN KEY (`inspector_no`) REFERENCES `users` (`no`)
        ON DELETE RESTRICT ON UPDATE CASCADE,

    CONSTRAINT `fk_inspections_created_by`
        FOREIGN KEY (`created_by`) REFERENCES `users` (`no`)
        ON DELETE RESTRICT ON UPDATE CASCADE

) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='교량 점검 기록';


-- =========================================================
-- 8. 점검 영상 연결
-- 하나의 점검 기록에 여러 영상 파일을 연결할 수 있습니다.
-- =========================================================

CREATE TABLE `inspection_media` (
    `no` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'PK',
    `inspection_no` BIGINT NOT NULL COMMENT '점검 기록 번호',
    `media_no` BIGINT NOT NULL COMMENT '영상 미디어 번호',
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        COMMENT '등록일자',

    PRIMARY KEY (`no`),
    UNIQUE KEY `uk_inspection_media` (`inspection_no`, `media_no`),
    KEY `idx_inspection_media_media` (`media_no`),

    CONSTRAINT `fk_inspection_media_inspection`
        FOREIGN KEY (`inspection_no`) REFERENCES `inspections` (`no`)
        ON DELETE CASCADE ON UPDATE CASCADE,

    CONSTRAINT `fk_inspection_media_media`
        FOREIGN KEY (`media_no`) REFERENCES `media` (`no`)
        ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='점검 기록별 영상 연결';


-- =========================================================
-- 9. 결함 정보
-- =========================================================


CREATE TABLE `defects` (
    `no` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'PK',
    `id` VARCHAR(64) NOT NULL COMMENT '시스템 고유 식별자',

    `inspection_no` BIGINT NOT NULL
        COMMENT '결함이 발견된 점검 기록 번호',

    `defect_type` VARCHAR(100) NOT NULL
        COMMENT '결함 유형',

    `location` VARCHAR(500) NOT NULL
        COMMENT '교량 내 결함 위치',

    `severity` ENUM('낮음', '보통', '높음', '긴급')
        DEFAULT NULL COMMENT '결함 심각도(분류 기준 확인 필요)',

    `description` TEXT DEFAULT NULL
        COMMENT '결함 상세 설명',

    `created_by` BIGINT NOT NULL
        COMMENT '등록 사용자 번호',

    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        COMMENT '등록일시',

    `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP
        COMMENT '수정일시',

    PRIMARY KEY (`no`),
    UNIQUE KEY `uk_defects_id` (`id`),

    KEY `idx_defects_inspection` (`inspection_no`),
    KEY `idx_defects_type` (`defect_type`),
    KEY `idx_defects_severity` (`severity`),

    CONSTRAINT `fk_defects_inspection`
        FOREIGN KEY (`inspection_no`)
        REFERENCES `inspections` (`no`)
        ON DELETE RESTRICT ON UPDATE CASCADE,

    CONSTRAINT `fk_defects_created_by`
        FOREIGN KEY (`created_by`)
        REFERENCES `users` (`no`)
        ON DELETE RESTRICT ON UPDATE CASCADE

) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='교량 점검에서 발견된 결함 정보';



-- =========================================================
-- 10. 유지보수 기록
-- =========================================================


CREATE TABLE `maintenance_records` (
    `no` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'PK',
    `id` VARCHAR(64) NOT NULL COMMENT '시스템 고유 식별자',

    `bridge_no` BIGINT NOT NULL COMMENT '유지보수 대상 교량 번호',
    `corp_no` BIGINT NOT NULL COMMENT '유지보수 기록 소유 기업 번호',

    `maintenance_type` VARCHAR(50) DEFAULT NULL
        COMMENT '유지보수 유형(분류 기준 확정 후 적용)',

    `status` ENUM('진행 대기', '진행 중', '완료')
        NOT NULL DEFAULT '진행 대기'
        COMMENT '유지보수 진행 상태',

    `maintenance_date` DATE DEFAULT NULL
        COMMENT '실제 유지보수 작업일',

    `manager_no` BIGINT NOT NULL
        COMMENT '유지보수 담당자 번호',

    `result` TEXT DEFAULT NULL
        COMMENT '유지보수 결과',

    `completed_at` DATETIME DEFAULT NULL
        COMMENT '완료 일시',

    `created_by` BIGINT NOT NULL
        COMMENT '기록 생성 사용자 번호',

    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        COMMENT '등록일시',

    `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP
        COMMENT '수정일시',

    PRIMARY KEY (`no`),
    UNIQUE KEY `uk_maintenance_records_id` (`id`),

    KEY `idx_maintenance_bridge` (`bridge_no`),
    KEY `idx_maintenance_corp` (`corp_no`),
    KEY `idx_maintenance_status` (`status`),
    KEY `idx_maintenance_date` (`maintenance_date`),
    KEY `idx_maintenance_manager` (`manager_no`),
    KEY `idx_maintenance_bridge_date`
        (`bridge_no`, `maintenance_date`),

    CONSTRAINT `fk_maintenance_bridge`
        FOREIGN KEY (`bridge_no`)
        REFERENCES `bridges` (`no`)
        ON DELETE RESTRICT ON UPDATE CASCADE,

    CONSTRAINT `fk_maintenance_corp`
        FOREIGN KEY (`corp_no`) REFERENCES `corps` (`no`)
        ON DELETE RESTRICT ON UPDATE CASCADE,

    CONSTRAINT `fk_maintenance_manager`
        FOREIGN KEY (`manager_no`)
        REFERENCES `users` (`no`)
        ON DELETE RESTRICT ON UPDATE CASCADE,

    CONSTRAINT `fk_maintenance_created_by`
        FOREIGN KEY (`created_by`)
        REFERENCES `users` (`no`)
        ON DELETE RESTRICT ON UPDATE CASCADE

) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='교량 유지보수 기록';



-- =========================================================
-- 11. 유지보수 영상 연결
-- 하나의 유지보수 기록에 여러 영상 파일을 연결할 수 있습니다.
-- =========================================================

CREATE TABLE `maintenance_media` (
    `no` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'PK',
    `maintenance_no` BIGINT NOT NULL COMMENT '유지보수 기록 번호',
    `media_no` BIGINT NOT NULL COMMENT '영상 미디어 번호',
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        COMMENT '등록일자',

    PRIMARY KEY (`no`),
    UNIQUE KEY `uk_maintenance_media`
        (`maintenance_no`, `media_no`),
    KEY `idx_maintenance_media_media` (`media_no`),

    CONSTRAINT `fk_maintenance_media_record`
        FOREIGN KEY (`maintenance_no`)
        REFERENCES `maintenance_records` (`no`)
        ON DELETE CASCADE ON UPDATE CASCADE,

    CONSTRAINT `fk_maintenance_media_media`
        FOREIGN KEY (`media_no`) REFERENCES `media` (`no`)
        ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='유지보수 기록별 영상 연결';


-- =========================================================
-- 12. 결함 조치 및 후속 조치 이력
-- 결함별로 조치 이력을 여러 건 저장할 수 있습니다.
-- 상태값은 발견, 조치 필요, 조치 중, 조치 완료를 포함합니다.
-- 후속 조치의 날짜, 담당자, 내용 및 결과를 관리합니다.
--
-- 교량 목록 및 후속 조치 목록은 이력과 교량/결함 관계를
-- 이용하여 조회할 수 있습니다.
-- =========================================================

CREATE TABLE `defect_actions` (
    `no` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'PK',
    `id` VARCHAR(64) NOT NULL COMMENT '고유 식별자',
    `defect_no` BIGINT NOT NULL COMMENT '대상 결함 번호',
    `maintenance_no` BIGINT DEFAULT NULL
        COMMENT '연결된 유지보수 기록 번호',
    `status` ENUM(
        '발견',
        '조치 필요',
        '조치 중',
        '조치 완료'
    ) NOT NULL DEFAULT '발견' COMMENT '결함 처리 상태',
    `action_date` DATE DEFAULT NULL COMMENT '조치일',
    `manager_no` BIGINT NOT NULL COMMENT '조치 담당자 번호',
    `action_description` TEXT DEFAULT NULL COMMENT '조치 내용',
    `result` TEXT DEFAULT NULL COMMENT '조치 결과',
    `completed_at` DATETIME DEFAULT NULL COMMENT '조치 완료 일시',
    `created_by` BIGINT NOT NULL COMMENT '이력 등록 사용자 번호',
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        COMMENT '등록일자',
    `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일자',

    PRIMARY KEY (`no`),
    UNIQUE KEY `uk_defect_actions_id` (`id`),
    KEY `idx_defect_actions_defect` (`defect_no`),
    KEY `idx_defect_actions_maintenance` (`maintenance_no`),
    KEY `idx_defect_actions_status` (`status`),
    KEY `idx_defect_actions_date` (`action_date`),
    KEY `idx_defect_actions_manager` (`manager_no`),

    CONSTRAINT `fk_defect_actions_defect`
        FOREIGN KEY (`defect_no`) REFERENCES `defects` (`no`)
        ON DELETE RESTRICT ON UPDATE CASCADE,

    CONSTRAINT `fk_defect_actions_maintenance`
        FOREIGN KEY (`maintenance_no`)
        REFERENCES `maintenance_records` (`no`)
        ON DELETE SET NULL ON UPDATE CASCADE,

    CONSTRAINT `fk_defect_actions_manager`
        FOREIGN KEY (`manager_no`) REFERENCES `users` (`no`)
        ON DELETE RESTRICT ON UPDATE CASCADE,

    CONSTRAINT `fk_defect_actions_created_by`
        FOREIGN KEY (`created_by`) REFERENCES `users` (`no`)
        ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='결함 조치 및 후속 조치 이력';

  
-- =========================================================
-- 13. 결함 조치 상태 변경 이력
-- 목적: 결함 조치 상태의 변경 전후 값과 변경 담당자 기록
-- =========================================================

CREATE TABLE `defect_history` (
    `no` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'PK',

    `defect_action_no` BIGINT NOT NULL
        COMMENT '결함 조치 기록 번호',

    `previous_status` ENUM(
        '발견',
        '조치 필요',
        '조치 중',
        '조치 완료'
    ) DEFAULT NULL COMMENT '변경 전 상태',

    `new_status` ENUM(
        '발견',
        '조치 필요',
        '조치 중',
        '조치 완료'
    ) NOT NULL COMMENT '변경 후 상태',

    `changed_by` BIGINT NOT NULL
        COMMENT '상태 변경 사용자 번호',

    `change_reason` TEXT DEFAULT NULL
        COMMENT '상태 변경 사유',

    `changed_at` TIMESTAMP NOT NULL
        DEFAULT CURRENT_TIMESTAMP COMMENT '상태 변경 일시',

    PRIMARY KEY (`no`),

    KEY `idx_action_history_action_date`
        (`defect_action_no`, `changed_at`),

    KEY `idx_action_history_changed_by`
        (`changed_by`),

    CONSTRAINT `fk_action_history_action`
        FOREIGN KEY (`defect_action_no`)
        REFERENCES `defect_actions` (`no`)
        ON DELETE RESTRICT ON UPDATE CASCADE,

    CONSTRAINT `fk_action_history_user`
        FOREIGN KEY (`changed_by`)
        REFERENCES `users` (`no`)
        ON DELETE RESTRICT ON UPDATE CASCADE

) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='결함 조치 상태 변경 이력';

-- ---------------------------------------------------------
-- 외래키 검사 복원
-- ---------------------------------------------------------

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
-- =========================================================
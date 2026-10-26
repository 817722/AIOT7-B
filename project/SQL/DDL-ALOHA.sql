-- Active: 1788828751938@@127.0.0.1@3306@project
-- =========================================================
-- 드론 영상 기반 교량 점검 업무 지원 시스템
-- MySQL 8.0.46 / 초기 구축용 DDL
-- =========================================================

CREATE DATABASE IF NOT EXISTS `project`
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE project;

SET FOREIGN_KEY_CHECKS = 0;


DROP TABLE IF EXISTS `defect_history`;
DROP TABLE IF EXISTS `defect_actions`;
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

-- ===========================
-- 1. 기업
-- ===========================
CREATE TABLE `corps` (
    `no` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'PK',
    `bis_no` VARCHAR(20) NOT NULL COMMENT '사업자등록번호',
    `ceo_name` VARCHAR(100) NOT NULL COMMENT '대표자명',
    `corp_name` VARCHAR(100) NOT NULL COMMENT '기업명',
    `opened_at` DATE NOT NULL COMMENT '개업일자',
    `bis_verified` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '기업 인증 완료 여부',
    `verified_at` DATETIME DEFAULT NULL COMMENT '기업 인증 일시',
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '등록일자',
    `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일자',

    PRIMARY KEY (`no`),
    UNIQUE KEY `uk_corps_bis_no` (`bis_no`),
    KEY `idx_corps_ceo` (`ceo_name`),

    CONSTRAINT `chk_corps_bis_verified`
        CHECK (`bis_verified` IN (0, 1))
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='기업 정보 및 인증 정보';


-- ==================
-- 2. 사용자
-- 담당자의 로그인 ID: username 
-- 외부 참조 및 식별용 고유 식별자: id
-- ==================
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


-- ==================
-- 3. 사용자 권한
-- SUPER_ADMIN : 웹 관리자
-- ADMIN : 기업
-- INSPECTOR : 담당자(=직원)
-- ==================
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


-- ===========================
-- 4. 전국교량표준데이터
-- .csv 데이터를 최초 1회 적재하여 보관
-- 이 테이블내의 데이터는 원본 csv가 바뀐게 아니라면
-- 절대로 수정하면 안됨.
-- ===========================

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


-- ===========================
-- 5. 업무용 교량
-- CSV 원본을 선택한 경우에만 등록
-- 기업별로 별도의 업무용 교량 레코드를 생성
--
-- [외래키 간소화]
-- 담당자(manager_no)/등록자(created_by)/수정자(updated_by)는
-- "사용자 존재 검증"과 "기업 소속 일치 검증"을 분리하여,
-- 복합 FK 대신 users.no를 참조하는 단일 FK만 둡니다.
-- 로그인 사용자 corp_no와 manager_no/created_by/updated_by가
-- 가리키는 사용자의 corp_no가 같은 기업인지는
-- 애플리케이션(서비스 레이어)에서 검증해야 합니다.
--
-- `uk_bridges_no_corp` (no, corp_no)는 inspections/maintenance_records가
-- "같은 기업의 교량인지"를 복합 FK로 강제하기 위해 계속 필요합니다.
-- ===========================

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

    CONSTRAINT `fk_bridges_manager`
        FOREIGN KEY (`manager_no`) REFERENCES `users` (`no`)
        ON DELETE RESTRICT ON UPDATE CASCADE,

    CONSTRAINT `fk_bridges_created_by`
        FOREIGN KEY (`created_by`) REFERENCES `users` (`no`)
        ON DELETE RESTRICT ON UPDATE CASCADE,

    CONSTRAINT `fk_bridges_updated_by`
        FOREIGN KEY (`updated_by`) REFERENCES `users` (`no`)
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


-- ===========================
-- 6. 미디어 (통합)
-- 교량 이미지, 점검 영상, 유지보수 영상을 연결 테이블 없이
-- media 테이블 하나로 관리합니다.
-- p_type + p_id로 부모 레코드(교량/점검/유지보수)를 식별하는
-- 다형(polymorphic) 연결 방식이며, 부모가 여러 테이블에
-- 걸쳐 있어 DB가 하나의 FK로 강제할 수 없으므로
-- p_id가 가리키는 레코드의 존재 및 기업(corp_no) 일치 여부는
-- 애플리케이션(서비스 레이어)에서 검증해야 합니다.
--
-- corp_no IS NULL인 공통 미디어는 업무 기록(p_type/p_id)에
-- 연결할 수 없습니다(연결하려면 corp_no가 반드시 있어야 함).
-- is_main(대표 이미지)은 p_type = 'BRIDGE'인 경우에만 유효하며,
-- 생성 컬럼(main_bridge_id)과 UNIQUE 제약으로 교량당 대표
-- 이미지 1건만 허용합니다.
--
-- [MySQL 제약] ON UPDATE CASCADE/SET NULL이 걸린 컬럼은 CHECK 제약에
-- 사용할 수 없습니다. corp_no는 chk_media_parent_corp에서 검사하므로
-- fk_media_corp는 ON UPDATE RESTRICT로 둡니다
-- (corps.no는 AUTO_INCREMENT PK라 사실상 변경되지 않음).
-- ===========================

CREATE TABLE `media` (
    `no` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'PK',
    `id` VARCHAR(64) NOT NULL COMMENT '시스템 고유 식별자',
    `corp_no` BIGINT DEFAULT NULL COMMENT '업로드 기업 번호(NULL=공통 미디어)',

    `p_type` ENUM('BRIDGE', 'INSPECTION', 'MAINTENANCE') DEFAULT NULL COMMENT '연결된 부모 테이블 구분(NULL=미연결 공통 파일)',
    `p_id` BIGINT DEFAULT NULL COMMENT '부모 레코드 no. p_type에 따라 bridges.no/inspections.no/maintenance_records.no를 의미(다형 연결, DB FK 미적용)',

    `type` ENUM('IMAGE', 'VIDEO') NOT NULL COMMENT '미디어 유형',
    `is_main` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '대표 이미지 여부(p_type=BRIDGE 전용)',
    `path` VARCHAR(1000) NOT NULL COMMENT '저장 파일 경로',
    `origin_name` VARCHAR(255) NOT NULL COMMENT '업로드 원본 파일명',
    `mime_type` VARCHAR(100) DEFAULT NULL COMMENT '파일 MIME 유형',
    `file_size` BIGINT DEFAULT NULL COMMENT '파일 크기(Byte)',
    `uploaded_by` BIGINT NOT NULL COMMENT '업로드 사용자 번호',

    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '업로드 일시',

    `main_bridge_id` BIGINT
        GENERATED ALWAYS AS (
            CASE
                WHEN `p_type` = 'BRIDGE' AND `is_main` = 1 THEN `p_id`
                ELSE NULL
            END
        ) STORED COMMENT '대표 이미지 중복 방지용 교량 번호',

    PRIMARY KEY (`no`),
    UNIQUE KEY `uk_media_id` (`id`),
    UNIQUE KEY `uk_media_single_main` (`main_bridge_id`),

    KEY `idx_media_parent` (`p_type`, `p_id`),
    KEY `idx_media_type` (`type`),
    KEY `idx_media_uploader` (`uploaded_by`),
    KEY `idx_media_corp` (`corp_no`),

    CONSTRAINT `fk_media_corp`
        FOREIGN KEY (`corp_no`) REFERENCES `corps` (`no`)
        ON DELETE RESTRICT ON UPDATE RESTRICT,

    CONSTRAINT `fk_media_uploader`
        FOREIGN KEY (`uploaded_by`) REFERENCES `users` (`no`)
        ON DELETE RESTRICT ON UPDATE CASCADE,

    CONSTRAINT `chk_media_file_size`
        CHECK (`file_size` IS NULL OR `file_size` >= 0),

    CONSTRAINT `chk_media_is_main`
        CHECK (`is_main` IN (0, 1)),

    CONSTRAINT `chk_media_parent_pair`
        CHECK ((`p_type` IS NULL AND `p_id` IS NULL)
            OR (`p_type` IS NOT NULL AND `p_id` IS NOT NULL)),

    CONSTRAINT `chk_media_parent_corp`
        CHECK (`p_type` IS NULL OR `corp_no` IS NOT NULL),

    CONSTRAINT `chk_media_main_scope`
        CHECK (`is_main` = 0 OR `p_type` = 'BRIDGE')
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='업로드 이미지 및 영상 파일 정보(부모 레코드 다형 연결)';


-- ===========================
-- 7. 교량 점검 기록
-- 점검 기록과 기업의 일치 여부를 복합 FK로 검증합니다.
--
-- bridge_no + corp_no: "같은 기업의 교량"이라는 업무 규칙을
-- 직접 강제하는 복합 FK이므로 유지합니다.
-- corp_no 단독 FK는 두지 않습니다: 위 복합 FK가 bridges.corp_no(→corps.no)를
-- 통해 corp_no 유효성을 이미 보장하므로 중복이기 때문입니다.
-- 점검 담당자/등록자/수정자는 사용자 존재만 단일 FK로 검증하며,
-- 기업 소속 일치 검증은 애플리케이션에서 수행합니다.
-- ===========================

CREATE TABLE `inspections` (
    `no` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'PK',
    `id` VARCHAR(64) NOT NULL COMMENT '고유 식별자',

    `bridge_no` BIGINT NOT NULL COMMENT '점검 대상 교량 번호',
    `corp_no` BIGINT NOT NULL COMMENT '점검 기록 소유 기업 번호',

    `title` VARCHAR(255) NOT NULL COMMENT '점검 기록명',
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

    `memo` TEXT DEFAULT NULL COMMENT '담당자 메모',
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

    CONSTRAINT `fk_inspections_inspector`
        FOREIGN KEY (`inspector_no`) REFERENCES `users` (`no`)
        ON DELETE RESTRICT ON UPDATE CASCADE,

    CONSTRAINT `fk_inspections_created_by`
        FOREIGN KEY (`created_by`) REFERENCES `users` (`no`)
        ON DELETE RESTRICT ON UPDATE CASCADE,

    CONSTRAINT `fk_inspections_updated_by`
        FOREIGN KEY (`updated_by`) REFERENCES `users` (`no`)
        ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='교량 점검 기록';

-- 점검 영상은 media 테이블에서 p_type='INSPECTION', p_id=inspections.no로
-- 연결합니다(별도의 inspection_media 연결 테이블 없음).


-- ===========================
-- 8. 결함 정보
-- inspection_no + corp_no: "같은 기업의 점검 기록"이라는 업무 규칙을
-- 직접 강제하는 복합 FK이므로 유지합니다.
-- 등록자/수정자는 사용자 존재만 단일 FK로 검증하며,
-- 기업 소속 일치 검증은 애플리케이션에서 수행합니다.
-- ===========================
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

    CONSTRAINT `fk_defects_created_by`
        FOREIGN KEY (`created_by`) REFERENCES `users` (`no`)
        ON DELETE RESTRICT ON UPDATE CASCADE,

    CONSTRAINT `fk_defects_updated_by`
        FOREIGN KEY (`updated_by`) REFERENCES `users` (`no`)
        ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='교량 점검에서 발견된 결함 정보';

-- 결함 관련 이미지/영상도 media 테이블에서
-- p_type='INSPECTION', p_id=inspections.no로 연결하여 조회합니다.


-- ===========================
-- 9. 유지보수 기록
-- bridge_no + corp_no: "같은 기업의 교량"이라는 업무 규칙을
-- 직접 강제하는 복합 FK이므로 유지합니다.
-- corp_no 단독 FK는 두지 않습니다: 위 복합 FK가 bridges.corp_no(→corps.no)를
-- 통해 corp_no 유효성을 이미 보장하므로 중복이기 때문입니다.
-- 담당자/등록자/수정자는 사용자 존재만 단일 FK로 검증하며,
-- 기업 소속 일치 검증은 애플리케이션에서 수행합니다.
-- ===========================
CREATE TABLE `maintenance_records` (
    `no` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'PK',
    `id` VARCHAR(64) NOT NULL COMMENT '시스템 고유 식별자',

    `bridge_no` BIGINT NOT NULL COMMENT '유지보수 대상 교량 번호',
    `corp_no` BIGINT NOT NULL COMMENT '유지보수 기록 소유 기업 번호',

    `title` VARCHAR(255) NOT NULL COMMENT '유지보수 기록명',
    `maintenance_type` VARCHAR(50) NOT NULL COMMENT '유지보수 분류',
    `memo` TEXT DEFAULT NULL COMMENT '담당자 메모',
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

    CONSTRAINT `fk_maintenance_manager`
        FOREIGN KEY (`manager_no`) REFERENCES `users` (`no`)
        ON DELETE RESTRICT ON UPDATE CASCADE,

    CONSTRAINT `fk_maintenance_created_by`
        FOREIGN KEY (`created_by`) REFERENCES `users` (`no`)
        ON DELETE RESTRICT ON UPDATE CASCADE,

    CONSTRAINT `fk_maintenance_updated_by`
        FOREIGN KEY (`updated_by`) REFERENCES `users` (`no`)
        ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='교량 유지보수 기록';

-- 유지보수 영상은 media 테이블에서 p_type='MAINTENANCE',
-- p_id=maintenance_records.no로 연결합니다
-- (별도의 maintenance_media 연결 테이블 없음).


-- ===========================
-- 10. 결함 조치 및 후속 조치 이력
-- 결함별로 조치 이력을 여러 건 저장할 수 있습니다.
-- 상태값은 발견, 조치 필요, 조치 중, 조치 완료를 포함합니다.
-- 후속 조치의 날짜, 담당자, 내용 및 결과를 관리합니다.
--
-- defect_no / maintenance_no + corp_no: 결함·유지보수 기록이
-- "같은 기업" 소속인지 보장하는 업무 규칙이므로 복합 FK를 유지합니다.
-- 조치 담당자/등록자/수정자는 사용자 존재만 단일 FK로 검증하며,
-- 기업 소속 일치 검증은 애플리케이션에서 수행합니다.
-- ===========================

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

    CONSTRAINT `fk_defect_actions_manager`
        FOREIGN KEY (`manager_no`) REFERENCES `users` (`no`)
        ON DELETE RESTRICT ON UPDATE CASCADE,

    CONSTRAINT `fk_defect_actions_created_by`
        FOREIGN KEY (`created_by`) REFERENCES `users` (`no`)
        ON DELETE RESTRICT ON UPDATE CASCADE,

    CONSTRAINT `fk_defect_actions_updated_by`
        FOREIGN KEY (`updated_by`) REFERENCES `users` (`no`)
        ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='결함 조치 및 후속 조치 이력';


-- ===========================
-- 11. 결함 조치 상태 변경 이력
-- 목적: 결함 조치 상태의 변경 전후 값과 변경 담당자 기록
-- 이력-조치기록, 이력-변경사용자 모두 기업 소속 일치를
-- 복합 FK로 강제하여 다른 기업 데이터 혼입을 방지합니다.
-- ===========================
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


-- ===========================
-- 12. 외래키 검사 복원
-- ===========================

SET FOREIGN_KEY_CHECKS = 1;

-- ===========================
-- DDL 종료
--
-- 구현 시 주의사항
-- 1. 대표 이미지 지정(media.is_main=1, p_type='BRIDGE')은
--    교량 1건당 1개만 존재하도록 main_bridge_id 생성 컬럼과
--    UNIQUE 제약으로 DB에서 강제하지만, is_main 해제 로직은
--    애플리케이션에서 트랜잭션으로 처리해야 합니다.
-- 2. 영상/이미지 삭제 시 media 레코드와 실제 파일 삭제를 함께
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
-- 8. [외래키 간소화] bridges/inspections/defects/maintenance_records/
--    defect_actions의 담당자·등록자·수정자(manager_no, inspector_no,
--    created_by, updated_by)는 users.no만 참조하는 단일 FK로
--    단순화했습니다. 로그인 사용자가 가리키는 사용자의 corp_no가
--    레코드의 corp_no와 같은 기업인지는 더 이상 DB가 보장하지
--    않으므로, 서비스 레이어(예: 공통 검증 유틸/AOP)에서 반드시
--    검증해야 합니다. 교량·점검·결함·유지보수의 "소유 기업" 자체를
--    보장하는 복합 FK(uk_*_no_corp 기반)는 실제 데이터 격리를 위해
--    그대로 유지됩니다.
-- 9. [미디어 통합] bridge_media/inspection_media/maintenance_media
--    연결 테이블을 제거하고, media.p_type + media.p_id로 부모
--    레코드(교량/점검/유지보수)를 가리키는 다형 연결 방식으로
--    통합했습니다. p_id는 여러 테이블에 걸쳐 있어 DB 단일 FK로
--    강제할 수 없으므로, 파일 업로드/연결 시 p_id가 가리키는
--    레코드가 실제 존재하고 media.corp_no와 같은 기업 소속인지
--    애플리케이션에서 검증해야 합니다. 또한 이 구조에서는 미디어
--    1건이 부모 레코드 1건에만 연결되며(기존 M:N 연결 테이블과
--    달리 동일 파일을 여러 부모에 공유할 수 없음), 공유가 필요하면
--    파일을 복제하거나 별도 연결 테이블을 다시 도입해야 합니다.
-- ===========================
-- =========================================================
-- 기업 상태 처리 이력 (웹 관리자 작업 이력)
-- - corps.status_at 은 "마지막" 처리 일시만 보관하므로,
--   승인/반려/정지/복구 일자를 모두 남기기 위해 이력 테이블을 추가합니다.
-- - 기존 테이블은 변경하지 않습니다. (CREATE TABLE IF NOT EXISTS 만 수행)
-- - 실행 전까지 애플리케이션은 이력 저장/표시를 건너뛰고 기존 기능은 정상 동작합니다.
-- =========================================================
USE `project`;

CREATE TABLE IF NOT EXISTS `corp_status_history` (
    `no` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'PK',
    `corp_no` BIGINT NOT NULL COMMENT '대상 기업 번호',
    `action` ENUM('APPROVE', 'REJECT', 'SUSPEND', 'RESTORE') NOT NULL COMMENT '처리 유형: 승인/반려/정지/정지 해제',
    `previous_status` ENUM('PENDING', 'ACTIVE', 'REJECTED', 'SUSPENDED') NOT NULL COMMENT '처리 전 기업 상태',
    `new_status` ENUM('PENDING', 'ACTIVE', 'REJECTED', 'SUSPENDED') NOT NULL COMMENT '처리 후 기업 상태',
    `reason` VARCHAR(500) DEFAULT NULL COMMENT '반려·정지 사유',
    `changed_by` BIGINT NOT NULL COMMENT '처리한 SUPER_ADMIN 사용자 번호',
    `changed_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '처리 일시',
    PRIMARY KEY (`no`),
    KEY `idx_corp_status_history_corp_date` (`corp_no`, `changed_at`),
    KEY `idx_corp_status_history_date` (`changed_at`),
    CONSTRAINT `fk_corp_status_history_corp` FOREIGN KEY (`corp_no`) REFERENCES `corps` (`no`) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT `fk_corp_status_history_changed_by` FOREIGN KEY (`changed_by`) REFERENCES `users` (`no`) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '기업 상태 처리 이력(웹 관리자 작업 이력)';

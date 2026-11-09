package com.aiotb.project.service.admin;

import java.util.Map;

/** 관리자 화면 표시용 상태/권한 라벨. DB에는 DDL에 정의된 원래 값(ENUM)만 저장한다. */
public final class AdminLabels {
    private AdminLabels() { }

    private static final Map<String, String> CORP_STATUS = Map.of(
            "PENDING", "승인 대기", "ACTIVE", "정상 이용", "REJECTED", "가입 반려", "SUSPENDED", "이용 정지");
    private static final Map<String, String> USER_STATUS = Map.of(
            "PENDING", "승인 대기", "ACTIVE", "활성", "REJECTED", "반려", "DISABLED", "비활성");
    private static final Map<String, String> ROLE = Map.of(
            "SUPER_ADMIN", "웹 관리자", "ADMIN", "기업 관리자", "INSPECTOR", "담당자");
    private static final Map<String, String> CORP_ACTION = Map.of(
            "APPROVE", "가입 승인", "REJECT", "가입 반려", "SUSPEND", "이용 정지", "RESTORE", "정지 해제");

    public static String corpStatus(String code) { return CORP_STATUS.getOrDefault(code, code); }
    public static String userStatus(String code) { return USER_STATUS.getOrDefault(code, code); }
    public static String role(String code) { return ROLE.getOrDefault(code, code == null ? "권한 없음" : code); }
    public static String corpAction(String code) { return CORP_ACTION.getOrDefault(code, code); }

    /** user_auth 목록(GROUP_CONCAT)에서 대표 권한 1개를 고른다. */
    public static String primaryRole(String auths) {
        if (auths == null) return null;
        if (auths.contains("SUPER_ADMIN")) return "SUPER_ADMIN";
        if (auths.contains("ADMIN")) return "ADMIN";
        if (auths.contains("INSPECTOR")) return "INSPECTOR";
        return null;
    }

    /** 사업자등록번호 표시 형식: 123-45-67890 */
    public static String bisNo(String raw) {
        if (raw == null || !raw.matches("\\d{10}")) return raw;
        return raw.substring(0, 3) + "-" + raw.substring(3, 5) + "-" + raw.substring(5);
    }
}

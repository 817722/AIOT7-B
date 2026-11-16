package com.aiotb.project.service.common;

import java.time.LocalDate;
import java.time.ZoneId;

/**
 * 점검 일자·유지보수 일자 공통 규칙 (서버 측 검증).
 * - 1900-01-01 ~ 오늘(한국 시간) 사이: 미래 날짜와 4자리를 벗어난 연도(예: 202612년)를 막는다
 */
public final class WorkDatePolicy {
    public static final ZoneId ZONE = ZoneId.of("Asia/Seoul");
    public static final LocalDate MIN_DATE = LocalDate.of(1900, 1, 1);

    private WorkDatePolicy() { }

    public static LocalDate today() {
        return LocalDate.now(ZONE);
    }

    /**
     * 날짜가 규칙에 맞지 않으면 사용자에게 보여 줄 안내 문구를, 맞으면 null 을 반환한다.
     * (서비스마다 사용하는 예외 종류가 달라 문구만 반환한다)
     *
     * @param label 화면에 표시되는 항목 이름 (예: "점검 일자")
     */
    public static String problem(LocalDate date, String label) {
        if (date == null) {
            return label + "를 입력해 주세요.";
        }
        LocalDate today = today();
        if (date.isAfter(today)) {
            return label + "는 오늘(" + today + ") 이후 날짜로 입력할 수 없습니다.";
        }
        if (date.isBefore(MIN_DATE)) {
            return label + "를 올바르게 입력해 주세요. (" + MIN_DATE + " 이후, YYYY-MM-DD 형식)";
        }
        return null;
    }

    /** 규칙에 맞지 않으면 IllegalArgumentException (REST API 의 400 응답으로 변환됨) */
    public static void require(LocalDate date, String label) {
        String problem = problem(date, label);
        if (problem != null) throw new IllegalArgumentException(problem);
    }
}

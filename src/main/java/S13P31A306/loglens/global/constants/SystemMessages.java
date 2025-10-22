package S13P31A306.loglens.global.constants;

import lombok.RequiredArgsConstructor;

/**
 * 시스템 공통 메시지 상수
 */
@RequiredArgsConstructor
public enum SystemMessages {
    // Utility 클래스 관련 메시지
    UTILITY_CLASS_ERROR("Utility class"),

    // 유효성 검증 메시지
    INVALID_INPUT("잘못된 입력입니다.");

    private final String message;

    public String message() {
        return this.message;
    }
}
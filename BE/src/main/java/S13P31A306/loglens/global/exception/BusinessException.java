package S13P31A306.loglens.global.exception;

import S13P31A306.loglens.global.constants.ErrorCode;
import lombok.Getter;

/**
 * 전역 공통 비즈니스 예외 처리 클래스. 서비스 로직 내에서 throw 시 프론트에 일관된 에러 응답을 전달한다.
 */
@Getter
public class BusinessException extends RuntimeException {

    // 프론트와 연동하기 위한 공통 에러코드
    private final ErrorCode errorCode;

    // 필드 에러, 파라미터 문제 등 상세 정보 (선택적)
    private final Object details;

    /**
     * 기본 생성자 - 상세 정보 없이 에러 코드만 전달
     */
    public BusinessException(final ErrorCode errorCode) {
        this(errorCode, null);
    }

    /**
     * 상세 정보 포함 생성자
     */
    public BusinessException(final ErrorCode errorCode, final Object details) {
        super(errorCode.getMessage(), null);
        this.errorCode = errorCode;
        this.details = details;
    }

    /**
     * 예외 체인 포함 생성자 - 원인 예외를 같이 전달하여 디버깅에 도움
     */
    public BusinessException(final ErrorCode errorCode, final Object details, final Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
        this.details = details;
    }

    /**
     * StackTrace 생략 - 비즈니스 예외에 대해 성능 최적화를 위해 스택 트레이스를 기록하지 않음
     */
    @Override
    public synchronized Throwable fillInStackTrace() {
        return this;
    }
}

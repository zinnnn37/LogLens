package S13P31A306.loglens.global.constants;

/**
 * 성공 응답 코드 인터페이스
 * <p>
 * {@link ResponseCode}를 상속하여 성공 응답에 특화된 인터페이스입니다.
 * 현재는 추가 메서드가 없지만, 향후 성공 전용 기능 추가 시 사용할 수 있습니다.
 * </p>
 */
public interface SuccessCode extends ResponseCode {
    // ResponseCode의 모든 메서드를 상속
    // 필요시 성공 전용 메서드 추가 가능
}

package S13P31A306.loglens.domain.alert.constants;

import S13P31A306.loglens.global.constants.SuccessCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 알림 도메인 성공 응답 코드
 */
@Getter
@RequiredArgsConstructor
public enum AlertSuccessCode implements SuccessCode {

    // Alert Config 관련 (200 OK)
    ALERT_CONFIG_RETRIEVED("AL200-1", "알림 설정을 성공적으로 조회했습니다.", HttpStatus.OK.value()),
    ALERT_CONFIG_UPDATED("AL200-2", "알림 설정을 성공적으로 수정했습니다.", HttpStatus.OK.value()),

    // Alert Config 생성 (201 CREATED)
    ALERT_CONFIG_CREATED("AL201-1", "알림 설정이 성공적으로 생성되었습니다.", HttpStatus.CREATED.value()),

    // Alert History 관련 (200 OK)
    ALERT_HISTORIES_RETRIEVED("AL200-3", "알림 이력을 성공적으로 조회했습니다.", HttpStatus.OK.value()),
    ALERT_MARKED_AS_READ("AL200-4", "알림을 읽음 처리했습니다.", HttpStatus.OK.value()),
    ALERT_UNREAD_COUNT_RETRIEVED("AL200-5", "읽지 않은 알림 개수를 성공적으로 조회했습니다.", HttpStatus.OK.value());

    private final String code;
    private final String message;
    private final int status;
}

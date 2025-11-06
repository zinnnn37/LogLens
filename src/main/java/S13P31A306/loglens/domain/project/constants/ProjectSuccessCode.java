package S13P31A306.loglens.domain.project.constants;

import S13P31A306.loglens.global.constants.SuccessCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ProjectSuccessCode implements SuccessCode {
    // 200 OK
    PROJECT_LIST_RETRIEVED("PJ200-1", "프로젝트 목록을 성공적으로 조회했습니다.", HttpStatus.OK.value()),
    PROJECT_RETRIEVED("PJ200-2", "프로젝트 상세 정보를 성공적으로 조회했습니다.", HttpStatus.OK.value()),
    PROJECT_DELETED("PJ200-3", "프로젝트를 성공적으로 삭제했습니다.", HttpStatus.OK.value()),
    MEMBER_DELETED("PJ200-4", "멤버를 성공적으로 삭제했습니다.", HttpStatus.OK.value()),
    PROJECT_CONNECTION_STATUS_RETRIEVED("PJ200-5", "프로젝트가 정상적으로 연결되었습니다.", HttpStatus.OK.value()),

    // 201 CREATED
    PROJECT_CREATED("PJ201-1", "프로젝트가 성공적으로 생성되었습니다.", HttpStatus.CREATED.value()),
    MEMBER_INVITED("PJ201-2", "멤버가 성공적으로 초대되었습니다.", HttpStatus.CREATED.value());
    private final String code;
    private final String message;
    private final int status;
}

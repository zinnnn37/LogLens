package S13P31A306.loglens.domain.project.constants;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ProjectErrorCode {

    // 400 Bad Request
    PROJECT_NAME_REQUIRED("PJ400-1", "프로젝트 이름은 필수입니다.", HttpStatus.BAD_REQUEST.value()),
    INVALID_PROJECT_NAME_LENGTH("PJ400-2", "프로젝트 이름은 2~100자 이내여야 합니다.", HttpStatus.BAD_REQUEST.value()),
    INVALID_DESCRIPTION_LENGTH("PJ400-3", "프로젝트 설명은 최대 500자까지 입력 가능합니다.", HttpStatus.BAD_REQUEST.value()),
    INVALID_PAGE_NUMBER("PJ400-5", "페이지 번호는 0 이상이어야 합니다.", HttpStatus.BAD_REQUEST.value()),
    INVALID_PAGE_SIZE("PJ400-6", "페이지 크기는 1~100 범위여야 합니다.", HttpStatus.BAD_REQUEST.value()),
    INVALID_PAGE_SORT("PJ400-7", "유효하지 않은 정렬 필드입니다.", HttpStatus.BAD_REQUEST.value()),
    INVALID_PAGE_ORDER("PJ400-8", "유효하지 않은 정렬 방향입니다.", HttpStatus.BAD_REQUEST.value()),
    INVALID_PROJECT_ID("PJ400-9", "유효하지 않은 프로젝트 ID입니다.", HttpStatus.BAD_REQUEST.value()),
    USER_ID_REQUIRED("PJ400-10", "사용자 ID는 필수입니다.", HttpStatus.BAD_REQUEST.value()),

    // 403 Forbidden
    ACCESS_FORBIDDEN("PJ403-1", "해당 프로젝트에 대한 접근 권한이 없습니다.", HttpStatus.FORBIDDEN.value()),
    PROJECT_DELETE_FORBIDDEN("PJ403-2", "프로젝트를 삭제할 권한이 없습니다.", HttpStatus.FORBIDDEN.value()),
    MEMBER_DELETE_FORBIDDEN("PJ403-3", "멤버를 삭제할 권한이 없습니다.", HttpStatus.FORBIDDEN.value()),
    CANNOT_DELETE_SELF("PJ403-4", "자기 자신을 프로젝트에서 삭제할 수 없습니다.", HttpStatus.FORBIDDEN.value()),

    // 404 Not Found
    PROJECT_NOT_FOUND("PJ404-1", "해당 프로젝트를 찾을 수 없습니다.", HttpStatus.NOT_FOUND.value()),
    USER_NOT_FOUND("PJ404-2", "해당 사용자를 찾을 수 없습니다.",  HttpStatus.NOT_FOUND.value()), // USER 자체가 없음
    MEMBER_NOT_FOUND("PJ404-3", "해당 멤버를 찾을 수 없습니다.", HttpStatus.NOT_FOUND.value()),  // 프로젝트에 해당 멤버가 존재하지 않음

    // 409 Conflict
    PROJECT_NAME_DUPLICATED("PJ409-1", "이미 사용 중인 프로젝트 이름입니다.", HttpStatus.CONFLICT.value()),
    MEMBER_EXISTS("PJ409-2", "이미 프로젝트 멤버입니다.", HttpStatus.CONFLICT.value());

    private final String code;
    private final String message;
    private final int status;

}

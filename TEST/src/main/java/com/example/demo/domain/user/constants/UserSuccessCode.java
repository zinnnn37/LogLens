package com.example.demo.domain.user.constants;

import com.example.demo.global.constants.SuccessCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum UserSuccessCode implements SuccessCode {

    USER_CREATED("U201", "사용자가 생성되었습니다", 201),
    USER_RETRIEVED("U200-1", "사용자 조회 성공", 200),
    USER_LIST_RETRIEVED("U200-2", "사용자 목록 조회 성공", 200),
    USER_UPDATED("U200-3", "사용자 정보가 수정되었습니다", 200),
    USER_DELETED("U204", "사용자가 삭제되었습니다", 204),
    PASSWORD_CHANGED("U200-4", "비밀번호가 변경되었습니다", 200);

    private final String code;
    private final String message;
    private final int status;
}

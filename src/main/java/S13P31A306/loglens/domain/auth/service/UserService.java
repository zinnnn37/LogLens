package S13P31A306.loglens.domain.auth.service;

import S13P31A306.loglens.domain.auth.dto.request.UserSignupRequest;
import S13P31A306.loglens.domain.auth.dto.response.EmailValidateResponse;
import S13P31A306.loglens.domain.auth.dto.response.UserSearchResponse;
import S13P31A306.loglens.domain.auth.dto.response.UserSignupResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * 사용자 인증 관련 비즈니스 로직을 처리하는 서비스
 */
public interface UserService {

    /**
     * 이메일 중복 확인
     *
     * @param email 확인할 이메일 주소
     * @return EmailValidateResponse 이메일 사용 가능 여부
     */
    EmailValidateResponse checkEmailAvailability(String email);

    /**
     * 회원가입
     *
     * @param request 회원가입 요청 DTO
     * @return UserSignupResponse 회원가입 응답 DTO
     */
    UserSignupResponse signup(UserSignupRequest request);

    /**
     * 이름으로 사용자 검색
     *
     * @param name     검색할 사용자 이름
     * @param pageable 페이지 정보
     * @return Page<UserSearchResponse> 사용자 검색 결과
     */
    Page<UserSearchResponse> findUsersByName(String name, Pageable pageable);
}

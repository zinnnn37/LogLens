package S13P31A306.loglens.domain.auth.controller.impl;

import S13P31A306.loglens.domain.auth.constants.UserSortField;
import S13P31A306.loglens.domain.auth.constants.UserSuccessCode;
import S13P31A306.loglens.domain.auth.controller.UserApi;
import S13P31A306.loglens.domain.auth.dto.request.UserSignupRequest;
import S13P31A306.loglens.domain.auth.dto.response.EmailValidateResponse;
import S13P31A306.loglens.domain.auth.dto.response.UserSearchResponse;
import S13P31A306.loglens.domain.auth.dto.response.UserSignupResponse;
import S13P31A306.loglens.domain.auth.service.UserService;
import S13P31A306.loglens.global.dto.response.ApiResponseFactory;
import S13P31A306.loglens.global.dto.response.BaseResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class UserController implements UserApi {

    private final UserService userService;

    /**
     * 회원가입
     */
    @Override
    @PostMapping("/users")
    public ResponseEntity<? extends BaseResponse> signup(@RequestBody UserSignupRequest request) {
        UserSignupResponse response = userService.signup(request);
        return ApiResponseFactory.success(UserSuccessCode.SIGNUP_SUCCESS, response);
    }

    /**
     * 이메일 중복 확인
     */
    @Override
    @GetMapping("/emails")
    public ResponseEntity<? extends BaseResponse> checkEmailAvailability(@RequestParam("email") String email) {
        EmailValidateResponse response = userService.checkEmailAvailability(email);

        if (response.available()) {
            return ApiResponseFactory.success(UserSuccessCode.EMAIL_AVAILABLE, response);
        } else {
            return ApiResponseFactory.success(UserSuccessCode.EMAIL_DUPLICATE, response);
        }
    }

    @Override
    @GetMapping("/users/search")
    public ResponseEntity<? extends BaseResponse> findUsersByName(String name, Integer page, Integer size, String sort, String order) {
        UserSortField sortField = UserSortField.valueOf(sort.toUpperCase());
        Sort.Direction direction = Sort.Direction.fromString(order.toUpperCase());
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortField.getFieldName()));

        Page<UserSearchResponse> response = userService.findUsersByName(name, pageable);
        return ApiResponseFactory.success(UserSuccessCode.USER_SEARCH_SUCCESS, response);
    }
}

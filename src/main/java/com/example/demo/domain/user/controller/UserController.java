package com.example.demo.domain.user.controller;

import com.example.demo.domain.user.dto.*;
import com.example.demo.domain.user.service.UserService;
import com.example.demo.domain.user.constants.UserSuccessCode;
import com.example.demo.global.dto.response.ApiResponseFactory;
import com.example.demo.global.dto.response.BaseResponse;
import com.example.demo.global.dto.response.SuccessResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping
    public ResponseEntity<SuccessResponse<UserResponse>> createUser(
            @Valid @RequestBody CreateUserRequest request) {

        log.info("사용자 생성 요청: email={}", request.email());
        UserResponse response = userService.createUser(request);
        return ApiResponseFactory.success(UserSuccessCode.USER_CREATED, response);
    }

    @GetMapping
    public ResponseEntity<SuccessResponse<UserListResponse>> getAllUsers() {

        log.info("전체 사용자 조회 요청");
        UserListResponse response = userService.getAllUsers();
        return ApiResponseFactory.success(UserSuccessCode.USER_LIST_RETRIEVED, response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<SuccessResponse<UserResponse>> getUserById(
            @PathVariable Long id) {

        log.info("사용자 조회 요청: id={}", id);
        UserResponse response = userService.getUserById(id);
        return ApiResponseFactory.success(UserSuccessCode.USER_RETRIEVED, response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<SuccessResponse<UserResponse>> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequest request) {

        log.info("사용자 수정 요청: id={}, name={}", id, request.name());
        UserResponse response = userService.updateUser(id, request);
        return ApiResponseFactory.success(UserSuccessCode.USER_UPDATED, response);
    }

    @PatchMapping("/{id}/password")
    public ResponseEntity<BaseResponse> changePassword(
            @PathVariable Long id,
            @Valid @RequestBody ChangePasswordRequest request) {

        log.info("비밀번호 변경 요청: id={}", id);
        userService.changePassword(id, request);
        return ApiResponseFactory.success(UserSuccessCode.PASSWORD_CHANGED);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<BaseResponse> deleteUser(@PathVariable Long id) {

        log.info("사용자 삭제 요청: id={}", id);
        userService.deleteUser(id);
        return ApiResponseFactory.success(UserSuccessCode.USER_DELETED);
    }
}

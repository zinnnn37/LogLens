package com.example.demo.domain.user.service.impl;

import com.example.demo.domain.user.constants.UserErrorCode;
import com.example.demo.domain.user.dto.*;
import com.example.demo.domain.user.entity.User;
import com.example.demo.domain.user.repository.UserJpaRepository;
import com.example.demo.domain.user.service.UserService;
import com.example.demo.global.exception.BusinessException;
import com.example.demo.global.service.AsyncNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final UserJpaRepository userRepository;
    private final AsyncNotificationService asyncNotificationService;
//    private final PasswordEncoder passwordEncoder; // 실제로는 추가 필요

    @Override
    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        // ✅ 비즈니스 로직만!
        validateEmailNotExists(request.email());

        String encodedPassword = encodePassword(request.password());

        User user = User.builder()
                .name(request.name())
                .email(request.email())
                .password(encodedPassword)
                .secret(request.secret())
                .build();

        User savedUser = userRepository.save(user);

        // 비동기 알림 예약
        asyncNotificationService.sendWelcomeNotificationAfterDelay(
                savedUser.getId(),
                savedUser.getEmail()
        );

        return UserResponse.from(savedUser);
    }


    @Override
    public UserListResponse getAllUsers() {
        List<User> users = userRepository.findAll();

        List<UserResponse> userResponses = users.stream()
                .map(UserResponse::from)
                .toList();

        log.debug("사용자 목록 조회 완료: count={}", users.size());

        return new UserListResponse(userResponses, users.size());
    }

    @Override
    public UserResponse getUserById(Long id) {
        User user = findUserByIdOrThrow(id);

        log.debug("사용자 조회 완료: id={}, email={}", user.getId(), user.getEmail());

        return UserResponse.from(user);
    }

    @Override
    @Transactional
    public UserResponse updateUser(Long id, UpdateUserRequest request) {
        // 1. 사용자 존재 확인
        User user = findUserByIdOrThrow(id);

        // 2. 이메일 변경 시 중복 체크
        String changedFields = "";
        if (!user.getEmail().equals(request.email())) {
            validateEmailNotExists(request.email());
            changedFields = "email";
        }

        if (!user.getName().equals(request.name())) {
            changedFields = changedFields.isEmpty() ? "name" : changedFields + ", name";
        }

        // 3. 정보 수정
        user.updateInfo(request.name(), request.email());

        log.debug("사용자 정보 수정 완료: id={}, name={}, email={}",
                user.getId(), user.getName(), user.getEmail());

//        // 4. 비동기 정보 변경 알림 예약 (1분 후 실행)
//        if (!changedFields.isEmpty()) {
//            asyncNotificationService.sendUpdateNotificationAfterDelay(
//                    user.getId(),
//                    user.getEmail(),
//                    changedFields
//            );
//            log.info("정보 변경 알림이 1분 후 전송 예약되었습니다: userId={}, fields={}",
//                    user.getId(), changedFields);
//        }

        return UserResponse.from(user);
    }

    @Override
    @Transactional
    public void changePassword(Long id, ChangePasswordRequest request) {
        // 1. 사용자 존재 확인
        User user = findUserByIdOrThrow(id);

        // 2. 기존 비밀번호 확인
        if (!passwordMatches(request.oldPassword(), user.getPassword())) {
            throw new BusinessException(UserErrorCode.INVALID_PASSWORD);
        }

        // 3. 새 비밀번호가 기존과 동일한지 확인
        if (request.oldPassword().equals(request.newPassword())) {
            throw new BusinessException(UserErrorCode.PASSWORD_SAME_AS_OLD);
        }

        // 4. 비밀번호 변경
        String encodedPassword = encodePassword(request.newPassword());
        user.updatePassword(encodedPassword);

        log.debug("비밀번호 변경 완료: id={}", user.getId());

        // 5. 비동기 보안 알림 예약 (1분 후 실행)
//        asyncNotificationService.sendPasswordChangeNotificationAfterDelay(
//                user.getId(),
//                user.getEmail()
//        );
        log.info("비밀번호 변경 알림이 1분 후 전송 예약되었습니다: userId={}", user.getId());
    }

    @Override
    @Transactional
    public void deleteUser(Long id) {
        // 1. 사용자 존재 확인
        User user = findUserByIdOrThrow(id);

        // 2. 삭제
        userRepository.delete(user);

        log.debug("사용자 삭제 완료: id={}, email={}", id, user.getEmail());

        // 3. 즉시 실행되는 비동기 작업 (예: 통계 업데이트, 로그 기록)
//        asyncNotificationService.sendImmediateNotification(id);
        log.info("사용자 삭제 후처리 작업이 비동기로 실행되었습니다: userId={}", id);
    }

    // ========== Private Helper Methods ==========

    private User findUserByIdOrThrow(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));
    }

    private void validateEmailNotExists(String email) {
        if (userRepository.existsByEmail(email)) {
            throw new BusinessException(UserErrorCode.EMAIL_ALREADY_EXISTS);
        }
    }

    private String encodePassword(String rawPassword) {
        // TODO: 실제로는 PasswordEncoder 사용
        return rawPassword; // 임시
    }

    private boolean passwordMatches(String rawPassword, String encodedPassword) {
        // TODO: 실제로는 PasswordEncoder.matches() 사용
        return rawPassword.equals(encodedPassword); // 임시
    }
}

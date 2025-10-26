package S13P31A306.loglens.domain.auth.service.impl;

import S13P31A306.loglens.domain.auth.dto.request.UserSignupRequest;
import S13P31A306.loglens.domain.auth.dto.response.EmailValidateResponse;
import S13P31A306.loglens.domain.auth.dto.response.UserSignupResponse;
import S13P31A306.loglens.domain.auth.entity.User;
import S13P31A306.loglens.domain.auth.mapper.UserMapper;
import S13P31A306.loglens.domain.auth.respository.UserRepository;
import S13P31A306.loglens.domain.auth.service.UserService;
import S13P31A306.loglens.domain.auth.validator.UserValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private static final String LOG_PREFIX = "[UserService]";

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final UserValidator userValidator;

    /**
     * 이메일 중복 확인
     *
     * @param email 확인할 이메일 주소
     * @return EmailValidateResponse 이메일 사용 가능 여부
     */
    @Override
    public EmailValidateResponse checkEmailAvailability(final String email) {
        log.debug("{} 이메일 중복 확인 요청: {}", LOG_PREFIX, email);
        boolean available = userValidator.isEmailAvailable(email);

        return new EmailValidateResponse(email, available);
    }

    /**
     * 회원가입
     *
     * @param request 회원가입 요청 DTO
     * @return UserSignupResponse 회원가입 응답 DTO
     * @throws IllegalArgumentException 비밀번호 불일치 또는 이메일 중복 시
     */
    @Override
    @Transactional
    public UserSignupResponse signup(final UserSignupRequest request) {
        log.info("{} 회원가입 요청: email={}, userName={}", LOG_PREFIX, request.email(), request.name());

        // 1. 유효성 검증
        userValidator.validatePasswordConfirmation(request.password(), request.passwordConfirm());
        userValidator.validateDuplicateEmail(request.email());
        log.debug("{}회원가입 유효성 검증 통과: {}", LOG_PREFIX, request.email());

        // 2. 비밀번호 암호화
        String encodedPassword = passwordEncoder.encode(request.password());
        log.debug("{}비밀번호 암호화 완료", LOG_PREFIX);

        // 3. User 엔티티 생성 및 저장
        User user = userMapper.toEntity(request, encodedPassword);
        User savedUser = userRepository.save(user);
        log.info("{} 회원가입 완료: userId={}, email={}", LOG_PREFIX, savedUser.getId(), savedUser.getEmail());

        // 4. 응답 DTO 생성 및 반환
        return userMapper.toSignupResponse(savedUser);
    }
}

package com.example.demo.domain.user.repository;

import com.example.demo.domain.user.entity.User;
import java.util.List;
import java.util.Optional;

/**
 * JDBC 기반 UserRepository 인터페이스
 *
 * 목적:
 * - JPA와 비교하기 위한 순수 JDBC 구현
 * - AOP 로깅 테스트 (TRACE 레벨)
 */
public interface UserJdbcRepository {

    /**
     * 사용자 저장 (생성 또는 수정)
     */
    User save(User user);

    /**
     * 모든 사용자 조회
     */
    List<User> findAll();

    /**
     * ID로 사용자 조회
     */
    Optional<User> findById(Long id);

    /**
     * 이메일로 사용자 조회
     */
    Optional<User> findByEmail(String email);

    /**
     * 이메일 존재 여부 확인
     */
    boolean existsByEmail(String email);

    /**
     * 사용자 삭제
     */
    void deleteById(Long id);

    /**
     * 사용자 수 카운트
     */
    int count();
}

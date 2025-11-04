package com.example.demo.domain.user.repository.impl;

import com.example.demo.domain.user.entity.User;
import com.example.demo.domain.user.repository.UserJdbcRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * JDBC 기반 UserRepository 구현체
 *
 * 목적:
 * 1. JPA가 아닌 순수 JDBC로 구현
 * 2. @Repository 어노테이션으로 AOP 로깅 테스트 (TRACE 레벨)
 * 3. 민감 정보(password, secret) 처리 확인
 *
 * 주의사항:
 * - AOP가 자동으로 메서드 시작/종료 로그를 남기므로
 * - 추가 로그는 비즈니스 의미가 있는 경우에만 사용
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class UserJdbcRepositoryImpl implements UserJdbcRepository {

    private final JdbcTemplate jdbcTemplate;

    // RowMapper: ResultSet을 User 객체로 변환
    private final RowMapper<User> userRowMapper = (rs, rowNum) ->
            User.builder()
                    .id(rs.getLong("id"))
                    .name(rs.getString("name"))
                    .email(rs.getString("email"))
                    .password(rs.getString("password"))
                    .secret(rs.getString("secret"))
                    .build();

    @Override
    public User save(User user) {
        if (user.getId() == null) {
            return insert(user);
        } else {
            return update(user);
        }
    }

    @Override
    public List<User> findAll() {
        String sql = """
            SELECT id, name, email, password, secret, created_at, updated_at 
            FROM users 
            ORDER BY id
            """;

        return jdbcTemplate.query(sql, userRowMapper);
    }

    @Override
    public Optional<User> findById(Long id) {
        String sql = """
            SELECT id, name, email, password, secret, created_at, updated_at 
            FROM users 
            WHERE id = ?
            """;

        try {
            User user = jdbcTemplate.queryForObject(sql, userRowMapper, id);
            return Optional.ofNullable(user);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<User> findByEmail(String email) {
        String sql = """
            SELECT id, name, email, password, secret, created_at, updated_at 
            FROM users 
            WHERE email = ?
            """;

        try {
            User user = jdbcTemplate.queryForObject(sql, userRowMapper, email);
            return Optional.ofNullable(user);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public boolean existsByEmail(String email) {
        String sql = "SELECT COUNT(*) FROM users WHERE email = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, email);
        return count != null && count > 0;
    }

    @Override
    public void deleteById(Long id) {
        String sql = "DELETE FROM users WHERE id = ?";
        int rowsAffected = jdbcTemplate.update(sql, id);

        // 삭제 실패는 비즈니스적으로 중요하므로 로그 남김
        if (rowsAffected == 0) {
            log.warn("삭제할 사용자를 찾을 수 없습니다. id={}", id);
        }
    }

    @Override
    public int count() {
        String sql = "SELECT COUNT(*) FROM users";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class);
        return count != null ? count : 0;
    }

    // ========== Private Helper Methods ==========

    /**
     * 새로운 사용자 INSERT
     */
    private User insert(User user) {
        String sql = """
            INSERT INTO users (name, email, password, secret, created_at, updated_at) 
            VALUES (?, ?, ?, ?, ?, ?)
            """;

        KeyHolder keyHolder = new GeneratedKeyHolder();
        LocalDateTime now = LocalDateTime.now();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, user.getName());
            ps.setString(2, user.getEmail());
            ps.setString(3, user.getPassword());
            ps.setString(4, user.getSecret());
            ps.setTimestamp(5, Timestamp.valueOf(now));
            ps.setTimestamp(6, Timestamp.valueOf(now));
            return ps;
        }, keyHolder);

        Long generatedId = keyHolder.getKey().longValue();

        // 생성된 ID로 새로운 User 객체 반환 (불변성 유지)
        return User.builder()
                .id(generatedId)
                .name(user.getName())
                .email(user.getEmail())
                .password(user.getPassword())
                .secret(user.getSecret())
                .build();
    }

    /**
     * 기존 사용자 UPDATE
     */
    private User update(User user) {
        String sql = """
            UPDATE users 
            SET name = ?, email = ?, password = ?, secret = ?, updated_at = ? 
            WHERE id = ?
            """;

        LocalDateTime now = LocalDateTime.now();

        int rowsAffected = jdbcTemplate.update(
                sql,
                user.getName(),
                user.getEmail(),
                user.getPassword(),
                user.getSecret(),
                Timestamp.valueOf(now),
                user.getId()
        );

        if (rowsAffected == 0) {
            log.warn("수정할 사용자를 찾을 수 없습니다. id={}", user.getId());
        }

        return user;
    }
}

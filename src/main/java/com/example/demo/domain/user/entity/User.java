package com.example.demo.domain.user.entity;

import a306.dependency_logger_starter.logging.annotation.ExcludeValue;
import a306.dependency_logger_starter.logging.annotation.Sensitive;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Sensitive
    @Column(nullable = false)
    private String password;

    @ExcludeValue
    @Column(length = 200)
    private String secret;

    @Builder
    public User(Long id, String name, String email, String password, String secret) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.password = password;
        this.secret = secret;
    }

    // 비즈니스 메서드
    public void updateInfo(String name, String email) {
        this.name = name;
        this.email = email;
    }

    public void updatePassword(String password) {
        this.password = password;
    }
}

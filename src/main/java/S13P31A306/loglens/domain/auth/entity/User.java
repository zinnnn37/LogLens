package S13P31A306.loglens.domain.auth.entity;

import S13P31A306.loglens.global.annotation.Sensitive;
import S13P31A306.loglens.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;

@Getter
@Entity
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(columnDefinition = "SMALLINT")
    private Integer id;

    @Column(name = "name", length = 50, nullable = false)
    private String name;

    @Email
    @Column(name = "email", length = 100, nullable = false, unique = true)
    private String email;

    @Sensitive
    @Column(name = "password", length = 64)
    private String password;

    @Builder
    public User(String name,
                String email,
                String password) {
        this.name = name;
        this.email = email;
        this.password = password;
    }
}

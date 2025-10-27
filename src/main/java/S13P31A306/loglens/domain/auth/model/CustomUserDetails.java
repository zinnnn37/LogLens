package S13P31A306.loglens.domain.auth.model;

import S13P31A306.loglens.domain.auth.entity.User;
import java.util.Collection;
import java.util.Collections;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;


@Getter
public class CustomUserDetails implements UserDetails {

    private final User user;      // 실제 User 엔티티
    private final Integer id;     // Spring Security에서 사용할 사용자 식별자 (PK)
    private final String email;   // username 대체용
    private final String password; // Spring Security에서 사용할 비밀번호

    public CustomUserDetails(final User user) {
        this.user = user;
        this.id = user.getId();
        this.email = user.getEmail();
        this.password = user.getPassword();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // 현재는 권한(Role) 기반 접근 제어를 사용하지 않으므로 빈 리스트 반환
        return Collections.emptyList();
    }

    @Override
    public String getPassword() {
        return this.password;
    }

    /**
     * Spring Security에서 username은 unique identifier로 사용됨. email을 username으로 대체.
     */
    @Override
    public String getUsername() {
        return this.email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true; // 계정 만료 안 함
    }

    @Override
    public boolean isAccountNonLocked() {
        return true; // 계정 잠금 안 함
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true; // 자격 증명(비밀번호) 만료 안 함
    }

    @Override
    public boolean isEnabled() {
        return true; // 기본적으로 활성화
    }

    // 추가 헬퍼 메서드
    public Integer getUserId() {
        return this.id;
    }

    public String getName() {
        return user.getName();
    }
}

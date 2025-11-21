package S13P31A306.loglens.domain.auth.respository;

import S13P31A306.loglens.domain.auth.entity.User;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Integer> {
    Optional<User> findByEmail(String email);

    Page<User> findByNameContaining(String name, Pageable pageable);

    boolean existsByEmail(String email);
}

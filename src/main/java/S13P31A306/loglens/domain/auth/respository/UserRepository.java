package S13P31A306.loglens.domain.auth.respository;

import S13P31A306.loglens.domain.auth.entity.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Integer> {
    Optional<User> findByEmail(String email);

    List<User> findByName(String name);

    boolean existsByEmail(String email);
}

package S13P31A306.loglens.domain.auth.respository.impl;

import S13P31A306.loglens.domain.auth.respository.AuthRepository;
import S13P31A306.loglens.global.annotation.Sensitive;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class AuthRepositoryImpl implements AuthRepository {

    private final RedisTemplate<String, String> redisTemplate;

    @Value("${jwt.refresh-token-validity-in-seconds}")
    private long refreshTokenValidityInSeconds;

    @Override
    public void saveRefreshToken(String email, @Sensitive String refreshToken) {
        redisTemplate.opsForValue().set(
                email,
                refreshToken,
                refreshTokenValidityInSeconds,
                TimeUnit.SECONDS
        );
    }

    @Override
    public Optional<String> findRefreshTokenByEmail(String email) {
        String refreshToken = redisTemplate.opsForValue().get(email);
        return Optional.ofNullable(refreshToken);
    }

    @Override
    public void deleteRefreshTokenByEmail(String email) {
        redisTemplate.delete(email);
    }
}

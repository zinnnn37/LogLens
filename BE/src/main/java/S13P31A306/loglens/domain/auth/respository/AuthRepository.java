package S13P31A306.loglens.domain.auth.respository;

import java.util.Optional;

public interface AuthRepository {

    /**
     * Refresh Token을 저장합니다.
     *
     * @param email        사용자의 email
     * @param refreshToken 저장할 Refresh Token
     */
    void saveRefreshToken(String email, String refreshToken);

    /**
     * 사용자 UUID를 기반으로 Refresh Token을 조회합니다.
     *
     * @param email 사용자의 email
     * @return Optional<String> Refresh Token
     */
    Optional<String> findRefreshTokenByEmail(String email);

    /**
     * Refresh Token을 삭제합니다.
     *
     * @param email 사용자의 email
     */
    void deleteRefreshTokenByEmail(String email);
}

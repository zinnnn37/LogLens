package S13P31A306.loglens.domain.auth.jwt;

import S13P31A306.loglens.global.annotation.Sensitive;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class Jwt {

    private Integer userId;

    private String email;

    private String tokenType;

    private Integer expiresIn;

    @Sensitive
    private String accessToken;

    @Sensitive
    private String refreshToken;
}

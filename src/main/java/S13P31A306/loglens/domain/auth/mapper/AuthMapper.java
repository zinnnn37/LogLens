package S13P31A306.loglens.domain.auth.mapper;

import S13P31A306.loglens.domain.auth.dto.response.TokenRefreshResponse;
import S13P31A306.loglens.domain.auth.dto.response.UserSigninResponse;
import S13P31A306.loglens.domain.auth.jwt.Jwt;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface AuthMapper {
    UserSigninResponse toUserSigninResponse(Jwt jwt);

    TokenRefreshResponse toTokenRefreshResponse(Jwt jwt);
}

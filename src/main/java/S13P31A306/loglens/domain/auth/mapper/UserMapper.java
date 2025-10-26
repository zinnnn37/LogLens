package S13P31A306.loglens.domain.auth.mapper;

import S13P31A306.loglens.domain.auth.dto.request.UserSignupRequest;
import S13P31A306.loglens.domain.auth.dto.response.UserSignupResponse;
import S13P31A306.loglens.domain.auth.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface UserMapper {

    /**
     * UserSignupRequest를 User 엔티티로 변환
     *
     * @param request         회원가입 요청 DTO
     * @param encodedPassword 인코딩된 비밀번호
     * @return User 엔티티
     */
    @Mapping(source = "encodedPassword", target = "password")
    User toEntity(UserSignupRequest request, String encodedPassword);

    /**
     * User 엔티티를 UserSignupResponse로 변환
     *
     * @param user User 엔티티
     * @return 회원가입 응답 DTO
     */
    @Mapping(source = "id", target = "userId")
    UserSignupResponse toSignupResponse(User user);
}

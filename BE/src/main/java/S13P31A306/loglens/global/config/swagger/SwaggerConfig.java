package S13P31A306.loglens.global.config.swagger;

import S13P31A306.loglens.global.constants.SwaggerMessages;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openApi() {
        return new OpenAPI()
                .components(createSecurityComponents())
                .security(List.of(createSecurityRequirement()))
                .servers(createServers())
                .info(createApiInfo());
    }

    // JWT 인증 관련 설정 생성
    private Components createSecurityComponents() {
        SecurityScheme scheme = createJwtSecurityScheme();
        return new Components().addSecuritySchemes(SwaggerMessages.SECURITY_SCHEME_NAME.message(), scheme);
    }

    // JWT 보안 구성 설정 생성
    private SecurityScheme createJwtSecurityScheme() {
        return new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme(SwaggerMessages.BEARER_SCHEME.message())
                .bearerFormat(SwaggerMessages.BEARER_FORMAT.message());
    }

    // 보안 요구사항 설정
    private SecurityRequirement createSecurityRequirement() {
        return new SecurityRequirement().addList(SwaggerMessages.SECURITY_SCHEME_NAME.message());
    }

    // API 정보 설정
    private Info createApiInfo() {
        return new Info()
                .title(SwaggerMessages.API_TITLE.message())
                .description(SwaggerMessages.API_DESCRIPTION.message())
                .version(SwaggerMessages.API_VERSION.message());
    }

    // 서버 정보 설정
    private List<Server> createServers() {
        Server localServer = new Server().url(SwaggerMessages.LOCAL_SERVER_URL.message());
        Server prodServer = new Server().url(SwaggerMessages.PROD_SERVER_URL.message());
        return List.of(localServer, prodServer);
    }
}

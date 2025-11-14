package S13P31A306.loglens.global.config.security;

import S13P31A306.loglens.global.constants.GlobalErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.web.util.NestedServletException;

@Slf4j
@Component
@RequiredArgsConstructor
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {

        // SSE 클라이언트 연결 종료와 같은 IO 예외는 무시
        if (response.isCommitted() && (authException.getCause() instanceof IOException
                || authException.getCause() instanceof NestedServletException)) {
            log.debug("Client connection closed before response completed.", authException);
            return;
        }

        // 401 Unauthorized 응답
        ErrorResponseUtils.sendErrorResponse(response, GlobalErrorCode.UNAUTHORIZED);
    }
}

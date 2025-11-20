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

        // SSE 스트림처럼 응답이 이미 커밋된 경우, 클라이언트 연결이 끊겼을 가능성이 높으므로 예외를 무시합니다.
        // 이미 커밋된 응답에 에러를 작성하려고 하면 IllegalStateException이 발생할 수 있습니다.
        if (response.isCommitted()) {
            log.debug("Response has already been committed. Ignoring authentication exception for {}: {}",
                request.getRequestURI(), authException.getMessage());
            return;
        }

        // 401 Unauthorized 응답
        ErrorResponseUtils.sendErrorResponse(response, GlobalErrorCode.UNAUTHORIZED);
    }
}

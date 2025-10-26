package S13P31A306.loglens.global.config.security;

import S13P31A306.loglens.global.constants.ErrorCode;
import S13P31A306.loglens.global.dto.response.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.MediaType;

public final class ErrorResponseUtils {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static void sendErrorResponse(final HttpServletResponse response,
                                         final ErrorCode errorCode) throws IOException {
        response.setStatus(errorCode.getStatus());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), ErrorResponse.of(errorCode));
    }
}

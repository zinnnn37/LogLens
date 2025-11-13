package S13P31A306.loglens.domain.log.controller.impl;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import S13P31A306.loglens.domain.log.constants.LogErrorCode;
import S13P31A306.loglens.domain.log.constants.LogSuccessCode;
import S13P31A306.loglens.domain.log.dto.ai.AiAnalysisDto;
import S13P31A306.loglens.domain.log.dto.response.LogDetailResponse;
import S13P31A306.loglens.domain.log.service.LogService;
import S13P31A306.loglens.global.exception.BusinessException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * LogController 테스트
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)  // Security 필터 비활성화
@ActiveProfiles("test")
public class LogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private LogService logService;

    @Nested
    @DisplayName("로그 상세 조회 API 테스트")
    class GetLogDetailTest {

        @Test
        @DisplayName("GET_/api/logs/{logId}_로그_상세_조회_성공")
        void GET_로그_상세_조회_성공() throws Exception {
            // given
            Long logId = 1234567890L;
            String projectUuid = "550e8400-e29b-41d4-a716-446655440000";

            AiAnalysisDto analysis = AiAnalysisDto.builder()
                    .summary("NULL 참조 에러 발생")
                    .errorCause("NULL 체크 없이 메서드 호출")
                    .solution("NULL 체크 추가")
                    .tags(List.of("NULL_POINTER", "ERROR"))
                    .analysisType("TRACE_BASED")
                    .targetType("LOG")
                    .analyzedAt(LocalDateTime.of(2025, 11, 7, 15, 0, 0))
                    .build();

            LogDetailResponse response = LogDetailResponse.builder()
                    .analysis(analysis)
                    .fromCache(true)
                    .similarLogId(1234567800L)
                    .similarityScore(0.92)
                    .build();

            given(logService.getLogDetail(logId, projectUuid)).willReturn(response);

            // when & then
            mockMvc.perform(get("/api/logs/{logId}", logId)
                            .param("projectUuid", projectUuid))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(LogSuccessCode.LOG_DETAIL_READ_SUCCESS.getCode()))
                    .andExpect(jsonPath("$.message").value(LogSuccessCode.LOG_DETAIL_READ_SUCCESS.getMessage()))
                    .andExpect(jsonPath("$.status").value(200))
                    .andExpect(jsonPath("$.data.logId").value(logId))
                    .andExpect(jsonPath("$.data.traceId").value("trace-abc-123"))
                    .andExpect(jsonPath("$.data.logLevel").value("ERROR"))
                    .andExpect(jsonPath("$.data.message").value("NullPointerException occurred in UserService"))
                    .andExpect(jsonPath("$.data.analysis").exists())
                    .andExpect(jsonPath("$.data.analysis.summary").value("NULL 참조 에러 발생"))
                    .andExpect(jsonPath("$.data.analysis.error_cause").value("NULL 체크 없이 메서드 호출"))
                    .andExpect(jsonPath("$.data.analysis.tags[0]").value("NULL_POINTER"))
                    .andExpect(jsonPath("$.data.fromCache").value(true))
                    .andExpect(jsonPath("$.data.similarLogId").value(1234567800L))
                    .andExpect(jsonPath("$.data.similarityScore").value(0.92));

            verify(logService).getLogDetail(logId, projectUuid);
        }

        @Test
        @DisplayName("GET_/api/logs/{logId}_로그_없음_404_에러")
        void GET_로그_없음_404_에러() throws Exception {
            // given
            Long logId = 9999999999L;
            String projectUuid = "550e8400-e29b-41d4-a716-446655440000";

            given(logService.getLogDetail(logId, projectUuid))
                    .willThrow(new BusinessException(LogErrorCode.LOG_NOT_FOUND));

            // when & then
            mockMvc.perform(get("/api/logs/{logId}", logId)
                            .param("projectUuid", projectUuid))
                    .andDo(print())
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value(LogErrorCode.LOG_NOT_FOUND.getCode()))
                    .andExpect(jsonPath("$.message").value(LogErrorCode.LOG_NOT_FOUND.getMessage()))
                    .andExpect(jsonPath("$.status").value(404));

            verify(logService).getLogDetail(logId, projectUuid);
        }

        @Test
        @DisplayName("GET_/api/logs/{logId}_projectUuid_파라미터_누락_시_500_에러")
        void GET_projectUuid_파라미터_누락_시_500_에러() throws Exception {
            // given
            Long logId = 1234567890L;

            // when & then
            // GlobalExceptionHandler에서 MissingServletRequestParameterException을 500으로 처리
            mockMvc.perform(get("/api/logs/{logId}", logId))
                    .andDo(print())
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.status").value(500));
        }

        @Test
        @DisplayName("GET_/api/logs/{logId}_잘못된_logId_타입_500_에러")
        void GET_잘못된_logId_타입_500_에러() throws Exception {
            // given
            String invalidLogId = "invalid-id";
            String projectUuid = "550e8400-e29b-41d4-a716-446655440000";

            // when & then
            // GlobalExceptionHandler에서 MethodArgumentTypeMismatchException을 500으로 처리
            mockMvc.perform(get("/api/logs/{logId}", invalidLogId)
                            .param("projectUuid", projectUuid))
                    .andDo(print())
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.status").value(500));
        }
    }
}

package S13P31A306.loglens.domain.alert.controller.impl;

import S13P31A306.loglens.domain.alert.dto.AlertConfigCreateRequest;
import S13P31A306.loglens.domain.alert.dto.AlertConfigResponse;
import S13P31A306.loglens.domain.alert.dto.AlertConfigUpdateRequest;
import S13P31A306.loglens.domain.alert.entity.AlertType;
import S13P31A306.loglens.domain.alert.exception.AlertErrorCode;
import S13P31A306.loglens.domain.alert.service.AlertConfigService;
import S13P31A306.loglens.domain.auth.util.AuthenticationHelper;
import S13P31A306.loglens.global.exception.BusinessException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static S13P31A306.loglens.domain.alert.constants.AlertSuccessCode.*;
import static S13P31A306.loglens.domain.project.constants.ProjectErrorCode.PROJECT_NOT_FOUND;
import static S13P31A306.loglens.global.constants.GlobalErrorCode.FORBIDDEN;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * AlertConfigController 통합 테스트
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("AlertConfigController 테스트")
class AlertConfigControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AlertConfigService alertConfigService;

    @MockBean
    private AuthenticationHelper authHelper;

    private static final Integer USER_ID = 1;
    private static final Integer PROJECT_ID = 1;
    private static final String PROJECT_UUID = "test-project-uuid-1234";

    @BeforeEach
    void setUp() {
        given(authHelper.getCurrentUserId()).willReturn(USER_ID);
    }

    @Nested
    @DisplayName("POST /api/alerts/config - 알림 설정 생성")
    class CreateAlertConfigTest {

        @Test
        @DisplayName("POST_/api/alerts/config_알림_설정_생성_성공")
        void POST_알림_설정_생성_성공() throws Exception {
            // given
            AlertConfigCreateRequest request = createValidRequest();
            AlertConfigResponse response = createResponse();

            given(alertConfigService.createAlertConfig(any(AlertConfigCreateRequest.class), eq(USER_ID)))
                    .willReturn(response);

            // when & then
            mockMvc.perform(post("/api/alerts/config")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.code").value(ALERT_CONFIG_CREATED.getCode()))
                    .andExpect(jsonPath("$.data.id").value(1))
                    .andExpect(jsonPath("$.data.alertType").value("ERROR_THRESHOLD"))
                    .andExpect(jsonPath("$.data.thresholdValue").value(10))
                    .andExpect(jsonPath("$.data.activeYN").value("Y"))
                    .andExpect(jsonPath("$.data.projectUuid").value(PROJECT_UUID))
                    .andExpect(jsonPath("$.data.projectName").value("Test Project"));

            verify(alertConfigService).createAlertConfig(any(AlertConfigCreateRequest.class), eq(USER_ID));
        }

        @Test
        @DisplayName("POST_/api/alerts/config_필수_필드_누락_시_400_에러")
        void POST_필수_필드_누락_시_400_에러() throws Exception {
            // given - projectId 누락
            String invalidRequest = """
                    {
                      "alertType": "ERROR_THRESHOLD",
                      "thresholdValue": 10
                    }
                    """;

            // when & then
            mockMvc.perform(post("/api/alerts/config")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidRequest))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("G400"));
        }

        @Test
        @DisplayName("POST_/api/alerts/config_임계값_범위_초과_시_400_에러")
        void POST_임계값_범위_초과_시_400_에러() throws Exception {
            // given - thresholdValue = 300 (255 초과)
            String invalidRequest = """
                    {
                      "projectId": 1,
                      "alertType": "ERROR_THRESHOLD",
                      "thresholdValue": 300
                    }
                    """;

            // when & then
            mockMvc.perform(post("/api/alerts/config")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidRequest))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("G400"));
        }

        @Test
        @DisplayName("POST_/api/alerts/config_이미_설정_존재_시_400_에러")
        void POST_이미_설정_존재_시_400_에러() throws Exception {
            // given
            AlertConfigCreateRequest request = createValidRequest();

            given(alertConfigService.createAlertConfig(any(AlertConfigCreateRequest.class), eq(USER_ID)))
                    .willThrow(new BusinessException(AlertErrorCode.ALERT_CONFIG_ALREADY_EXISTS));

            // when & then
            mockMvc.perform(post("/api/alerts/config")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("AL400-1"));
        }

        @Test
        @DisplayName("POST_/api/alerts/config_권한_없음_시_403_에러")
        void POST_권한_없음_시_403_에러() throws Exception {
            // given
            AlertConfigCreateRequest request = createValidRequest();

            given(alertConfigService.createAlertConfig(any(AlertConfigCreateRequest.class), eq(USER_ID)))
                    .willThrow(new BusinessException(FORBIDDEN));

            // when & then
            mockMvc.perform(post("/api/alerts/config")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("G403"));
        }

        @Test
        @DisplayName("POST_/api/alerts/config_프로젝트_없음_시_404_에러")
        void POST_프로젝트_없음_시_404_에러() throws Exception {
            // given
            AlertConfigCreateRequest request = createValidRequest();

            given(alertConfigService.createAlertConfig(any(AlertConfigCreateRequest.class), eq(USER_ID)))
                    .willThrow(new BusinessException(PROJECT_NOT_FOUND));

            // when & then
            mockMvc.perform(post("/api/alerts/config")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value(PROJECT_NOT_FOUND.getCode()));
        }
    }

    @Nested
    @DisplayName("GET /api/alerts/config - 알림 설정 조회")
    class GetAlertConfigTest {

        @Test
        @DisplayName("GET_/api/alerts/config_알림_설정_조회_성공")
        void GET_알림_설정_조회_성공() throws Exception {
            // given
            AlertConfigResponse response = createResponse();

            given(alertConfigService.getAlertConfig(eq(PROJECT_UUID), eq(USER_ID)))
                    .willReturn(response);

            // when & then
            mockMvc.perform(get("/api/alerts/config")
                            .param("projectUuid", PROJECT_UUID))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(ALERT_CONFIG_RETRIEVED.getCode()))
                    .andExpect(jsonPath("$.data.id").value(1))
                    .andExpect(jsonPath("$.data.projectName").value("Test Project"));

            verify(alertConfigService).getAlertConfig(eq(PROJECT_UUID), eq(USER_ID));
        }

        @Test
        @DisplayName("GET_/api/alerts/config_설정_없음_시_data는_null")
        void GET_설정_없음_시_data는_null() throws Exception {
            // given
            given(alertConfigService.getAlertConfig(eq(PROJECT_UUID), eq(USER_ID)))
                    .willReturn(null);

            // when & then
            mockMvc.perform(get("/api/alerts/config")
                            .param("projectUuid", PROJECT_UUID))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(ALERT_CONFIG_RETRIEVED.getCode()))
                    .andExpect(jsonPath("$.data").doesNotExist());

            verify(alertConfigService).getAlertConfig(eq(PROJECT_UUID), eq(USER_ID));
        }

        @Test
        @DisplayName("GET_/api/alerts/config_projectId_누락_시_500_에러")
        void GET_projectId_누락_시_500_에러() throws Exception {
            // when & then
            // GlobalExceptionHandler에서 MissingServletRequestParameterException을 500으로 처리
            mockMvc.perform(get("/api/alerts/config"))
                    .andDo(print())
                    .andExpect(status().isInternalServerError());
        }
    }

    @Nested
    @DisplayName("PUT /api/alerts/config - 알림 설정 수정")
    class UpdateAlertConfigTest {

        @Test
        @DisplayName("PUT_/api/alerts/config_알림_설정_수정_성공")
        void PUT_알림_설정_수정_성공() throws Exception {
            // given
            AlertConfigUpdateRequest request = new AlertConfigUpdateRequest(
                    1, AlertType.LATENCY, 100, "N");
            AlertConfigResponse response = new AlertConfigResponse(
                    1, AlertType.LATENCY, 100, "N", PROJECT_UUID, "Test Project", null, null);

            given(alertConfigService.updateAlertConfig(any(AlertConfigUpdateRequest.class), eq(USER_ID)))
                    .willReturn(response);

            // when & then
            mockMvc.perform(put("/api/alerts/config")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(ALERT_CONFIG_UPDATED.getCode()))
                    .andExpect(jsonPath("$.data.alertType").value("LATENCY"))
                    .andExpect(jsonPath("$.data.thresholdValue").value(100))
                    .andExpect(jsonPath("$.data.activeYN").value("N"));

            verify(alertConfigService).updateAlertConfig(any(AlertConfigUpdateRequest.class), eq(USER_ID));
        }

        @Test
        @DisplayName("PUT_/api/alerts/config_alertConfigId_누락_시_400_에러")
        void PUT_alertConfigId_누락_시_400_에러() throws Exception {
            // given - alertConfigId 누락
            String invalidRequest = """
                    {
                      "alertType": "LATENCY",
                      "thresholdValue": 100
                    }
                    """;

            // when & then
            mockMvc.perform(put("/api/alerts/config")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidRequest))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("G400"));
        }

        @Test
        @DisplayName("PUT_/api/alerts/config_설정_없음_시_404_에러")
        void PUT_설정_없음_시_404_에러() throws Exception {
            // given
            AlertConfigUpdateRequest request = new AlertConfigUpdateRequest(
                    99, AlertType.LATENCY, 100, "N");

            given(alertConfigService.updateAlertConfig(any(AlertConfigUpdateRequest.class), eq(USER_ID)))
                    .willThrow(new BusinessException(AlertErrorCode.ALERT_CONFIG_NOT_FOUND));

            // when & then
            mockMvc.perform(put("/api/alerts/config")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("AL404"));
        }
    }

    // ============ Helper Methods ============

    private AlertConfigCreateRequest createValidRequest() {
        return new AlertConfigCreateRequest(
                PROJECT_UUID,
                AlertType.ERROR_THRESHOLD,
                10,
                "Y"
        );
    }

    private AlertConfigResponse createResponse() {
        return new AlertConfigResponse(
                1,
                AlertType.ERROR_THRESHOLD,
                10,
                "Y",
                PROJECT_UUID,
                "Test Project",
                null,
                null
        );
    }
}

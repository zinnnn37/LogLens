//package S13P31A306.loglens.domain.alert.controller.impl;
//
//import S13P31A306.loglens.domain.alert.dto.AlertHistoryResponse;
//import S13P31A306.loglens.domain.alert.exception.AlertErrorCode;
//import S13P31A306.loglens.domain.alert.service.AlertHistoryService;
//import S13P31A306.loglens.domain.auth.util.AuthenticationHelper;
//import S13P31A306.loglens.global.exception.BusinessException;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Nested;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.boot.test.mock.mockito.MockBean;
//import org.springframework.http.MediaType;
//import org.springframework.test.context.ActiveProfiles;
//import org.springframework.test.web.servlet.MockMvc;
//import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
//
//import java.time.LocalDateTime;
//import java.util.Arrays;
//import java.util.Collections;
//import java.util.List;
//
//import static S13P31A306.loglens.domain.alert.constants.AlertSuccessCode.*;
//import static S13P31A306.loglens.global.constants.GlobalErrorCode.FORBIDDEN;
//import static S13P31A306.loglens.domain.project.constants.ProjectErrorCode.PROJECT_NOT_FOUND;
//import static org.mockito.ArgumentMatchers.eq;
//import static org.mockito.BDDMockito.given;
//import static org.mockito.Mockito.verify;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
//import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
//
///**
// * AlertHistoryController 통합 테스트
// */
//@SpringBootTest
//@AutoConfigureMockMvc(addFilters = false)
//@ActiveProfiles("test")
//@DisplayName("AlertHistoryController 테스트")
//class AlertHistoryControllerTest {
//
//    @Autowired
//    private MockMvc mockMvc;
//
//    @Autowired
//    private ObjectMapper objectMapper;
//
//    @MockBean
//    private AlertHistoryService alertHistoryService;
//
//    @MockBean
//    private AuthenticationHelper authHelper;
//
//    private static final Integer USER_ID = 1;
//    private static final Integer PROJECT_ID = 1;
//    private static final String PROJECT_UUID = "test-project-uuid-1234";
//    private static final Integer ALERT_ID = 1;
//
//    @BeforeEach
//    void setUp() {
//        given(authHelper.getCurrentUserId()).willReturn(USER_ID);
//    }
//
//    @Nested
//    @DisplayName("GET /api/alerts/histories - 알림 이력 조회")
//    class GetAlertHistoriesTest {
//
//        @Test
//        @DisplayName("GET_/api/alerts/histories_전체_조회_성공")
//        void GET_전체_조회_성공() throws Exception {
//            // given
//            List<AlertHistoryResponse> responses = Arrays.asList(
//                    createAlertHistoryResponse(1, "N"),
//                    createAlertHistoryResponse(2, "Y")
//            );
//
//            given(alertHistoryService.getAlertHistories(eq(PROJECT_UUID), eq(USER_ID), eq(null)))
//                    .willReturn(responses);
//
//            // when & then
//            mockMvc.perform(get("/api/alerts/histories")
//                            .param("projectUuid", PROJECT_UUID))
//                    .andDo(print())
//                    .andExpect(status().isOk())
//                    .andExpect(jsonPath("$.code").value(ALERT_HISTORIES_RETRIEVED.getCode()))
//                    .andExpect(jsonPath("$.data").isArray())
//                    .andExpect(jsonPath("$.data.length()").value(2))
//                    .andExpect(jsonPath("$.data[0].id").value(1))
//                    .andExpect(jsonPath("$.data[0].resolvedYN").value("N"))
//                    .andExpect(jsonPath("$.data[1].id").value(2))
//                    .andExpect(jsonPath("$.data[1].resolvedYN").value("Y"));
//
//            verify(alertHistoryService).getAlertHistories(eq(PROJECT_UUID), eq(USER_ID), eq(null));
//        }
//
//        @Test
//        @DisplayName("GET_/api/alerts/histories_읽지_않음_필터링_성공")
//        void GET_읽지_않음_필터링_성공() throws Exception {
//            // given
//            List<AlertHistoryResponse> responses = Collections.singletonList(
//                    createAlertHistoryResponse(1, "N")
//            );
//
//            given(alertHistoryService.getAlertHistories(eq(PROJECT_UUID), eq(USER_ID), eq("N")))
//                    .willReturn(responses);
//
//            // when & then
//            mockMvc.perform(get("/api/alerts/histories")
//                            .param("projectUuid", PROJECT_UUID)
//                            .param("resolvedYN", "N"))
//                    .andDo(print())
//                    .andExpect(status().isOk())
//                    .andExpect(jsonPath("$.code").value(ALERT_HISTORIES_RETRIEVED.getCode()))
//                    .andExpect(jsonPath("$.data.length()").value(1))
//                    .andExpect(jsonPath("$.data[0].resolvedYN").value("N"));
//
//            verify(alertHistoryService).getAlertHistories(eq(PROJECT_UUID), eq(USER_ID), eq("N"));
//        }
//
//        @Test
//        @DisplayName("GET_/api/alerts/histories_빈_목록_반환_성공")
//        void GET_빈_목록_반환_성공() throws Exception {
//            // given
//            given(alertHistoryService.getAlertHistories(eq(PROJECT_UUID), eq(USER_ID), eq(null)))
//                    .willReturn(Collections.emptyList());
//
//            // when & then
//            mockMvc.perform(get("/api/alerts/histories")
//                            .param("projectUuid", PROJECT_UUID))
//                    .andDo(print())
//                    .andExpect(status().isOk())
//                    .andExpect(jsonPath("$.code").value(ALERT_HISTORIES_RETRIEVED.getCode()))
//                    .andExpect(jsonPath("$.data").isArray())
//                    .andExpect(jsonPath("$.data.length()").value(0));
//        }
//
//        @Test
//        @DisplayName("GET_/api/alerts/histories_권한_없음_시_403_에러")
//        void GET_권한_없음_시_403_에러() throws Exception {
//            // given
//            given(alertHistoryService.getAlertHistories(eq(PROJECT_UUID), eq(USER_ID), eq(null)))
//                    .willThrow(new BusinessException(FORBIDDEN));
//
//            // when & then
//            mockMvc.perform(get("/api/alerts/histories")
//                            .param("projectUuid", PROJECT_UUID))
//                    .andDo(print())
//                    .andExpect(status().isForbidden())
//                    .andExpect(jsonPath("$.code").value("G403"));
//        }
//    }
//
//    @Nested
//    @DisplayName("PATCH /api/alerts/{alertId}/read - 알림 읽음 처리")
//    class MarkAsReadTest {
//
//        @Test
//        @DisplayName("PATCH_/api/alerts/{alertId}/read_읽음_처리_성공")
//        void PATCH_읽음_처리_성공() throws Exception {
//            // given
//            AlertHistoryResponse response = createAlertHistoryResponse(ALERT_ID, "Y");
//
//            given(alertHistoryService.markAsRead(eq(ALERT_ID), eq(USER_ID)))
//                    .willReturn(response);
//
//            // when & then
//            mockMvc.perform(patch("/api/alerts/{alertId}/read", ALERT_ID))
//                    .andDo(print())
//                    .andExpect(status().isOk())
//                    .andExpect(jsonPath("$.code").value(ALERT_MARKED_AS_READ.getCode()))
//                    .andExpect(jsonPath("$.data.id").value(ALERT_ID))
//                    .andExpect(jsonPath("$.data.resolvedYN").value("Y"));
//
//            verify(alertHistoryService).markAsRead(eq(ALERT_ID), eq(USER_ID));
//        }
//
//        @Test
//        @DisplayName("PATCH_/api/alerts/{alertId}/read_이미_읽은_알림_재요청_성공")
//        void PATCH_이미_읽은_알림_재요청_성공() throws Exception {
//            // given - 멱등성 테스트: 이미 읽은 알림
//            AlertHistoryResponse response = createAlertHistoryResponse(ALERT_ID, "Y");
//
//            given(alertHistoryService.markAsRead(eq(ALERT_ID), eq(USER_ID)))
//                    .willReturn(response);
//
//            // when & then
//            mockMvc.perform(patch("/api/alerts/{alertId}/read", ALERT_ID))
//                    .andDo(print())
//                    .andExpect(status().isOk())
//                    .andExpect(jsonPath("$.code").value(ALERT_MARKED_AS_READ.getCode()))
//                    .andExpect(jsonPath("$.data.resolvedYN").value("Y"));
//
//            verify(alertHistoryService).markAsRead(eq(ALERT_ID), eq(USER_ID));
//        }
//
//        @Test
//        @DisplayName("PATCH_/api/alerts/{alertId}/read_알림_없음_시_404_에러")
//        void PATCH_알림_없음_시_404_에러() throws Exception {
//            // given
//            given(alertHistoryService.markAsRead(eq(ALERT_ID), eq(USER_ID)))
//                    .willThrow(new BusinessException(AlertErrorCode.ALERT_HISTORY_NOT_FOUND));
//
//            // when & then
//            mockMvc.perform(patch("/api/alerts/{alertId}/read", ALERT_ID))
//                    .andDo(print())
//                    .andExpect(status().isNotFound())
//                    .andExpect(jsonPath("$.code").value("AL404-1"));
//        }
//
//        @Test
//        @DisplayName("PATCH_/api/alerts/{alertId}/read_권한_없음_시_403_에러")
//        void PATCH_권한_없음_시_403_에러() throws Exception {
//            // given
//            given(alertHistoryService.markAsRead(eq(ALERT_ID), eq(USER_ID)))
//                    .willThrow(new BusinessException(FORBIDDEN));
//
//            // when & then
//            mockMvc.perform(patch("/api/alerts/{alertId}/read", ALERT_ID))
//                    .andDo(print())
//                    .andExpect(status().isForbidden())
//                    .andExpect(jsonPath("$.code").value("G403"));
//        }
//    }
//
//    @Nested
//    @DisplayName("GET /api/alerts/unread-count - 읽지 않은 알림 개수 조회")
//    class GetUnreadCountTest {
//
//        @Test
//        @DisplayName("GET_/api/alerts/unread-count_개수_조회_성공")
//        void GET_개수_조회_성공() throws Exception {
//            // given
//            given(alertHistoryService.getUnreadCount(eq(PROJECT_UUID), eq(USER_ID)))
//                    .willReturn(5L);
//
//            // when & then
//            mockMvc.perform(get("/api/alerts/unread-count")
//                            .param("projectUuid", PROJECT_UUID))
//                    .andDo(print())
//                    .andExpect(status().isOk())
//                    .andExpect(jsonPath("$.code").value(ALERT_UNREAD_COUNT_RETRIEVED.getCode()))
//                    .andExpect(jsonPath("$.data.unreadCount").value(5));
//
//            verify(alertHistoryService).getUnreadCount(eq(PROJECT_UUID), eq(USER_ID));
//        }
//
//        @Test
//        @DisplayName("GET_/api/alerts/unread-count_0개_반환_성공")
//        void GET_0개_반환_성공() throws Exception {
//            // given
//            given(alertHistoryService.getUnreadCount(eq(PROJECT_UUID), eq(USER_ID)))
//                    .willReturn(0L);
//
//            // when & then
//            mockMvc.perform(get("/api/alerts/unread-count")
//                            .param("projectUuid", PROJECT_UUID))
//                    .andDo(print())
//                    .andExpect(status().isOk())
//                    .andExpect(jsonPath("$.code").value(ALERT_UNREAD_COUNT_RETRIEVED.getCode()))
//                    .andExpect(jsonPath("$.data.unreadCount").value(0));
//
//            verify(alertHistoryService).getUnreadCount(eq(PROJECT_UUID), eq(USER_ID));
//        }
//
//        @Test
//        @DisplayName("GET_/api/alerts/unread-count_권한_없음_시_403_에러")
//        void GET_권한_없음_시_403_에러() throws Exception {
//            // given
//            given(alertHistoryService.getUnreadCount(eq(PROJECT_UUID), eq(USER_ID)))
//                    .willThrow(new BusinessException(FORBIDDEN));
//
//            // when & then
//            mockMvc.perform(get("/api/alerts/unread-count")
//                            .param("projectUuid", PROJECT_UUID))
//                    .andDo(print())
//                    .andExpect(status().isForbidden())
//                    .andExpect(jsonPath("$.code").value("G403"));
//        }
//    }
//
//    @Nested
//    @DisplayName("GET /api/alerts/stream - 실시간 알림 스트리밍")
//    class StreamAlertsTest {
//
//        @Test
//        @DisplayName("GET_/api/alerts/stream_SSE_연결_생성_성공")
//        void GET_SSE_연결_생성_성공() throws Exception {
//            // given
//            given(alertHistoryService.streamAlerts(eq(PROJECT_UUID)))
//                    .willReturn(new SseEmitter(300000L));
//
//            // when & then
//            // SSE는 비동기 응답이므로 Content-Type은 데이터 전송 시점에 설정됨
//            // 연결 생성 시점에는 status와 async 시작 여부만 확인
//            mockMvc.perform(get("/api/alerts/stream")
//                            .param("projectUuid", PROJECT_UUID))
//                    .andDo(print())
//                    .andExpect(status().isOk())
//                    .andExpect(request().asyncStarted());
//
//            verify(alertHistoryService).streamAlerts(eq(PROJECT_UUID));
//        }
//
//        @Test
//        @DisplayName("GET_/api/alerts/stream_프로젝트_없음_404_에러")
//        void GET_프로젝트_없음_404_에러() throws Exception {
//            // given
//            given(alertHistoryService.streamAlerts(eq(PROJECT_UUID)))
//                    .willThrow(new BusinessException(PROJECT_NOT_FOUND));
//
//            // when & then
//            mockMvc.perform(get("/api/alerts/stream")
//                            .param("projectUuid", PROJECT_UUID))
//                    .andDo(print())
//                    .andExpect(status().isNotFound())
//                    .andExpect(jsonPath("$.code").value(PROJECT_NOT_FOUND.getCode()));
//        }
//
//        @Test
//        @DisplayName("GET_/api/alerts/stream_권한_없음_403_에러")
//        void GET_권한_없음_403_에러() throws Exception {
//            // given
//            given(alertHistoryService.streamAlerts(eq(PROJECT_UUID)))
//                    .willThrow(new BusinessException(FORBIDDEN));
//
//            // when & then
//            mockMvc.perform(get("/api/alerts/stream")
//                            .param("projectUuid", PROJECT_UUID))
//                    .andDo(print())
//                    .andExpect(status().isForbidden())
//                    .andExpect(jsonPath("$.code").value("G403"));
//        }
//
//        @Test
//        @DisplayName("GET_/api/alerts/stream_projectId_파라미터_누락_500_에러")
//        void GET_projectId_파라미터_누락_500_에러() throws Exception {
//            // when & then
//            // GlobalExceptionHandler에서 MissingServletRequestParameterException을 500으로 처리
//            mockMvc.perform(get("/api/alerts/stream"))
//                    .andDo(print())
//                    .andExpect(status().isInternalServerError());
//        }
//    }
//
//    // ============ Helper Methods ============
//
//    private AlertHistoryResponse createAlertHistoryResponse(Integer id, String resolvedYN) {
//        return new AlertHistoryResponse(
//                id,
//                "에러 발생: " + id,
//                LocalDateTime.now(),
//                resolvedYN,
//                "{\"logId\": " + id + "}",
//                PROJECT_UUID
//        );
//    }
//}

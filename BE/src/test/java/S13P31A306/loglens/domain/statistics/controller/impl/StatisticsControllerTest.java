//package S13P31A306.loglens.domain.statistics.controller.impl;
//
//import S13P31A306.loglens.domain.statistics.dto.response.LogTrendResponse;
//import S13P31A306.loglens.domain.statistics.dto.response.TrafficResponse;
//import S13P31A306.loglens.domain.statistics.service.LogTrendService;
//import S13P31A306.loglens.domain.statistics.service.TrafficService;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.test.context.ActiveProfiles;
//import org.springframework.test.context.bean.override.mockito.MockitoBean;
//import org.springframework.test.web.servlet.MockMvc;
//
//import java.time.LocalDateTime;
//import java.util.ArrayList;
//import java.util.List;
//
//import static org.mockito.ArgumentMatchers.anyString;
//import static org.mockito.BDDMockito.given;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
//
///**
// * StatisticsController 테스트
// */
//@SpringBootTest
//@AutoConfigureMockMvc(addFilters = false)  // Security 필터 비활성화
//@ActiveProfiles("test")
//@DisplayName("StatisticsController 테스트")
//class StatisticsControllerTest {
//
//    @Autowired
//    private MockMvc mockMvc;
//
//    @Autowired
//    private ObjectMapper objectMapper;
//
//    @MockitoBean
//    private LogTrendService logTrendService;
//
//    @MockitoBean
//    private TrafficService trafficService;
//
//    private static final String PROJECT_UUID = "3a73c7d4-8176-3929-b72f-d5b921daae67";
//
//    @Test
//    void 로그_추이_API_호출_시_200_OK를_반환한다() throws Exception {
//        // given
//        LogTrendResponse response = createResponse();
//        given(logTrendService.getLogTrend(anyString())).willReturn(response);
//
//        // when & then
//        mockMvc.perform(get("/api/statistics/log-trend")
//                        .param("projectUuid", PROJECT_UUID))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.code").value("STATISTICS_2001"))
//                .andExpect(jsonPath("$.message").value("로그 추이 조회 성공"))
//                .andExpect(jsonPath("$.data.projectUuid").value(PROJECT_UUID))
//                .andExpect(jsonPath("$.data.dataPoints").isArray());
//    }
//
//    @Test
//    void 응답의_dataPoints가_8개이다() throws Exception {
//        // given
//        LogTrendResponse response = createResponse();
//        given(logTrendService.getLogTrend(anyString())).willReturn(response);
//
//        // when & then
//        mockMvc.perform(get("/api/statistics/log-trend")
//                        .param("projectUuid", PROJECT_UUID))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.data.dataPoints.length()").value(8));
//    }
//
//    @Test
//    void 각_dataPoint의_hour가_HH_mm_형식이다() throws Exception {
//        // given
//        LogTrendResponse response = createResponse();
//        given(logTrendService.getLogTrend(anyString())).willReturn(response);
//
//        // when & then
//        mockMvc.perform(get("/api/statistics/log-trend")
//                        .param("projectUuid", PROJECT_UUID))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.data.dataPoints[0].hour").value("15:00"))
//                .andExpect(jsonPath("$.data.dataPoints[1].hour").value("18:00"));
//    }
//
//    @Test
//    void 잘못된_UUID_형식이면_400_BadRequest를_반환한다() throws Exception {
//        // when & then
//        mockMvc.perform(get("/api/statistics/log-trend")
//                        .param("projectUuid", "invalid-uuid"))
//                .andExpect(status().isBadRequest());
//    }
//
//    @Test
//    void Traffic_API_호출_시_200_OK를_반환한다() throws Exception {
//        // given
//        TrafficResponse response = createTrafficResponse();
//        given(trafficService.getTraffic(anyString())).willReturn(response);
//
//        // when & then
//        mockMvc.perform(get("/api/statistics/traffic")
//                        .param("projectUuid", PROJECT_UUID))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.code").value("STATISTICS_2002"))
//                .andExpect(jsonPath("$.message").value("Traffic 조회 성공"))
//                .andExpect(jsonPath("$.data.projectUuid").value(PROJECT_UUID))
//                .andExpect(jsonPath("$.data.dataPoints").isArray());
//    }
//
//    @Test
//    void Traffic_응답의_dataPoints가_8개이다() throws Exception {
//        // given
//        TrafficResponse response = createTrafficResponse();
//        given(trafficService.getTraffic(anyString())).willReturn(response);
//
//        // when & then
//        mockMvc.perform(get("/api/statistics/traffic")
//                        .param("projectUuid", PROJECT_UUID))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.data.dataPoints.length()").value(8));
//    }
//
//    @Test
//    void Traffic의_각_dataPoint가_feCount와_beCount를_포함한다() throws Exception {
//        // given
//        TrafficResponse response = createTrafficResponse();
//        given(trafficService.getTraffic(anyString())).willReturn(response);
//
//        // when & then
//        mockMvc.perform(get("/api/statistics/traffic")
//                        .param("projectUuid", PROJECT_UUID))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.data.dataPoints[0].feCount").exists())
//                .andExpect(jsonPath("$.data.dataPoints[0].beCount").exists())
//                .andExpect(jsonPath("$.data.dataPoints[0].hour").value("15:00"));
//    }
//
//    @Test
//    void Traffic_잘못된_UUID_형식이면_400_BadRequest를_반환한다() throws Exception {
//        // when & then
//        mockMvc.perform(get("/api/statistics/traffic")
//                        .param("projectUuid", "invalid-uuid"))
//                .andExpect(status().isBadRequest());
//    }
//
//    // 헬퍼 메서드
//    private LogTrendResponse createResponse() {
//        List<LogTrendResponse.DataPoint> dataPoints = new ArrayList<>();
//        LocalDateTime start = LocalDateTime.of(2025, 11, 13, 15, 0);
//
//        for (int i = 0; i < 8; i++) {
//            dataPoints.add(new LogTrendResponse.DataPoint(
//                    start.plusHours(i * 3).toString(),
//                    String.format("%02d:00", (15 + i * 3) % 24),
//                    1500 + i * 100,
//                    1200, 250, 50
//            ));
//        }
//
//        return new LogTrendResponse(
//                PROJECT_UUID,
//                new LogTrendResponse.Period(start.toString(), start.plusHours(24).toString()),
//                "3h",
//                dataPoints,
//                new LogTrendResponse.Summary(12000, 1500, "12:00", 2100)
//        );
//    }
//
//    private TrafficResponse createTrafficResponse() {
//        List<TrafficResponse.DataPoint> dataPoints = new ArrayList<>();
//        LocalDateTime start = LocalDateTime.of(2025, 11, 13, 15, 0);
//
//        for (int i = 0; i < 8; i++) {
//            dataPoints.add(new TrafficResponse.DataPoint(
//                    start.plusHours(i * 3).toString(),
//                    String.format("%02d:00", (15 + i * 3) % 24),
//                    1500 + i * 100,
//                    800 + i * 50,
//                    700 + i * 50
//            ));
//        }
//
//        return new TrafficResponse(
//                PROJECT_UUID,
//                new TrafficResponse.Period(start.toString(), start.plusHours(24).toString()),
//                "3h",
//                dataPoints,
//                new TrafficResponse.Summary(12000, 6400, 5600, 1500, "12:00", 2200)
//        );
//    }
//}

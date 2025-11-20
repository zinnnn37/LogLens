package S13P31A306.loglens.domain.statistics.controller;

import S13P31A306.loglens.domain.statistics.dto.response.AIComparisonResponse;
import S13P31A306.loglens.domain.statistics.dto.response.LogTrendResponse;
import S13P31A306.loglens.domain.statistics.dto.response.TrafficResponse;
import S13P31A306.loglens.global.annotation.ValidUuid;
import S13P31A306.loglens.global.config.swagger.annotation.ApiInternalServerError;
import S13P31A306.loglens.global.config.swagger.annotation.ApiUnauthorizedError;
import S13P31A306.loglens.global.constants.SwaggerMessages;
import S13P31A306.loglens.global.dto.response.BaseResponse;
import S13P31A306.loglens.global.dto.response.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 통계 API 인터페이스
 */
@ApiInternalServerError
@ApiUnauthorizedError
@Tag(name = "Statistics API", description = "통계 관련 API")
public interface StatisticsApi {

    @Operation(
            summary = "로그 발생 추이 조회",
            description = "24시간 전부터 3시간 간격으로 로그 발생 추이를 조회합니다. 총 8개의 데이터 포인트를 반환합니다.",
            security = @SecurityRequirement(name = SwaggerMessages.BEARER_AUTH),
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "로그 추이 조회 성공",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = LogTrendResponse.class),
                                    examples = @ExampleObject(
                                            name = "LogTrendSuccess",
                                            summary = "로그 추이 조회 성공 예시",
                                            value = """
                                                    {
                                                      "code": "STATISTICS_2001",
                                                      "message": "로그 추이 조회 성공",
                                                      "status": 200,
                                                      "data": {
                                                        "projectUuid": "3a73c7d4-8176-3929-b72f-d5b921daae67",
                                                        "period": {
                                                          "startTime": "2025-11-13T15:00:00+09:00",
                                                          "endTime": "2025-11-14T15:00:00+09:00"
                                                        },
                                                        "interval": "3h",
                                                        "dataPoints": [
                                                          {
                                                            "timestamp": "2025-11-13T15:00:00+09:00",
                                                            "hour": "15:00",
                                                            "totalCount": 1523,
                                                            "infoCount": 1200,
                                                            "warnCount": 250,
                                                            "errorCount": 73
                                                          },
                                                          {
                                                            "timestamp": "2025-11-13T18:00:00+09:00",
                                                            "hour": "18:00",
                                                            "totalCount": 1820,
                                                            "infoCount": 1450,
                                                            "warnCount": 280,
                                                            "errorCount": 90
                                                          },
                                                          {
                                                            "timestamp": "2025-11-13T21:00:00+09:00",
                                                            "hour": "21:00",
                                                            "totalCount": 980,
                                                            "infoCount": 750,
                                                            "warnCount": 180,
                                                            "errorCount": 50
                                                          },
                                                          {
                                                            "timestamp": "2025-11-14T00:00:00+09:00",
                                                            "hour": "00:00",
                                                            "totalCount": 450,
                                                            "infoCount": 380,
                                                            "warnCount": 55,
                                                            "errorCount": 15
                                                          },
                                                          {
                                                            "timestamp": "2025-11-14T03:00:00+09:00",
                                                            "hour": "03:00",
                                                            "totalCount": 320,
                                                            "infoCount": 280,
                                                            "warnCount": 30,
                                                            "errorCount": 10
                                                          },
                                                          {
                                                            "timestamp": "2025-11-14T06:00:00+09:00",
                                                            "hour": "06:00",
                                                            "totalCount": 1150,
                                                            "infoCount": 950,
                                                            "warnCount": 160,
                                                            "errorCount": 40
                                                          },
                                                          {
                                                            "timestamp": "2025-11-14T09:00:00+09:00",
                                                            "hour": "09:00",
                                                            "totalCount": 2100,
                                                            "infoCount": 1700,
                                                            "warnCount": 310,
                                                            "errorCount": 90
                                                          },
                                                          {
                                                            "timestamp": "2025-11-14T12:00:00+09:00",
                                                            "hour": "12:00",
                                                            "totalCount": 2650,
                                                            "infoCount": 2150,
                                                            "warnCount": 380,
                                                            "errorCount": 120
                                                          }
                                                        ],
                                                        "summary": {
                                                          "totalLogs": 12000,
                                                          "avgLogsPerInterval": 1500,
                                                          "peakHour": "12:00",
                                                          "peakCount": 2650
                                                        }
                                                      },
                                                      "timestamp": "2025-11-14T07:30:00Z"
                                                    }
                                                    """
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "잘못된 요청 (UUID 형식 오류)",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = @ExampleObject(
                                            name = "InvalidUuidFormat",
                                            summary = "잘못된 UUID 형식",
                                            value = """
                                                    {
                                                      "code": "G400",
                                                      "message": "입력값이 유효하지 않습니다.",
                                                      "status": 400,
                                                      "data": {
                                                        "uri": "/api/statistics/log-trend",
                                                        "validationErrors": [
                                                          {
                                                            "field": "projectUuid",
                                                            "rejectedValue": "invalid-uuid",
                                                            "code": "C400-1",
                                                            "message": "UUID 형식이 올바르지 않습니다."
                                                          }
                                                        ]
                                                      },
                                                      "timestamp": "2025-11-14T07:30:00Z"
                                                    }
                                                    """
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "403",
                            description = "권한 없음 (프로젝트 멤버 아님)",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = @ExampleObject(
                                            name = "NotProjectMember",
                                            summary = "프로젝트 멤버가 아님",
                                            value = """
                                                    {
                                                      "code": "PJ403-3",
                                                      "message": "해당 프로젝트의 멤버가 아닙니다.",
                                                      "status": 403,
                                                      "timestamp": "2025-11-14T07:30:00Z"
                                                    }
                                                    """
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "프로젝트를 찾을 수 없음",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = @ExampleObject(
                                            name = "ProjectNotFound",
                                            summary = "프로젝트를 찾을 수 없음",
                                            value = """
                                                    {
                                                      "code": "PJ404-1",
                                                      "message": "해당 프로젝트를 찾을 수 없습니다.",
                                                      "status": 404,
                                                      "timestamp": "2025-11-14T07:30:00Z"
                                                    }
                                                    """
                                    )
                            )
                    )
            }
    )
    ResponseEntity<? extends BaseResponse> getLogTrend(
            @Parameter(description = "프로젝트 UUID", required = true, example = "3a73c7d4-8176-3929-b72f-d5b921daae67")
            @ValidUuid
            @RequestParam String projectUuid
    );

    @Operation(
            summary = "Traffic 그래프 조회",
            description = "24시간 전부터 3시간 간격으로 FE/BE 로그 발생 추이를 조회합니다. 총 8개의 데이터 포인트를 반환합니다.",
            security = @SecurityRequirement(name = SwaggerMessages.BEARER_AUTH),
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Traffic 조회 성공",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = TrafficResponse.class),
                                    examples = @ExampleObject(
                                            name = "TrafficSuccess",
                                            summary = "Traffic 조회 성공 예시",
                                            value = """
                                                    {
                                                      "code": "STATISTICS_2002",
                                                      "message": "Traffic 조회 성공",
                                                      "status": 200,
                                                      "data": {
                                                        "projectUuid": "3a73c7d4-8176-3929-b72f-d5b921daae67",
                                                        "period": {
                                                          "startTime": "2025-11-13T15:00:00+09:00",
                                                          "endTime": "2025-11-14T15:00:00+09:00"
                                                        },
                                                        "interval": "3h",
                                                        "dataPoints": [
                                                          {
                                                            "timestamp": "2025-11-13T15:00:00+09:00",
                                                            "hour": "15:00",
                                                            "totalCount": 1523,
                                                            "feCount": 812,
                                                            "beCount": 711
                                                          },
                                                          {
                                                            "timestamp": "2025-11-13T18:00:00+09:00",
                                                            "hour": "18:00",
                                                            "totalCount": 1820,
                                                            "feCount": 970,
                                                            "beCount": 850
                                                          },
                                                          {
                                                            "timestamp": "2025-11-13T21:00:00+09:00",
                                                            "hour": "21:00",
                                                            "totalCount": 980,
                                                            "feCount": 520,
                                                            "beCount": 460
                                                          },
                                                          {
                                                            "timestamp": "2025-11-14T00:00:00+09:00",
                                                            "hour": "00:00",
                                                            "totalCount": 450,
                                                            "feCount": 240,
                                                            "beCount": 210
                                                          },
                                                          {
                                                            "timestamp": "2025-11-14T03:00:00+09:00",
                                                            "hour": "03:00",
                                                            "totalCount": 320,
                                                            "feCount": 170,
                                                            "beCount": 150
                                                          },
                                                          {
                                                            "timestamp": "2025-11-14T06:00:00+09:00",
                                                            "hour": "06:00",
                                                            "totalCount": 1150,
                                                            "feCount": 610,
                                                            "beCount": 540
                                                          },
                                                          {
                                                            "timestamp": "2025-11-14T09:00:00+09:00",
                                                            "hour": "09:00",
                                                            "totalCount": 2100,
                                                            "feCount": 1120,
                                                            "beCount": 980
                                                          },
                                                          {
                                                            "timestamp": "2025-11-14T12:00:00+09:00",
                                                            "hour": "12:00",
                                                            "totalCount": 2650,
                                                            "feCount": 1410,
                                                            "beCount": 1240
                                                          }
                                                        ],
                                                        "summary": {
                                                          "totalLogs": 12000,
                                                          "totalFeCount": 6400,
                                                          "totalBeCount": 5600,
                                                          "avgLogsPerInterval": 1500,
                                                          "peakHour": "12:00",
                                                          "peakCount": 2650
                                                        }
                                                      },
                                                      "timestamp": "2025-11-14T07:30:00Z"
                                                    }
                                                    """
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "잘못된 요청 (UUID 형식 오류)",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = @ExampleObject(
                                            name = "InvalidUuidFormat",
                                            summary = "잘못된 UUID 형식",
                                            value = """
                                                    {
                                                      "code": "G400",
                                                      "message": "입력값이 유효하지 않습니다.",
                                                      "status": 400,
                                                      "data": {
                                                        "uri": "/api/statistics/traffic",
                                                        "validationErrors": [
                                                          {
                                                            "field": "projectUuid",
                                                            "rejectedValue": "invalid-uuid",
                                                            "code": "C400-1",
                                                            "message": "UUID 형식이 올바르지 않습니다."
                                                          }
                                                        ]
                                                      },
                                                      "timestamp": "2025-11-14T07:30:00Z"
                                                    }
                                                    """
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "403",
                            description = "권한 없음 (프로젝트 멤버 아님)",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = @ExampleObject(
                                            name = "NotProjectMember",
                                            summary = "프로젝트 멤버가 아님",
                                            value = """
                                                    {
                                                      "code": "PJ403-3",
                                                      "message": "해당 프로젝트의 멤버가 아닙니다.",
                                                      "status": 403,
                                                      "timestamp": "2025-11-14T07:30:00Z"
                                                    }
                                                    """
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "프로젝트를 찾을 수 없음",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = @ExampleObject(
                                            name = "ProjectNotFound",
                                            summary = "프로젝트를 찾을 수 없음",
                                            value = """
                                                    {
                                                      "code": "PJ404-1",
                                                      "message": "해당 프로젝트를 찾을 수 없습니다.",
                                                      "status": 404,
                                                      "timestamp": "2025-11-14T07:30:00Z"
                                                    }
                                                    """
                                    )
                            )
                    )
            }
    )
    ResponseEntity<? extends BaseResponse> getTraffic(
            @Parameter(description = "프로젝트 UUID", required = true, example = "3a73c7d4-8176-3929-b72f-d5b921daae67")
            @ValidUuid
            @RequestParam String projectUuid
    );

    @Operation(
            summary = "AI vs DB 통계 비교 검증",
            description = "LLM 기반 통계 추론과 DB 직접 조회의 정확도를 비교하여 AI의 DB 대체 역량을 검증합니다.",
            security = @SecurityRequirement(name = SwaggerMessages.BEARER_AUTH),
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "AI vs DB 통계 비교 검증 성공",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = AIComparisonResponse.class),
                                    examples = @ExampleObject(
                                            name = "AIComparisonSuccess",
                                            summary = "AI vs DB 통계 비교 성공 예시",
                                            value = """
                                                    {
                                                      "code": "STATISTICS_2003",
                                                      "message": "AI vs DB 통계 비교 검증 성공",
                                                      "status": 200,
                                                      "data": {
                                                        "projectUuid": "3a73c7d4-8176-3929-b72f-d5b921daae67",
                                                        "analysisPeriodHours": 24,
                                                        "sampleSize": 100,
                                                        "analyzedAt": "2025-11-14T15:30:00",
                                                        "dbStatistics": {
                                                          "totalLogs": 15420,
                                                          "errorCount": 342,
                                                          "warnCount": 1205,
                                                          "infoCount": 13873,
                                                          "errorRate": 2.22,
                                                          "peakHour": "2025-11-14T12",
                                                          "peakCount": 892
                                                        },
                                                        "aiStatistics": {
                                                          "estimatedTotalLogs": 15380,
                                                          "estimatedErrorCount": 338,
                                                          "estimatedWarnCount": 1198,
                                                          "estimatedInfoCount": 13844,
                                                          "estimatedErrorRate": 2.20,
                                                          "confidenceScore": 85,
                                                          "reasoning": "샘플 100개 중 ERROR 2.2% 비율을 전체에 적용"
                                                        },
                                                        "accuracyMetrics": {
                                                          "totalLogsAccuracy": 99.74,
                                                          "errorCountAccuracy": 98.83,
                                                          "warnCountAccuracy": 99.42,
                                                          "infoCountAccuracy": 99.79,
                                                          "errorRateAccuracy": 99.80,
                                                          "overallAccuracy": 99.28,
                                                          "aiConfidence": 85
                                                        },
                                                        "verdict": {
                                                          "grade": "매우 우수",
                                                          "canReplaceDb": true,
                                                          "explanation": "오차율 5% 미만으로 프로덕션 환경에서 신뢰성 있게 사용 가능합니다.",
                                                          "recommendations": [
                                                            "프로덕션 환경 적용 권장",
                                                            "실시간 대시보드 AI 기반 분석 도입 가능",
                                                            "DB 부하 감소를 위한 AI 캐싱 레이어 구축"
                                                          ]
                                                        },
                                                        "technicalHighlights": [
                                                          "Temperature 0.1로 일관된 추론 (기본값 대비 7배 낮음)",
                                                          "샘플 100개로 15,420개 통계 추론",
                                                          "종합 정확도 99.28% 달성",
                                                          "Structured Output으로 JSON 스키마 강제",
                                                          "자동화된 다차원 정확도 검증",
                                                          "MCP/멀티모달 없이 단일 LLM으로 구현"
                                                        ]
                                                      },
                                                      "timestamp": "2025-11-14T15:30:00Z"
                                                    }
                                                    """
                                    )
                            )
                    )
            }
    )
    ResponseEntity<? extends BaseResponse> getAiComparison(
            @Parameter(description = "프로젝트 UUID", required = true, example = "3a73c7d4-8176-3929-b72f-d5b921daae67")
            @ValidUuid
            @RequestParam String projectUuid,

            @Parameter(description = "분석 기간 (시간)", example = "24")
            @RequestParam(defaultValue = "24") Integer timeHours,

            @Parameter(description = "AI 분석용 샘플 크기", example = "100")
            @RequestParam(defaultValue = "100") Integer sampleSize
    );
}


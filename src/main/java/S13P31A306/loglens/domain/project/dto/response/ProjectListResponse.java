package S13P31A306.loglens.domain.project.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record ProjectListResponse(

        @Schema(description = "프로젝트 리스트")
        List<ProjectInfo> content,

        @Schema(description = "페이지네이션 정보")
        Pagination pageable,

        @Schema(description = "총 프로젝트 갯수", example = "42")
        int totalElements,

        @Schema(description = "총 페이지 갯수", example = "10")
        int totalPages,

        @Schema(description = "첫 페이지 여부", example = "true")
        boolean first,

        @Schema(description = "마지막 페이지 여부", example = "false")
        boolean last

) {

    public record ProjectInfo(
            @Schema(description = "프로젝트 ID", example = "42")
            int projectId,

            @Schema(description = "프로직트 이름", example = "LogLens")
            String projectName,

            @Schema(description = "프로젝트 설명", example = "로그 수집 및 분석 프로젝트")
            String description,

            @Schema(description = "프로젝트 UUID", example = "pk_1a2b3c4d5e6f")
            String uuid,

            @Schema(description = "프로젝트에 참여 중인 멤버 수", example = "6")
            int memberCount,

            @Schema(description = "프로젝트 생성 시간", example = "2025-10-29T10:30:00")
            String createdAt,

            @Schema(description = "프로젝트 업데이트 시간", example = "2025-10-29T10:30:00")
            String updatedAt
    ) {
    }

    public record Pagination(
            @Schema(description = "현재 페이지", example = "1")
            int page,

            @Schema(description = "한 페이지에 들어가는 프로젝트 수", example = "20")
            int size,

            @Schema(description = "정렬 방식", example = "CREATED_AT")
            String sort,

            @Schema(description = "정렬 방향", example = "DESC")
            String order
    ) {
    }

}

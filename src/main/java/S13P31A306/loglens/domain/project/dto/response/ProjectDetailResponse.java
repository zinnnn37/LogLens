package S13P31A306.loglens.domain.project.dto.response;


import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record ProjectDetailResponse(
        @Schema(description = "프로젝트 ID", example = "42")
        int projectId,

        @Schema(description = "프로직트 이름", example = "LogLens")
        String projectName,

        @Schema(description = "프로젝트 설명", example = "로그 수집 및 분석 프로젝트")
        String description,

        @Schema(description = "프로젝트 API KEY", example = "pk_1a2b3c4d5e6f")
        String projectUuid,

        @Schema(description = "멤버 정보 리스트")
        List<Member> members,

        @Schema(description = "프로젝트 생성 시간", example = "2025-10-29T10:30:00")
        String createdAt,

        @Schema(description = "프로젝트 업데이트 시간", example = "2025-10-29T10:30:00")
        String updatedAt
        ) {

    public record Member(
            @Schema(description = "ID", example = "42")
            int userId,
            
            @Schema(description = "이름", example = "김철수")
            String name,

            @Schema(description = "이메일", example = "email@email.com")
            String email,

            @Schema(description = "프로젝트에 초대된 일시", example = "2025-10-29T10:30:00")
            String joinedAt
    ) {}

}

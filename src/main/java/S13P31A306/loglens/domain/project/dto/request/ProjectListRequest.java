package S13P31A306.loglens.domain.project.dto.request;

import S13P31A306.loglens.domain.project.constants.ProjectOrderParam;
import S13P31A306.loglens.domain.project.constants.ProjectConstants;
import S13P31A306.loglens.domain.project.constants.ProjectSortParam;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Objects;

public record ProjectListRequest(
        @Schema(description = "페이지 번호 (0부터 시작)", example = "0", defaultValue = "0")
        Integer page,

        @Schema(description = "페이지당 항목 수 (1~100)", example = "10", defaultValue = "10")
        Integer size,

        @Schema(description = "정렬 기준 (CREATED_AT, PROJECT_NAME, UPDATED_AT)", example = "CREATED_AT", defaultValue = "CREATED_AT")
        String sort,

        @Schema(description = "정렬 방향 (ASC, DESC)", example = "DESC", defaultValue = "DESC")
        String order
) {
    // 기본값
    public ProjectListRequest {
        page = Objects.isNull(page) ? ProjectConstants.MIN_PAGE_NUMBER : page;
        size = Objects.isNull(size) ? ProjectConstants.DEFAULT_PAGE_SIZE : size;
        sort = (Objects.isNull(sort) || sort.isBlank()) ? "CREATED_AT" : sort;
        order = (Objects.isNull(order) || order.isBlank()) ? "DESC" : order;
    }

    public ProjectSortParam getSortParam() {
        return ProjectSortParam.from(sort);
    }

    public ProjectOrderParam getOrderParam() {
        return ProjectOrderParam.from(order);
    }
}

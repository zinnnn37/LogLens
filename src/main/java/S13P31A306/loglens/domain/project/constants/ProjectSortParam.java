package S13P31A306.loglens.domain.project.constants;

import S13P31A306.loglens.global.exception.BusinessException;

import static S13P31A306.loglens.domain.project.constants.ProjectErrorCode.INVALID_PAGE_SORT;

public enum ProjectSortParam {
    CREATED_AT,
    UPDATED_AT,
    PROJECT_NAME;

    public static ProjectSortParam from(String value) {
        try {
            return ProjectSortParam.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(INVALID_PAGE_SORT);
        }
    }
}
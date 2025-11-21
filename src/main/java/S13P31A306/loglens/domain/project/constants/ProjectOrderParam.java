package S13P31A306.loglens.domain.project.constants;

import S13P31A306.loglens.global.exception.BusinessException;

import static S13P31A306.loglens.domain.project.constants.ProjectErrorCode.INVALID_PAGE_ORDER;

public enum ProjectOrderParam {
    ASC,
    DESC;

    public static ProjectOrderParam from(String value) {
        try {
            return ProjectOrderParam.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(INVALID_PAGE_ORDER);
        }
    }
}


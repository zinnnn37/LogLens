package S13P31A306.loglens.domain.dashboard.dto.response;

import java.util.List;

public record DatabaseComponentResponse(
        List<String> databases
) {
}

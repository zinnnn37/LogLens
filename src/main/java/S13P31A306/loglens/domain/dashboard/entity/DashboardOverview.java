package S13P31A306.loglens.domain.dashboard.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@Entity
@RequiredArgsConstructor
public class DashboardOverview {

    @Id
    private Integer id;

}

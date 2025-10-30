package S13P31A306.loglens.global.entity;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;

// @formatter:off
/**
 * 모든 엔티티에서 공통적으로 사용하는 속성(id, createdAt, updatedAt)을 정의한 추상 클래스
 * - @MappedSuperclass: 해당 클래스를 상속한 자식 엔티티가 이 필드들을 컬럼으로 인식하게 합니다.
 * - BaseTimeEntity를 상속하여 시간 관련 필드를 포함합니다.
 */
// @formatter:on
@MappedSuperclass
@Getter
public abstract class BaseEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
}

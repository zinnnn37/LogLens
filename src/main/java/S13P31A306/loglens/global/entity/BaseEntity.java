package S13P31A306.loglens.global.entity;

import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.MappedSuperclass;
import java.time.LocalDateTime;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import jakarta.persistence.Id;
import jakarta.persistence.Column;

// @formatter:off
/**
 * 모든 엔티티에서 공통적으로 사용하는 속성(createdAt, updatedAt, deleted 등)을 정의한 추상 클래스
 * - @MappedSuperclass: 해당 클래스를 상속한 자식 엔티티가 이 필드들을 컬럼으로 인식하게 합니다.
 * - @EntityListeners(AuditingEntityListener.class): Date 필드를 자동으로 관리하기 위해 등록합니다.
 */
// @formatter:on
@EntityListeners(AuditingEntityListener.class)
@MappedSuperclass
@Getter
public abstract class BaseEntity {

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}

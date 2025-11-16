package S13P31A306.loglens.domain.analysis.repository;

import S13P31A306.loglens.domain.analysis.entity.AnalysisDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 분석 문서 레포지토리
 */
@Repository
public interface AnalysisDocumentRepository extends JpaRepository<AnalysisDocument, Integer> {

    /**
     * 프로젝트별 문서 목록 조회 (최신순)
     */
    Page<AnalysisDocument> findByProjectIdOrderByCreatedAtDesc(Integer projectId, Pageable pageable);

    /**
     * 프로젝트와 문서 ID로 조회
     */
    Optional<AnalysisDocument> findByIdAndProjectId(Integer id, Integer projectId);

    /**
     * 프로젝트별 문서 수 조회
     */
    long countByProjectId(Integer projectId);
}

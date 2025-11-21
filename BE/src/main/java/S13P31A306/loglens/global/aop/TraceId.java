package S13P31A306.loglens.global.aop;

import java.util.UUID;

/**
 * TraceId - Record 버전 (권장)
 *
 * 특징:
 * - 불변(immutable) 데이터 구조
 * - 자동 생성: id(), level(), equals(), hashCode(), toString()
 * - 정적 팩토리 메서드로 깔끔한 생성
 *
 * 사용법:
 * <pre>
 * TraceId trace = TraceId.create();       // 새 TraceId 생성
 * String id = trace.id();                 // getter → id()
 * int level = trace.level();              // getter → level()
 * TraceId next = trace.createNextId();    // 레벨 증가
 * </pre>
 */
public record TraceId(String id, int level) {

    /**
     * 새로운 TraceId 생성 (레벨 1)
     *
     * @return 랜덤 ID를 가진 레벨 1 TraceId
     */
    public static TraceId create() {
        return new TraceId(generateId(), 1);
    }

    /**
     * 다음 레벨 TraceId 생성
     *
     * @return 동일한 ID, 레벨 +1인 새 TraceId
     */
    public TraceId createNextId() {
        return new TraceId(id, level + 1);
    }

    /**
     * 이전 레벨 TraceId 생성
     *
     * @return 동일한 ID, 레벨 -1인 새 TraceId
     */
    public TraceId createPreviousId() {
        return new TraceId(id, level - 1);
    }

    /**
     * 첫 번째 레벨인지 확인
     *
     * @return 레벨이 1이면 true
     */
    public boolean isFirstLevel() {
        return level == 1;
    }

    /**
     * UUID 기반 8자리 ID 생성
     *
     * @return 8자리 랜덤 문자열
     */
    private static String generateId() {
        return UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 8);
    }
}

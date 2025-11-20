package com.example.demo.global.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 실제 알림 전송 담당 컴포넌트
 * ✅ 별도 Bean이므로 Aspect 적용됨
 */
@Slf4j
@Component
public class NotificationSender {

    /**
     * 웰컴 알림 전송
     * ✅ Public 메서드 + 외부 호출 → Aspect 로깅됨
     */
    public void sendWelcomeNotification(Long userId, String email) {
        log.info("[알림] ✉️ 웰컴 알림 전송 완료: userId={}, email={}", userId, email);

        // 실제 이메일/SMS 전송 로직
        // emailService.send(email, "Welcome!", "환영합니다!");
    }

    /**
     * 주문 확인 알림 전송
     */
    public void sendOrderConfirmation(String orderId, String userId) {
        log.info("[알림] 📦 주문 확인 알림 전송 완료: orderId={}, userId={}", orderId, userId);

        // 실제 알림 전송 로직
    }

    /**
     * 복잡한 작업 단계별 실행
     */
    public String executeStep1(String taskId) {
        log.info("[작업] 1단계 실행: taskId={}", taskId);
        // 실제 로직
        return "STEP1_DONE";
    }

    public String executeStep2(String taskId, String previousResult) {
        log.info("[작업] 2단계 실행: taskId={}, previous={}", taskId, previousResult);
        // 실제 로직
        return "STEP2_DONE";
    }

    public String executeStep3(String taskId, String previousResult) {
        log.info("[작업] 3단계 실행 완료: taskId={}, previous={}", taskId, previousResult);
        // 실제 로직
        return "ALL_STEPS_COMPLETED";
    }
}

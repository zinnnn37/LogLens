package com.example.demo.global.service;

import a306.dependency_logger_starter.logging.async.AsyncExecutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/**
 * 비동기 알림 스케줄러 (AsyncExecutor 활용)
 * ✅ MDC 전파를 신경쓰지 않아도 됨
 * ✅ 실제 작업은 NotificationSender에 위임
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AsyncNotificationService {

    private final AsyncExecutor asyncExecutor;  // ✅ 라이브러리 제공
    private final NotificationSender notificationSender;

    /**
     * 웰컴 알림을 60초 후 전송
     * ✅ AsyncExecutor가 MDC 전파를 자동 처리
     */
    public CompletableFuture<Void> sendWelcomeNotificationAfterDelay(Long userId, String email) {
        return asyncExecutor.runAfterDelay(
                () -> sendWelcomeNotification(userId, email),
                Duration.ofMinutes(1)
        );
    }

    private void sendWelcomeNotification(Long userId, String email) {
        log.info("[알림] ✉️ 웰컴 알림 전송 완료: userId={}, email={}", userId, email);

        // 실제 이메일/SMS 전송 로직
        // emailService.send(email, "Welcome!", "환영합니다!");
    }

//    @Async  // ✅ Spring 표준 방식
//    public void sendWelcomeNotificationAfterDelay(Long userId, String email) {
//        // ✅ MDC 자동 전파됨 (BeanPostProcessor가 처리)
//        // ✅ 메서드 자동 로깅됨 (MethodLoggingAspect)
//        notificationSender.sendWelcomeNotification(userId, email);
//    }


//    /**
//     * 비밀번호 변경 알림을 60초 후 전송
//     */
//    public CompletableFuture<Void> sendPasswordChangeNotificationAfterDelay(Long userId, String email) {
//        log.info("[예약] 비밀번호 변경 알림 예약: userId={}, email={}", userId, email);
//
//        return asyncExecutor.runAfterDelay(
//                () -> notificationSender.sendPasswordChangeNotification(userId, email),
//                Duration.ofMinutes(1)
//        );
//    }
//
//    /**
//     * 정보 변경 알림을 60초 후 전송
//     */
//    public CompletableFuture<Void> sendUpdateNotificationAfterDelay(
//            Long userId, String email, String changedFields) {
//
//        log.info("[예약] 정보 변경 알림 예약: userId={}, email={}, fields={}",
//                userId, email, changedFields);
//
//        return asyncExecutor.runAfterDelay(
//                () -> notificationSender.sendUpdateNotification(userId, email, changedFields),
//                Duration.ofMinutes(1)
//        );
//    }
//
//    /**
//     * 커스텀 알림을 지정된 시간 후 전송
//     */
//    public CompletableFuture<Void> sendCustomDelayNotification(
//            Long userId, long delaySeconds, String message) {
//
//        log.info("[예약] 커스텀 알림 예약: userId={}, delay={}초, message={}",
//                userId, delaySeconds, message);
//
//        return asyncExecutor.runAfterDelay(
//                () -> notificationSender.sendCustomNotification(userId, message),
//                Duration.ofSeconds(delaySeconds)
//        );
//    }
//
//    /**
//     * 즉시 알림 전송
//     */
//    public CompletableFuture<Void> sendImmediateNotification(Long userId) {
//        log.info("[예약] 즉시 알림 전송 시작: userId={}", userId);
//
//        return asyncExecutor.run(
//                () -> notificationSender.sendImmediateNotification(userId)
//        );
//    }
//
//    /**
//     * 여러 알림을 병렬로 즉시 전송
//     */
//    public CompletableFuture<Void> sendMultipleNotifications(
//            Long userId, String email, String... messages) {
//
//        log.info("[예약] 다중 알림 전송 시작: userId={}, count={}", userId, messages.length);
//
//        return asyncExecutor.runAll(
//                () -> notificationSender.sendWelcomeNotification(userId, email),
//                () -> notificationSender.sendImmediateNotification(userId)
//                // 필요한 만큼 추가 가능
//        );
//    }
}

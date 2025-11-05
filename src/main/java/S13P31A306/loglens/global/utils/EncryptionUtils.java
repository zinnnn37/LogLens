package S13P31A306.loglens.global.utils;

import S13P31A306.loglens.global.annotation.Sensitive;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;

/**
 * 암호화/복호화 유틸리티
 * AES-256 알고리즘을 사용하여 민감 정보(API 토큰 등)를 암호화/복호화합니다.
 */
@Slf4j
@Component
public class EncryptionUtils {

    private static final String ALGORITHM = "AES/CBC/PKCS5Padding";
    private static final String KEY_ALGORITHM = "AES";
    private static final String LOG_PREFIX = "[EncryptionUtils]";

    private final byte[] keyBytes;
    private final byte[] ivBytes;

    /**
     * 생성자
     * application.yml의 secret-key를 사용하여 암호화 키를 생성합니다.
     * ENCRYPTION_SECRET_KEY 환경변수가 설정되지 않은 경우 예외가 발생합니다.
     *
     * @param secretKey 암호화 시크릿 키 (최소 16자 이상 권장)
     * @throws IllegalStateException secretKey가 null이거나 비어있을 경우
     */
    public EncryptionUtils(@Value("${encryption.secret-key}") @Sensitive String secretKey) {
        // secretKey null/empty 검증
        if (secretKey == null || secretKey.trim().isEmpty()) {
            String errorMessage = "ENCRYPTION_SECRET_KEY 는 환경 변수로 설정해야 한다 ";
            log.error("{} {}", LOG_PREFIX, errorMessage);
            throw new IllegalStateException(errorMessage);
        }

        // secretKey 최소 길이 검증 (보안 강화)
        if (secretKey.length() < 16) {
            String errorMessage = "ENCRYPTION_SECRET_KEY는 16자 이상이어야 한다. 현재 길이: " + secretKey.length();
            log.error("{} {}", LOG_PREFIX, errorMessage);
            throw new IllegalStateException(errorMessage);
        }

        try {
            // 시크릿 키를 SHA-256으로 해싱하여 32바이트 키 생성
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            byte[] key = sha.digest(secretKey.getBytes(StandardCharsets.UTF_8));
            this.keyBytes = Arrays.copyOf(key, 32); // AES-256용 32바이트
            this.ivBytes = Arrays.copyOf(key, 16);  // IV용 16바이트

            log.info("{} ✅ 암호화 유틸리티 초기화 완료", LOG_PREFIX);
        } catch (Exception e) {
            log.error("{} 암호화 키 생성 실패", LOG_PREFIX, e);
            throw new IllegalStateException("암호화 초기화 실패", e);
        }
    }

    /**
     * 문자열 암호화
     *
     * @param plainText 평문
     * @return Base64 인코딩된 암호문
     */
    public String encrypt(String plainText) {
        if (plainText == null || plainText.isEmpty()) {
            return plainText;
        }

        try {
            SecretKeySpec secretKey = new SecretKeySpec(keyBytes, KEY_ALGORITHM);
            IvParameterSpec iv = new IvParameterSpec(ivBytes);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, iv);

            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            String encoded = Base64.getEncoder().encodeToString(encrypted);

            log.debug("{} 🔐 암호화 완료: length={}", LOG_PREFIX, encoded.length());
            return encoded;

        } catch (Exception e) {
            log.error("{} 암호화 실패", LOG_PREFIX, e);
            throw new IllegalStateException("암호화 처리 중 오류 발생", e);
        }
    }

    /**
     * 문자열 복호화
     *
     * @param encryptedText Base64 인코딩된 암호문
     * @return 평문
     */
    public String decrypt(String encryptedText) {
        if (encryptedText == null || encryptedText.isEmpty()) {
            return encryptedText;
        }

        try {
            SecretKeySpec secretKey = new SecretKeySpec(keyBytes, KEY_ALGORITHM);
            IvParameterSpec iv = new IvParameterSpec(ivBytes);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, iv);

            byte[] decodedBytes = Base64.getDecoder().decode(encryptedText);
            byte[] decrypted = cipher.doFinal(decodedBytes);
            String plainText = new String(decrypted, StandardCharsets.UTF_8);

            log.debug("{} 🔓 복호화 완료", LOG_PREFIX);
            return plainText;

        } catch (Exception e) {
            log.error("{} 복호화 실패", LOG_PREFIX, e);
            throw new IllegalStateException("복호화 처리 중 오류 발생", e);
        }
    }
}

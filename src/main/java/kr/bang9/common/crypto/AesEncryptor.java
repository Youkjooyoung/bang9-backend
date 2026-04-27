package kr.bang9.common.crypto;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import kr.bang9.common.exception.CustomException;
import kr.bang9.common.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AesEncryptor {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int IV_LENGTH = 12;
    private static final int TAG_LENGTH_BIT = 128;

    private final SecretKeySpec keySpec;

    public AesEncryptor(@Value("${aes.secret-key}") String base64Key) {
        byte[] keyBytes = Base64.getDecoder().decode(base64Key);
        if (keyBytes.length != 32) {
            throw new IllegalStateException("AES_SECRET_KEY 는 Base64 디코딩 시 32바이트여야 합니다. 현재: " + keyBytes.length);
        }
        this.keySpec = new SecretKeySpec(keyBytes, "AES");
    }

    public byte[] encrypt(String plain) {
        if (plain == null) return null;
        try {
            byte[] iv = new byte[IV_LENGTH];
            new SecureRandom().nextBytes(iv);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, new GCMParameterSpec(TAG_LENGTH_BIT, iv));
            byte[] cipherText = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));
            byte[] result = new byte[IV_LENGTH + cipherText.length];
            System.arraycopy(iv, 0, result, 0, IV_LENGTH);
            System.arraycopy(cipherText, 0, result, IV_LENGTH, cipherText.length);
            return result;
        } catch (Exception ex) {
            log.error("AES 암호화 실패", ex);
            throw new CustomException(ErrorCode.ERR_INTERNAL, "데이터 암호화에 실패했습니다.");
        }
    }

    public String decrypt(byte[] payload) {
        if (payload == null || payload.length <= IV_LENGTH) return null;
        try {
            byte[] iv = new byte[IV_LENGTH];
            byte[] cipherText = new byte[payload.length - IV_LENGTH];
            System.arraycopy(payload, 0, iv, 0, IV_LENGTH);
            System.arraycopy(payload, IV_LENGTH, cipherText, 0, cipherText.length);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, new GCMParameterSpec(TAG_LENGTH_BIT, iv));
            byte[] plain = cipher.doFinal(cipherText);
            return new String(plain, StandardCharsets.UTF_8);
        } catch (Exception ex) {
            log.error("AES 복호화 실패", ex);
            throw new CustomException(ErrorCode.ERR_INTERNAL, "데이터 복호화에 실패했습니다.");
        }
    }

    public String hash(String plain) {
        if (plain == null) return null;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(plain.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : digest) hex.append(String.format("%02x", b));
            return hex.toString();
        } catch (Exception ex) {
            throw new CustomException(ErrorCode.ERR_INTERNAL, "해시 생성에 실패했습니다.");
        }
    }
}

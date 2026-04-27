package kr.bang9.common.crypto;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Base64;

@Component
public class AesCryptoService {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;

    private final SecretKeySpec keySpec;
    private final SecureRandom random = new SecureRandom();

    public AesCryptoService(@Value("${aes.secret-key}") String base64Key) {
        byte[] keyBytes = Base64.getDecoder().decode(base64Key);
        if (keyBytes.length != 32) {
            throw new IllegalArgumentException("AES 키는 32바이트(256비트)여야 합니다.");
        }
        this.keySpec = new SecretKeySpec(keyBytes, "AES");
    }

    public byte[] encrypt(String plaintext) {
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            byte[] cipherText = cipher.doFinal(plaintext.getBytes());
            ByteBuffer buffer = ByteBuffer.allocate(iv.length + cipherText.length);
            buffer.put(iv).put(cipherText);
            return buffer.array();
        } catch (Exception e) {
            throw new IllegalStateException("암호화 실패", e);
        }
    }

    public String decrypt(byte[] encrypted) {
        try {
            ByteBuffer buffer = ByteBuffer.wrap(encrypted);
            byte[] iv = new byte[GCM_IV_LENGTH];
            buffer.get(iv);
            byte[] cipherText = new byte[buffer.remaining()];
            buffer.get(cipherText);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            return new String(cipher.doFinal(cipherText));
        } catch (Exception e) {
            throw new IllegalStateException("복호화 실패", e);
        }
    }
}

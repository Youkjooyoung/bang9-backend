package kr.bang9.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtUtil {

    private final SecretKey signingKey;
    private final long accessExpireMillis;
    private final long refreshExpireMillis;

    public JwtUtil(
        @Value("${jwt.secret}") String secret,
        @Value("${jwt.access-expire-minutes:30}") long accessMinutes,
        @Value("${jwt.refresh-expire-days:14}") long refreshDays
    ) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessExpireMillis = Duration.ofMinutes(accessMinutes).toMillis();
        this.refreshExpireMillis = Duration.ofDays(refreshDays).toMillis();
    }

    public String generateAccessToken(long userId, String role) {
        return buildToken(userId, role, accessExpireMillis, "access");
    }

    public String generateRefreshToken(long userId, String role) {
        return buildToken(userId, role, refreshExpireMillis, "refresh");
    }

    public Claims parseClaims(String token) {
        return Jwts.parser()
            .verifyWith(signingKey)
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }

    public long getAccessExpireMillis() {
        return accessExpireMillis;
    }

    public long getRefreshExpireMillis() {
        return refreshExpireMillis;
    }

    private String buildToken(long userId, String role, long expireMillis, String type) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expireMillis);
        return Jwts.builder()
            .id(UUID.randomUUID().toString())
            .subject(String.valueOf(userId))
            .claim("role", role)
            .claim("type", type)
            .issuedAt(now)
            .expiration(expiry)
            .signWith(signingKey)
            .compact();
    }
}

package kr.bang9.identity.dto;

import java.time.LocalDateTime;

public record IdentityStatusResponse(
        boolean verified,
        String realName,
        LocalDateTime verifiedAt
) {
    public static IdentityStatusResponse notVerified() {
        return new IdentityStatusResponse(false, null, null);
    }

    public static IdentityStatusResponse verified(String name, LocalDateTime at) {
        return new IdentityStatusResponse(true, name, at);
    }
}

package kr.bang9.identity.dto;

import jakarta.validation.constraints.NotBlank;

public record IdentityVerifyRequest(
        @NotBlank(message = "본인인증 impUid는 필수입니다.") String impUid
) {}

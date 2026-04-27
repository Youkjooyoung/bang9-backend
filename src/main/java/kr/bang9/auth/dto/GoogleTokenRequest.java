package kr.bang9.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record GoogleTokenRequest(
        @NotBlank String idToken
) {}

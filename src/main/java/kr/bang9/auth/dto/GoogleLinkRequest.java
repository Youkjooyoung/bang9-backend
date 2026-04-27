package kr.bang9.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record GoogleLinkRequest(@NotBlank String idToken) {}

package kr.bang9.common.security;

public record AuthPrincipal(
    long userId,
    String role
) {
}

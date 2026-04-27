package kr.bang9.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kr.bang9.auth.service.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        String token = resolveToken(request);
        if (token != null) {
            try {
                Claims claims = jwtUtil.parseClaims(token);
                String type = claims.get("type", String.class);
                String jti = claims.getId();
                if ("access".equals(type) && !refreshTokenService.isBlacklisted(jti)) {
                    setAuthentication(claims);
                }
            } catch (JwtException ignored) {
            }
        }
        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String bearer = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(bearer) && bearer.startsWith(BEARER_PREFIX)) {
            return bearer.substring(BEARER_PREFIX.length());
        }
        return null;
    }

    private void setAuthentication(Claims claims) {
        long userId = Long.parseLong(claims.getSubject());
        String role = claims.get("role", String.class);
        AuthPrincipal principal = new AuthPrincipal(userId, role);
        var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role));
        UsernamePasswordAuthenticationToken auth =
            new UsernamePasswordAuthenticationToken(principal, null, authorities);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}

package kr.bang9.identity.controller;

import jakarta.validation.Valid;
import kr.bang9.common.security.AuthPrincipal;
import kr.bang9.identity.dto.IdentityStatusResponse;
import kr.bang9.identity.dto.IdentityVerifyRequest;
import kr.bang9.identity.service.IdentityService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/identity")
@RequiredArgsConstructor
public class IdentityController {

    private final IdentityService identityService;

    @GetMapping("/me")
    public ResponseEntity<IdentityStatusResponse> me(@AuthenticationPrincipal AuthPrincipal principal) {
        return ResponseEntity.ok(identityService.getStatus(principal.userId()));
    }

    @PostMapping("/verify")
    public ResponseEntity<IdentityStatusResponse> verify(
            @AuthenticationPrincipal AuthPrincipal principal,
            @Valid @RequestBody IdentityVerifyRequest request
    ) {
        return ResponseEntity.ok(identityService.verify(principal.userId(), request.impUid()));
    }
}

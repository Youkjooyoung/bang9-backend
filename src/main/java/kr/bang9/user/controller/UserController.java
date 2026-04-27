package kr.bang9.user.controller;

import jakarta.validation.Valid;
import kr.bang9.auth.dto.MeResponse;
import kr.bang9.common.exception.CustomException;
import kr.bang9.common.exception.ErrorCode;
import kr.bang9.common.security.AuthPrincipal;
import kr.bang9.user.domain.User;
import kr.bang9.user.dto.ChangePasswordRequest;
import kr.bang9.user.dto.UpdateProfileRequest;
import kr.bang9.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<MeResponse> me(@AuthenticationPrincipal AuthPrincipal principal) {
        requireLogin(principal);
        User user = userService.getUser(principal.userId());
        return ResponseEntity.ok(MeResponse.from(user));
    }

    @PutMapping("/me")
    public ResponseEntity<MeResponse> updateMe(
        @AuthenticationPrincipal AuthPrincipal principal,
        @Valid @RequestBody UpdateProfileRequest request
    ) {
        requireLogin(principal);
        String bd = request.birthDate();
        LocalDate birthDate = (bd != null && !bd.isBlank()) ? LocalDate.parse(bd) : null;
        String phone = (request.phone() != null && !request.phone().isBlank()) ? request.phone() : null;
        String name = (request.name() != null && !request.name().isBlank()) ? request.name() : null;
        String gender = (request.gender() != null && !request.gender().isBlank()) ? request.gender() : null;
        User updated = userService.updateProfile(
            principal.userId(),
            request.nickname(),
            request.profileImageUrl(),
            phone,
            name,
            birthDate,
            gender
        );
        return ResponseEntity.ok(MeResponse.from(updated));
    }

    @PutMapping("/me/password")
    public ResponseEntity<Void> changePassword(
        @AuthenticationPrincipal AuthPrincipal principal,
        @Valid @RequestBody ChangePasswordRequest request
    ) {
        requireLogin(principal);
        userService.changePassword(principal.userId(), request.currentPassword(), request.newPassword());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> withdraw(@AuthenticationPrincipal AuthPrincipal principal) {
        requireLogin(principal);
        userService.withdraw(principal.userId());
        return ResponseEntity.noContent().build();
    }

    private void requireLogin(AuthPrincipal principal) {
        if (principal == null) {
            throw new CustomException(ErrorCode.ERR_AUTH_INVALID);
        }
    }
}

package kr.bang9.user.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    private Long userId;
    private String email;
    private String passwordHash;
    private String nickname;
    private String name;
    private LocalDate birthDate;
    private String gender;
    private String profileImageUrl;
    private String role;
    private String oauthProvider;
    private String oauthProviderId;
    private String phone;
    private String status;
    private LocalDateTime lastLoginAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
}

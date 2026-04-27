package kr.bang9.identity.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserIdentity {

    private Long identityId;
    private Long userId;
    private String realName;
    private byte[] rrnEncrypted;
    private String rrnHash;
    private LocalDateTime verifiedAt;
    private String portoneImpUid;
    private LocalDateTime createdAt;
}

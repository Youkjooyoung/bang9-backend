package kr.bang9.identity.service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import kr.bang9.common.crypto.AesEncryptor;
import kr.bang9.common.exception.CustomException;
import kr.bang9.common.exception.ErrorCode;
import kr.bang9.external.portone.PortOneCertification;
import kr.bang9.external.portone.PortOneClient;
import kr.bang9.identity.dao.UserIdentityDao;
import kr.bang9.identity.domain.UserIdentity;
import kr.bang9.identity.dto.IdentityStatusResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class IdentityService {

    private final PortOneClient portOneClient;
    private final AesEncryptor aesEncryptor;
    private final UserIdentityDao identityDao;

    @Transactional(readOnly = true)
    public IdentityStatusResponse getStatus(Long userId) {
        UserIdentity identity = identityDao.findByUserId(userId);
        if (identity == null) return IdentityStatusResponse.notVerified();
        return IdentityStatusResponse.verified(identity.getRealName(), identity.getVerifiedAt());
    }

    @Transactional
    public IdentityStatusResponse verify(Long userId, String impUid) {
        UserIdentity existing = identityDao.findByUserId(userId);
        if (existing != null) {
            return IdentityStatusResponse.verified(existing.getRealName(), existing.getVerifiedAt());
        }

        PortOneCertification cert = portOneClient.fetchCertification(impUid);

        if (cert.rrn() == null || cert.rrn().length() != 7) {
            throw new CustomException(ErrorCode.ERR_IDENTITY_VERIFICATION_FAILED, "주민번호 앞 7자리 형식이 올바르지 않습니다.");
        }

        String rrnHash = aesEncryptor.hash(cert.rrn());
        UserIdentity dupe = identityDao.findByRrnHash(rrnHash);
        if (dupe != null && !dupe.getUserId().equals(userId)) {
            throw new CustomException(ErrorCode.ERR_IDENTITY_DUPLICATE);
        }

        LocalDateTime verifiedAt = cert.certifiedAtEpoch() > 0
                ? LocalDateTime.ofInstant(Instant.ofEpochSecond(cert.certifiedAtEpoch()), ZoneId.of("Asia/Seoul"))
                : LocalDateTime.now();

        UserIdentity identity = UserIdentity.builder()
                .userId(userId)
                .realName(cert.realName())
                .rrnEncrypted(aesEncryptor.encrypt(cert.rrn()))
                .rrnHash(rrnHash)
                .verifiedAt(verifiedAt)
                .portoneImpUid(cert.impUid())
                .build();
        identityDao.insert(identity);
        log.info("본인인증 완료 userId={} impUid={}", userId, cert.impUid());
        return IdentityStatusResponse.verified(identity.getRealName(), identity.getVerifiedAt());
    }

    @Transactional(readOnly = true)
    public boolean isVerified(Long userId) {
        return identityDao.findByUserId(userId) != null;
    }
}

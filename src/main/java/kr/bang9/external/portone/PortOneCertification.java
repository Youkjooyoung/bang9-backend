package kr.bang9.external.portone;

public record PortOneCertification(
        String impUid,
        String realName,
        String rrn,
        String uniqueKey,
        long certifiedAtEpoch
) {}

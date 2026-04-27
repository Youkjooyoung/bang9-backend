package kr.bang9.common.upload.dto;

public record PresignedUrlResponse(
    String uploadUrl,
    String publicUrl,
    String objectKey,
    long expiresInSeconds
) {
}

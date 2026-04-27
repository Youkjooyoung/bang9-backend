package kr.bang9.common.upload.service;

import kr.bang9.common.exception.CustomException;
import kr.bang9.common.exception.ErrorCode;
import kr.bang9.common.upload.dto.PresignedUrlRequest;
import kr.bang9.common.upload.dto.PresignedUrlResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.upload.mode", havingValue = "s3", matchIfMissing = false)
public class S3UploadService implements UploadService {

    private static final long EXPIRE_SECONDS = 300L;

    private final S3Presigner s3Presigner;

    @Value("${aws.s3-bucket}")
    private String bucket;

    @Value("${aws.region:ap-northeast-2}")
    private String region;

    @Override
    public PresignedUrlResponse generatePresignedUrl(long userId, PresignedUrlRequest request) {
        validateRequest(request);
        String objectKey = buildObjectKey(userId, request);
        String uploadUrl = createPutPresignedUrl(objectKey, request.contentType());
        String publicUrl = buildPublicUrl(objectKey);
        return new PresignedUrlResponse(uploadUrl, publicUrl, objectKey, EXPIRE_SECONDS);
    }

    @Override
    public String extractObjectKeyFromUrl(String publicUrl) {
        if (publicUrl == null || publicUrl.isBlank()) {
            return null;
        }
        String prefix = buildPublicUrlPrefix();
        if (publicUrl.startsWith(prefix)) {
            return publicUrl.substring(prefix.length());
        }
        return null;
    }

    private void validateRequest(PresignedUrlRequest request) {
        if (request.fileName().contains("..") || request.fileName().contains("/")) {
            throw new CustomException(ErrorCode.ERR_INVALID_PARAMETER);
        }
    }

    private String buildObjectKey(long userId, PresignedUrlRequest request) {
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String extension = resolveExtension(request.fileName());
        String fileId = UUID.randomUUID().toString().replace("-", "");
        return String.format("%s/%s/%d/%s%s",
            request.kind(), datePart, userId, fileId, extension);
    }

    private String resolveExtension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        if (dot < 0 || dot == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(dot).toLowerCase();
    }

    private String createPutPresignedUrl(String objectKey, String contentType) {
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
            .bucket(bucket)
            .key(objectKey)
            .contentType(contentType)
            .build();
        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
            .signatureDuration(Duration.ofSeconds(EXPIRE_SECONDS))
            .putObjectRequest(putObjectRequest)
            .build();
        PresignedPutObjectRequest presigned = s3Presigner.presignPutObject(presignRequest);
        return presigned.url().toString();
    }

    private String buildPublicUrl(String objectKey) {
        return buildPublicUrlPrefix() + objectKey;
    }

    private String buildPublicUrlPrefix() {
        return String.format("https://%s.s3.%s.amazonaws.com/", bucket, region);
    }
}

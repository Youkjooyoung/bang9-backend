package kr.bang9.common.upload.service;

import kr.bang9.common.exception.CustomException;
import kr.bang9.common.exception.ErrorCode;
import kr.bang9.common.upload.dto.PresignedUrlRequest;
import kr.bang9.common.upload.dto.PresignedUrlResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
@ConditionalOnProperty(name = "app.upload.mode", havingValue = "local", matchIfMissing = true)
public class LocalUploadService implements UploadService {

    private static final long EXPIRE_SECONDS = 300L;

    private final String storagePath;
    private final String publicBaseUrl;

    public LocalUploadService(
        @Value("${app.upload.local.storage-path:./uploads}") String storagePath,
        @Value("${app.upload.local.public-base-url:http://localhost:8089}") String publicBaseUrl
    ) {
        this.storagePath = storagePath;
        this.publicBaseUrl = stripTrailingSlash(publicBaseUrl);
    }

    @Override
    public PresignedUrlResponse generatePresignedUrl(long userId, PresignedUrlRequest request) {
        validateRequest(request);
        String objectKey = buildObjectKey(userId, request);
        String uploadUrl = publicBaseUrl + "/api/uploads/local/" + objectKey;
        String publicUrl = publicBaseUrl + "/uploads/" + objectKey;
        return new PresignedUrlResponse(uploadUrl, publicUrl, objectKey, EXPIRE_SECONDS);
    }

    @Override
    public String extractObjectKeyFromUrl(String publicUrl) {
        if (publicUrl == null || publicUrl.isBlank()) {
            return null;
        }
        String prefix = this.publicBaseUrl + "/uploads/";
        if (publicUrl.startsWith(prefix)) {
            return publicUrl.substring(prefix.length());
        }
        return null;
    }

    public Path resolveTarget(String objectKey) {
        Path base = Paths.get(storagePath).toAbsolutePath().normalize();
        Path resolved = base.resolve(objectKey).normalize();
        if (!resolved.startsWith(base)) {
            throw new CustomException(ErrorCode.ERR_INVALID_PARAMETER);
        }
        return resolved;
    }

    public String getStoragePath() {
        return storagePath;
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

    private String stripTrailingSlash(String url) {
        if (url == null) {
            return "";
        }
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}

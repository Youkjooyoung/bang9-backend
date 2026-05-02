package kr.bang9.common.upload.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import kr.bang9.common.exception.CustomException;
import kr.bang9.common.exception.ErrorCode;
import kr.bang9.common.security.AuthPrincipal;
import kr.bang9.common.upload.dto.PresignedUrlRequest;
import kr.bang9.common.upload.dto.PresignedUrlResponse;
import kr.bang9.common.upload.service.LocalUploadService;
import kr.bang9.common.upload.service.UploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@RestController
@RequestMapping("/api/uploads")
@RequiredArgsConstructor
public class UploadController {

    private static final long MAX_UPLOAD_BYTES = 10L * 1024 * 1024;
    private static final String OBJECT_KEY_PATTERN = "^(listing|profile|product)/\\d{4}/\\d{2}/\\d{2}/\\d+/[a-f0-9]{32}\\.(jpg|jpeg|png|webp|gif)$";

    private final UploadService uploadService;
    private final ObjectProvider<LocalUploadService> localUploadServiceProvider;

    @PostMapping("/presigned")
    public ResponseEntity<PresignedUrlResponse> presignedUrl(
        @AuthenticationPrincipal AuthPrincipal principal,
        @Valid @RequestBody PresignedUrlRequest request
    ) {
        return ResponseEntity.ok(
            uploadService.generatePresignedUrl(principal.userId(), request)
        );
    }

    @PutMapping("/local/**")
    public ResponseEntity<Void> localUpload(HttpServletRequest request) {
        LocalUploadService local = localUploadServiceProvider.getIfAvailable();
        if (local == null) {
            throw new CustomException(ErrorCode.ERR_INVALID_PARAMETER);
        }
        String objectKey = resolveObjectKey(request);
        saveRawBody(local, objectKey, request);
        return ResponseEntity.ok().build();
    }

    private String resolveObjectKey(HttpServletRequest request) {
        String path = request.getRequestURI();
        String prefix = request.getContextPath() + "/api/uploads/local/";
        if (!path.startsWith(prefix)) {
            throw new CustomException(ErrorCode.ERR_INVALID_PARAMETER);
        }
        String objectKey = path.substring(prefix.length());
        if (!objectKey.matches(OBJECT_KEY_PATTERN)) {
            throw new CustomException(ErrorCode.ERR_INVALID_PARAMETER);
        }
        return objectKey;
    }

    private void saveRawBody(LocalUploadService local, String objectKey, HttpServletRequest request) {
        if (request.getContentLengthLong() > MAX_UPLOAD_BYTES) {
            throw new CustomException(ErrorCode.ERR_INVALID_PARAMETER, "업로드 파일 크기가 너무 큽니다.");
        }
        try {
            Path target = local.resolveTarget(objectKey);
            Files.createDirectories(target.getParent());
            try (var in = request.getInputStream(); var out = Files.newOutputStream(target)) {
                copyLimited(in, out, target);
            }
        } catch (IOException e) {
            throw new CustomException(ErrorCode.ERR_INTERNAL);
        }
    }

    private void copyLimited(java.io.InputStream in, java.io.OutputStream out, Path target) throws IOException {
        byte[] buffer = new byte[8192];
        long total = 0;
        int read;
        while ((read = in.read(buffer)) != -1) {
            total += read;
            if (total > MAX_UPLOAD_BYTES) {
                Files.deleteIfExists(target);
                throw new CustomException(ErrorCode.ERR_INVALID_PARAMETER, "업로드 파일 크기가 너무 큽니다.");
            }
            out.write(buffer, 0, read);
        }
    }
}

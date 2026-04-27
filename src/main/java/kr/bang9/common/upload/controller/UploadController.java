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
import java.nio.file.StandardCopyOption;

@RestController
@RequestMapping("/api/uploads")
@RequiredArgsConstructor
public class UploadController {

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
        return path.substring(prefix.length());
    }

    private void saveRawBody(LocalUploadService local, String objectKey, HttpServletRequest request) {
        try {
            Path target = local.resolveTarget(objectKey);
            Files.createDirectories(target.getParent());
            try (var in = request.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new CustomException(ErrorCode.ERR_INTERNAL);
        }
    }
}

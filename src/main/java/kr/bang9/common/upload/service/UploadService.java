package kr.bang9.common.upload.service;

import kr.bang9.common.upload.dto.PresignedUrlRequest;
import kr.bang9.common.upload.dto.PresignedUrlResponse;

public interface UploadService {

    PresignedUrlResponse generatePresignedUrl(long userId, PresignedUrlRequest request);

    String extractObjectKeyFromUrl(String publicUrl);
}

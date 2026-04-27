package kr.bang9.common.upload.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PresignedUrlRequest(
    @NotBlank(message = "파일명을 입력해주세요.")
    @Size(max = 255, message = "파일명이 너무 깁니다.")
    String fileName,

    @NotBlank(message = "콘텐츠 타입을 입력해주세요.")
    @Pattern(regexp = "^image/(jpeg|png|webp|gif)$", message = "이미지 파일만 업로드할 수 있습니다.")
    String contentType,

    @NotBlank(message = "업로드 종류를 지정해주세요.")
    @Pattern(regexp = "^(listing|profile|product)$", message = "업로드 종류가 올바르지 않습니다.")
    String kind
) {
}

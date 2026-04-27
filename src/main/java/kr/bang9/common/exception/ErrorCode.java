package kr.bang9.common.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {

    ERR_AUTH_INVALID(HttpStatus.UNAUTHORIZED, "인증 정보가 올바르지 않습니다."),
    ERR_LOGIN_INVALID(HttpStatus.UNAUTHORIZED, "아이디 또는 비밀번호를 다시 확인해주세요."),
    ERR_AUTH_EXPIRED(HttpStatus.UNAUTHORIZED, "토큰이 만료되었습니다."),
    ERR_FORBIDDEN(HttpStatus.FORBIDDEN, "권한이 없습니다."),
    ERR_NOT_FOUND(HttpStatus.NOT_FOUND, "요청하신 리소스를 찾을 수 없습니다."),
    ERR_INVALID_PARAMETER(HttpStatus.BAD_REQUEST, "요청 값이 올바르지 않습니다."),
    ERR_INVALID_PASSWORD(HttpStatus.BAD_REQUEST, "비밀번호 규칙을 확인해주세요."),
    ERR_EMAIL_DUPLICATE(HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다."),
    ERR_SOCIAL_ACCOUNT_EXISTS(HttpStatus.CONFLICT, "소셜 계정으로 가입된 이메일입니다."),
    ERR_STOCK_NOT_ENOUGH(HttpStatus.CONFLICT, "재고가 부족합니다."),
    ERR_PAYMENT_FAILED(HttpStatus.UNPROCESSABLE_ENTITY, "결제 처리에 실패했습니다."),
    ERR_LISTING_NOT_OWNED(HttpStatus.FORBIDDEN, "본인 매물만 수정할 수 있습니다."),
    ERR_FAVORITE_ALREADY(HttpStatus.CONFLICT, "이미 찜한 매물입니다."),
    ERR_FAVORITE_NOT_FOUND(HttpStatus.NOT_FOUND, "찜 목록에 없는 매물입니다."),
    ERR_IDENTITY_REQUIRED(HttpStatus.FORBIDDEN, "본인인증이 필요합니다."),
    ERR_IDENTITY_DUPLICATE(HttpStatus.CONFLICT, "이미 다른 계정에서 사용된 주민번호입니다."),
    ERR_IDENTITY_VERIFICATION_FAILED(HttpStatus.UNPROCESSABLE_ENTITY, "본인인증 검증에 실패했습니다."),
    ERR_RATE_LIMIT(HttpStatus.TOO_MANY_REQUESTS, "잠시 후 다시 시도해주세요."),
    ERR_REFUND_ALREADY_REQUESTED(HttpStatus.CONFLICT, "이미 환불 요청이 접수되었습니다."),
    ERR_REFUND_NOT_ALLOWED(HttpStatus.CONFLICT, "환불 요청이 불가능한 주문 상태입니다."),
    ERR_REVIEW_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 리뷰가 등록된 상품입니다."),
    ERR_REVIEW_NOT_ALLOWED(HttpStatus.CONFLICT, "리뷰를 작성할 수 없는 주문 상태입니다."),
    ERR_DUPLICATE_RESOURCE(HttpStatus.CONFLICT, "이미 존재하는 요청입니다."),
    ERR_PROFANITY(HttpStatus.BAD_REQUEST, "부적절한 표현이 포함되어 있습니다."),
    ERR_INTERNAL(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }
}

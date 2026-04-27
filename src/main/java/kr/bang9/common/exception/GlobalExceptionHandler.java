package kr.bang9.common.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ErrorResponse> handleCustom(CustomException ex) {
        ErrorCode code = ex.getErrorCode();
        return ResponseEntity.status(code.getStatus())
            .body(ErrorResponse.of(code, ex.getCustomMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
            .findFirst()
            .map(fe -> fe.getDefaultMessage())
            .orElse(ErrorCode.ERR_INVALID_PARAMETER.getMessage());
        return ResponseEntity.status(ErrorCode.ERR_INVALID_PARAMETER.getStatus())
            .body(ErrorResponse.of(ErrorCode.ERR_INVALID_PARAMETER, message));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuth(AuthenticationException ex) {
        return ResponseEntity.status(ErrorCode.ERR_AUTH_INVALID.getStatus())
            .body(ErrorResponse.of(ErrorCode.ERR_AUTH_INVALID));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(ErrorCode.ERR_FORBIDDEN.getStatus())
            .body(ErrorResponse.of(ErrorCode.ERR_FORBIDDEN));
    }

    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateKey(DuplicateKeyException ex) {
        log.warn("Duplicate key violation: {}", ex.getMessage());
        ErrorCode code = ErrorCode.ERR_DUPLICATE_RESOURCE;
        return ResponseEntity.status(code.getStatus())
            .body(ErrorResponse.of(code));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnknown(Exception ex) {
        log.error("Unhandled exception", ex);
        return ResponseEntity.status(ErrorCode.ERR_INTERNAL.getStatus())
            .body(ErrorResponse.of(ErrorCode.ERR_INTERNAL));
    }
}

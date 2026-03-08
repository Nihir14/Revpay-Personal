package com.revpay.exception;

import com.revpay.model.dto.ApiResponse;
import com.revpay.model.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // --- 1. VALIDATION & TYPE MISMATCH HANDLERS ---

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleValidationExceptions(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> details = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            details.put(fieldName, errorMessage);
        });
        return buildErrorResponse("VAL_001", "Validation failed", request.getRequestURI(), HttpStatus.BAD_REQUEST, details);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        String message = String.format("Parameter '%s' should be of type %s", ex.getName(), ex.getRequiredType().getSimpleName());
        return buildErrorResponse("VAL_002", message, request.getRequestURI(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleMissingParams(
            MissingServletRequestParameterException ex, HttpServletRequest request) {
        return buildErrorResponse("VAL_003", ex.getMessage(), request.getRequestURI(), HttpStatus.BAD_REQUEST);
    }

    // --- 2. BUSINESS & SECURITY EXCEPTIONS ---

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleUserExists(
            UserAlreadyExistsException ex, HttpServletRequest request) {
        return buildErrorResponse("REG_001", ex.getMessage(), request.getRequestURI(), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(InsufficientBalanceException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleInsufficientBalance(
            InsufficientBalanceException ex, HttpServletRequest request) {
        return buildErrorResponse("WAL_001", ex.getMessage(), request.getRequestURI(), HttpStatus.PAYMENT_REQUIRED);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleNotFound(
            ResourceNotFoundException ex, HttpServletRequest request) {
        return buildErrorResponse("RES_404", ex.getMessage(), request.getRequestURI(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleUnauthorized(
            UnauthorizedException ex, HttpServletRequest request) {
        return buildErrorResponse("AUTH_401", ex.getMessage(), request.getRequestURI(), HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleAccessDenied(
            AccessDeniedException ex, HttpServletRequest request) {
        return buildErrorResponse("AUTH_403", "Access Denied: You do not have permission for this resource", request.getRequestURI(), HttpStatus.FORBIDDEN);
    }

    // --- 3. DATA & RUNTIME HANDLERS ---

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleDataIntegrity(
            DataIntegrityViolationException ex, HttpServletRequest request) {
        log.error("DATABASE_ERROR | Path: {} | Error: {}", request.getRequestURI(), ex.getMessage());
        return buildErrorResponse("DB_001", "Database integrity violation (possible duplicate record)", request.getRequestURI(), HttpStatus.CONFLICT);
    }

    /**
     * Re-synchronized as 'handleWalletRuntime' for backward compatibility.
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleWalletRuntime(
            RuntimeException ex, HttpServletRequest request) {
        log.warn("BUSINESS_LOGIC_ERROR | Path: {} | Message: {}", request.getRequestURI(), ex.getMessage());

        String code = "GEN_ERR";
        String message = ex.getMessage();
        if (message != null) {
            String lower = message.toLowerCase();
            if (lower.contains("pin")) code = "AUTH_002";
            else if (lower.contains("balance")) code = "WAL_001";
            else if (lower.contains("limit")) code = "LIM_001";
        }
        return buildErrorResponse(code, message, request.getRequestURI(), HttpStatus.BAD_REQUEST);
    }

    /**
     * Re-synchronized as 'handleGeneral' for system-wide fallbacks.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleGeneral(
            Exception ex, HttpServletRequest request) {
        log.error("SYSTEM_FAILURE | Path: {} | Error: ", request.getRequestURI(), ex);

        if (request.getRequestURI().contains("swagger") || request.getRequestURI().contains("api-docs")) {
            return null;
        }
        return buildErrorResponse("SYS_500", "An unexpected system error occurred", request.getRequestURI(), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    // --- PRIVATE UTILITIES ---

    private ResponseEntity<ApiResponse<ErrorResponse>> buildErrorResponse(
            String code, String message, String path, HttpStatus status) {
        return buildErrorResponse(code, message, path, status, null);
    }

    private ResponseEntity<ApiResponse<ErrorResponse>> buildErrorResponse(
            String code, String message, String path, HttpStatus status, Map<String, String> details) {
        ErrorResponse errorRes = ErrorResponse.builder()
                .errorCode(code)
                .message(message)
                .path(path)
                .details(details)
                .build();
        return ResponseEntity.status(status).body(ApiResponse.error(errorRes, message, code));
    }
}
package com.revpay.model.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    private String errorCode; // E.g., "AUTH_001", "VAL_002"
    private String message;
    private String path;

    @Builder.Default
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp = LocalDateTime.now();

    /**
     * Captures field-level validation errors.
     * Key: Field name (e.g., "email")
     * Value: Error message (e.g., "Invalid email format")
     */
    private Map<String, String> details;

    // Helper for quick business logic errors
    public static ErrorResponse of(String code, String msg, String path) {
        return ErrorResponse.builder()
                .errorCode(code)
                .message(msg)
                .path(path)
                .build();
    }
}
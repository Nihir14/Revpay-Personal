package com.revpay.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.revpay.model.dto.ApiResponse;
import com.revpay.model.dto.ErrorResponse;
import com.revpay.model.entity.IdempotencyKey;
import com.revpay.repository.IdempotencyRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class IdempotencyInterceptor implements HandlerInterceptor {

    private final IdempotencyRepository idempotencyRepository;
    private final ObjectMapper objectMapper; // Injected to serialize our standard response

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // Idempotency usually only applies to state-mutating requests
        if ("POST".equalsIgnoreCase(request.getMethod()) || "PUT".equalsIgnoreCase(request.getMethod())) {

            String idempotencyKey = request.getHeader("Idempotency-Key");

            if (idempotencyKey != null && !idempotencyKey.trim().isEmpty()) {
                log.debug("Idempotency check initiated for key: {}", idempotencyKey);

                // Note: In a high-concurrency production setup (Phase 3), this check and save
                // should ideally be handled by Redis or a DB UNIQUE constraint to prevent race conditions.
                if (idempotencyRepository.existsById(idempotencyKey)) {
                    log.warn("IDEMPOTENCY CONFLICT | Duplicate request blocked for key: {}", idempotencyKey);

                    // 1. Build our standardized ErrorResponse
                    ErrorResponse errorRes = ErrorResponse.of(
                            "IDEMP_409",
                            "Duplicate request detected. This operation has already been processed or is currently processing.",
                            request.getRequestURI()
                    );

                    // 2. Wrap it in our standardized ApiResponse
                    ApiResponse<ErrorResponse> apiResponse = ApiResponse.error(errorRes, "Conflict", "IDEMP_409");

                    // 3. Write it cleanly to the response body
                    response.setStatus(HttpServletResponse.SC_CONFLICT);
                    response.setContentType("application/json");
                    response.getWriter().write(objectMapper.writeValueAsString(apiResponse));

                    return false; // Halts the request chain immediately
                }

                // Record the key so subsequent identical requests are blocked
                IdempotencyKey keyRecord = new IdempotencyKey(
                        idempotencyKey,
                        request.getRequestURI(),
                        LocalDateTime.now()
                );
                idempotencyRepository.save(keyRecord);
            }
        }
        return true; // Proceed to the Controller
    }
}
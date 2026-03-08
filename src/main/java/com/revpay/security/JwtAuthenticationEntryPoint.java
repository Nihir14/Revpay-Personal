package com.revpay.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.revpay.model.dto.ApiResponse;
import com.revpay.model.dto.ErrorResponse;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    // Inject Spring's auto-configured ObjectMapper that supports Java 8 Date/Time
    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException, ServletException {

        log.error("UNAUTHORIZED_ACCESS | Path: {} | Error: {}", request.getRequestURI(), authException.getMessage());

        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        // Construct the exact same ErrorResponse used in GlobalExceptionHandler
        ErrorResponse errorRes = ErrorResponse.of(
                "AUTH_401",
                "Full authentication is required to access this resource",
                request.getRequestURI()
        );

        ApiResponse<ErrorResponse> apiResponse = ApiResponse.error(errorRes, "Unauthorized Access", "AUTH_401");

        // Serialize and write the standardized response
        objectMapper.writeValue(response.getOutputStream(), apiResponse);
    }
}
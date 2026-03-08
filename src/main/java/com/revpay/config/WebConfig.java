package com.revpay.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    // Constructor injection via Lombok for better testability and immutability
    private final IdempotencyInterceptor idempotencyInterceptor;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // Note for Phase 3: Externalize "http://localhost:4200" to application.yml
        // so it can dynamically change in staging/production environments.
        registry.addMapping("/**")
                .allowedOrigins("http://localhost:4200")
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600); // Caches the preflight response for 1 hour to reduce network chatter
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Targets only the state-mutating financial endpoints
        registry.addInterceptor(idempotencyInterceptor)
                .addPathPatterns(
                        "/api/wallet/send",
                        "/api/wallet/add-funds",
                        "/api/wallet/withdraw",
                        "/api/wallet/pay-invoice"
                );
    }
}
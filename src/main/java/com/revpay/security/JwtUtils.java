package com.revpay.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

@Slf4j
@Component
public class JwtUtils {

    @Value("${revpay.app.jwtSecret}")
    private String jwtSecret;

    @Value("${revpay.app.jwtExpirationMs:86400000}")
    private int jwtExpirationMs;

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateTokenFromUsername(String email) {
        log.debug("JWT_GENERATE | Creating token for subject: {}", email);
        return Jwts.builder()
                .setSubject(email)
                .setIssuedAt(new Date())
                .setExpiration(new Date((new Date()).getTime() + jwtExpirationMs))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public String getUserNameFromJwtToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    public boolean validateJwtToken(String authToken) {
        try {
            Jwts.parserBuilder().setSigningKey(getSigningKey()).build().parseClaimsJws(authToken);
            return true;
        } catch (SignatureException e) {
            log.error("JWT_VALIDATION_ERROR | Invalid JWT signature: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.error("JWT_VALIDATION_ERROR | Invalid JWT token format: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            // Changed to WARN: Token expiration is a normal application event, not a system error
            log.warn("JWT_VALIDATION_ERROR | JWT token is expired: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.error("JWT_VALIDATION_ERROR | JWT token is unsupported: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.error("JWT_VALIDATION_ERROR | JWT claims string is empty: {}", e.getMessage());
        }
        return false;
    }
}
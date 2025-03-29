package com.aicompanion.mod.web.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

import java.security.Key;
import java.util.Date;
import java.util.UUID;

/**
 * Manages JWT token generation and validation for web interface authentication
 */
public class JWTManager {
    private final Key secretKey;
    private static final long TOKEN_EXPIRATION_MS = 24 * 60 * 60 * 1000; // 24 hours
    private static JWTManager instance;

    public JWTManager(Key secretKey) {
        this.secretKey = secretKey;
    }

    /**
     * Get the singleton instance of JWTManager
     */
    public static synchronized JWTManager getInstance() {
        if (instance == null) {
            // Generate a secure key for HS512
            Key key = Keys.secretKeyFor(SignatureAlgorithm.HS512);
            instance = new JWTManager(key);
        }
        return instance;
    }

    /**
     * Generate a new JWT token for a username
     */
    public String generateToken(String username) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + TOKEN_EXPIRATION_MS);
        
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(secretKey, SignatureAlgorithm.HS512)
                .compact();
    }

    /**
     * Validate a JWT token and extract the claims
     */
    public Claims validateToken(String token) throws JwtException {
        return Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * Extract the username from a validated token
     */
    public String getUsernameFromToken(String token) throws JwtException {
        Claims claims = validateToken(token);
        return claims.getSubject();
    }

    /**
     * Check if a token is valid and not expired
     */
    public boolean isTokenValid(String token) {
        try {
            Claims claims = validateToken(token);
            return !claims.getExpiration().before(new Date());
        } catch (JwtException e) {
            return false;
        }
    }
}
package dev.ograh.videostreaming.security;

import dev.ograh.videostreaming.entity.User;
import dev.ograh.videostreaming.exception.InvalidTokenException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.UUID;

@Slf4j
@Service
public class JwtService {

    @Value("${security.jwt.secret}")
    private String jwtSecret;

    @Value("${security.jwt.expiration}")
    private long jwtExpiration;

    @Value("${security.jwt.refresh-expiration}")
    private long jwtRefreshExpiration;

    private SecretKey getSigningKey(String secret) {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    public String generateToken(User user) {
        return createToken(user, jwtExpiration);
    }

    public String generateRefreshToken(User user) {
        return createToken(user, jwtRefreshExpiration);
    }

    public Claims extractClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getSigningKey(jwtSecret))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            log.error("Invalid JWT token: {}", e.getMessage());
            throw new InvalidTokenException("Invalid or Expired JWT token");
        }
    }

    public String extractEmail(String token) {
        return extractClaims(token).getSubject();
    }

    private String createToken(User user, long expiration) {
        String email = user.getEmail();
        String role = user.getRole().name();
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .subject(email)
                .claim("role", role)
                .issuedAt(now)
                .id(String.valueOf(UUID.randomUUID()))
                .expiration(expiryDate)
                .signWith(getSigningKey(jwtSecret))
                .compact();
    }

    public boolean isTokenValid(String refreshToken) {
        try {
            Claims claims = Jwts.parser()
                    .decryptWith(getSigningKey(jwtSecret))
                    .build()
                    .parseSignedClaims(refreshToken)
                    .getPayload();
            return claims.getExpiration().after(new Date());
        } catch (JwtException | IllegalArgumentException e) {
            log.error("Invalid JWT Refresh token: {}", e.getMessage());
            return false;
        }
    }
}
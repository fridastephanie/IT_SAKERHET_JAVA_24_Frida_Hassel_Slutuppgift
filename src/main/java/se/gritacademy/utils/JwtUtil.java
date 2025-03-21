package se.gritacademy.utils;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import se.gritacademy.controllers.AdminController;
import se.gritacademy.models.UserInfo;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;

public class JwtUtil {

    private static final SecretKey jwtKey = Keys.secretKeyFor(SignatureAlgorithm.HS256);

    public static String generateJwtToken(UserInfo user) {
        return Jwts.builder()
                .setSubject(user.getEmail())
                .claim("role", user.getRole())
                .claim("blocked", user.isBlocked())
                .setIssuedAt(Date.from(Instant.now()))
                .setExpiration(Date.from(Instant.now().plusSeconds(60 * 60 * 24)))
                .signWith(jwtKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public static Claims parseJwtToken(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(jwtKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            if (claims.getExpiration().before(new Date())) {
                throw new RuntimeException("JWT token has expired");
            }
            return claims;
        } catch (JwtException e) {
            throw new RuntimeException("Invalid JWT token", e);
        }
    }
}

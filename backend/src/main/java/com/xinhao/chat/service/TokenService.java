package com.xinhao.chat.service;

import com.xinhao.chat.entity.User;
import com.xinhao.chat.repository.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Optional;

@Service
public class TokenService {

    @Autowired
    private UserRepository userRepository;

    @Value("${jwt.secret:xinhaochatsecretkeymustbelongenoughfortestingpurposes1234567890}")
    private String secret;

    @Value("${jwt.expiration:86400000}") // 24 hours
    private long expiration;

    private SecretKey getSigningKey() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String createToken(User user) {
        return Jwts.builder()
                .setSubject(user.getUsername())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public User getUserByToken(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            String username = claims.getSubject();
            Optional<User> userOptional = userRepository.findByUsername(username);
            return userOptional.orElse(null);
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }
    
    public void removeToken(String token) {
        // JWT is stateless, so we cannot invalidate it without a blacklist.
        // For this simple implementation, we rely on client-side deletion.
    }
}

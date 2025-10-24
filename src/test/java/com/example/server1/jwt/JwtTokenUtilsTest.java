package com.example.server1.jwt;

import com.example.server1.entity.Role;
import com.example.server1.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class JwtTokenUtilsTest {

    @InjectMocks
    private JwtTokenUtils jwtTokenUtils;

    private User user;
    private String secret;
    private Integer lifetime;

    @BeforeEach
    void setUp() {
        // Настройка тестовых данных
        secret = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
        lifetime = 24; // 24 часа

        jwtTokenUtils.setSecret(secret);
        jwtTokenUtils.setLifetime(lifetime);

        user = User.builder()
                .id(1L)
                .username("testuser")
                .password("password")
                .role(Role.USER)
                .build();
    }

    @Test
    void generateToken_WithValidUser_ShouldReturnToken() {
        // When
        String token = jwtTokenUtils.generateToken(user);

        // Then
        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();
        assertThat(token.split("\\.")).hasSize(3); // JWT должен иметь 3 части
    }

    @Test
    void generateToken_WithUserDetails_ShouldReturnToken() {
        // Given
        UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
                .username("testuser")
                .password("password")
                .authorities("ROLE_USER")
                .build();

        // When
        String token = jwtTokenUtils.generateToken(userDetails);

        // Then
        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();
        assertThat(token.split("\\.")).hasSize(3);
    }

    @Test
    void getUsernameFromToken_WithValidToken_ShouldReturnUsername() {
        // Given
        String token = jwtTokenUtils.generateToken(user);

        // When
        String username = jwtTokenUtils.getUsernameFromToken(token);

        // Then
        assertThat(username).isEqualTo("testuser");
    }

    @Test
    void getClaimFromToken_WithValidToken_ShouldReturnClaim() {
        // Given
        String token = jwtTokenUtils.generateToken(user);

        // When
        String subject = jwtTokenUtils.getClaimFromToken(token, claims -> claims.getSubject());

        // Then
        assertThat(subject).isEqualTo("testuser");
    }

    @Test
    void getClaimsAllFromToken_WithValidToken_ShouldReturnClaims() {
        // Given
        String token = jwtTokenUtils.generateToken(user);

        // When
        var claims = jwtTokenUtils.getClaimsAllFromToken(token);

        // Then
        assertThat(claims).isNotNull();
        assertThat(claims.getSubject()).isEqualTo("testuser");
        assertThat(claims.get("role")).isEqualTo("USER");
    }

    @Test
    void validateToken_WithValidTokenAndUser_ShouldReturnTrue() {
        // Given
        String token = jwtTokenUtils.generateToken(user);

        // When
        boolean isValid = jwtTokenUtils.validateToken(token, user);

        // Then
        assertThat(isValid).isTrue();
    }

    @Test
    void validateToken_WithValidTokenAndDifferentUser_ShouldReturnFalse() {
        // Given
        String token = jwtTokenUtils.generateToken(user);
        User differentUser = User.builder()
                .id(2L)
                .username("differentuser")
                .password("password")
                .role(Role.USER)
                .build();

        // When
        boolean isValid = jwtTokenUtils.validateToken(token, differentUser);

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    void validateToken_WithExpiredToken_ShouldReturnFalse() {
        // Given
        // Создаем токен с очень коротким временем жизни
        jwtTokenUtils.setLifetime(0); // 0 часов
        String token = jwtTokenUtils.generateToken(user);
        
        // Ждем немного времени, чтобы токен истек
        try {
            Thread.sleep(1000); // 1 секунда
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // When
        boolean isValid = jwtTokenUtils.validateToken(token, user);

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    void validateToken_WithInvalidToken_ShouldThrowException() {
        // Given
        String invalidToken = "invalid.token.here";

        // When & Then
        assertThrows(Exception.class, () -> {
            jwtTokenUtils.validateToken(invalidToken, user);
        });
    }

    @Test
    void generateToken_WithAdminUser_ShouldIncludeRoleInToken() {
        // Given
        User adminUser = User.builder()
                .id(2L)
                .username("admin")
                .password("password")
                .role(Role.ADMIN)
                .build();

        // When
        String token = jwtTokenUtils.generateToken(adminUser);
        var claims = jwtTokenUtils.getClaimsAllFromToken(token);

        // Then
        assertThat(claims.get("role")).isEqualTo("ADMIN");
        assertThat(claims.getSubject()).isEqualTo("admin");
    }

    @Test
    void tokenExpiration_ShouldBeSetCorrectly() {
        // Given
        String token = jwtTokenUtils.generateToken(user);
        var claims = jwtTokenUtils.getClaimsAllFromToken(token);
        Date expiration = claims.getExpiration();
        Date issuedAt = claims.getIssuedAt();

        // When
        long expirationTime = expiration.getTime();
        long issuedTime = issuedAt.getTime();
        long expectedLifetime = lifetime * 60 * 60 * 1000; // в миллисекундах

        // Then
        assertThat(expirationTime - issuedTime).isEqualTo(expectedLifetime);
        assertThat(expiration.after(new Date())).isTrue();
    }

    @Test
    void generateToken_WithNullUser_ShouldThrowException() {
        // When & Then
        assertThrows(Exception.class, () -> {
            jwtTokenUtils.generateToken((User) null);
        });
    }

    @Test
    void getUsernameFromToken_WithInvalidToken_ShouldThrowException() {
        // Given
        String invalidToken = "invalid.token.here";

        // When & Then
        assertThrows(Exception.class, () -> {
            jwtTokenUtils.getUsernameFromToken(invalidToken);
        });
    }
}


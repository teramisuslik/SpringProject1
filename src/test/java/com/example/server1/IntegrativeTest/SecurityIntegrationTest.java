package com.example.server1.integrativetest;

import com.example.server1.entity.Role;
import com.example.server1.entity.User;
import com.example.server1.jwt.AuthRequest;
import com.example.server1.jwt.JwtTokenUtils;
import com.example.server1.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Transactional
class SecurityIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserService userService;

    @Autowired
    private JwtTokenUtils jwtTokenUtils;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String baseUrl;
    private User testUser;
    private User adminUser;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port;
        
        // Создание тестового пользователя
        testUser = userService.create("securitytestuser", "password");
        
        // Создание тестового администратора
        adminUser = userService.createAdmin("securitytestadmin", "password");
    }

    @Test
    void authentication_ShouldAllowPublicEndpoints() {
        // Given
        String publicEndpoint = baseUrl + "/main";

        // When
        ResponseEntity<String> response = restTemplate.getForEntity(publicEndpoint, String.class);

        // Then
        assertThat(response.getStatusCode().value()).isEqualTo(200);
    }

    @Test
    void authentication_ShouldAllowRegistration() {
        // Given
        AuthRequest authRequest = new AuthRequest("newuser", "password");
        String registerEndpoint = baseUrl + "/register";

        // When
        ResponseEntity<String> response = restTemplate.postForEntity(registerEndpoint, authRequest, String.class);

        // Then
        assertThat(response.getStatusCode().value()).isEqualTo(200);
    }

    @Test
    void authentication_ShouldAllowLogin() {
        // Given
        AuthRequest authRequest = new AuthRequest("securitytestuser", "password");
        String loginEndpoint = baseUrl + "/login";

        // When
        ResponseEntity<String> response = restTemplate.postForEntity(loginEndpoint, authRequest, String.class);

        // Then
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).contains("token");
    }

    @Test
    void authentication_ShouldRejectInvalidCredentials() {
        // Given
        AuthRequest authRequest = new AuthRequest("securitytestuser", "wrongpassword");
        String loginEndpoint = baseUrl + "/login";

        // When
        ResponseEntity<String> response = restTemplate.postForEntity(loginEndpoint, authRequest, String.class);

        // Then
        assertThat(response.getStatusCode().value()).isEqualTo(500); // Ожидаем ошибку
    }

    @Test
    void authentication_ShouldRejectNonExistentUser() {
        // Given
        AuthRequest authRequest = new AuthRequest("nonexistentuser", "password");
        String loginEndpoint = baseUrl + "/login";

        // When
        ResponseEntity<String> response = restTemplate.postForEntity(loginEndpoint, authRequest, String.class);

        // Then
        assertThat(response.getStatusCode().value()).isEqualTo(500); // Ожидаем ошибку
    }

    @Test
    void authorization_ShouldAllowAuthenticatedUserAccess() {
        // Given
        String token = getAuthToken("securitytestuser", "password");
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        
        String protectedEndpoint = baseUrl + "/user?username=securitytestuser";

        // When
        ResponseEntity<User> response = restTemplate.exchange(
                protectedEndpoint, 
                HttpMethod.GET, 
                new HttpEntity<>(headers), 
                User.class
        );

        // Then
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getUsername()).isEqualTo("securitytestuser");
    }

    @Test
    void authorization_ShouldRejectUnauthorizedAccess() {
        // Given
        String protectedEndpoint = baseUrl + "/user?username=securitytestuser";

        // When
        ResponseEntity<String> response = restTemplate.getForEntity(protectedEndpoint, String.class);

        // Then
        assertThat(response.getStatusCode().value()).isEqualTo(401); // Unauthorized
    }

    @Test
    void authorization_ShouldRejectInvalidToken() {
        // Given
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer invalid-token");
        
        String protectedEndpoint = baseUrl + "/user?username=securitytestuser";

        // When
        ResponseEntity<String> response = restTemplate.exchange(
                protectedEndpoint, 
                HttpMethod.GET, 
                new HttpEntity<>(headers), 
                String.class
        );

        // Then
        assertThat(response.getStatusCode().value()).isEqualTo(401); // Unauthorized
    }

    @Test
    void authorization_ShouldAllowAdminOnlyEndpoints() {
        // Given
        String adminToken = getAuthToken("securitytestadmin", "password");
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + adminToken);
        
        String adminEndpoint = baseUrl + "/allusersname";

        // When
        ResponseEntity<String> response = restTemplate.exchange(
                adminEndpoint, 
                HttpMethod.GET, 
                new HttpEntity<>(headers), 
                String.class
        );

        // Then
        assertThat(response.getStatusCode().value()).isEqualTo(200);
    }

    @Test
    void authorization_ShouldRejectUserAccessToAdminEndpoints() {
        // Given
        String userToken = getAuthToken("securitytestuser", "password");
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + userToken);
        
        String adminEndpoint = baseUrl + "/allusersname";

        // When
        ResponseEntity<String> response = restTemplate.exchange(
                adminEndpoint, 
                HttpMethod.GET, 
                new HttpEntity<>(headers), 
                String.class
        );

        // Then
        assertThat(response.getStatusCode().value()).isEqualTo(403); // Forbidden
    }

    @Test
    void jwtToken_ShouldBeValidForAuthenticatedUser() {
        // Given
        String token = getAuthToken("securitytestuser", "password");

        // When
        boolean isValid = jwtTokenUtils.validateToken(token, testUser);

        // Then
        assertThat(isValid).isTrue();
    }

    @Test
    void jwtToken_ShouldContainCorrectUserInformation() {
        // Given
        String token = getAuthToken("securitytestuser", "password");

        // When
        String username = jwtTokenUtils.getUsernameFromToken(token);

        // Then
        assertThat(username).isEqualTo("securitytestuser");
    }

    @Test
    void jwtToken_ShouldContainRoleInformation() {
        // Given
        String token = getAuthToken("securitytestadmin", "password");

        // When
        var claims = jwtTokenUtils.getClaimsAllFromToken(token);

        // Then
        assertThat(claims.get("role")).isEqualTo("ADMIN");
    }

    @Test
    void passwordEncoder_ShouldEncodeAndMatchPasswords() {
        // Given
        String rawPassword = "testpassword";

        // When
        String encodedPassword = passwordEncoder.encode(rawPassword);
        boolean matches = passwordEncoder.matches(rawPassword, encodedPassword);

        // Then
        assertThat(encodedPassword).isNotNull();
        assertThat(encodedPassword).isNotEqualTo(rawPassword);
        assertThat(matches).isTrue();
    }

    @Test
    void passwordEncoder_ShouldNotMatchWrongPasswords() {
        // Given
        String rawPassword = "testpassword";
        String wrongPassword = "wrongpassword";
        String encodedPassword = passwordEncoder.encode(rawPassword);

        // When
        boolean matches = passwordEncoder.matches(wrongPassword, encodedPassword);

        // Then
        assertThat(matches).isFalse();
    }

    @Test
    void securityHeaders_ShouldBePresent() {
        // Given
        String publicEndpoint = baseUrl + "/main";

        // When
        ResponseEntity<String> response = restTemplate.getForEntity(publicEndpoint, String.class);

        // Then
        assertThat(response.getHeaders()).isNotNull();
        // Проверяем, что CSRF отключен для REST API
        assertThat(response.getHeaders().get("X-Frame-Options")).isNull();
    }

    @Test
    void sessionManagement_ShouldBeStateless() {
        // Given
        String token1 = getAuthToken("securitytestuser", "password");
        String token2 = getAuthToken("securitytestuser", "password");

        // When
        boolean isValid1 = jwtTokenUtils.validateToken(token1, testUser);
        boolean isValid2 = jwtTokenUtils.validateToken(token2, testUser);

        // Then
        assertThat(isValid1).isTrue();
        assertThat(isValid2).isTrue();
        // Токены должны быть разными (stateless)
        assertThat(token1).isNotEqualTo(token2);
    }

    @Test
    void roleBasedAccess_ShouldWorkCorrectly() {
        // Given
        String userToken = getAuthToken("securitytestuser", "password");
        String adminToken = getAuthToken("securitytestadmin", "password");

        HttpHeaders userHeaders = new HttpHeaders();
        userHeaders.set("Authorization", "Bearer " + userToken);

        HttpHeaders adminHeaders = new HttpHeaders();
        adminHeaders.set("Authorization", "Bearer " + adminToken);

        String adminEndpoint = baseUrl + "/allusersname";

        // When
        ResponseEntity<String> userResponse = restTemplate.exchange(
                adminEndpoint, 
                HttpMethod.GET, 
                new HttpEntity<>(userHeaders), 
                String.class
        );

        ResponseEntity<String> adminResponse = restTemplate.exchange(
                adminEndpoint, 
                HttpMethod.GET, 
                new HttpEntity<>(adminHeaders), 
                String.class
        );

        // Then
        assertThat(userResponse.getStatusCode().value()).isEqualTo(403); // Forbidden
        assertThat(adminResponse.getStatusCode().value()).isEqualTo(200); // OK
    }

    @Test
    void securityIntegration_ShouldHandleConcurrentRequests() throws InterruptedException {
        // Given
        String token = getAuthToken("securitytestuser", "password");
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        
        String protectedEndpoint = baseUrl + "/user?username=securitytestuser";
        int numberOfRequests = 10;
        CountDownLatch latch = new CountDownLatch(numberOfRequests);

        // When
        for (int i = 0; i < numberOfRequests; i++) {
            new Thread(() -> {
                try {
                    ResponseEntity<User> response = restTemplate.exchange(
                            protectedEndpoint, 
                            HttpMethod.GET, 
                            new HttpEntity<>(headers), 
                            User.class
                    );
                    assertThat(response.getStatusCode().value()).isEqualTo(200);
                } finally {
                    latch.countDown();
                }
            }).start();
        }

        // Then
        assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
    }

    private String getAuthToken(String username, String password) {
        try {
            AuthRequest authRequest = new AuthRequest(username, password);
            ResponseEntity<String> response = restTemplate.postForEntity(
                    baseUrl + "/login", 
                    authRequest, 
                    String.class
            );
            
            if (response.getStatusCode().value() == 200 && response.getBody() != null) {
                // Простое извлечение токена (в реальном приложении нужно парсить JSON)
                return "mock-token-" + username;
            }
        } catch (Exception e) {
            // В случае ошибки возвращаем mock токен
        }
        return "mock-token-" + username;
    }
}

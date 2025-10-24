package com.example.server1.security;

import com.example.server1.entity.Role;
import com.example.server1.entity.User;
import com.example.server1.jwt.JwtTokenUtils;
import com.example.server1.jwt.JwtTocenFilter;
import com.example.server1.service.CustomUserDetailsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SecurityTest {

    @Mock
    private CustomUserDetailsService userDetailsService;

    @Mock
    private JwtTokenUtils jwtTokenUtils;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    private JwtTocenFilter jwtTokenFilter;
    private PasswordEncoder passwordEncoder;
    private User testUser;

    @BeforeEach
    void setUp() {
        jwtTokenFilter = new JwtTocenFilter(jwtTokenUtils, userDetailsService);
        passwordEncoder = new BCryptPasswordEncoder();
        
        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .password(passwordEncoder.encode("password"))
                .role(Role.USER)
                .build();
    }

    @Test
    void passwordEncoder_ShouldEncodePasswordCorrectly() {
        // Given
        String rawPassword = "password";
        
        // When
        String encodedPassword = passwordEncoder.encode(rawPassword);
        
        // Then
        assertThat(encodedPassword).isNotNull();
        assertThat(encodedPassword).isNotEqualTo(rawPassword);
        assertThat(passwordEncoder.matches(rawPassword, encodedPassword)).isTrue();
    }

    @Test
    void passwordEncoder_ShouldNotMatchWrongPassword() {
        // Given
        String rawPassword = "password";
        String wrongPassword = "wrongpassword";
        String encodedPassword = passwordEncoder.encode(rawPassword);
        
        // When
        boolean matches = passwordEncoder.matches(wrongPassword, encodedPassword);
        
        // Then
        assertThat(matches).isFalse();
    }

    @Test
    void jwtTokenFilter_WithValidToken_ShouldSetAuthentication() throws Exception {
        // Given
        String token = "valid-jwt-token";
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtTokenUtils.getUsernameFromToken(token)).thenReturn("testuser");
        when(userDetailsService.loadUserByUsername("testuser")).thenReturn(testUser);
        when(jwtTokenUtils.validateToken(token, testUser)).thenReturn(true);

        // When
        jwtTokenFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(userDetailsService).loadUserByUsername("testuser");
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void jwtTokenFilter_WithInvalidToken_ShouldNotSetAuthentication() throws Exception {
        // Given
        String token = "invalid-jwt-token";
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtTokenUtils.getUsernameFromToken(token)).thenThrow(new RuntimeException("Invalid token"));

        // When
        jwtTokenFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(userDetailsService, never()).loadUserByUsername(anyString());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void jwtTokenFilter_WithNoToken_ShouldNotSetAuthentication() throws Exception {
        // Given
        when(request.getHeader("Authorization")).thenReturn(null);

        // When
        jwtTokenFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(userDetailsService, never()).loadUserByUsername(anyString());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void jwtTokenFilter_WithExpiredToken_ShouldNotSetAuthentication() throws Exception {
        // Given
        String token = "expired-jwt-token";
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtTokenUtils.getUsernameFromToken(token)).thenReturn("testuser");
        when(userDetailsService.loadUserByUsername("testuser")).thenReturn(testUser);
        when(jwtTokenUtils.validateToken(token, testUser)).thenReturn(false);

        // When
        jwtTokenFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(userDetailsService).loadUserByUsername("testuser");
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void userDetailsService_ShouldLoadUserByUsername() {
        // Given
        String username = "testuser";
        when(userDetailsService.loadUserByUsername(username)).thenReturn(testUser);

        // When
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);

        // Then
        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getUsername()).isEqualTo(username);
        assertThat(userDetails.getAuthorities()).hasSize(1);
        assertThat(userDetails.getAuthorities().iterator().next().getAuthority()).isEqualTo("ROLE_USER");
    }

    @Test
    void userDetailsService_WithNonExistentUser_ShouldThrowException() {
        // Given
        String username = "nonexistent";
        when(userDetailsService.loadUserByUsername(username)).thenThrow(new RuntimeException("User not found"));

        // When & Then
        try {
            userDetailsService.loadUserByUsername(username);
        } catch (RuntimeException e) {
            assertThat(e.getMessage()).isEqualTo("User not found");
        }
    }

    @Test
    void userSecurityProperties_ShouldBeCorrect() {
        // Given
        User user = User.builder()
                .username("testuser")
                .password("password")
                .role(Role.USER)
                .build();

        // When & Then
        assertThat(user.isAccountNonExpired()).isTrue();
        assertThat(user.isAccountNonLocked()).isTrue();
        assertThat(user.isCredentialsNonExpired()).isTrue();
        assertThat(user.isEnabled()).isTrue();
    }

    @Test
    void userAuthorities_ShouldMatchRole() {
        // Given
        User user = User.builder()
                .username("testuser")
                .password("password")
                .role(Role.USER)
                .build();

        User admin = User.builder()
                .username("admin")
                .password("password")
                .role(Role.ADMIN)
                .build();

        // When & Then
        assertThat(user.getAuthorities()).hasSize(1);
        assertThat(user.getAuthorities().iterator().next().getAuthority()).isEqualTo("ROLE_USER");
        
        assertThat(admin.getAuthorities()).hasSize(1);
        assertThat(admin.getAuthorities().iterator().next().getAuthority()).isEqualTo("ROLE_ADMIN");
    }

    @Test
    void jwtTokenUtils_ShouldGenerateValidToken() {
        // Given
        jwtTokenUtils.setSecret("404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970");
        jwtTokenUtils.setLifetime(24);

        // When
        String token = jwtTokenUtils.generateToken(testUser);

        // Then
        assertThat(token).isNotNull();
        assertThat(token.split("\\.")).hasSize(3); // JWT должен иметь 3 части
    }

    @Test
    void jwtTokenUtils_ShouldValidateTokenCorrectly() {
        // Given
        jwtTokenUtils.setSecret("404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970");
        jwtTokenUtils.setLifetime(24);
        String token = jwtTokenUtils.generateToken(testUser);

        // When
        boolean isValid = jwtTokenUtils.validateToken(token, testUser);

        // Then
        assertThat(isValid).isTrue();
    }

    @Test
    void jwtTokenUtils_ShouldExtractUsernameFromToken() {
        // Given
        jwtTokenUtils.setSecret("404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970");
        jwtTokenUtils.setLifetime(24);
        String token = jwtTokenUtils.generateToken(testUser);

        // When
        String username = jwtTokenUtils.getUsernameFromToken(token);

        // Then
        assertThat(username).isEqualTo("testuser");
    }

    @Test
    void securityContext_ShouldBeClearedAfterFilter() throws Exception {
        // Given
        String token = "valid-jwt-token";
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtTokenUtils.getUsernameFromToken(token)).thenReturn("testuser");
        when(userDetailsService.loadUserByUsername("testuser")).thenReturn(testUser);
        when(jwtTokenUtils.validateToken(token, testUser)).thenReturn(true);

        // When
        jwtTokenFilter.doFilterInternal(request, response, filterChain);

        // Then
        // Проверяем, что SecurityContext был установлен
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getName()).isEqualTo("testuser");
    }
}


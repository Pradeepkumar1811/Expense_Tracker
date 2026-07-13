package com.tracker.service;

import com.tracker.dto.LoginRequest;
import com.tracker.dto.LoginResponse;
import com.tracker.dto.RegisterRequest;
import com.tracker.dto.RegisterResponse;
import com.tracker.entity.User;
import com.tracker.exception.EmailAlreadyExistsException;
import com.tracker.exception.InvalidCredentialsException;
import com.tracker.repository.UserRepository;
import com.tracker.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private AuthService authService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");
        testUser.setPassword("$2a$10$hashedpassword");
        testUser.setCreatedAt(LocalDateTime.now());
    }

    @Test
    void register_withValidRequest_shouldCreateUserAndReturnResponse() {
        RegisterRequest request = new RegisterRequest("new@example.com", "password123");

        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("$2a$10$encodedpassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1L);
            return user;
        });

        RegisterResponse response = authService.register(request);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.email()).isEqualTo("new@example.com");
        verify(userRepository).save(any(User.class));
        verify(passwordEncoder).encode("password123");
    }

    @Test
    void register_withExistingEmail_shouldThrowEmailAlreadyExistsException() {
        RegisterRequest request = new RegisterRequest("existing@example.com", "password123");

        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(EmailAlreadyExistsException.class);

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void register_shouldEncodePasswordBeforeSaving() {
        RegisterRequest request = new RegisterRequest("user@example.com", "mypassword");

        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode("mypassword")).thenReturn("$2a$10$encoded");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(2L);
            return user;
        });

        authService.register(request);

        verify(userRepository).save(argThat(user ->
                user.getPassword().equals("$2a$10$encoded")
        ));
    }

    @Test
    void login_withValidCredentials_shouldReturnLoginResponse() {
        LoginRequest request = new LoginRequest("test@example.com", "password123");

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("password123", testUser.getPassword())).thenReturn(true);
        when(jwtTokenProvider.generateToken(1L, "test@example.com")).thenReturn("jwt-token");
        when(jwtTokenProvider.getExpirationMs()).thenReturn(86400000L);

        LoginResponse response = authService.login(request);

        assertThat(response.accessToken()).isEqualTo("jwt-token");
        assertThat(response.expiresIn()).isEqualTo(86400000L);
    }

    @Test
    void login_withNonExistentEmail_shouldThrowInvalidCredentialsException() {
        LoginRequest request = new LoginRequest("notfound@example.com", "password123");

        when(userRepository.findByEmail("notfound@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void login_withWrongPassword_shouldThrowInvalidCredentialsException() {
        LoginRequest request = new LoginRequest("test@example.com", "wrongpassword");

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrongpassword", testUser.getPassword())).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(InvalidCredentialsException.class);

        verify(jwtTokenProvider, never()).generateToken(any(), any());
    }
}

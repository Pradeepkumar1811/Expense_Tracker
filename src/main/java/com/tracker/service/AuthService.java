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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider jwtTokenProvider) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    public RegisterResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyExistsException("Email already exists: " + request.email());
        }

        User user = new User();
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setCreatedAt(LocalDateTime.now());

        User savedUser = userRepository.save(user);

        return new RegisterResponse(savedUser.getId(), savedUser.getEmail());
    }

    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new InvalidCredentialsException();
        }

        String token = jwtTokenProvider.generateToken(user.getId(), user.getEmail());
        long expiresIn = jwtTokenProvider.getExpirationMs();

        return new LoginResponse(token, expiresIn);
    }
}

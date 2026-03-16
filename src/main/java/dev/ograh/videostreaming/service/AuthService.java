package dev.ograh.videostreaming.service;

import dev.ograh.videostreaming.dto.request.LoginRequest;
import dev.ograh.videostreaming.dto.request.RegisterRequest;
import dev.ograh.videostreaming.dto.response.AuthResponse;
import dev.ograh.videostreaming.dto.response.AuthResponseDTO;
import dev.ograh.videostreaming.dto.response.UserResponse;
import dev.ograh.videostreaming.entity.User;
import dev.ograh.videostreaming.enums.UserRole;
import dev.ograh.videostreaming.exception.InvalidTokenException;
import dev.ograh.videostreaming.exception.ResourceNotFoundException;
import dev.ograh.videostreaming.exception.UnauthorizedException;
import dev.ograh.videostreaming.exception.UserNotFoundException;
import dev.ograh.videostreaming.repository.UserRepository;
import dev.ograh.videostreaming.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthResponseDTO register(RegisterRequest request) {
        String email = request.email();
        String password = passwordEncoder.encode(request.password());

        User user = createUserEntity(request);
        userRepository.save(user);
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, password));

        String token = jwtService.generateToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        AuthResponse authResponse = createAuthResponse(user, token);
        return new AuthResponseDTO(authResponse, refreshToken);
    }


    public AuthResponseDTO login(LoginRequest request) {
        String email = request.email();
        String password = request.password();

        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, password));
            User user = userRepository.findByEmailIgnoreCase(email).orElseThrow(
                    () -> new UserNotFoundException("User not found with email: " + email)
            );

            String token = jwtService.generateToken(user);
            String refreshToken = jwtService.generateRefreshToken(user);

            AuthResponse authResponse = createAuthResponse(user, token);
            return new AuthResponseDTO(authResponse, refreshToken);
        } catch (BadCredentialsException e) {
            throw new UnauthorizedException("Invalid email or password.");
        }
    }


    private User createUserEntity(RegisterRequest request) {
        User user = new User();
        user.setName(request.name());
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(UserRole.VIEWER);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        return user;
    }

    private AuthResponse createAuthResponse(User user, String token) {
        UserResponse userResponse = new UserResponse(
                String.valueOf(user.getId()), user.getName(), user.getEmail(), user.getRole().name()
        );
        return new AuthResponse(token, userResponse);
    }

    public AuthResponseDTO refreshToken(String refreshToken) {
        if (!jwtService.isTokenValid(refreshToken)) {
            throw new InvalidTokenException("Invalid refresh token");
        }

        String email = jwtService.extractEmail(refreshToken);
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        String newToken = jwtService.generateToken(user);
        String newRefreshToken = jwtService.generateRefreshToken(user);

        AuthResponse authResponse = createAuthResponse(user, newToken);
        return new AuthResponseDTO(authResponse, newRefreshToken);
    }
}
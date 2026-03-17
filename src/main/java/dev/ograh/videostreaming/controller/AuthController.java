package dev.ograh.videostreaming.controller;

import dev.ograh.videostreaming.dto.request.LoginRequest;
import dev.ograh.videostreaming.dto.request.RegisterRequest;
import dev.ograh.videostreaming.dto.response.AuthResponse;
import dev.ograh.videostreaming.dto.response.AuthResponseDTO;
import dev.ograh.videostreaming.dto.shared.ApiResponse;
import dev.ograh.videostreaming.exception.InvalidTokenException;
import dev.ograh.videostreaming.service.AuthService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private static final int REFRESH_TOKEN_MAX_AGE_SECONDS = 60 * 60 * 24 * 7;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request, HttpServletResponse response) {
        AuthResponseDTO authResponseDTO = authService.register(request);
        AuthResponse authResponse = authResponseDTO.getAuthResponse();
        String refreshToken = authResponseDTO.getRefreshToken();
        ApiResponse<AuthResponse> apiResponse = ApiResponse.success(
                "User registered successfully", authResponse);

        setRefreshTokenCookie(response, refreshToken);
        return ResponseEntity.ok(apiResponse);
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        AuthResponseDTO authResponseDTO = authService.login(request);
        AuthResponse authResponse = authResponseDTO.getAuthResponse();
        String refreshToken = authResponseDTO.getRefreshToken();
        ApiResponse<AuthResponse> apiResponse = ApiResponse.success(
                "User logged in successfully", authResponse);

        setRefreshTokenCookie(response, refreshToken);
        return ResponseEntity.ok(apiResponse);
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(
            @CookieValue(value = "refreshToken", required = false) String refreshToken, HttpServletResponse response
    ) {
        if (refreshToken == null) {
            throw new InvalidTokenException("Refresh token is missing");
        }
        AuthResponseDTO authResponseDTO = authService.refreshToken(refreshToken);
        AuthResponse authResponse = authResponseDTO.getAuthResponse();
        ApiResponse<AuthResponse> apiResponse = ApiResponse.success(
                "Token refreshed successfully", authResponse);

        setRefreshTokenCookie(response, authResponseDTO.getRefreshToken());
        return ResponseEntity.ok(apiResponse);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(value = "refreshToken", required = false) String refreshToken, HttpServletResponse response
    ) {
        if (refreshToken == null) {
            throw new InvalidTokenException("Refresh token is missing");
        }
        setRefreshTokenCookie(response, "");
        return ResponseEntity.noContent().build();
    }

    private void setRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        ResponseCookie cookie = ResponseCookie.from("refreshToken", refreshToken)
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path("/auth/refresh")
                .maxAge(REFRESH_TOKEN_MAX_AGE_SECONDS)
                .build();
        response.addHeader("Set-Cookie", cookie.toString());
    }

}
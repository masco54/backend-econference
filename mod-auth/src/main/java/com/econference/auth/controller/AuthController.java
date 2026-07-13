package com.econference.auth.controller;

import com.econference.auth.adapter.in.web.dto.mapper.UserMapper;
import com.econference.auth.adapter.in.web.dto.request.ChangePasswordRequest;
import com.econference.auth.adapter.in.web.dto.request.ForgotPasswordRequest;
import com.econference.auth.adapter.in.web.dto.request.LoginRequest;
import com.econference.auth.adapter.in.web.dto.request.RegisterRequest;
import com.econference.auth.adapter.in.web.dto.request.ResendVerificationRequest;
import com.econference.auth.adapter.in.web.dto.request.ResetPasswordRequest;
import com.econference.auth.adapter.in.web.dto.request.VerifyEmailRequest;
import com.econference.auth.adapter.in.web.dto.response.LoginResponse;
import com.econference.auth.adapter.in.web.dto.response.RegisterResponse;
import com.econference.auth.adapter.in.web.dto.response.UserResponse;
import com.econference.auth.application.port.in.ChangePasswordUseCase;
import com.econference.auth.application.port.in.ForgotPasswordUseCase;
import com.econference.auth.application.port.in.GetCurrentUserUseCase;
import com.econference.auth.application.port.in.LoginUseCase;
import com.econference.auth.application.port.in.LogoutUseCase;
import com.econference.auth.application.port.in.RegisterUserUseCase;
import com.econference.auth.application.port.in.ResendVerificationUseCase;
import com.econference.auth.application.port.in.ResetPasswordUseCase;
import com.econference.auth.application.port.in.VerifyEmailUseCase;
import com.econference.auth.domain.User;
import com.econference.auth.security.AuthenticatedUserPrincipal;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final RegisterUserUseCase registerUserUseCase;
    private final VerifyEmailUseCase verifyEmailUseCase;
    private final ResendVerificationUseCase resendVerificationUseCase;
    private final LoginUseCase loginUseCase;
    private final LogoutUseCase logoutUseCase;
    private final GetCurrentUserUseCase getCurrentUserUseCase;
    private final ForgotPasswordUseCase forgotPasswordUseCase;
    private final ResetPasswordUseCase resetPasswordUseCase;
    private final ChangePasswordUseCase changePasswordUseCase;

    @Value("${app.jwt.expiration-days:7}")
    private int expirationDays;

    @GetMapping("/api/v1/auth/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("module", "auth", "status", "up"));
    }

    @PostMapping("/api/v1/auth/register")
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        log.info("Registro solicitado para email={}", request.email());
        var result = registerUserUseCase.register(
                request.firstName(), request.lastName(), request.email(),
                request.password(), request.phone(),
                request.invitationToken(), request.coOrganizerToken());

        log.info("Usuário registrado com sucesso userId={} email={}", result.userId(), request.email());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new RegisterResponse(result.userId(), result.reviewerLinked(), result.coOrganizerLinked()));
    }

    /**
     * Verifica e-mail via link clicado no e-mail (GET com ?token=).
     * O front-end redireciona o usuário para http://localhost:4200/verify-email?token=...
     * que por sua vez chama este endpoint.
     */
    @GetMapping("/api/v1/auth/verify-email")
    public ResponseEntity<Map<String, String>> verifyEmailGet(@RequestParam String token) {
        log.info("Verificação de email via GET token={}", token);
        MDC.put("emailToken", token.substring(0, Math.min(8, token.length())) + "…");
        verifyEmailUseCase.verify(token);
        log.info("Email verificado com sucesso via GET");
        MDC.remove("emailToken");
        return ResponseEntity.ok(Map.of("message", "Email verified successfully"));
    }

    /**
     * Verifica e-mail via chamada JSON (POST com corpo { "token": "..." }).
     */
    @PostMapping("/api/v1/auth/verify-email")
    public ResponseEntity<Map<String, String>> verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        log.info("Verificação de email via POST");
        MDC.put("emailToken", request.token().substring(0, Math.min(8, request.token().length())) + "…");
        verifyEmailUseCase.verify(request.token());
        log.info("Email verificado com sucesso via POST");
        MDC.remove("emailToken");
        return ResponseEntity.ok(Map.of("message", "Email verified successfully"));
    }

    @PostMapping("/api/v1/auth/resend-verification")
    public ResponseEntity<Map<String, String>> resendVerification(@Valid @RequestBody ResendVerificationRequest request) {
        log.info("Reenvio de verificação solicitado para email={}", request.email());
        resendVerificationUseCase.resend(request.email());
        return ResponseEntity.ok(Map.of("message", "Verification email sent"));
    }

    @PostMapping("/api/v1/auth/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        log.info("Tentativa de login email={}", request.email());
        var result = loginUseCase.login(request.email(), request.password());
        User user = result.user();

        MDC.put("userId", user.getId().toString());
        MDC.put("userEmail", user.getEmail());
        log.info("Login efetuado com sucesso userId={}", user.getId());

        ResponseCookie cookie = ResponseCookie.from("auth_token", result.rawJwt())
                .httpOnly(true)
                .secure(true)
                .sameSite("Lax")
                .maxAge(Duration.ofDays(expirationDays))
                .path("/")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        return ResponseEntity.ok(UserMapper.toLoginResponse(user));
    }

    @PostMapping("/api/v1/auth/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        log.info("Logout solicitado");
        String jwt = extractTokenFromCookie(request);
        if (jwt != null) {
            logoutUseCase.logout(jwt);
        }

        ResponseCookie cookie = ResponseCookie.from("auth_token", "")
                .httpOnly(true)
                .secure(true)
                .sameSite("Lax")
                .maxAge(0)
                .path("/")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        log.info("Logout efetuado com sucesso");
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/v1/auth/me")
    public ResponseEntity<UserResponse> getCurrentUser(@AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
        log.debug("Buscando dados do usuário atual userId={}", principal.getUserId());
        User user = getCurrentUserUseCase.getUser(principal.getUserId());
        return ResponseEntity.ok(UserMapper.toUserResponse(user));
    }

    @PostMapping("/api/v1/auth/password/forgot")
    public ResponseEntity<Map<String, String>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        log.info("Solicitação de recuperação de senha email={}", request.email());
        forgotPasswordUseCase.forgotPassword(request.email());
        return ResponseEntity.ok(Map.of("message", "If the email exists, a reset link has been sent"));
    }

    @PostMapping("/api/v1/auth/password/reset")
    public ResponseEntity<Map<String, String>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        log.info("Redefinição de senha via token");
        resetPasswordUseCase.resetPassword(request.token(), request.newPassword());
        log.info("Senha redefinida com sucesso");
        return ResponseEntity.ok(Map.of("message", "Password reset successfully"));
    }

    @PostMapping("/api/v1/auth/password/change")
    public ResponseEntity<Map<String, String>> changePassword(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @Valid @RequestBody ChangePasswordRequest request) {
        log.info("Alteração de senha solicitada userId={}", principal.getUserId());
        changePasswordUseCase.changePassword(principal.getUserId(), request.currentPassword(), request.newPassword());
        log.info("Senha alterada com sucesso userId={}", principal.getUserId());
        return ResponseEntity.ok(Map.of("message", "Password changed successfully"));
    }

    private String extractTokenFromCookie(HttpServletRequest request) {
        if (request.getCookies() == null) return null;
        for (Cookie cookie : request.getCookies()) {
            if ("auth_token".equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
}

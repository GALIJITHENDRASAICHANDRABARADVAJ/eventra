package com.eventra.service;

import com.eventra.config.AppProperties;
import com.eventra.dto.AuthDtos.*;
import com.eventra.entity.PasswordResetToken;
import com.eventra.entity.RefreshToken;
import com.eventra.entity.User;
import com.eventra.exception.ApiException;
import com.eventra.exception.ErrorCode;
import com.eventra.notification.EmailService;
import com.eventra.repository.*;
import com.eventra.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {
    /** Used to keep login timing uniform when the email is unknown. */
    private volatile String dummyHash;

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final CityRepository cityRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordResetTokenRepository resetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserService userService;
    private final EmailService emailService;
    private final AuditService auditService;
    private final AppProperties props;
    private final Clock clock;

    public record IssuedTokens(AuthResponse response, String refreshToken, Duration refreshTtl) {}

    @Transactional
    public IssuedTokens register(RegisterRequest req, String userAgent, String ip) {
        if (userRepository.existsByEmail(req.email())) {
            throw new ApiException(ErrorCode.EMAIL_ALREADY_REGISTERED);
        }
        User user = new User();
        user.setEmail(req.email().trim().toLowerCase());
        user.setFullName(req.fullName().trim());
        user.setPhone(req.phone() == null || req.phone().isBlank() ? null : req.phone().trim());
        user.setPasswordHash(passwordEncoder.encode(req.password()));
        user.getRoles().add(roleRepository.findByName("USER").orElseThrow());
        userRepository.save(user);
        auditService.record(user.getId(), "USER_REGISTERED", "USER", user.getId(), null);
        return issue(user, userAgent, ip);
    }

    @Transactional
    public IssuedTokens login(LoginRequest req, String userAgent, String ip) {
        User user = userRepository.findByEmail(req.email().trim()).orElse(null);
        if (user == null) {
            passwordEncoder.matches(req.password(), dummyHash());
            throw new ApiException(ErrorCode.INVALID_CREDENTIALS);
        }
        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            auditService.record(user.getId(), "LOGIN_FAILED", "USER", user.getId(), null);
            throw new ApiException(ErrorCode.INVALID_CREDENTIALS);
        }
        if (!user.isEnabled()) {
            throw new ApiException(ErrorCode.ACCOUNT_DISABLED);
        }
        user.setLastLoginAt(Instant.now(clock));
        return issue(user, userAgent, ip);
    }

    /**
     * Rotating refresh tokens: each refresh revokes the presented token and issues a new one.
     * Presenting an already-revoked token is treated as theft and revokes the whole family.
     */
    @Transactional(noRollbackFor = ApiException.class)
    public IssuedTokens refresh(String rawToken, String userAgent, String ip) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new ApiException(ErrorCode.TOKEN_INVALID);
        }
        RefreshToken token = refreshTokenRepository.findByTokenHash(TokenHasher.sha256(rawToken))
                .orElseThrow(() -> new ApiException(ErrorCode.TOKEN_INVALID));
        Instant now = Instant.now(clock);
        if (token.getRevokedAt() != null) {
            log.warn("Refresh token reuse detected for user {}", token.getUser().getId());
            refreshTokenRepository.revokeAllForUser(token.getUser().getId(), now);
            auditService.record(token.getUser().getId(), "REFRESH_TOKEN_REUSE", "USER", token.getUser().getId(), null);
            throw new ApiException(ErrorCode.TOKEN_INVALID);
        }
        if (!token.getExpiresAt().isAfter(now) || !token.getUser().isEnabled()) {
            throw new ApiException(ErrorCode.TOKEN_INVALID);
        }
        IssuedTokens issued = issue(token.getUser(), userAgent, ip);
        token.setRevokedAt(now);
        token.setReplacedBy(TokenHasher.sha256(issued.refreshToken()));
        return issued;
    }

    @Transactional
    public void logout(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) return;
        refreshTokenRepository.findByTokenHash(TokenHasher.sha256(rawToken)).ifPresent(t -> {
            if (t.getRevokedAt() == null) t.setRevokedAt(Instant.now(clock));
        });
    }

    @Transactional
    public void forgotPassword(ForgotPasswordRequest req) {
        userRepository.findByEmail(req.email().trim()).filter(User::isEnabled).ifPresent(user -> {
            String raw = TokenHasher.newToken(32);
            PasswordResetToken t = new PasswordResetToken();
            t.setUser(user);
            t.setTokenHash(TokenHasher.sha256(raw));
            t.setExpiresAt(Instant.now(clock).plus(Duration.ofMinutes(30)));
            resetTokenRepository.save(t);
            String link = props.frontendUrl() + "/forgot-password?token=" + raw;
            emailService.send(user.getEmail(), "Reset your Eventra password", "password-reset",
                    Map.of("name", user.getFullName(), "link", link));
            auditService.record(user.getId(), "PASSWORD_RESET_REQUESTED", "USER", user.getId(), null);
        });
        // Always succeed so the endpoint cannot be used to discover registered emails.
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest req) {
        PasswordResetToken t = resetTokenRepository.findByTokenHash(TokenHasher.sha256(req.token()))
                .orElseThrow(() -> new ApiException(ErrorCode.TOKEN_INVALID, "This reset link is invalid or has expired"));
        Instant now = Instant.now(clock);
        if (t.getUsedAt() != null || !t.getExpiresAt().isAfter(now)) {
            throw new ApiException(ErrorCode.TOKEN_INVALID, "This reset link is invalid or has expired");
        }
        t.setUsedAt(now);
        User user = t.getUser();
        user.setPasswordHash(passwordEncoder.encode(req.newPassword()));
        refreshTokenRepository.revokeAllForUser(user.getId(), now);
        auditService.record(user.getId(), "PASSWORD_RESET", "USER", user.getId(), null);
    }

    @Transactional
    public IssuedTokens changePassword(Long userId, ChangePasswordRequest req, String userAgent, String ip) {
        User user = userRepository.findById(userId).orElseThrow(() -> new ApiException(ErrorCode.UNAUTHORIZED));
        if (!passwordEncoder.matches(req.currentPassword(), user.getPasswordHash())) {
            throw new ApiException(ErrorCode.INVALID_CREDENTIALS, "Current password is incorrect");
        }
        user.setPasswordHash(passwordEncoder.encode(req.newPassword()));
        refreshTokenRepository.revokeAllForUser(userId, Instant.now(clock));
        auditService.record(userId, "PASSWORD_CHANGED", "USER", userId, null);
        return issue(user, userAgent, ip);
    }

    private String dummyHash() {
        if (dummyHash == null) {
            dummyHash = passwordEncoder.encode("eventra-timing-equaliser");
        }
        return dummyHash;
    }

    private IssuedTokens issue(User user, String userAgent, String ip) {
        String raw = TokenHasher.newToken(48);
        Duration ttl = props.jwt().refreshExpiration();
        RefreshToken rt = new RefreshToken();
        rt.setUser(user);
        rt.setTokenHash(TokenHasher.sha256(raw));
        rt.setExpiresAt(Instant.now(clock).plus(ttl));
        rt.setUserAgent(userAgent == null ? null : userAgent.substring(0, Math.min(300, userAgent.length())));
        rt.setIpAddress(ip);
        refreshTokenRepository.save(rt);
        AuthResponse response = new AuthResponse(jwtService.issueAccessToken(user), raw, jwtService.accessTokenTtlSeconds(),
                userService.toResponse(user));
        return new IssuedTokens(response, raw, ttl);
    }
}

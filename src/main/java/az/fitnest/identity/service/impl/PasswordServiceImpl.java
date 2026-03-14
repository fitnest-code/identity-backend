package az.fitnest.identity.service.impl;

import az.fitnest.identity.model.enums.UserStatus;
import az.fitnest.identity.repository.UserRepository;

import az.fitnest.identity.dto.response.PasswordVerificationResultResponse;
import az.fitnest.identity.service.PasswordService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class PasswordServiceImpl implements PasswordService {

    private static final Logger log = LoggerFactory.getLogger(PasswordServiceImpl.class);
    private static final int MIN_PASSWORD_LENGTH = 10;
    private static final int MAX_PASSWORD_LENGTH = 128;
    private static final Pattern UPPERCASE = Pattern.compile("[A-Z]");
    private static final Pattern LOWERCASE = Pattern.compile("[a-z]");
    private static final Pattern DIGIT = Pattern.compile("\\d");
    private static final Pattern SPECIAL = Pattern.compile("[^A-Za-z0-9]");
    private static final Pattern WHITESPACE = Pattern.compile("\\s");

    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final MessageSource messageSource;

    @Override
    public String hashPassword(String rawPassword) {
        if (rawPassword == null) {
            throw new IllegalArgumentException(getMessage("error.service.password_invalid"));
        }
        String trimmed = rawPassword.trim();
        validatePassword(trimmed);
        return passwordEncoder.encode(trimmed);
    }

    @Override
    public PasswordVerificationResultResponse verifyPassword(String rawPassword, String passwordHash) {
        if (!StringUtils.hasText(rawPassword) || !StringUtils.hasText(passwordHash)) {
            log.warn("Password or hash is empty");
            return new PasswordVerificationResultResponse(false, false);
        }
        String trimmed = rawPassword.trim();
        try {
            boolean matches = passwordEncoder.matches(trimmed, passwordHash);
            boolean upgradeRecommended = matches && passwordEncoder.upgradeEncoding(passwordHash);
            return new PasswordVerificationResultResponse(matches, upgradeRecommended);
        } catch (IllegalArgumentException e) {
            log.warn("Password hash format invalid", e);
            return new PasswordVerificationResultResponse(false, false);
        }
    }

    @Override
    public boolean isStrongPassword(String password) {
        if (password == null || password.length() < MIN_PASSWORD_LENGTH) return false;
        if (WHITESPACE.matcher(password).find()) return false;
        return UPPERCASE.matcher(password).find() &&
               LOWERCASE.matcher(password).find() &&
               DIGIT.matcher(password).find() &&
               SPECIAL.matcher(password).find();
    }

    @Override
    public boolean isPasswordReused(Long userId, String newPassword) {
        az.fitnest.identity.model.entity.User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            log.warn("User not found for password reuse check: userId={}", userId);
            return false;
        }
        return passwordEncoder.matches(newPassword.trim(), user.getPasswordHash());
    }

    private void validatePassword(String rawPassword) {
        if (!StringUtils.hasText(rawPassword)) {
            throw new IllegalArgumentException(getMessage("error.service.password_invalid"));
        }
        if (rawPassword.length() > MAX_PASSWORD_LENGTH) {
            throw new IllegalArgumentException(getMessage("error.service.password_invalid"));
        }
        if (rawPassword.length() < MIN_PASSWORD_LENGTH) {
            throw new IllegalArgumentException(getMessage("error.service.password_invalid"));
        }
        if (WHITESPACE.matcher(rawPassword).find()) {
            throw new IllegalArgumentException(getMessage("error.service.password_invalid"));
        }
    }

    private String getMessage(String code) {
        return messageSource.getMessage(code, null, LocaleContextHolder.getLocale());
    }
}

package az.fitnest.identity.service;
import az.fitnest.identity.model.enums.UserStatus;

import az.fitnest.identity.model.enums.OtpPurpose;
import az.fitnest.identity.dto.ForgotPasswordRequest;
import az.fitnest.identity.dto.OtpSendRequest;
import az.fitnest.identity.dto.OtpSendResponse;
import az.fitnest.identity.dto.OtpVerifyRequest;
import az.fitnest.identity.dto.ResetPasswordRequest;
import az.fitnest.identity.dto.ResetPasswordResponse;
import az.fitnest.identity.model.entity.AuthToken;
import az.fitnest.identity.model.entity.User;
import az.fitnest.identity.exception.InvalidCredentialsException;
import az.fitnest.identity.repository.AuthTokenRepository;
import az.fitnest.identity.repository.UserRepository;
import az.fitnest.identity.security.RedisTokenService;
import az.fitnest.identity.service.*;
import az.fitnest.identity.service.OtpService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

public interface PasswordResetService {
    OtpSendResponse forgotPassword(ForgotPasswordRequest request);
    ResetPasswordResponse resetPassword(ResetPasswordRequest request);
}

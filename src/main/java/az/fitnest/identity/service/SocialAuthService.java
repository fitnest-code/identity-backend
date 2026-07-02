package az.fitnest.identity.service;

import az.fitnest.identity.model.enums.UserStatus;

import az.fitnest.identity.model.enums.SocialProvider;
import az.fitnest.identity.dto.request.AppleSocialRequest;
import az.fitnest.identity.dto.request.GoogleSocialRequest;
import az.fitnest.identity.dto.response.LoginResponse;
import az.fitnest.identity.model.entity.SocialAuth;
import az.fitnest.identity.model.entity.User;
import az.fitnest.identity.exception.ConflictException;
import az.fitnest.identity.exception.InvalidCredentialsException;
import az.fitnest.identity.repository.RoleRepository;
import az.fitnest.identity.repository.SocialAuthRepository;
import az.fitnest.identity.repository.UserRepository;
import az.fitnest.identity.service.*;

import java.util.Optional;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

public interface SocialAuthService {
    LoginResponse socialLoginApple(AppleSocialRequest request);

    LoginResponse socialLoginGoogle(GoogleSocialRequest request);

    LoginResponse socialLoginAppleV2(az.fitnest.identity.dto.request.AppleSocialRequestV2 request);

    LoginResponse socialLoginGoogleV2(az.fitnest.identity.dto.request.GoogleSocialRequestV2 request);

    az.fitnest.identity.dto.response.OtpSendResponse requestAddNumberOtpGoogle(az.fitnest.identity.dto.request.AddNumberOtpRequest request);

    az.fitnest.identity.dto.response.OtpSendResponse requestAddNumberOtpApple(az.fitnest.identity.dto.request.AddNumberOtpRequest request);

    LoginResponse verifyAddNumberOtpGoogle(az.fitnest.identity.dto.request.AddNumberOtpVerifyRequest request);

    LoginResponse verifyAddNumberOtpApple(az.fitnest.identity.dto.request.AddNumberOtpVerifyRequest request);
}

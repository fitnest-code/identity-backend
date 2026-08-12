package az.fitnest.identity.service;

import az.fitnest.identity.dto.request.LoginRequest;
import az.fitnest.identity.dto.response.LoginEligibilityResponse;
import az.fitnest.identity.dto.response.RefreshResponse;

public interface AuthService {
    az.fitnest.identity.dto.response.LoginResult login(LoginRequest request);
    az.fitnest.identity.dto.response.LoginResult loginV2(az.fitnest.identity.dto.request.LoginRequestV2 request);

    RefreshResponse refresh(String refreshToken);

    RefreshResponse refresh(az.fitnest.identity.dto.request.RefreshRequest request, String userAgent);

    RefreshResponse refresh(az.fitnest.identity.dto.request.RefreshRequest request, String userAgent, String xDeviceType, String xPlatform);

    void logout(String accessToken);

    void logoutFromHeader(String authHeader);

    az.fitnest.identity.dto.response.OtpSendResponse startLoginV3(az.fitnest.identity.dto.request.LoginRequestV3 request);

    az.fitnest.identity.dto.response.LoginResponse verifyLoginV3(az.fitnest.identity.dto.request.LoginVerifyRequestV3 request);

    LoginEligibilityResponse checkLoginEligibility(az.fitnest.identity.dto.request.LoginCheckRequestV3 request);
}

package az.fitnest.identity.service;

import az.fitnest.identity.model.enums.UserStatus;

import az.fitnest.identity.dto.request.LoginRequest;
import az.fitnest.identity.dto.response.LoginResponse;
import az.fitnest.identity.dto.response.RefreshResponse;
import az.fitnest.identity.model.entity.User;
import az.fitnest.identity.exception.InvalidCredentialsException;
import az.fitnest.identity.exception.UnauthorizedException;
import az.fitnest.identity.repository.AuthTokenRepository;
import az.fitnest.identity.repository.UserRepository;
import az.fitnest.identity.security.JwtService;
import io.jsonwebtoken.JwtException;

import java.time.Instant;
import java.time.LocalDateTime;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

public interface AuthService {
    az.fitnest.identity.dto.response.LoginResult login(LoginRequest request);
    az.fitnest.identity.dto.response.LoginResult loginV2(az.fitnest.identity.dto.request.LoginRequestV2 request);

    RefreshResponse refresh(String refreshToken);

    void logout(String accessToken);

    void logoutFromHeader(String authHeader);

    az.fitnest.identity.dto.response.OtpSendResponse startLoginV3(az.fitnest.identity.dto.request.LoginRequestV3 request);

    az.fitnest.identity.dto.response.LoginResponse verifyLoginV3(az.fitnest.identity.dto.request.LoginVerifyRequestV3 request);
}

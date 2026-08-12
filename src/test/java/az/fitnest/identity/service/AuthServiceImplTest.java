package az.fitnest.identity.service;

import az.fitnest.identity.dto.request.RefreshRequest;
import az.fitnest.identity.dto.response.LoginResponse;
import az.fitnest.identity.dto.response.RefreshResponse;
import az.fitnest.identity.dto.response.UserResponse;
import az.fitnest.identity.exception.UnauthorizedException;
import az.fitnest.identity.model.entity.AuthToken;
import az.fitnest.identity.model.entity.User;
import az.fitnest.identity.model.enums.SessionStatus;
import az.fitnest.identity.model.enums.UserStatus;
import az.fitnest.identity.repository.AuthTokenRepository;
import az.fitnest.identity.repository.UserRepository;
import az.fitnest.identity.security.JwtService;
import az.fitnest.identity.security.RedisTokenService;
import az.fitnest.identity.service.impl.AuthServiceImpl;
import az.fitnest.identity.service.impl.TestUserHelper;
import az.fitnest.identity.util.TokenHasher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordService passwordService;
    @Mock
    private JwtService jwtService;
    @Mock
    private AuthTokenRepository authTokenRepository;
    @Mock
    private RedisTokenService redisTokenService;
    @Mock
    private TokenIssuanceService tokenIssuanceService;
    @Mock
    private OtpService otpService;
    @Mock
    private TokenHasher tokenHasher;
    @Mock
    private MessageSource messageSource;
    @Mock
    private DeviceService deviceService;
    @Mock
    private TestUserHelper testUserHelper;
    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        authService = new AuthServiceImpl(
                userRepository,
                passwordService,
                jwtService,
                authTokenRepository,
                redisTokenService,
                tokenIssuanceService,
                otpService,
                tokenHasher,
                messageSource,
                deviceService,
                testUserHelper,
                kafkaTemplate
        );
    }

    @Test
    void refresh_shouldUseDeviceTypeFromRequest_whenProvided() {
        String refreshToken = "valid-refresh-token";
        String hashedToken = "hashed-refresh-token";
        Long userId = 100L;
        Instant futureExpiry = Instant.now().plusSeconds(3600);

        RefreshRequest request = new RefreshRequest(refreshToken, "iOS");

        User user = User.builder()
                .status(UserStatus.ACTIVE)
                .sessionStatus(SessionStatus.HAVE_SESSIONS)
                .build();
        user.setId(userId);

        UserResponse userResp = mock(UserResponse.class);
        LoginResponse issuedTokens = new LoginResponse("new-access-token", "new-refresh-token", userResp);

        when(jwtService.parseUserId(refreshToken, "refresh")).thenReturn(userId);
        when(jwtService.parseExpiration(refreshToken)).thenReturn(futureExpiry);
        when(tokenHasher.hash(refreshToken)).thenReturn(hashedToken);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(authTokenRepository.consumeRefreshToken(eq(userId), eq(hashedToken), any(Instant.class))).thenReturn(1);
        when(tokenIssuanceService.issueTokens(user, "iOS")).thenReturn(issuedTokens);

        RefreshResponse response = authService.refresh(request, null);

        assertNotNull(response);
        assertEquals("new-access-token", response.accessToken());
        assertEquals("new-refresh-token", response.refreshToken());
        verify(tokenIssuanceService).issueTokens(user, "iOS");
    }

    @Test
    void refresh_shouldFallbackToUserAgent_whenDeviceTypeNotInRequestOrDb() {
        String refreshToken = "valid-refresh-token";
        String hashedToken = "hashed-refresh-token";
        Long userId = 101L;
        Instant futureExpiry = Instant.now().plusSeconds(3600);

        RefreshRequest request = new RefreshRequest(refreshToken, null);
        String userAgent = "Fitnest/1.0 (iPhone; iOS 16.5)";

        User user = User.builder()
                .status(UserStatus.ACTIVE)
                .sessionStatus(SessionStatus.HAVE_SESSIONS)
                .build();
        user.setId(userId);

        UserResponse userResp = mock(UserResponse.class);
        LoginResponse issuedTokens = new LoginResponse("new-access-token-2", "new-refresh-token-2", userResp);

        when(jwtService.parseUserId(refreshToken, "refresh")).thenReturn(userId);
        when(jwtService.parseExpiration(refreshToken)).thenReturn(futureExpiry);
        when(tokenHasher.hash(refreshToken)).thenReturn(hashedToken);
        when(authTokenRepository.findByRefreshTokenHash(hashedToken)).thenReturn(null);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(authTokenRepository.consumeRefreshToken(eq(userId), eq(hashedToken), any(Instant.class))).thenReturn(1);
        when(tokenIssuanceService.issueTokens(user, "iOS")).thenReturn(issuedTokens);

        RefreshResponse response = authService.refresh(request, userAgent);

        assertNotNull(response);
        assertEquals("new-access-token-2", response.accessToken());
        verify(tokenIssuanceService).issueTokens(user, "iOS");
    }

    @Test
    void refresh_shouldDetectAndroidFromUserAgent() {
        String refreshToken = "valid-android-refresh-token";
        String hashedToken = "hashed-android-refresh-token";
        Long userId = 103L;
        Instant futureExpiry = Instant.now().plusSeconds(3600);

        RefreshRequest request = new RefreshRequest(refreshToken, null);
        String userAgent = "okhttp/4.9.0 (Android 12; Pixel 6)";

        User user = User.builder()
                .status(UserStatus.ACTIVE)
                .sessionStatus(SessionStatus.HAVE_SESSIONS)
                .build();
        user.setId(userId);

        UserResponse userResp = mock(UserResponse.class);
        LoginResponse issuedTokens = new LoginResponse("new-access-token-3", "new-refresh-token-3", userResp);

        when(jwtService.parseUserId(refreshToken, "refresh")).thenReturn(userId);
        when(jwtService.parseExpiration(refreshToken)).thenReturn(futureExpiry);
        when(tokenHasher.hash(refreshToken)).thenReturn(hashedToken);
        when(authTokenRepository.findByRefreshTokenHash(hashedToken)).thenReturn(null);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(authTokenRepository.consumeRefreshToken(eq(userId), eq(hashedToken), any(Instant.class))).thenReturn(1);
        when(tokenIssuanceService.issueTokens(user, "Android")).thenReturn(issuedTokens);

        RefreshResponse response = authService.refresh(request, userAgent, null, null);

        assertNotNull(response);
        assertEquals("new-access-token-3", response.accessToken());
        verify(tokenIssuanceService).issueTokens(user, "Android");
    }

    @Test
    void refresh_shouldUseXPlatformHeader_whenProvided() {
        String refreshToken = "valid-platform-refresh-token";
        String hashedToken = "hashed-platform-refresh-token";
        Long userId = 104L;
        Instant futureExpiry = Instant.now().plusSeconds(3600);

        RefreshRequest request = new RefreshRequest(refreshToken, null);

        User user = User.builder()
                .status(UserStatus.ACTIVE)
                .sessionStatus(SessionStatus.HAVE_SESSIONS)
                .build();
        user.setId(userId);

        UserResponse userResp = mock(UserResponse.class);
        LoginResponse issuedTokens = new LoginResponse("new-access-token-4", "new-refresh-token-4", userResp);

        when(jwtService.parseUserId(refreshToken, "refresh")).thenReturn(userId);
        when(jwtService.parseExpiration(refreshToken)).thenReturn(futureExpiry);
        when(tokenHasher.hash(refreshToken)).thenReturn(hashedToken);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(authTokenRepository.consumeRefreshToken(eq(userId), eq(hashedToken), any(Instant.class))).thenReturn(1);
        when(tokenIssuanceService.issueTokens(user, "Android")).thenReturn(issuedTokens);

        RefreshResponse response = authService.refresh(request, null, null, "ANDROID");

        assertNotNull(response);
        assertEquals("new-access-token-4", response.accessToken());
        verify(tokenIssuanceService).issueTokens(user, "Android");
    }

    @Test
    void refresh_shouldThrowUnauthorizedException_whenTokenIsExpired() {
        String refreshToken = "expired-refresh-token";
        Long userId = 102L;
        Instant pastExpiry = Instant.now().minusSeconds(3600);

        RefreshRequest request = new RefreshRequest(refreshToken, "Android");

        when(jwtService.parseUserId(refreshToken, "refresh")).thenReturn(userId);
        when(jwtService.parseExpiration(refreshToken)).thenReturn(pastExpiry);

        assertThrows(UnauthorizedException.class, () -> authService.refresh(request, null));
    }
}

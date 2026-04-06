package az.fitnest.identity.mapper;

import az.fitnest.identity.dto.response.UserResponse;
import az.fitnest.identity.model.entity.User;
import az.fitnest.identity.model.enums.UserStatus;
import az.fitnest.identity.service.LegalService;
import az.fitnest.identity.service.UserProfileGrpcClient;
import az.fitnest.user.grpc.UserProfileDetailsResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserResponseMapperTest {

    @Mock
    private UserProfileGrpcClient userProfileGrpcClient;

    @Mock
    private LegalService legalService;

    private UserResponseMapper userResponseMapper;

    @BeforeEach
    void setUp() {
        userResponseMapper = new UserResponseMapper(userProfileGrpcClient, legalService);
    }

    @Test
    void toResponse_ShouldPrefixProfileImageUrl_WhenIdIsProvided() {
        Long userId = 1L;
        User user = new User();
        user.setId(userId);
        user.setStatus(UserStatus.ACTIVE);

        UserProfileDetailsResponse profile = UserProfileDetailsResponse.newBuilder()
                .setProfileImageUrl("858482831")
                .setFirstName("John")
                .setLastName("Doe")
                .setEmail("john.doe@example.com")
                .build();

        when(userProfileGrpcClient.getUserProfileDetails(userId)).thenReturn(profile);
        when(legalService.isConsentRequired(userId)).thenReturn(false);

        UserResponse response = userResponseMapper.toResponse(user);

        assertEquals("/api/v1/me/profile/images/858482831", response.profileImageUrl());
        assertEquals("John", response.firstName());
        assertEquals("Doe", response.lastName());
    }

    @Test
    void toResponse_ShouldNotPrefix_WhenUrlIsHttp() {
        Long userId = 1L;
        User user = new User();
        user.setId(userId);
        user.setStatus(UserStatus.ACTIVE);

        UserProfileDetailsResponse profile = UserProfileDetailsResponse.newBuilder()
                .setProfileImageUrl("https://example.com/photo.jpg")
                .build();

        when(userProfileGrpcClient.getUserProfileDetails(userId)).thenReturn(profile);
        when(legalService.isConsentRequired(userId)).thenReturn(false);

        UserResponse response = userResponseMapper.toResponse(user);

        assertEquals("https://example.com/photo.jpg", response.profileImageUrl());
    }

    @Test
    void toResponse_ShouldReturnNull_WhenImageUrlIsBlank() {
        Long userId = 1L;
        User user = new User();
        user.setId(userId);
        user.setStatus(UserStatus.ACTIVE);

        UserProfileDetailsResponse profile = UserProfileDetailsResponse.newBuilder()
                .setProfileImageUrl("")
                .build();

        when(userProfileGrpcClient.getUserProfileDetails(userId)).thenReturn(profile);
        when(legalService.isConsentRequired(userId)).thenReturn(false);

        UserResponse response = userResponseMapper.toResponse(user);

        assertNull(response.profileImageUrl());
    }
}

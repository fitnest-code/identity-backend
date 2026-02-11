package az.fitnest.identity.service;

import az.fitnest.identity.constants.OtpPurpose;
import az.fitnest.identity.dto.LoginResponse;
import az.fitnest.identity.dto.OtpSendRequest;
import az.fitnest.identity.dto.OtpSendResponse;
import az.fitnest.identity.dto.RegisterCompleteRequest;
import az.fitnest.identity.dto.RegisterRequest;
import az.fitnest.identity.entity.OtpVerificationResult;
import az.fitnest.identity.entity.User;
import az.fitnest.identity.exception.ConflictException;
import az.fitnest.identity.exception.InvalidCredentialsException;
import az.fitnest.identity.repository.UserRepository;
import az.fitnest.identity.service.*;
import az.fitnest.identity.service.OtpService;
import az.fitnest.identity.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

public interface RegistrationService {
    OtpSendResponse startRegistration(RegisterRequest request);
    LoginResponse completeRegistration(RegisterCompleteRequest request);
}

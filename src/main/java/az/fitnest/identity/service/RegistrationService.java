package az.fitnest.identity.service;

import az.fitnest.identity.dto.response.LoginResponse;
import az.fitnest.identity.dto.response.OtpSendResponse;
import az.fitnest.identity.dto.request.RegisterCompleteRequest;
import az.fitnest.identity.dto.request.RegisterRequest;

public interface RegistrationService {
    OtpSendResponse startRegistration(RegisterRequest request);

    LoginResponse completeRegistration(RegisterCompleteRequest request);

    LoginResponse completeRegistrationV2(az.fitnest.identity.dto.request.RegisterCompleteRequestV2 request);

    OtpSendResponse startRegistrationV3(az.fitnest.identity.dto.request.RegisterRequestV3 request);

    LoginResponse completeRegistrationV3(az.fitnest.identity.dto.request.RegisterCompleteRequestV3 request);
}

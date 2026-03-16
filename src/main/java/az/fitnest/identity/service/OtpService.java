package az.fitnest.identity.service;

import az.fitnest.identity.model.enums.OtpPurpose;
import az.fitnest.identity.dto.request.OtpSendRequest;
import az.fitnest.identity.dto.response.OtpSendResponse;
import az.fitnest.identity.dto.request.OtpVerifyRequest;
import az.fitnest.identity.dto.response.OtpVerifyResponse;
import az.fitnest.identity.model.entity.OtpVerificationResult;

public interface OtpService {
    OtpSendResponse sendOtp(OtpSendRequest request);

    OtpSendResponse sendOtp(OtpSendRequest request, String firstName, String lastName, String userPasswordHash, String mobile);

    OtpSendResponse sendOtpByUserId(Long userId, OtpSendRequest request);

    OtpVerificationResult verifyOtp(String sessionId, String otpCode);

    OtpVerificationResult verifyOtpByIdentifier(String identifier, OtpPurpose purpose, String otpCode);

    OtpVerificationResult verifyOtpByUserId(Long userId, OtpPurpose purpose, String otpCode);

    OtpVerifyResponse verifyOtpAndIssueToken(OtpVerifyRequest request);

    OtpSendResponse resendOtp(String sessionId, OtpPurpose purpose);
}

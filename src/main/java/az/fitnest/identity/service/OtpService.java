package az.fitnest.identity.service;

import az.fitnest.identity.model.enums.OtpPurpose;
import az.fitnest.identity.dto.OtpSendRequest;
import az.fitnest.identity.dto.OtpSendResponse;
import az.fitnest.identity.dto.OtpVerifyRequest;
import az.fitnest.identity.dto.OtpVerifyResponse;
import az.fitnest.identity.model.entity.OtpVerificationResult;

public interface OtpService {
    OtpSendResponse sendOtp(OtpSendRequest request);

    OtpSendResponse sendOtp(OtpSendRequest request, String firstName, String lastName, String userPasswordHash, String mobile);

    OtpSendResponse sendOtpByUserId(Long userId, OtpSendRequest request);

    OtpVerificationResult verifyOtp(String sessionId, String otpCode);

    OtpVerificationResult verifyOtpByIdentifier(String identifier, OtpPurpose purpose, String otpCode);

    OtpVerificationResult verifyOtpByUserId(Long userId, OtpPurpose purpose, String otpCode);

    OtpVerifyResponse verifyOtpAndIssueToken(OtpVerifyRequest request);
}

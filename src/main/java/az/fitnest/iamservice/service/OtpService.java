package az.fitnest.iamservice.service;

import az.fitnest.iamservice.dto.request.OtpSendRequest;
import az.fitnest.iamservice.dto.response.OtpSendResponse;

public interface OtpService {

    OtpSendResponse sendOtp(OtpSendRequest request);
}

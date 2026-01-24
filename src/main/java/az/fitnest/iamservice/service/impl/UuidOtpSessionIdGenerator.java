package az.fitnest.iamservice.service.impl;

import az.fitnest.iamservice.service.OtpSessionIdGenerator;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class UuidOtpSessionIdGenerator implements OtpSessionIdGenerator {

    @Override
    public String generateSessionId() {
        return UUID.randomUUID().toString();
    }
}

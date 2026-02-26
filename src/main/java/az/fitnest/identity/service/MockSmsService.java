package az.fitnest.identity.service;

import org.springframework.stereotype.Service;

// @Service  // Active while notifications service is offline — uses mock OTP 1111
// @Service  // Active while notifications service is offline — uses mock OTP 1111
public class MockSmsService implements SmsService {

    @Override
    public void sendSms(String to, String message) {
        // Mock SMS sent (logging removed)
    }
}

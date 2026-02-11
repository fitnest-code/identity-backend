package az.fitnest.identity.service.impl;

import az.fitnest.identity.service.SmsService;
import org.springframework.stereotype.Service;

@Service
public class MockSmsServiceImpl implements SmsService {

    @Override
    public void sendSms(String to, String message) {
        // Mock SMS implementation
    }
}

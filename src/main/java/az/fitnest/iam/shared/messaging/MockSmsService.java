package az.fitnest.iam.shared.messaging;


import org.springframework.stereotype.Service;

@Service
public class MockSmsService implements SmsService {

    @Override
    public void sendSms(String to, String message) {
        // Mock SMS implementation
    }
}

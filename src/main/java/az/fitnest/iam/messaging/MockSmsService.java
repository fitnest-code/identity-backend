package az.fitnest.iam.messaging;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class MockSmsService implements SmsService {

    @Override
    public void sendSms(String to, String message) {
        log.info("Mock SMS sent to {}: {}", to, message);
    }
}

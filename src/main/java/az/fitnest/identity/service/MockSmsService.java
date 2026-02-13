package az.fitnest.identity.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service  // Active while notifications service is offline — uses mock OTP 1111
@Slf4j
public class MockSmsService implements SmsService {

    @Override
    public void sendSms(String to, String message) {
        log.info("[MOCK SMS] To: {}, Message: {}", to, message);
    }
}

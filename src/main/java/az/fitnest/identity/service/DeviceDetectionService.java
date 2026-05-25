package az.fitnest.identity.service;

import az.fitnest.identity.util.DeviceDetector;
import org.springframework.stereotype.Service;

@Service
public class DeviceDetectionService {

    public String detectDeviceType() {
        return DeviceDetector.detectDeviceType();
    }
}

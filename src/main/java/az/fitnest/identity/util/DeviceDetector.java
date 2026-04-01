package az.fitnest.identity.util;

import az.fitnest.identity.model.enums.UserStatus;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

public class DeviceDetector {

    public static String detectDeviceType() {
        HttpServletRequest request = getCurrentRequest();
        if (request == null) {
            return "UNKNOWN";
        }

        String userAgent = request.getHeader("User-Agent");
        if (userAgent == null) {
            return "UNKNOWN";
        }

        String ua = userAgent.toLowerCase();
        if (ua.contains("android")) {
            return "Android";
        } else if (ua.contains("iphone") || ua.contains("ipad") || ua.contains("ios")) {
            return "iOS";
        }

        return "UNKNOWN";
    }

    private static HttpServletRequest getCurrentRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes != null ? attributes.getRequest() : null;
    }
}

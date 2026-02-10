package az.fitnest.identity.otp.domain.constants;

public final class OtpMessages {

    private OtpMessages() {}

    public static final String OTP_SENT =
            "OTP code has been sent to your mobile number.";

    public static final String OTP_SENT_IF_EXISTS =
            "If an account exists with this mobile number, an OTP code has been sent.";

    public static final String INVALID_OTP =
            "Invalid OTP code";

    public static final String OTP_LOCKED =
            "OTP session is locked due to too many failed attempts. Please request a new code.";

    public static final String OTP_ALREADY_VERIFIED =
            "OTP code has already been used. Please request a new one.";

    public static final String OTP_VERIFIED =
            "OTP successfully verified.";

    public static String rateLimitSeconds(long seconds) {
        return "Please wait " + seconds + " seconds before requesting another OTP.";
    }

    public static String rateLimitMinutes(long minutes) {
        return "Too many OTP requests. Please try again in " + minutes + " minutes.";
    }
}

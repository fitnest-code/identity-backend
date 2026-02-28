package az.fitnest.identity.constants;
import az.fitnest.identity.model.enums.UserStatus;

public final class OtpMessages {

    private OtpMessages() {}

    public static final String OTP_SENT =
            "Əgər qeyd olunan mobil nömrə sistemdə mövcuddursa, təsdiq kodu göndərildi.";

    public static final String OTP_SENT_IF_EXISTS =
            "Əgər qeyd olunan mobil nömrə sistemdə mövcuddursa, təsdiq kodu göndərildi.";

    public static final String INVALID_OTP =
            "Yanlış OTP kodu";

    public static final String OTP_LOCKED =
            "Həddindən artıq sayda uğursuz cəhd səbəbindən OTP sessiyası bloklanıb. Zəhmət olmasa yeni kod sorğulayın.";

    public static final String OTP_ALREADY_VERIFIED =
            "OTP kodu artıq istifadə edilib. Zəhmət olmasa yenisini sorğulayın.";

    public static final String OTP_VERIFIED =
            "OTP uğurla təsdiqləndi.";

    public static String rateLimitSeconds(long seconds) {
        return "Yenidən OTP sorğulamadan əvvəl " + seconds + " saniyə gözləyin.";
    }

    public static String rateLimitMinutes(long minutes) {
        return "Həddindən artıq OTP sorğusu. Zəhmət olmasa " + minutes + " dəqiqədən sonra yenidən yoxlayın.";
    }

    public static String rateLimitGeneric() {
        return "Həddindən artıq sorğu. Zəhmət olmasa bir az sonra yenidən yoxlayın.";
    }
}

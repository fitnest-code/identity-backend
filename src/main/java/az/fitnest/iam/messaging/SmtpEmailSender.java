package az.fitnest.iam.messaging;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class SmtpEmailSender {

    private final JavaMailSender mailSender;

    @Value("${mail.from:no-reply@fitnest.az}")
    private String fromAddress;

    public void sendOtp(String email, String otp, String purpose) {
        try {
            String html = """
                    <!DOCTYPE html>
                    <html>
                    <head>
                      <meta charset="UTF-8" />
                      <title>Your verification code</title>
                    </head>
                    <body style="margin:0; padding:0; background:#f6f7fb; font-family: Arial, Helvetica, sans-serif;">
                    
                      <table width="100%%" cellpadding="0" cellspacing="0" style="background:#f6f7fb; padding:20px;">
                        <tr>
                          <td align="center">
                    
                            <table width="600" cellpadding="0" cellspacing="0" style="background:#ffffff; border-radius:10px; overflow:hidden; box-shadow:0 4px 10px rgba(0,0,0,0.05);">
                    
                              <!-- Header -->
                              <tr>
                                <td style="background:#4f46e5; color:#ffffff; padding:20px; text-align:center;">
                                  <h1 style="margin:0; font-size:24px;">Fitnest</h1>
                                </td>
                              </tr>
                    
                              <!-- Body -->
                              <tr>
                                <td style="padding:30px; color:#333333;">
                                  <h2 style="margin-top:0;">Your verification code</h2>
                    
                                  <p>
                                    Use the code below to complete your action in the Fitnest app:
                                  </p>
                    
                                  <p style="font-size:28px; font-weight:bold; letter-spacing:4px; text-align:center; margin:24px 0;">
                                    %s
                                  </p>
                    
                                  <p style="font-size:14px; color:#666;">
                                    This code is valid for a limited time and can be used only once.
                                    If you did not request this code, you can safely ignore this email.
                                  </p>
                    
                                  <p style="margin-top:30px;">
                                    — The Fitnest Team
                                  </p>
                                </td>
                              </tr>
                    
                              <!-- Footer -->
                              <tr>
                                <td style="background:#f1f1f1; padding:15px; text-align:center; font-size:12px; color:#888;">
                                  © 2026 Fitnest. All rights reserved.
                                </td>
                              </tr>
                    
                            </table>
                    
                          </td>
                        </tr>
                      </table>
                    
                    </body>
                    </html>
                    """.formatted(otp);

            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "UTF-8");
            helper.setTo(email);
            helper.setFrom(fromAddress);
            helper.setSubject("Your Fitnest verification code");
            helper.setText(html, true);

            mailSender.send(mimeMessage);
        } catch (MessagingException ex) {
            log.error("Failed to send OTP email to {}", email, ex);
        }
    }

    public void sendAccountDeletionNotice(String email, int gracePeriodDays) {
        String recoverUrl = "https://app.fitnest.az/account/recover";

        String html = """
                <!DOCTYPE html>
                <html>
                <head>
                  <meta charset="UTF-8" />
                  <title>Account Deletion Request</title>
                </head>
                <body style="margin:0; padding:0; background:#f6f7fb; font-family: Arial, Helvetica, sans-serif;">
                
                  <table width="100%%" cellpadding="0" cellspacing="0" style="background:#f6f7fb; padding:20px;">
                    <tr>
                      <td align="center">
                
                        <table width="600" cellpadding="0" cellspacing="0" style="background:#ffffff; border-radius:10px; overflow:hidden; box-shadow:0 4px 10px rgba(0,0,0,0.05);">
                
                          <!-- Header -->
                          <tr>
                            <td style="background:#4f46e5; color:#ffffff; padding:20px; text-align:center;">
                              <h1 style="margin:0; font-size:24px;">Fitnest</h1>
                            </td>
                          </tr>
                
                          <!-- Body -->
                          <tr>
                            <td style="padding:30px; color:#333333;">
                              <h2 style="margin-top:0;">We received a request to delete your account</h2>
                
                              <p>
                                We received a request to permanently delete your Fitnest account.  
                                Your account and all associated data are now scheduled for deletion.
                              </p>
                
                              <p>
                                <strong>You have %d days to cancel this request.</strong><br />
                                If you did not request this, or if you changed your mind, you can recover your account by clicking the button below.
                              </p>
                
                              <!-- Button -->
                              <div style="text-align:center; margin:30px 0;">
                                <a href="%s"
                                   style="background:#4f46e5; color:#ffffff; text-decoration:none; padding:14px 28px; border-radius:6px; display:inline-block; font-weight:bold;">
                                  Recover My Account
                                </a>
                              </div>
                
                              <p style="font-size:14px; color:#666;">
                                If the button doesn’t work, copy and paste this link into your browser:
                              </p>
                
                              <p style="font-size:14px; word-break:break-all;">
                                %s
                              </p>
                
                              <p style="margin-top:30px;">
                                If you requested this deletion, you can safely ignore this email.
                              </p>
                
                              <p>
                                — The Fitnest Team
                              </p>
                            </td>
                          </tr>
                
                          <!-- Footer -->
                          <tr>
                            <td style="background:#f1f1f1; padding:15px; text-align:center; font-size:12px; color:#888;">
                              © 2026 Fitnest. All rights reserved.
                            </td>
                          </tr>
                
                        </table>
                
                      </td>
                    </tr>
                  </table>
                
                </body>
                </html>
                """.formatted(gracePeriodDays, recoverUrl, recoverUrl);

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "UTF-8");
            helper.setTo(email);
            helper.setFrom(fromAddress);
            helper.setSubject("Your Fitnest account is scheduled for deletion");
            helper.setText(html, true);

            mailSender.send(mimeMessage);
        } catch (MessagingException ex) {
            log.error("Failed to send account deletion email to {}", email, ex);
        }
    }

    public void sendAccountRecoveryNotice(String email) {
        try {
            String html = """
                    <!DOCTYPE html>
                    <html>
                    <head>
                      <meta charset="UTF-8" />
                      <title>Account Reactivated</title>
                    </head>
                    <body style="margin:0; padding:0; background:#f6f7fb; font-family: Arial, Helvetica, sans-serif;">
                    
                      <table width="100%%" cellpadding="0" cellspacing="0" style="background:#f6f7fb; padding:20px;">
                        <tr>
                          <td align="center">
                    
                            <table width="600" cellpadding="0" cellspacing="0" style="background:#ffffff; border-radius:10px; overflow:hidden; box-shadow:0 4px 10px rgba(0,0,0,0.05);">
                    
                              <!-- Header -->
                              <tr>
                                <td style="background:#4f46e5; color:#ffffff; padding:20px; text-align:center;">
                                  <h1 style="margin:0; font-size:24px;">Fitnest</h1>
                                </td>
                              </tr>
                    
                              <!-- Body -->
                              <tr>
                                <td style="padding:30px; color:#333333;">
                                  <h2 style="margin-top:0;">Your account has been reactivated</h2>
                    
                                  <p>
                                    Your Fitnest account has been successfully reactivated, and the previous deletion request has been cancelled.
                                  </p>
                    
                                  <p>
                                    You can continue using the app as usual. If you did not perform this action, please change your password immediately and contact support.
                                  </p>
                    
                                  <p style="margin-top:30px;">
                                    — The Fitnest Team
                                  </p>
                                </td>
                              </tr>
                    
                              <!-- Footer -->
                              <tr>
                                <td style="background:#f1f1f1; padding:15px; text-align:center; font-size:12px; color:#888;">
                                  © 2026 Fitnest. All rights reserved.
                                </td>
                              </tr>
                    
                            </table>
                    
                          </td>
                        </tr>
                      </table>
                    
                    </body>
                    </html>
                    """;

            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "UTF-8");
            helper.setTo(email);
            helper.setFrom(fromAddress);
            helper.setSubject("Your Fitnest account has been reactivated");
            helper.setText(html, true);

            mailSender.send(mimeMessage);
        } catch (MessagingException ex) {
            log.error("Failed to send account recovery email to {}", email, ex);
        }
    }
}


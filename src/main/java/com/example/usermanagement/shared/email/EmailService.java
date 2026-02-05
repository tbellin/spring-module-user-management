package com.example.usermanagement.shared.email;

import com.example.usermanagement.shared.config.AppProperties;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

/**
 * Service for sending emails using Spring Mail and Thymeleaf templates.
 * <p>
 * This service is in the shared module as it will be used by multiple
 * features (email verification, password reset).
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final String fromAddress;

    public EmailService(JavaMailSender mailSender,
                        TemplateEngine templateEngine,
                        AppProperties appProperties) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
        this.fromAddress = appProperties.mail().from();
    }

    /**
     * Sends a verification email with HTML and plain text content.
     *
     * @param to               recipient email address
     * @param verificationLink the full verification URL
     * @throws EmailSendException if email sending fails
     */
    public void sendVerificationEmail(String to, String verificationLink) {
        Context ctx = new Context();
        ctx.setVariable("verificationLink", verificationLink);

        String htmlContent = templateEngine.process("email/verification", ctx);
        String textContent = templateEngine.process("email/verification.txt", ctx);

        sendMultipartEmail(to, "Verify your email", textContent, htmlContent);
        log.info("Verification email sent to {}", to);
    }

    /**
     * Sends a multipart email with both plain text and HTML content.
     *
     * @param to      recipient email address
     * @param subject email subject
     * @param text    plain text content
     * @param html    HTML content
     * @throws EmailSendException if email sending fails
     */
    private void sendMultipartEmail(String to, String subject, String text, String html) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromAddress);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(text, html); // text first, html second for multipart/alternative

            mailSender.send(message);
        } catch (MessagingException e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
            throw new EmailSendException("Failed to send email", e);
        }
    }
}

package com.eventra.notification;

import com.eventra.config.AppProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Map;

/** Production provider over SMTP (EMAIL_HOST / EMAIL_PORT / EMAIL_USERNAME / EMAIL_PASSWORD). */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "eventra.email.provider", havingValue = "smtp")
public class SmtpEmailService implements EmailService {
    private final JavaMailSender mailSender;
    private final AppProperties props;

    @Async
    @Override
    public void send(String to, String subject, String template, Map<String, Object> model) {
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(props.email().from());
            msg.setTo(to);
            msg.setSubject(subject);
            msg.setText(EmailTemplates.render(template, model));
            mailSender.send(msg);
            log.info("email_sent template={} to={}", template, mask(to));
        } catch (Exception e) {
            log.error("email_failed template={} to={} error={}", template, mask(to), e.getMessage());
        }
    }

    private static String mask(String email) {
        int at = email.indexOf('@');
        return at <= 1 ? "***" : email.charAt(0) + "***" + email.substring(at);
    }
}

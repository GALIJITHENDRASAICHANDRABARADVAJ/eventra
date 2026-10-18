package com.eventra.notification;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Map;

/** Development provider: prints emails to the log instead of sending them. */
@Slf4j
@Service
@ConditionalOnProperty(name = "eventra.email.provider", havingValue = "console", matchIfMissing = true)
public class ConsoleEmailService implements EmailService {
    @Async
    @Override
    public void send(String to, String subject, String template, Map<String, Object> model) {
        log.info("\n====== EMAIL (console provider) ======\nTo: {}\nSubject: {}\n\n{}\n======================================",
                to, subject, EmailTemplates.render(template, model));
    }
}

package com.eventra.notification;

import java.util.Map;

/** Provider abstraction for transactional email. */
public interface EmailService {
    void send(String to, String subject, String template, Map<String, Object> model);
}

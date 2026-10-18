package com.eventra.notification;

import java.util.Map;

/** Plain-text templates. Kept deliberately simple; swap for Thymeleaf/MJML when branding matters. */
public final class EmailTemplates {
    private EmailTemplates() {}

    public static String render(String template, Map<String, Object> m) {
        return switch (template) {
            case "password-reset" -> """
                    Hi %s,

                    We received a request to reset your Eventra password. This link is valid for 30 minutes:
                    %s

                    If you did not request this, you can ignore this email.
                    """.formatted(m.get("name"), m.get("link"));
            case "booking-confirmed" -> """
                    Hi %s,

                    Your booking %s is confirmed.
                    %s
                    %s
                    %s
                    Seats / tickets: %s
                    Total paid: ₹%s

                    Show the QR code in the Eventra app at the entrance: %s
                    """.formatted(m.get("name"), m.get("reference"), m.get("title"), m.get("venue"), m.get("when"),
                    m.get("seats"), m.get("total"), m.get("link"));
            case "payment-confirmed" -> """
                    Hi %s,

                    We received your payment of ₹%s for booking %s (%s).
                    """.formatted(m.get("name"), m.get("amount"), m.get("reference"), m.get("method"));
            case "booking-cancelled" -> """
                    Hi %s,

                    Booking %s has been cancelled. %s
                    """.formatted(m.get("name"), m.get("reference"), m.getOrDefault("refundNote", ""));
            case "refund-update" -> """
                    Hi %s,

                    Your refund of ₹%s for booking %s is now %s.
                    Refunds usually reach your account within 5-7 working days, depending on your bank.
                    """.formatted(m.get("name"), m.get("amount"), m.get("reference"), m.get("status"));
            default -> m.toString();
        };
    }
}

package com.eventra.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;

/** Strongly typed view over the {@code eventra.*} configuration tree. */
@ConfigurationProperties(prefix = "eventra")
public record AppProperties(
        @DefaultValue("Eventra") String appName,
        @DefaultValue("http://localhost:3000") String frontendUrl,
        @DefaultValue("Asia/Kolkata") String timezone,
        Cors cors,
        Jwt jwt,
        Cookies cookies,
        SeatLock seatLock,
        EventHold eventHold,
        PayAtVenue payAtVenue,
        Pricing pricing,
        Cancellation cancellation,
        RateLimit rateLimit,
        Payments payments,
        Email email,
        Qr qr,
        Seed seed
) {
    public record Cors(List<String> allowedOrigins) {}

    public record Jwt(String secret, Duration accessExpiration, Duration refreshExpiration, @DefaultValue("eventra") String issuer) {}

    public record Cookies(@DefaultValue("true") boolean secure, @DefaultValue("Lax") String sameSite) {}

    public record SeatLock(@DefaultValue("PT5M") Duration duration, @DefaultValue("10") int maxSeats, @DefaultValue("5000") long cleanupIntervalMs) {}

    public record EventHold(@DefaultValue("PT10M") Duration duration) {}

    public record PayAtVenue(@DefaultValue("PT2H") Duration holdDuration, @DefaultValue("PT30M") Duration cutoffBeforeShow) {}

    public record Pricing(BigDecimal convenienceFeePerTicket, BigDecimal convenienceFeeMax, BigDecimal taxRateOnFee, @DefaultValue("INR") String currency) {}

    public record Cancellation(@DefaultValue("120") int cutoffMinutesBeforeShow, @DefaultValue("100") int refundPercent, @DefaultValue("false") boolean refundConvenienceFee) {}

    public record RateLimit(@DefaultValue("true") boolean enabled, @DefaultValue("20") int authPerMinute, @DefaultValue("300") int generalPerMinute) {}

    public record Payments(boolean mockEnabled, Stripe stripe, Razorpay razorpay, PhonePe phonepe) {
        public record Stripe(String secretKey, String publishableKey, String webhookSecret, String apiBase) {
            public boolean configured() { return notBlank(secretKey); }
        }
        public record Razorpay(String keyId, String keySecret, String webhookSecret, String apiBase) {
            public boolean configured() { return notBlank(keyId) && notBlank(keySecret); }
        }
        public record PhonePe(String clientId, String clientSecret, String clientVersion, String environment,
                              String webhookUsername, String webhookPassword) {
            public boolean configured() { return notBlank(clientId) && notBlank(clientSecret); }
        }
    }

    public record Email(@DefaultValue("console") String provider, String from) {}

    public record Qr(String secret) {}

    public record Seed(boolean demoData, String demoPassword, @DefaultValue("6") int showDaysAhead) {}

    static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }
}

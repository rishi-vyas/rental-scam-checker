package com.rishivyas.rentalscam.engine;

import com.rishivyas.rentalscam.domain.ContactType;

import java.util.Locale;

/**
 * Canonical forms for emails and phone numbers so that reports and lookups match
 * regardless of formatting ("087 123 4567" == "+353871234567").
 */
public final class ContactNormalizer {

    private ContactNormalizer() {
    }

    public static String normalize(ContactType type, String raw) {
        return switch (type) {
            case EMAIL -> email(raw);
            case PHONE -> phone(raw);
        };
    }

    /** Lower-cases and, for Gmail, drops dots and "+tag" in the local part. */
    public static String email(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String email = raw.trim().toLowerCase(Locale.ROOT);
        int at = email.lastIndexOf('@');
        if (at <= 0) return email;
        String local = email.substring(0, at);
        String domain = email.substring(at + 1);
        if (domain.equals("gmail.com") || domain.equals("googlemail.com")) {
            int plus = local.indexOf('+');
            if (plus >= 0) local = local.substring(0, plus);
            local = local.replace(".", "");
            domain = "gmail.com";
        }
        return local + "@" + domain;
    }

    /**
     * E.164-style: keeps a leading '+', strips formatting, converts "00" prefixes and
     * Irish national numbers ("08x...") to +353.
     */
    public static String phone(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String trimmed = raw.trim();
        boolean plus = trimmed.startsWith("+");
        String digits = trimmed.replaceAll("\\D", "");
        if (digits.isEmpty()) return null;
        if (plus) return "+" + digits;
        if (digits.startsWith("00")) return "+" + digits.substring(2);
        if (digits.startsWith("0")) return "+353" + digits.substring(1);
        return "+" + digits;
    }

    public static boolean isIrishNumber(String normalizedPhone) {
        return normalizedPhone != null && normalizedPhone.startsWith("+353");
    }
}

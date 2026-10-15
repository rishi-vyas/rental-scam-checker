package com.rishivyas.rentalscam.engine;

import com.rishivyas.rentalscam.api.dto.ListingRequest;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;

/**
 * The request plus pre-computed, normalized views of it that several rules share.
 */
public final class ListingContext {

    private final ListingRequest request;
    private final String text;
    private final String normalizedEmail;
    private final String normalizedPhone;
    private final String fingerprint;

    public ListingContext(ListingRequest request) {
        this.request = request;
        String title = request.title() == null ? "" : request.title();
        // Collapse line breaks and runs of spaces so phrases split across lines still match.
        this.text = (title + " \n " + request.description()).toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", " ");
        this.normalizedEmail = ContactNormalizer.email(request.contactEmail());
        this.normalizedPhone = ContactNormalizer.phone(request.contactPhone());
        this.fingerprint = fingerprint(request.description());
    }

    public ListingRequest request() { return request; }

    /** Title + description, lower-cased, whitespace collapsed to single spaces. */
    public String text() { return text; }

    public String normalizedEmail() { return normalizedEmail; }

    public String normalizedPhone() { return normalizedPhone; }

    /** SHA-256 of the description with case, digits, punctuation and whitespace removed. */
    public String fingerprint() { return fingerprint; }

    static String fingerprint(String description) {
        String canonical = description.toLowerCase(Locale.ROOT)
                .replaceAll("[^\\p{L}]+", "");
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(canonical.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}

package com.rishivyas.rentalscam.rules;

import com.rishivyas.rentalscam.domain.Severity;
import com.rishivyas.rentalscam.domain.Signal;
import com.rishivyas.rentalscam.domain.SignalCategory;
import com.rishivyas.rentalscam.engine.ListingContext;
import com.rishivyas.rentalscam.engine.ScamRule;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Look-alike domains for rental platforms (e.g. "daft-ie-secure.com"), a favourite way to
 * collect "deposits" through a fake booking page.
 */
@Component
public class SuspiciousLinkRule implements ScamRule {

    private static final Pattern URL = Pattern.compile(
            "(?:https?://)?(?:[a-z0-9-]+\\.)+[a-z]{2,}(?:/[^\\s]*)?", Pattern.CASE_INSENSITIVE);

    /** Brand keyword -> the registrable domains it legitimately lives on. */
    private static final Map<String, Set<String>> BRANDS = Map.of(
            "daft", Set.of("daft.ie"),
            "myhome", Set.of("myhome.ie"),
            "rent.ie", Set.of("rent.ie"),
            "airbnb", Set.of("airbnb.com", "airbnb.ie", "airbnb.co.uk"),
            "booking", Set.of("booking.com"),
            "rtb", Set.of("rtb.ie"));

    @Override
    public List<Signal> evaluate(ListingContext listing) {
        Set<String> hosts = new LinkedHashSet<>();
        String listingUrl = listing.request().listingUrl();
        if (listingUrl != null) {
            String h = host(listingUrl);
            if (h != null) hosts.add(h);
        }
        Matcher m = URL.matcher(listing.text());
        while (m.find()) {
            String h = host(m.group());
            if (h != null) hosts.add(h);
        }

        List<String> lookalikes = new ArrayList<>();
        for (String host : hosts) {
            for (Map.Entry<String, Set<String>> brand : BRANDS.entrySet()) {
                if (mentionsBrand(host, brand.getKey()) && !isOnDomain(host, brand.getValue())) {
                    lookalikes.add(host);
                    break;
                }
            }
        }
        if (lookalikes.isEmpty()) return List.of();

        return List.of(new Signal("LINK_LOOKALIKE_DOMAIN", SignalCategory.LINK, Severity.CRITICAL, 55,
                "Link to a look-alike rental platform site",
                "Suspicious domain(s): " + String.join(", ", lookalikes),
                "Do not enter payment or login details on that site; type the platform's real address yourself."));
    }

    /**
     * Token-based so "rtb" doesn't fire on "artbooks.com": the brand must be a whole
     * dot/dash-separated label, or (for longer brand names) the start of one ("daftsecure").
     */
    static boolean mentionsBrand(String host, String brand) {
        String dotted = "." + host.replace('-', '.') + ".";
        if (dotted.contains("." + brand + ".")) return true;
        if (brand.length() < 4 || brand.contains(".")) return false;
        for (String label : host.split("[.-]")) {
            if (label.startsWith(brand)) return true;
        }
        return false;
    }

    private static boolean isOnDomain(String host, Set<String> allowed) {
        for (String domain : allowed) {
            if (host.equals(domain) || host.endsWith("." + domain)) return true;
        }
        return false;
    }

    static String host(String raw) {
        try {
            String url = raw.toLowerCase(Locale.ROOT).trim();
            if (!url.startsWith("http://") && !url.startsWith("https://")) url = "https://" + url;
            String host = URI.create(url).getHost();
            if (host == null) return null;
            return host.startsWith("www.") ? host.substring(4) : host;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}

package com.rishivyas.rentalscam.engine;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Small helper for "does the text contain any of these phrases" checks that also reports
 * which phrases matched, so every signal carries its evidence.
 */
public final class PhraseMatcher {

    private final List<Pattern> patterns;

    private PhraseMatcher(List<Pattern> patterns) {
        this.patterns = patterns;
    }

    /** Each regex is matched case-insensitively against lower-cased text. */
    public static PhraseMatcher of(String... regexes) {
        List<Pattern> compiled = new ArrayList<>();
        for (String r : regexes) {
            compiled.add(Pattern.compile("\\b(?:" + r + ")\\b", Pattern.CASE_INSENSITIVE));
        }
        return new PhraseMatcher(List.copyOf(compiled));
    }

    /** Distinct matched snippets, in pattern order. */
    public List<String> find(String text) {
        List<String> hits = new ArrayList<>();
        for (Pattern p : patterns) {
            Matcher m = p.matcher(text);
            if (m.find()) {
                String hit = m.group().trim();
                if (!hits.contains(hit)) hits.add(hit);
            }
        }
        return hits;
    }

    public static String quote(List<String> hits) {
        return "Found: \"" + String.join("\", \"", hits) + "\"";
    }
}

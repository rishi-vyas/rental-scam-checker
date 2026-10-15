package com.rishivyas.rentalscam.service;

import com.rishivyas.rentalscam.api.dto.ListingRequest;
import com.rishivyas.rentalscam.domain.ListingAnalysis;
import com.rishivyas.rentalscam.domain.RiskLevel;
import com.rishivyas.rentalscam.domain.Signal;
import com.rishivyas.rentalscam.engine.ListingContext;
import com.rishivyas.rentalscam.engine.RiskScorer;
import com.rishivyas.rentalscam.engine.ScamRule;
import com.rishivyas.rentalscam.repository.ListingAnalysisRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class AnalysisService {

    private static final Logger log = LoggerFactory.getLogger(AnalysisService.class);

    private final List<ScamRule> rules;
    private final ListingAnalysisRepository repository;

    public AnalysisService(List<ScamRule> rules, ListingAnalysisRepository repository) {
        this.rules = List.copyOf(rules);
        this.repository = repository;
        log.info("Loaded {} scam rules: {}", rules.size(),
                rules.stream().map(r -> r.getClass().getSimpleName()).toList());
    }

    @Transactional
    public ListingAnalysis analyze(ListingRequest request) {
        ListingContext context = new ListingContext(request);

        List<Signal> signals = new ArrayList<>();
        for (ScamRule rule : rules) {
            try {
                signals.addAll(rule.evaluate(context));
            } catch (RuntimeException e) {
                // One faulty rule must not take down the whole check.
                log.error("Rule {} failed; skipping it for this listing", rule.getClass().getSimpleName(), e);
            }
        }
        signals.sort(Comparator.comparingInt(Signal::points).reversed());

        int score = RiskScorer.score(signals);
        ListingAnalysis analysis = new ListingAnalysis(
                context.fingerprint(),
                context.normalizedEmail(),
                context.normalizedPhone(),
                request.location(),
                request.monthlyRent(),
                score,
                RiskLevel.fromScore(score),
                signals);
        return repository.save(analysis);
    }

    @Transactional(readOnly = true)
    public Optional<ListingAnalysis> find(UUID id) {
        return repository.findById(id);
    }
}

package com.rishivyas.rentalscam.repository;

import com.rishivyas.rentalscam.domain.ListingAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ListingAnalysisRepository extends JpaRepository<ListingAnalysis, UUID> {

    List<ListingAnalysis> findTop50ByFingerprintOrderByAnalyzedAtDesc(String fingerprint);
}

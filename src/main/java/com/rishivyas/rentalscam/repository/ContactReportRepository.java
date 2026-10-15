package com.rishivyas.rentalscam.repository;

import com.rishivyas.rentalscam.domain.ContactReport;
import com.rishivyas.rentalscam.domain.ContactType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ContactReportRepository extends JpaRepository<ContactReport, Long> {

    long countByContactTypeAndNormalizedValue(ContactType type, String normalizedValue);

    Optional<ContactReport> findFirstByContactTypeAndNormalizedValueOrderByReportedAtDesc(
            ContactType type, String normalizedValue);
}

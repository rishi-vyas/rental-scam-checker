package com.rishivyas.rentalscam.api.dto;

import com.rishivyas.rentalscam.domain.ContactType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ContactReportRequest(
        @NotNull ContactType type,
        @NotBlank @Size(max = 254) String value,
        @Size(max = 500) String reason) {
}

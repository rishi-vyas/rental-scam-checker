package com.rishivyas.rentalscam.api;

import com.rishivyas.rentalscam.api.dto.ContactCheckResponse;
import com.rishivyas.rentalscam.api.dto.ContactReportRequest;
import com.rishivyas.rentalscam.domain.ContactType;
import com.rishivyas.rentalscam.service.ContactReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/contacts")
@Tag(name = "Community contact reports")
public class ContactController {

    private final ContactReportService service;

    public ContactController(ContactReportService service) {
        this.service = service;
    }

    @PostMapping("/reports")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Report an email or phone number used in a rental scam")
    public ContactCheckResponse report(@Valid @RequestBody ContactReportRequest request) {
        return service.report(request);
    }

    @GetMapping("/check")
    @Operation(summary = "Check whether an email or phone number has been reported")
    public ContactCheckResponse check(@RequestParam ContactType type, @RequestParam String value) {
        return service.check(type, value);
    }
}

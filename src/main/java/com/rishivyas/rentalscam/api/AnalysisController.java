package com.rishivyas.rentalscam.api;

import com.rishivyas.rentalscam.api.dto.AnalysisResponse;
import com.rishivyas.rentalscam.api.dto.ListingRequest;
import com.rishivyas.rentalscam.domain.ListingAnalysis;
import com.rishivyas.rentalscam.service.AnalysisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/analyses")
@Tag(name = "Listing analysis")
public class AnalysisController {

    private final AnalysisService service;

    public AnalysisController(AnalysisService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Score a rental listing for scam risk")
    public ResponseEntity<AnalysisResponse> analyze(@Valid @RequestBody ListingRequest request) {
        ListingAnalysis analysis = service.analyze(request);
        return ResponseEntity
                .created(ServletUriComponentsBuilder.fromCurrentRequest()
                        .path("/{id}").buildAndExpand(analysis.getId()).toUri())
                .body(AnalysisResponse.from(analysis));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Fetch a previous analysis")
    public ResponseEntity<AnalysisResponse> get(@PathVariable UUID id) {
        return service.find(id)
                .map(AnalysisResponse::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}

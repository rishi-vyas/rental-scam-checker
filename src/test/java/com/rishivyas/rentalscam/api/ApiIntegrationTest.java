package com.rishivyas.rentalscam.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rishivyas.rentalscam.TestListings;
import com.rishivyas.rentalscam.api.dto.ListingRequest;
import com.rishivyas.rentalscam.domain.PaymentMethod;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ApiIntegrationTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper json;

    private JsonNode analyze(ListingRequest request) throws Exception {
        String body = mvc.perform(post("/api/v1/analyses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andReturn().getResponse().getContentAsString();
        return json.readTree(body);
    }

    @Test
    void classicScamIsCritical() throws Exception {
        JsonNode result = analyze(TestListings.scam());
        assertThat(result.get("riskLevel").asText()).isEqualTo("CRITICAL");
        assertThat(result.get("score").asInt()).isGreaterThanOrEqualTo(90);
        assertThat(result.get("signals").findValuesAsText("code"))
                .contains("PAYMENT_UNTRACEABLE_METHOD", "VIEWING_ABSENT_LANDLORD", "LINK_LOOKALIKE_DOMAIN");
        // Highest-impact signal first.
        assertThat(result.get("signals").get(0).get("points").asInt()).isGreaterThanOrEqualTo(55);
    }

    @Test
    void genuineListingIsLowRisk() throws Exception {
        JsonNode result = analyze(TestListings.genuine());
        assertThat(result.get("riskLevel").asText()).isEqualTo("LOW");
        assertThat(result.get("recommendations").size()).isGreaterThan(0);
    }

    @Test
    void savedAnalysisCanBeFetchedById() throws Exception {
        String id = analyze(TestListings.scam()).get("id").asText();
        mvc.perform(get("/api/v1/analyses/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.riskLevel").value("CRITICAL"));
    }

    @Test
    void unknownAnalysisIs404() throws Exception {
        mvc.perform(get("/api/v1/analyses/{id}", "00000000-0000-0000-0000-000000000000"))
                .andExpect(status().isNotFound());
    }

    @Test
    void missingDescriptionIsRejected() throws Exception {
        mvc.perform(post("/api/v1/analyses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"monthlyRent\": 1500}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.description").exists());
    }

    @Test
    void unknownPaymentMethodIsRejected() throws Exception {
        mvc.perform(post("/api/v1/analyses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\": \"x\", \"requestedPaymentMethods\": [\"CHEQUE\"]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void communityReportsRaiseRiskOnLaterChecks() throws Exception {
        String phone = "+353 86 000 1234";
        ListingRequest listing = new ListingRequest(null,
                "Double room in a shared house in Drumcondra, close to DCU and bus routes. Bills included, "
                        + "available from next month. Message for details and a viewing time.",
                new BigDecimal("950"), "Drumcondra, Dublin 9", 1, new BigDecimal("950"),
                null, phone, null, List.of(PaymentMethod.BANK_TRANSFER), true, false);

        JsonNode before = analyze(listing);
        assertThat(before.get("signals").findValuesAsText("code")).doesNotContain("COMMUNITY_REPORTED_PHONE");

        for (int i = 0; i < 3; i++) {
            mvc.perform(post("/api/v1/contacts/reports")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"type\":\"PHONE\",\"value\":\"086 000 1234\",\"reason\":\"took deposit, no flat\"}"))
                    .andExpect(status().isCreated());
        }

        mvc.perform(get("/api/v1/contacts/check").param("type", "PHONE").param("value", phone))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reportCount").value(3))
                .andExpect(jsonPath("$.flagged").value(true));

        JsonNode after = analyze(listing);
        assertThat(after.get("signals").findValuesAsText("code")).contains("COMMUNITY_REPORTED_PHONE");
        assertThat(after.get("riskLevel").asText()).isEqualTo("CRITICAL");
    }

    @Test
    void clonedListingIsDetected() throws Exception {
        // Own description so this test can't leak a duplicate into other tests sharing the database.
        ListingRequest base = TestListings.genuine();
        ListingRequest original = new ListingRequest(base.title(), base.description() + " Pets considered.",
                base.monthlyRent(), base.location(), base.bedrooms(), base.depositAmount(), base.contactEmail(),
                base.contactPhone(), base.listingUrl(), base.requestedPaymentMethods(), base.viewingOffered(),
                base.paymentRequestedBeforeViewing());
        analyze(original);

        ListingRequest clone = new ListingRequest(original.title(), original.description(),
                original.monthlyRent(), original.location(), original.bedrooms(), original.depositAmount(),
                "rathmines.landlord.ie@gmail.com", null, null, original.requestedPaymentMethods(),
                original.viewingOffered(), original.paymentRequestedBeforeViewing());

        assertThat(analyze(clone).get("signals").findValuesAsText("code")).contains("DUPLICATE_CLONED_LISTING");
    }

    @Test
    void invalidContactReportIsRejected() throws Exception {
        mvc.perform(post("/api/v1/contacts/reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"EMAIL\",\"value\":\"not-an-email\"}"))
                .andExpect(status().isBadRequest());
    }
}

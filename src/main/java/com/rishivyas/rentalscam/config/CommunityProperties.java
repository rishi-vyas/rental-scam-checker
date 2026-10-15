package com.rishivyas.rentalscam.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "scam-checker.community")
public record CommunityProperties(Integer criticalReportThreshold) {

    public CommunityProperties {
        if (criticalReportThreshold == null || criticalReportThreshold < 1) criticalReportThreshold = 3;
    }
}

package com.rishivyas.rentalscam;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class RentalScamCheckerApplication {

    public static void main(String[] args) {
        SpringApplication.run(RentalScamCheckerApplication.class, args);
    }
}

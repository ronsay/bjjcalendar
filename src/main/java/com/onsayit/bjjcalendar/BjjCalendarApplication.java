package com.onsayit.bjjcalendar;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class BjjCalendarApplication {

    public static void main(final String[] args) {
        SpringApplication.run(BjjCalendarApplication.class, args);
    }
}

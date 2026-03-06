package com.onsayit.bjjcalendar.domain.model;

import lombok.Getter;

@Getter
public enum Federation {
    IBJJF("IBJJF"),
    AJP("AJP"),
    CFJJB("CFJJB"),
    GRAPPLING_INDUSTRIES("Grappling Industries"),
    NAGA("NAGA"),
    OTHER("Divers");

    private final String label;

    Federation(final String label) {
        this.label = label;
    }
}

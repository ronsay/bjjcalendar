package com.onsayit.bjjcalendar.infrastructure.read.cfjjb;

import java.time.YearMonth;

public record CfjjbEvent(
    String name,
    String city,
    YearMonth yearMonth,
    String startDay,
    String endDay,
    String url
) { }

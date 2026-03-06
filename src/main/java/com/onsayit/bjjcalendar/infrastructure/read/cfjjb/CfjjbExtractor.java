package com.onsayit.bjjcalendar.infrastructure.read.cfjjb;

import com.onsayit.bjjcalendar.infrastructure.read.utils.ReaderUtils;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

@Component
public final class CfjjbExtractor {
    private static final String CSS_PAGE = "div.page1";
    private static final String CSS_MONTH_HEADERS = "> div.text-blue-900";
    private static final String CSS_EVENT_ITEMS = "ul > li";
    private static final String CSS_COMPETITION_TITLE = "p[id^=compet_]";
    private static final String CSS_DATE = "div.flex.items-center.text-sm.text-black.w-64 > div";
    private static final String CSS_CITY = "svg[viewBox='0 0 20 20'] + p";
    private static final String CSS_LINK = "a.btn[href]";
    private static final String CSS_EVENT_BLOCK = "div.bg-white";

    // "Octobre 2025", "Fevrier 2026" (with or without accents)
    private static final Pattern MONTH_HEADER = Pattern.compile(
            "^\\s*([\\p{L}]+)\\s+(\\d{4})\\s*$", Pattern.CASE_INSENSITIVE);

    // "Le 7 février 2026"
    private static final Pattern DATE_SINGLE = Pattern.compile(
            "^\\s*Le\\s+(\\d{1,2})\\s+([\\p{L}]+)\\s*(\\d{4})?\\s*$",
            Pattern.CASE_INSENSITIVE);

    // "Du 29 novembre au 30 novembre" / "Du 31 janvier au 1 février"
    private static final Pattern DATE_RANGE = Pattern.compile(
            "^\\s*Du\\s+(\\d{1,2})\\s+([\\p{L}]+)\\s+au\\s+(\\d{1,2})(?:\\s+([\\p{L}]+))?\\s*$",
            Pattern.CASE_INSENSITIVE
    );

    public List<CfjjbEvent> extract(final Document doc) {
        final var results = new ArrayList<CfjjbEvent>();

        final var page = doc.selectFirst(CSS_PAGE);

        if (page != null) {
            final var monthHeaders = page.select(CSS_MONTH_HEADERS);

            monthHeaders.forEach(mh -> this.extractMonth(mh, results));
        }

        return results;
    }

    private void extractMonth(final Element monthHeader, final List<CfjjbEvent> results) {
        final var yearMonth = this.parseMonthHeader(monthHeader.text());
        if (yearMonth == null) {
            return;
        }

        // The next block contains the <ul> of the month's events
        final var listContainer = this.nextElementSiblingMatching(monthHeader);
        if (listContainer == null) {
            return;
        }

        final var lis = listContainer.select(CSS_EVENT_ITEMS);
        lis.stream().filter(li -> !li.select(CSS_COMPETITION_TITLE).isEmpty())
                .forEach(li -> this.extractEvent(li, yearMonth, results));
    }

    private void extractEvent(final Element eventEl, final YearMonth yearMonth, final List<CfjjbEvent> results) {
        final var titleEl = eventEl.selectFirst(CSS_COMPETITION_TITLE);
        if (titleEl == null) {
            return;
        }

        final var dateText = Optional.ofNullable(eventEl.selectFirst(CSS_DATE))
                .map(Element::text)
                .orElse("")
                .trim();

        final var range = this.parseDate(dateText);
        if (range.isEmpty()) {
            return;
        }

        final var city = Optional.ofNullable(eventEl.selectFirst(CSS_CITY))
                .map(Element::text)
                .map(String::trim)
                .orElse(null);

        final var url = Optional.ofNullable(eventEl.selectFirst(CSS_LINK))
                .map(a -> a.attr("href"))
                .orElse(null);

        results.add(new CfjjbEvent(
                titleEl.text(),
                city,
                yearMonth,
                range.get(0),
                range.size() > 1 ? range.get(1) : range.get(0),
                url
        ));
    }

    private YearMonth parseMonthHeader(final String monthRaw) {
        final var matcher = MONTH_HEADER.matcher(monthRaw == null ? "" : monthRaw.trim());
        return matcher.matches()
                ? YearMonth.of(Integer.parseInt(matcher.group(2)), ReaderUtils.getMonth(matcher.group(1))) : null;
    }

    private List<String> parseDate(final String dateText) {
        if (dateText != null && !dateText.isBlank()) {
            final var single = DATE_SINGLE.matcher(dateText);
            if (single.matches()) {
                return List.of(single.group(1));
            }

            final var range = DATE_RANGE.matcher(dateText);
            if (range.matches()) {
                return List.of(range.group(1), range.group(3));
            }

            // Fallback: sometimes there is additional text (e.g. "* ..."). Try a quick cleanup.
            final var cleaned = dateText.replaceAll("\\*.*$", "").trim();
            if (!cleaned.equals(dateText)) {
                return this.parseDate(cleaned);
            }
        }

        return List.of();
    }

    private Element nextElementSiblingMatching(final Element start) {
        var cur = start.nextElementSibling();
        while (cur != null) {
            if (!cur.select(CSS_EVENT_BLOCK).isEmpty() || cur.is(CSS_EVENT_BLOCK)) {
                return cur;
            }
            cur = cur.nextElementSibling();
        }
        return null;
    }
}

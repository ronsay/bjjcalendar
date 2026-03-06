package com.onsayit.bjjcalendar.infrastructure.read.ajp;

import com.onsayit.bjjcalendar.infrastructure.read.utils.ReaderUtils;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.TextNode;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Component
public class AjpCalendarExtractor {
    private static final Pattern DATE_LOC_PATTERN = Pattern.compile(
            "(?i)\\b([A-Z]{3,})\\s+(\\d{1,2})(?:\\s*-\\s*(\\d{1,2}))?(?:\\s*@\\s*(.*))?$");

    private static final Pattern TRAILING_FLAG_PATTERN =
            Pattern.compile("\\s*([\\uD83C\\uDDE6-\\uD83C\\uDDFF]{2})\\s*$");

    private static final String EVENT_INFO_SELECTOR = "> p";
    private static final String EVENT_LINK_SELECTOR = "a[href]";
    private static final String HREF = "href";

    public List<AjpEvent> extractFromContainer(final Element container, final int year) {
        final var elements = container.select(EVENT_INFO_SELECTOR);
        final List<AjpEventBuilder> builders = new ArrayList<>();

        for (int i = 0; i < elements.size(); i++) {
            final var element = elements.get(i);
            final var link = element.selectFirst(EVENT_LINK_SELECTOR);

            if (link == null) {
                if (!builders.isEmpty() && hasNoLocation(builders.getLast())) {
                    appendLocation(builders.getLast(), ReaderUtils.normalizeWhitespace(element.text()));
                }
                continue;
            }

            final var builder = this.buildEvent(link, element, year);
            this.fillFromNextElement(builder, elements, i);
            builders.add(builder);
        }
        return builders.stream().map(AjpEventBuilder::build).toList();
    }

    private AjpEventBuilder buildEvent(final Element link, final Element parent, final int year) {
        final var builder = new AjpEventBuilder();
        final var absUrl = link.absUrl(HREF);

        builder.url = absUrl.isBlank() ? link.attr(HREF) : absUrl;
        builder.name = ReaderUtils.normalizeWhitespace(link.text());
        builder.year = year;

        final var suffix = this.extractSuffixAfterAnchor(parent, link);
        if (!suffix.isBlank()) {
            this.fillDatesAndLocation(builder, suffix);
        }
        return builder;
    }

    private void fillFromNextElement(final AjpEventBuilder builder, final List<Element> elements, final int index) {
        if (!this.hasNoLocation(builder)) {
            return;
        }

        final var nextText = peekNextText(elements, index);

        if (nextText == null) {
            return;
        }

        if (!this.hasDates(builder)) {
            fillDatesAndLocation(builder, nextText);
        }

        appendLocation(builder, nextText);
    }

    private String peekNextText(final List<Element> elements, final int index) {
        if (index + 1 >= elements.size()) {
            return null;
        }

        final var next = elements.get(index + 1);
        if (next.selectFirst(EVENT_LINK_SELECTOR) != null) {
            return null;
        }

        final var text = ReaderUtils.normalizeWhitespace(next.text());
        return text.isBlank() ? null : text;
    }

    private String extractSuffixAfterAnchor(final Element element, final Element link) {
        final var sb = new StringBuilder();
        boolean seenA = false;
        for (var node : element.childNodes()) {
            if (!seenA) {
                seenA = node.equals(link);
                continue;
            }

            sb.append(node instanceof TextNode tn ? tn.text() : " " + ((Element) node).text());
        }
        return ReaderUtils.normalizeWhitespace(sb.toString());
    }

    private void fillDatesAndLocation(final AjpEventBuilder builder, final String tail) {
        if (tail == null || tail.isBlank()) {
            return;
        }

        final var matcher = DATE_LOC_PATTERN.matcher(tail);

        if (matcher.find()) {
            builder.month = matcher.group(1);
            builder.startDay = matcher.group(2);
            builder.endDay = matcher.group(3);

            if (matcher.group(4) != null && !matcher.group(4).isBlank()) {
                appendLocation(builder, matcher.group(4));
            }

        } else {
            appendLocation(builder, tail);
        }
    }

    private void appendLocation(final AjpEventBuilder builder, final String rawLocation) {
        if (rawLocation == null || rawLocation.isBlank()) {
            return;
        }

        if (builder.countryCode == null) {
            builder.countryCode = extractCountryCode(rawLocation);
        }

        final var location = removeTrailingFlag(rawLocation).trim();
        final var parts = location.split("\\s*,\\s*");

        if (builder.venue == null) {
            builder.venue = parts[0];
        }

        if (parts.length >= 2 && builder.city == null) {
            builder.city = parts[1];
        }

        if (parts.length >= 3 && builder.country == null) {
            builder.country = parts[parts.length - 1];
        }
    }

    private String extractCountryCode(final String text) {
        final int[] indicators = text.codePoints()
                .filter(cp -> cp >= 0x1F1E6 && cp <= 0x1F1FF)
                .toArray();

        if (indicators.length != 2) {
            return null;
        }

        return String.valueOf((char) ('A' + (indicators[0] - 0x1F1E6))) + (char) ('A' + (indicators[1] - 0x1F1E6));
    }

    private String removeTrailingFlag(final String information) {
        return TRAILING_FLAG_PATTERN.matcher(information).replaceFirst("");
    }

    private boolean hasDates(final AjpEventBuilder builder) {
        return builder.startDay != null && builder.endDay != null;
    }

    private boolean hasNoLocation(final AjpEventBuilder builder) {
        return builder.venue == null && builder.city == null;
    }

    private static final class AjpEventBuilder {
        private String name;
        private String venue;
        private String city;
        private String country;
        private String countryCode;
        private String month;
        private int year;
        private String startDay;
        private String endDay;
        private String url;

        AjpEvent build() {
            return new AjpEvent(name, venue, city, country, countryCode, month, year, startDay, endDay, url);
        }
    }
}

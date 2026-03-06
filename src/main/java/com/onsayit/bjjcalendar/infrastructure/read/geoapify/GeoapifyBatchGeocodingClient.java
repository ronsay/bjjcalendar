package com.onsayit.bjjcalendar.infrastructure.read.geoapify;

import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.onsayit.bjjcalendar.infrastructure.config.properties.GeoapifyProperties;
import com.onsayit.bjjcalendar.infrastructure.geoapify.ApiException;
import com.onsayit.bjjcalendar.infrastructure.geoapify.api.BatchGeocodingApi;
import com.onsayit.bjjcalendar.infrastructure.geoapify.model.BatchForwardGeocodingResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class GeoapifyBatchGeocodingClient {

    private final BatchGeocodingApi batchGeocodingApi;
    private final GeoapifyProperties props;

    public Map<String, GeocodingResult> batchGeocode(final List<String> addresses) {
        if (props.apiKey() == null || props.apiKey().isBlank()) {
            log.warn("Geoapify: API key is empty, skipping geocoding");
            return Map.of();
        }

        if (addresses.isEmpty()) {
            return Map.of();
        }

        try {
            final var response = this.batchGeocodingApi.createForwardBatchGeocodingJobWithHttpInfo(
                    props.apiKey(), addresses, null, null, null, null);

            if (response.getStatusCode() == HttpStatus.OK.value()) {
                final var jobId = response.getData().getId();
                return fetchAndParseResults(jobId, addresses);
            }
            if (response.getStatusCode() == HttpStatus.ACCEPTED.value()) {
                final var jobId = response.getData().getId();
                return pollForResults(jobId, addresses);
            }

            log.error("Geoapify: unexpected HTTP status {}", response.getStatusCode());
            return Map.of();

        } catch (final ApiException e) {
            if (e.getCause() instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            log.error("Geoapify: batch geocoding failed", e);
            return Map.of();
        }
    }

    @SuppressWarnings("BusyWait")
    private Map<String, GeocodingResult> pollForResults(final String jobId,
                                                        final List<String> addresses) throws ApiException {

        for (int attempt = 0; attempt < props.maxPollingAttempts(); attempt++) {
            try {
                Thread.sleep(props.pollingIntervalMs());
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new ApiException(e);
            }

            try {
                final var response = batchGeocodingApi.getBatchGeocodingResultsWithHttpInfo(
                        props.apiKey(), jobId, null);

                if (response.getStatusCode() == HttpStatus.OK.value()) {
                    return parseResults(response.getData(), addresses);
                }

            } catch (final ApiException e) {
                // The API returns a JSON object (job status) for 202 responses,
                // but the generated client expects a JSON array — causing a
                // MismatchedInputException. This simply means the job is still
                // processing, so we continue polling.
                if (e.getCause() instanceof MismatchedInputException) {
                    log.debug("Geoapify: job {} still processing (attempt {}/{})",
                            jobId, attempt + 1, props.maxPollingAttempts());
                    continue;
                }
                throw e;
            }
        }

        log.error("Geoapify: polling timed out after {} attempts", props.maxPollingAttempts());
        return Map.of();
    }

    private Map<String, GeocodingResult> fetchAndParseResults(final String jobId,
                                                              final List<String> addresses) throws ApiException {
        final var results = batchGeocodingApi.getBatchGeocodingResults(props.apiKey(), jobId, null);
        return parseResults(results, addresses);
    }

    private Map<String, GeocodingResult> parseResults(final List<BatchForwardGeocodingResult> items,
                                                      final List<String> addresses) {
        final var results = new HashMap<String, GeocodingResult>();

        for (int i = 0; i < items.size() && i < addresses.size(); i++) {
            final var item = items.get(i);
            final double lat = item.getLat() != null ? item.getLat() : 0;
            final double lon = item.getLon() != null ? item.getLon() : 0;

            if (lat != 0 || lon != 0) {
                results.put(addresses.get(i), new GeocodingResult(lat, lon));
            } else {
                log.warn("Geoapify: no coordinates for '{}'", addresses.get(i));
            }
        }

        log.info("Geoapify: geocoded {}/{} addresses", results.size(), addresses.size());
        return results;
    }
}

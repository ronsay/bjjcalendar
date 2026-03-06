package com.onsayit.bjjcalendar.infrastructure.read.geoapify;

import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.onsayit.bjjcalendar.infrastructure.config.properties.GeoapifyProperties;
import com.onsayit.bjjcalendar.infrastructure.geoapify.ApiException;
import com.onsayit.bjjcalendar.infrastructure.geoapify.ApiResponse;
import com.onsayit.bjjcalendar.infrastructure.geoapify.api.BatchGeocodingApi;
import com.onsayit.bjjcalendar.infrastructure.geoapify.model.BatchForwardGeocodingResult;
import com.onsayit.bjjcalendar.infrastructure.geoapify.model.BatchJobResponse;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GeoapifyBatchGeocodingClientTest {

    @Mock
    private BatchGeocodingApi batchGeocodingApi;

    @Mock
    private GeoapifyProperties props;

    @InjectMocks
    private GeoapifyBatchGeocodingClient client;

    @Nested
    class WhenApiKeyMissing {

        @ParameterizedTest
        @NullSource
        @ValueSource(strings = {"  "})
        void should_return_empty_when_api_key_is_missing(final String apiKey) throws ApiException {
            // given
            when(props.apiKey()).thenReturn(apiKey);

            // when
            final var result = client.batchGeocode(List.of("Paris, France"));

            // then
            assertThat(result).isEmpty();
            verify(batchGeocodingApi, never()).createForwardBatchGeocodingJobWithHttpInfo(
                    any(), any(), any(), any(), any(), any());
        }
    }

    @Nested
    class WhenEmptyAddresses {

        @Test
        void should_return_empty_for_empty_list() {
            // given
            when(props.apiKey()).thenReturn("test-key");

            // when
            final var result = client.batchGeocode(List.of());

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    class WhenJobReturnsImmediateResults {

        @Test
        void should_parse_results_on_200_response() throws ApiException {
            // given
            when(props.apiKey()).thenReturn("test-key");

            final var jobResponse = new BatchJobResponse();
            jobResponse.setId("job-123");
            final var apiResponse = new ApiResponse<>(200, Map.of(), jobResponse);
            when(batchGeocodingApi.createForwardBatchGeocodingJobWithHttpInfo(
                    eq("test-key"), any(), isNull(), isNull(), isNull(), isNull()))
                    .thenReturn(apiResponse);

            final var geocodingResult = new BatchForwardGeocodingResult();
            geocodingResult.setLat(48.8566);
            geocodingResult.setLon(2.3522);
            when(batchGeocodingApi.getBatchGeocodingResults(eq("test-key"), eq("job-123"), isNull()))
                    .thenReturn(List.of(geocodingResult));

            // when
            final var result = client.batchGeocode(List.of("Paris, France"));

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get("Paris, France").latitude()).isEqualTo(48.8566);
            assertThat(result.get("Paris, France").longitude()).isEqualTo(2.3522);
        }
    }

    @Nested
    class WhenPolling {

        @Test
        void should_poll_and_return_results_on_202_then_200() throws ApiException {
            // given
            when(props.apiKey()).thenReturn("test-key");
            when(props.pollingIntervalMs()).thenReturn(1);
            when(props.maxPollingAttempts()).thenReturn(3);

            final var jobResponse = new BatchJobResponse();
            jobResponse.setId("job-456");
            final var acceptedResponse = new ApiResponse<>(202, Map.of(), jobResponse);
            when(batchGeocodingApi.createForwardBatchGeocodingJobWithHttpInfo(
                    eq("test-key"), any(), isNull(), isNull(), isNull(), isNull()))
                    .thenReturn(acceptedResponse);

            final var geocodingResult = new BatchForwardGeocodingResult();
            geocodingResult.setLat(40.7128);
            geocodingResult.setLon(-74.0060);
            final var pollResponse = new ApiResponse<>(200, Map.of(), List.of(geocodingResult));
            when(batchGeocodingApi.getBatchGeocodingResultsWithHttpInfo(
                    eq("test-key"), eq("job-456"), isNull()))
                    .thenReturn(pollResponse);

            // when
            final var result = client.batchGeocode(List.of("New York, USA"));

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get("New York, USA").latitude()).isEqualTo(40.7128);
        }

        @Test
        void should_continue_polling_on_mismatched_input_exception() throws ApiException {
            // given
            when(props.apiKey()).thenReturn("test-key");
            when(props.pollingIntervalMs()).thenReturn(1);
            when(props.maxPollingAttempts()).thenReturn(3);

            final var jobResponse = new BatchJobResponse();
            jobResponse.setId("job-789");
            final var acceptedResponse = new ApiResponse<>(202, Map.of(), jobResponse);
            when(batchGeocodingApi.createForwardBatchGeocodingJobWithHttpInfo(
                    eq("test-key"), any(), isNull(), isNull(), isNull(), isNull()))
                    .thenReturn(acceptedResponse);

            final var mismatchedException = mock(MismatchedInputException.class);
            final var mismatchedApiException = new ApiException(mismatchedException);
            final var geocodingResult = new BatchForwardGeocodingResult();
            geocodingResult.setLat(51.5074);
            geocodingResult.setLon(-0.1278);
            final var pollResponse = new ApiResponse<>(200, Map.of(), List.of(geocodingResult));

            when(batchGeocodingApi.getBatchGeocodingResultsWithHttpInfo(
                    eq("test-key"), eq("job-789"), isNull()))
                    .thenThrow(mismatchedApiException)
                    .thenReturn(pollResponse);

            // when
            final var result = client.batchGeocode(List.of("London, UK"));

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get("London, UK").latitude()).isEqualTo(51.5074);
        }

        @Test
        void should_return_empty_when_polling_times_out() throws ApiException {
            // given
            final var mismatchedException = mock(MismatchedInputException.class);
            final var mismatchedApiException = new ApiException(mismatchedException);

            when(props.apiKey()).thenReturn("test-key");
            when(props.pollingIntervalMs()).thenReturn(1);
            when(props.maxPollingAttempts()).thenReturn(2);

            final var jobResponse = new BatchJobResponse();
            jobResponse.setId("job-timeout");
            final var acceptedResponse = new ApiResponse<>(202, Map.of(), jobResponse);
            when(batchGeocodingApi.createForwardBatchGeocodingJobWithHttpInfo(
                    eq("test-key"), any(), isNull(), isNull(), isNull(), isNull()))
                    .thenReturn(acceptedResponse);

            when(batchGeocodingApi.getBatchGeocodingResultsWithHttpInfo(
                    eq("test-key"), eq("job-timeout"), isNull()))
                    .thenThrow(mismatchedApiException);

            // when
            final var result = client.batchGeocode(List.of("Tokyo, Japan"));

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    class WhenApiError {

        @Test
        void should_return_empty_on_api_exception() throws ApiException {
            // given
            when(props.apiKey()).thenReturn("test-key");
            when(batchGeocodingApi.createForwardBatchGeocodingJobWithHttpInfo(
                    any(), any(), any(), any(), any(), any()))
                    .thenThrow(new ApiException("Server error"));

            // when
            final var result = client.batchGeocode(List.of("Paris, France"));

            // then
            assertThat(result).isEmpty();
        }

        @Test
        void should_restore_interrupt_flag_on_interrupted_exception() throws ApiException {
            // given
            when(props.apiKey()).thenReturn("test-key");
            when(batchGeocodingApi.createForwardBatchGeocodingJobWithHttpInfo(
                    any(), any(), any(), any(), any(), any()))
                    .thenThrow(new ApiException(new InterruptedException("interrupted")));

            // when
            final var result = client.batchGeocode(List.of("Paris, France"));

            // then
            assertThat(result).isEmpty();
            assertThat(Thread.currentThread().isInterrupted()).isTrue();

            // cleanup interrupt flag
            Thread.interrupted();
        }

        @Test
        void should_return_empty_on_unexpected_status_code() throws ApiException {
            // given
            when(props.apiKey()).thenReturn("test-key");

            final var jobResponse = new BatchJobResponse();
            jobResponse.setId("job-err");
            final var errorResponse = new ApiResponse<>(500, Map.of(), jobResponse);
            when(batchGeocodingApi.createForwardBatchGeocodingJobWithHttpInfo(
                    any(), any(), any(), any(), any(), any()))
                    .thenReturn(errorResponse);

            // when
            final var result = client.batchGeocode(List.of("Paris, France"));

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    class WhenParsingResults {

        @Test
        void should_skip_results_with_zero_coordinates() throws ApiException {
            // given
            when(props.apiKey()).thenReturn("test-key");

            final var jobResponse = new BatchJobResponse();
            jobResponse.setId("job-zero");
            final var apiResponse = new ApiResponse<>(200, Map.of(), jobResponse);
            when(batchGeocodingApi.createForwardBatchGeocodingJobWithHttpInfo(
                    eq("test-key"), any(), isNull(), isNull(), isNull(), isNull()))
                    .thenReturn(apiResponse);

            final var validResult = new BatchForwardGeocodingResult();
            validResult.setLat(48.8566);
            validResult.setLon(2.3522);
            final var zeroResult = new BatchForwardGeocodingResult();
            zeroResult.setLat(null);
            zeroResult.setLon(null);
            when(batchGeocodingApi.getBatchGeocodingResults(eq("test-key"), eq("job-zero"), isNull()))
                    .thenReturn(List.of(validResult, zeroResult));

            // when
            final var result = client.batchGeocode(List.of("Paris, France", "Unknown Place"));

            // then
            assertThat(result).hasSize(1);
            assertThat(result).containsKey("Paris, France");
            assertThat(result).doesNotContainKey("Unknown Place");
        }
    }
}

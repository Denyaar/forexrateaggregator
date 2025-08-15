/**
 * Created by tendaimupezeni for forexrateaggregator
 * Date: 8/14/25
 * Time: 7:54 PM
 */

package co.zw.mupezeni.wiremit.forexrateaggregator.dto;


import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Data Transfer Objects for Forex Rate endpoints
 */
public class ForexDTOs {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ForexRateResponse {

        @JsonProperty("currency_pair")
        private String currencyPair;

        @JsonProperty("base_currency")
        private String baseCurrency;

        @JsonProperty("target_currency")
        private String targetCurrency;

        @JsonProperty("customer_rate")
        private BigDecimal customerRate;

        @JsonProperty("average_rate")
        private BigDecimal averageRate;

        private BigDecimal markup;

        @JsonProperty("markup_amount")
        private BigDecimal markupAmount;

        @JsonProperty("api_sources")
        private List<String> apiSources;

        @JsonProperty("source_count")
        private Integer sourceCount;

        private LocalDateTime timestamp;

        @JsonProperty("display_name")
        private String displayName;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AllRatesResponse {

        private List<ForexRateResponse> rates;

        @JsonProperty("last_updated")
        private LocalDateTime lastUpdated;

        @JsonProperty("total_pairs")
        private Integer totalPairs;

        @JsonProperty("base_currency")
        private String baseCurrency;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HistoricalRatesResponse {

        @JsonProperty("currency_pair")
        private String currencyPair;

        private List<HistoricalRate> history;

        @JsonProperty("total_records")
        private Integer totalRecords;

        @JsonProperty("date_range")
        private DateRange dateRange;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HistoricalRate {

        @JsonProperty("customer_rate")
        private BigDecimal customerRate;

        @JsonProperty("average_rate")
        private BigDecimal averageRate;

        private BigDecimal markup;

        private LocalDateTime timestamp;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DateRange {

        private LocalDateTime from;
        private LocalDateTime to;
    }

    // External API Response DTOs
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExternalApiResponse {

        private String source;
        private String base;
        private Map<String, BigDecimal> rates;
        private LocalDateTime timestamp;
        private boolean success;
        private String error;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AggregationResult {

        @JsonProperty("currency_pair")
        private String currencyPair;

        @JsonProperty("base_currency")
        private String baseCurrency;

        @JsonProperty("target_currency")
        private String targetCurrency;

        @JsonProperty("raw_rates")
        private List<RawRate> rawRates;

        @JsonProperty("average_rate")
        private BigDecimal averageRate;

        @JsonProperty("customer_rate")
        private BigDecimal customerRate;

        private BigDecimal markup;

        @JsonProperty("source_count")
        private Integer sourceCount;

        private LocalDateTime timestamp;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RawRate {

        private String source;
        private BigDecimal rate;
        private LocalDateTime timestamp;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RateUpdateStatus {

        @JsonProperty("update_successful")
        private boolean updateSuccessful;

        @JsonProperty("updated_pairs")
        private List<String> updatedPairs;

        @JsonProperty("failed_pairs")
        private List<String> failedPairs;

        @JsonProperty("api_responses")
        private Map<String, Boolean> apiResponses;

        @JsonProperty("update_timestamp")
        private LocalDateTime updateTimestamp;

        @JsonProperty("next_update")
        private LocalDateTime nextUpdate;
    }
}
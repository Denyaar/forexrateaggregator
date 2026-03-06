package co.zw.mupezeni.wiremit.forexrateaggregator.service;


import co.zw.mupezeni.wiremit.forexrateaggregator.dto.ForexDTOs;
import co.zw.mupezeni.wiremit.forexrateaggregator.model.ForexRate;
import co.zw.mupezeni.wiremit.forexrateaggregator.repository.ForexRateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Service for aggregating forex rates from multiple sources
 * Fixed configuration binding for target currencies
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ForexAggregationService {

    private final ForexRateRepository forexRateRepository;
    private final ExternalForexApiService externalForexApiService;
    private final CacheService cacheService;

    @Value("${forex.markup}")
    private BigDecimal markup;

    @Value("${forex.currencies.base}")
    private String baseCurrency;

    @Value("${forex.currencies.targets}")
    private String targetCurrenciesString;

    private List<String> targetCurrencies;

    private static final int SCALE = 6;
    private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;

    @PostConstruct
    public void init() {
        if (targetCurrenciesString != null && !targetCurrenciesString.trim().isEmpty()) {
            targetCurrencies = Arrays.stream(targetCurrenciesString.split(","))
                    .map(String::trim)
                    .map(String::toUpperCase)
                    .collect(Collectors.toList());
        } else {
            targetCurrencies = Arrays.asList("GBP", "ZAR");
        }

        log.info("Initialized with base currency: {} and target currencies: {}",
                baseCurrency, targetCurrencies);
    }

    @Cacheable(value = "allRates", unless = "#result == null")
    public ForexDTOs.AllRatesResponse getAllRates() {
        log.info("Fetching all current forex rates");

        List<ForexRate> latestRates = forexRateRepository.findLatestRates();

        if (latestRates.isEmpty()) {
            log.warn("No rates found, triggering immediate update");
            updateAllRates();
            latestRates = forexRateRepository.findLatestRates();
        }

        List<ForexDTOs.ForexRateResponse> rateResponses = latestRates.stream()
                .map(this::mapToForexRateResponse)
                .collect(Collectors.toList());

        LocalDateTime lastUpdated = latestRates.stream()
                .map(ForexRate::getTimestamp)
                .max(LocalDateTime::compareTo)
                .orElse(LocalDateTime.now());

        return ForexDTOs.AllRatesResponse.builder()
                .rates(rateResponses)
                .lastUpdated(lastUpdated)
                .totalPairs(rateResponses.size())
                .baseCurrency(baseCurrency)
                .build();
    }

    @Cacheable(value = "currencyRate", key = "#currency", unless = "#result == null")
    public ForexDTOs.ForexRateResponse getRateForCurrency(String currency) {
        log.info("Fetching rate for currency: {}", currency);

        String currencyPair = generateCurrencyPair(baseCurrency, currency);

        Optional<ForexRate> latestRate = forexRateRepository
                .findFirstByCurrencyPairOrderByTimestampDesc(currencyPair);

        if (latestRate.isEmpty()) {
            log.warn("No rate found for currency pair: {}, triggering update", currencyPair);
            updateRateForCurrency(currency);
            latestRate = forexRateRepository
                    .findFirstByCurrencyPairOrderByTimestampDesc(currencyPair);
        }

        return latestRate.map(this::mapToForexRateResponse)
                .orElseThrow(() -> new RuntimeException("Unable to fetch rate for currency: " + currency));
    }


    public ForexDTOs.HistoricalRatesResponse getHistoricalRates(String currency, int days, int page, int size) {
        log.info("Fetching historical rates for currency: {} for {} days", currency, days);

        String currencyPair = generateCurrencyPair(baseCurrency, currency);
        LocalDateTime endDate = LocalDateTime.now();
        LocalDateTime startDate = endDate.minusDays(days);

        Pageable pageable = PageRequest.of(page, size);
        Page<ForexRate> historicalRates = forexRateRepository
                .findByCurrencyPairAndTimestampBetweenOrderByTimestampDesc(
                        currencyPair, startDate, endDate, pageable);

        List<ForexDTOs.HistoricalRate> history = historicalRates.getContent().stream()
                .map(rate -> ForexDTOs.HistoricalRate.builder()
                        .customerRate(rate.getCustomerRate())
                        .averageRate(rate.getAverageRate())
                        .markup(rate.getMarkup())
                        .timestamp(rate.getTimestamp())
                        .build())
                .collect(Collectors.toList());

        return ForexDTOs.HistoricalRatesResponse.builder()
                .currencyPair(currencyPair)
                .history(history)
                .totalRecords((int) historicalRates.getTotalElements())
                .dateRange(ForexDTOs.DateRange.builder()
                        .from(startDate)
                        .to(endDate)
                        .build())
                .build();
    }

    /**
     * Scheduled task to update all rates every hour
     */
    @Scheduled(fixedRateString = "${forex.refresh-interval}")
    @Async
    public void scheduledRateUpdate() {
        log.info("Starting scheduled forex rate update");
        updateAllRates();
    }

    /**
     * Update all forex rates including cross-rates
     */
    @Async
    public CompletableFuture<ForexDTOs.RateUpdateStatus> updateAllRates() {
        log.info("Updating all forex rates");

        List<String> updatedPairs = new ArrayList<>();
        List<String> failedPairs = new ArrayList<>();
        Map<String, Boolean> apiResponses = new HashMap<>();

        // Update direct pairs (USD-GBP, USD-ZAR)
        for (String targetCurrency : targetCurrencies) {
            try {
                updateRateForCurrency(targetCurrency);
                String currencyPair = generateCurrencyPair(baseCurrency, targetCurrency);
                updatedPairs.add(currencyPair);
                log.info("Successfully updated rate for: {}", currencyPair);
            } catch (Exception e) {
                String currencyPair = generateCurrencyPair(baseCurrency, targetCurrency);
                failedPairs.add(currencyPair);
                log.error("Failed to update rate for: {}", currencyPair, e);
            }
        }

        // Calculate and update cross-rates (ZAR-GBP)
        try {
            updateCrossRates();
            log.info("Successfully updated cross-rates");
        } catch (Exception e) {
            log.error("Failed to update cross-rates", e);
        }

        // Clear cache after update
        cacheService.evictAllRatesCaches();

        ForexDTOs.RateUpdateStatus status = ForexDTOs.RateUpdateStatus.builder()
                .updateSuccessful(failedPairs.isEmpty())
                .updatedPairs(updatedPairs)
                .failedPairs(failedPairs)
                .apiResponses(apiResponses)
                .updateTimestamp(LocalDateTime.now())
                .nextUpdate(LocalDateTime.now().plusHours(1))
                .build();

        log.info("Rate update completed. Updated: {}, Failed: {}",
                updatedPairs.size(), failedPairs.size());

        return CompletableFuture.completedFuture(status);
    }

    /**
     * Update rate for specific currency
     */
    private void updateRateForCurrency(String targetCurrency) {
        String currencyPair = generateCurrencyPair(baseCurrency, targetCurrency);
        log.debug("Updating rate for currency pair: {}", currencyPair);

        // Fetch rates from all external APIs
        List<ForexDTOs.ExternalApiResponse> apiResponses = externalForexApiService
                .fetchRatesFromAllApis(baseCurrency);

        // Extract rates for the target currency
        List<ForexDTOs.RawRate> rawRates = extractRatesForCurrency(apiResponses, targetCurrency);

        if (rawRates.isEmpty()) {
            throw new RuntimeException("No rates available for currency: " + targetCurrency);
        }

        // Calculate aggregated rate
        ForexDTOs.AggregationResult aggregationResult = calculateAggregatedRate(
                currencyPair, baseCurrency, targetCurrency, rawRates);

        // Save to database
        saveForexRate(aggregationResult);

        log.debug("Successfully updated rate for currency pair: {}", currencyPair);
    }

    /**
     * Extract rates for specific currency from API responses
     */
    private List<ForexDTOs.RawRate> extractRatesForCurrency(
            List<ForexDTOs.ExternalApiResponse> apiResponses, String targetCurrency) {

        return apiResponses.stream()
                .filter(ForexDTOs.ExternalApiResponse::isSuccess)
                .filter(response -> response.getRates().containsKey(targetCurrency))
                .map(response -> ForexDTOs.RawRate.builder()
                        .source(response.getSource())
                        .rate(response.getRates().get(targetCurrency))
                        .timestamp(response.getTimestamp())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * Calculate aggregated rate with markup
     */
    private ForexDTOs.AggregationResult calculateAggregatedRate(
            String currencyPair, String baseCurrency, String targetCurrency,
            List<ForexDTOs.RawRate> rawRates) {

        // Calculate average rate
        BigDecimal averageRate = rawRates.stream()
                .map(ForexDTOs.RawRate::getRate)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(rawRates.size()), SCALE, ROUNDING_MODE);

        // Apply markup
        BigDecimal customerRate = averageRate.add(
                averageRate.multiply(markup).setScale(SCALE, ROUNDING_MODE));

        return ForexDTOs.AggregationResult.builder()
                .currencyPair(currencyPair)
                .baseCurrency(baseCurrency)
                .targetCurrency(targetCurrency)
                .rawRates(rawRates)
                .averageRate(averageRate)
                .customerRate(customerRate)
                .markup(markup)
                .sourceCount(rawRates.size())
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Save forex rate to database
     */
    private void saveForexRate(ForexDTOs.AggregationResult aggregationResult) {
        ForexRate forexRate = ForexRate.builder()
                .currencyPair(aggregationResult.getCurrencyPair())
                .baseCurrency(aggregationResult.getBaseCurrency())
                .targetCurrency(aggregationResult.getTargetCurrency())
                .averageRate(aggregationResult.getAverageRate())
                .customerRate(aggregationResult.getCustomerRate())
                .markup(aggregationResult.getMarkup())
                .apiSources(aggregationResult.getRawRates().stream()
                        .map(ForexDTOs.RawRate::getSource)
                        .collect(Collectors.joining(",")))
                .sourceCount(aggregationResult.getSourceCount())
                .timestamp(aggregationResult.getTimestamp())
                .build();

        forexRateRepository.save(forexRate);
        log.debug("Saved forex rate: {}", forexRate.getCurrencyPair());
    }

    /**
     * Map ForexRate entity to DTO
     */
    private ForexDTOs.ForexRateResponse mapToForexRateResponse(ForexRate forexRate) {
        return ForexDTOs.ForexRateResponse.builder()
                .currencyPair(forexRate.getCurrencyPair())
                .baseCurrency(forexRate.getBaseCurrency())
                .targetCurrency(forexRate.getTargetCurrency())
                .customerRate(forexRate.getCustomerRate())
                .averageRate(forexRate.getAverageRate())
                .markup(forexRate.getMarkup())
                .markupAmount(forexRate.getMarkupAmount())
                .apiSources(forexRate.getApiSources() != null ?
                        Arrays.asList(forexRate.getApiSources().split(",")) : Collections.emptyList())
                .sourceCount(forexRate.getSourceCount())
                .timestamp(forexRate.getTimestamp())
                .displayName(forexRate.getDisplayName())
                .build();
    }

    /**
     * Generate currency pair string
     */
    private String generateCurrencyPair(String base, String target) {
        return String.format("%s-%s", base.toUpperCase(), target.toUpperCase());
    }

    /**
     * Update cross-rates (e.g., ZAR-GBP calculated from USD-ZAR and USD-GBP)
     */
    private void updateCrossRates() {
        log.info("Calculating cross-rates");

        // For ZAR-GBP: divide USD-ZAR by USD-GBP (how many GBP for 1 ZAR)
        if (targetCurrencies.contains("ZAR") && targetCurrencies.contains("GBP")) {
            calculateAndSaveCrossRate("ZAR", "GBP");
        }
    }

    /**
     * Calculate cross-rate between two non-base currencies
     * Formula: If we have USD-ZAR and USD-GBP, then ZAR-GBP = USD-GBP / USD-ZAR
     */
    private void calculateAndSaveCrossRate(String fromCurrency, String toCurrency) {
        try {
            // Get latest rates for both currency pairs
            String pair1 = generateCurrencyPair(baseCurrency, fromCurrency); // USD-ZAR
            String pair2 = generateCurrencyPair(baseCurrency, toCurrency);   // USD-GBP

            Optional<ForexRate> rate1 = forexRateRepository.findFirstByCurrencyPairOrderByTimestampDesc(pair1);
            Optional<ForexRate> rate2 = forexRateRepository.findFirstByCurrencyPairOrderByTimestampDesc(pair2);

            if (rate1.isEmpty() || rate2.isEmpty()) {
                log.warn("Cannot calculate cross-rate {}-{}: missing base rates", fromCurrency, toCurrency);
                return;
            }

            ForexRate usdFrom = rate1.get(); // USD-ZAR
            ForexRate usdTo = rate2.get();   // USD-GBP

            // Calculate cross-rate: ZAR-GBP = USD-GBP / USD-ZAR
            BigDecimal crossAverageRate = usdTo.getAverageRate()
                    .divide(usdFrom.getAverageRate(), SCALE, ROUNDING_MODE);

            BigDecimal crossCustomerRate = usdTo.getCustomerRate()
                    .divide(usdFrom.getCustomerRate(), SCALE, ROUNDING_MODE);

            // Create cross-rate entry
            String crossPair = generateCurrencyPair(fromCurrency, toCurrency);

            ForexRate crossRate = ForexRate.builder()
                    .currencyPair(crossPair)
                    .baseCurrency(fromCurrency)
                    .targetCurrency(toCurrency)
                    .averageRate(crossAverageRate)
                    .customerRate(crossCustomerRate)
                    .markup(markup)
                    .apiSources("CROSS-RATE")
                    .sourceCount(1)
                    .timestamp(LocalDateTime.now())
                    .build();

            forexRateRepository.save(crossRate);
            log.info("Calculated and saved cross-rate: {} = {}", crossPair, crossCustomerRate);

        } catch (Exception e) {
            log.error("Error calculating cross-rate {}-{}", fromCurrency, toCurrency, e);
        }
    }

    /**
     * Get configured target currencies
     */
    public List<String> getTargetCurrencies() {
        return new ArrayList<>(targetCurrencies);
    }
}
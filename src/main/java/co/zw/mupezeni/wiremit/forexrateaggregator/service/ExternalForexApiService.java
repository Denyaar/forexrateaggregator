/**
 * Created by tendaimupezeni for forexrateaggregator
 * Date: 8/14/25
 * Time: 8:06 PM
 */

package co.zw.mupezeni.wiremit.forexrateaggregator.service;

import co.zw.mupezeni.wiremit.forexrateaggregator.dto.ForexDTOs;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Service for fetching forex rates from external APIs
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ExternalForexApiService {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    @Value("${forex.apis.exchangerate-api.url}")
    private String exchangeRateApiUrl;

    @Value("${forex.apis.exchangerate-api.enabled}")
    private boolean exchangeRateApiEnabled;

    @Value("${forex.apis.fixer-api.url}")
    private String fixerApiUrl;

    @Value("${forex.apis.fixer-api.access-key}")
    private String fixerApiKey;

    @Value("${forex.apis.fixer-api.enabled}")
    private boolean fixerApiEnabled;

    @Value("${forex.apis.currencylayer-api.url}")
    private String currencyLayerApiUrl;

    @Value("${forex.apis.currencylayer-api.access-key}")
    private String currencyLayerApiKey;

    @Value("${forex.apis.currencylayer-api.enabled}")
    private boolean currencyLayerApiEnabled;

    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    /**
     * Fetch rates from all enabled external APIs
     */
    public List<ForexDTOs.ExternalApiResponse> fetchRatesFromAllApis(String baseCurrency) {
        log.info("Fetching rates from all external APIs for base currency: {}", baseCurrency);

        List<CompletableFuture<ForexDTOs.ExternalApiResponse>> futures = new ArrayList<>();

        // Exchange Rate API
        if (exchangeRateApiEnabled) {
            futures.add(fetchFromExchangeRateApi(baseCurrency));
        }

        // Fixer API
        if (fixerApiEnabled) {
            futures.add(fetchFromFixerApi(baseCurrency));
        }

        // Currency Layer API
        if (currencyLayerApiEnabled) {
            futures.add(fetchFromCurrencyLayerApi(baseCurrency));
        }

        // Wait for all API calls to complete
        List<ForexDTOs.ExternalApiResponse> responses = futures.stream()
                .map(CompletableFuture::join)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        log.info("Received {} responses from external APIs", responses.size());
        return responses;
    }

    /**
     * Fetch rates from Exchange Rate API
     */
    private CompletableFuture<ForexDTOs.ExternalApiResponse> fetchFromExchangeRateApi(String baseCurrency) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.debug("Fetching rates from Exchange Rate API");

                String url = String.format("%s/%s", exchangeRateApiUrl, baseCurrency);

                Mono<String> response = webClient.get()
                        .uri(url)
                        .retrieve()
                        .bodyToMono(String.class)
                        .timeout(TIMEOUT);

                String responseBody = response.block();
                return parseExchangeRateApiResponse(responseBody);

            } catch (Exception e) {
                log.error("Error fetching from Exchange Rate API", e);
                return ForexDTOs.ExternalApiResponse.builder()
                        .source("ExchangeRate-API")
                        .success(false)
                        .error(e.getMessage())
                        .timestamp(LocalDateTime.now())
                        .build();
            }
        });
    }

    /**
     * Fetch rates from Fixer API
     */
    private CompletableFuture<ForexDTOs.ExternalApiResponse> fetchFromFixerApi(String baseCurrency) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.debug("Fetching rates from Fixer API");

                Mono<String> response = webClient.get()
                        .uri(uriBuilder -> uriBuilder
                                .path(fixerApiUrl)
                                .queryParam("access_key", fixerApiKey)
                                .queryParam("base", baseCurrency)
                                .queryParam("symbols", "USD,GBP,ZAR")
                                .build())
                        .retrieve()
                        .bodyToMono(String.class)
                        .timeout(TIMEOUT);

                String responseBody = response.block();
                return parseFixerApiResponse(responseBody);

            } catch (Exception e) {
                log.error("Error fetching from Fixer API", e);
                return ForexDTOs.ExternalApiResponse.builder()
                        .source("Fixer")
                        .success(false)
                        .error(e.getMessage())
                        .timestamp(LocalDateTime.now())
                        .build();
            }
        });
    }

    /**
     * Fetch rates from Currency Layer API
     */
    private CompletableFuture<ForexDTOs.ExternalApiResponse> fetchFromCurrencyLayerApi(String baseCurrency) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.debug("Fetching rates from Currency Layer API");

                Mono<String> response = webClient.get()
                        .uri(uriBuilder -> uriBuilder
                                .path(currencyLayerApiUrl)
                                .queryParam("access_key", currencyLayerApiKey)
                                .queryParam("source", baseCurrency)
                                .queryParam("currencies", "USD,GBP,ZAR")
                                .build())
                        .retrieve()
                        .bodyToMono(String.class)
                        .timeout(TIMEOUT);

                String responseBody = response.block();
                return parseCurrencyLayerApiResponse(responseBody);

            } catch (Exception e) {
                log.error("Error fetching from Currency Layer API", e);
                return ForexDTOs.ExternalApiResponse.builder()
                        .source("CurrencyLayer")
                        .success(false)
                        .error(e.getMessage())
                        .timestamp(LocalDateTime.now())
                        .build();
            }
        });
    }

    /**
     * Parse Exchange Rate API response
     */
    private ForexDTOs.ExternalApiResponse parseExchangeRateApiResponse(String responseBody) {
        try {
            JsonNode jsonNode = objectMapper.readTree(responseBody);

            if (jsonNode.has("error")) {
                return ForexDTOs.ExternalApiResponse.builder()
                        .source("ExchangeRate-API")
                        .success(false)
                        .error(jsonNode.get("error").asText())
                        .timestamp(LocalDateTime.now())
                        .build();
            }

            String base = jsonNode.get("base").asText();
            JsonNode ratesNode = jsonNode.get("rates");

            Map<String, BigDecimal> rates = new HashMap<>();
            ratesNode.fieldNames().forEachRemaining(currency -> {
                BigDecimal rate = new BigDecimal(ratesNode.get(currency).asText());
                rates.put(currency, rate);
            });

            return ForexDTOs.ExternalApiResponse.builder()
                    .source("ExchangeRate-API")
                    .base(base)
                    .rates(rates)
                    .success(true)
                    .timestamp(LocalDateTime.now())
                    .build();

        } catch (Exception e) {
            log.error("Error parsing Exchange Rate API response", e);
            return ForexDTOs.ExternalApiResponse.builder()
                    .source("ExchangeRate-API")
                    .success(false)
                    .error("Parse error: " + e.getMessage())
                    .timestamp(LocalDateTime.now())
                    .build();
        }
    }

    /**
     * Parse Fixer API response
     */
    private ForexDTOs.ExternalApiResponse parseFixerApiResponse(String responseBody) {
        try {
            JsonNode jsonNode = objectMapper.readTree(responseBody);

            if (!jsonNode.get("success").asBoolean()) {
                JsonNode errorNode = jsonNode.get("error");
                return ForexDTOs.ExternalApiResponse.builder()
                        .source("Fixer")
                        .success(false)
                        .error(errorNode.get("info").asText())
                        .timestamp(LocalDateTime.now())
                        .build();
            }

            String base = jsonNode.get("base").asText();
            JsonNode ratesNode = jsonNode.get("rates");

            Map<String, BigDecimal> rates = new HashMap<>();
            ratesNode.fieldNames().forEachRemaining(currency -> {
                BigDecimal rate = new BigDecimal(ratesNode.get(currency).asText());
                rates.put(currency, rate);
            });

            return ForexDTOs.ExternalApiResponse.builder()
                    .source("Fixer")
                    .base(base)
                    .rates(rates)
                    .success(true)
                    .timestamp(LocalDateTime.now())
                    .build();

        } catch (Exception e) {
            log.error("Error parsing Fixer API response", e);
            return ForexDTOs.ExternalApiResponse.builder()
                    .source("Fixer")
                    .success(false)
                    .error("Parse error: " + e.getMessage())
                    .timestamp(LocalDateTime.now())
                    .build();
        }
    }

    /**
     * Parse Currency Layer API response
     */
    private ForexDTOs.ExternalApiResponse parseCurrencyLayerApiResponse(String responseBody) {
        try {
            JsonNode jsonNode = objectMapper.readTree(responseBody);

            if (!jsonNode.get("success").asBoolean()) {
                JsonNode errorNode = jsonNode.get("error");
                return ForexDTOs.ExternalApiResponse.builder()
                        .source("CurrencyLayer")
                        .success(false)
                        .error(errorNode.get("info").asText())
                        .timestamp(LocalDateTime.now())
                        .build();
            }

            String source = jsonNode.get("source").asText();
            JsonNode quotesNode = jsonNode.get("quotes");

            Map<String, BigDecimal> rates = new HashMap<>();
            quotesNode.fieldNames().forEachRemaining(quote -> {
                // Quote format is like "USDGBP", extract target currency
                String targetCurrency = quote.substring(3);
                BigDecimal rate = new BigDecimal(quotesNode.get(quote).asText());
                rates.put(targetCurrency, rate);
            });

            return ForexDTOs.ExternalApiResponse.builder()
                    .source("CurrencyLayer")
                    .base(source)
                    .rates(rates)
                    .success(true)
                    .timestamp(LocalDateTime.now())
                    .build();

        } catch (Exception e) {
            log.error("Error parsing Currency Layer API response", e);
            return ForexDTOs.ExternalApiResponse.builder()
                    .source("CurrencyLayer")
                    .success(false)
                    .error("Parse error: " + e.getMessage())
                    .timestamp(LocalDateTime.now())
                    .build();
        }
    }
}
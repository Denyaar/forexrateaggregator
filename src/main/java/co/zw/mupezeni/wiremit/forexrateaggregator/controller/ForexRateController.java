/**
 * Created by tendaimupezeni for forexrateaggregator
 * Date: 8/14/25
 * Time: 8:13 PM
 */

package co.zw.mupezeni.wiremit.forexrateaggregator.controller;


import co.zw.mupezeni.wiremit.forexrateaggregator.dto.ForexDTOs;
import co.zw.mupezeni.wiremit.forexrateaggregator.service.ForexAggregationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

/**
 * Controller handling forex rate endpoints
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*", maxAge = 3600)
public class ForexRateController {

    private final ForexAggregationService forexAggregationService;


    @GetMapping("/rates")
//    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<?> getAllRates(Principal principal) {
        try {
            log.info("User {} requested all forex rates", principal.getName());

            ForexDTOs.AllRatesResponse response = forexAggregationService.getAllRates();

            log.info("Successfully retrieved {} forex rates for user {}",
                    response.getTotalPairs(), principal.getName());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error retrieving all forex rates for user {}", principal.getName(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Failed to retrieve forex rates", e.getMessage()));
        }
    }


    @GetMapping("/rates/{currency}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<?> getRateForCurrency(@PathVariable String currency, Principal principal) {
        try {
            log.info("User {} requested rate for currency: {}", principal.getName(), currency.toUpperCase());

            // Validate currency format
            if (currency.length() != 3) {
                return ResponseEntity.badRequest()
                        .body(new ErrorResponse("Invalid currency format",
                                "Currency must be a 3-letter code (e.g., GBP, ZAR)"));
            }

            ForexDTOs.ForexRateResponse response = forexAggregationService
                    .getRateForCurrency(currency.toUpperCase());

            log.info("Successfully retrieved rate for currency {} for user {}",
                    currency.toUpperCase(), principal.getName());

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            log.error("Error retrieving rate for currency {} for user {}",
                    currency, principal.getName(), e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse("Currency rate not found", e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error retrieving rate for currency {} for user {}",
                    currency, principal.getName(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Failed to retrieve currency rate", e.getMessage()));
        }
    }


    @GetMapping("/historical/rates")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<?> getHistoricalRates(
            @RequestParam String currency,
            @RequestParam(defaultValue = "7") int days,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            Principal principal) {

        try {
            log.info("User {} requested historical rates for currency: {} for {} days",
                    principal.getName(), currency.toUpperCase(), days);

            // Validate parameters
            if (currency.length() != 3) {
                return ResponseEntity.badRequest()
                        .body(new ErrorResponse("Invalid currency format",
                                "Currency must be a 3-letter code (e.g., GBP, ZAR)"));
            }

            if (days < 1 || days > 365) {
                return ResponseEntity.badRequest()
                        .body(new ErrorResponse("Invalid days parameter",
                                "Days must be between 1 and 365"));
            }

            if (page < 0) {
                return ResponseEntity.badRequest()
                        .body(new ErrorResponse("Invalid page parameter",
                                "Page must be non-negative"));
            }

            if (size < 1 || size > 1000) {
                return ResponseEntity.badRequest()
                        .body(new ErrorResponse("Invalid size parameter",
                                "Size must be between 1 and 1000"));
            }

            ForexDTOs.HistoricalRatesResponse response = forexAggregationService
                    .getHistoricalRates(currency.toUpperCase(), days, page, size);

            log.info("Successfully retrieved {} historical rates for currency {} for user {}",
                    response.getTotalRecords(), currency.toUpperCase(), principal.getName());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error retrieving historical rates for currency {} for user {}",
                    currency, principal.getName(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Failed to retrieve historical rates", e.getMessage()));
        }
    }


    @PostMapping("/rates/update")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateRates(Principal principal) {
        try {
            log.info("Admin {} triggered manual rate update", principal.getName());

            CompletableFuture<ForexDTOs.RateUpdateStatus> updateFuture =
                    forexAggregationService.updateAllRates();

            // Return immediate response for async operation
            return ResponseEntity.accepted().body(
                    new UpdateInitiatedResponse("Rate update initiated successfully",
                            LocalDateTime.now())
            );

        } catch (Exception e) {
            log.error("Error initiating rate update by admin {}", principal.getName(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Failed to initiate rate update", e.getMessage()));
        }
    }

    /**
     * Get supported currencies
     */
    @GetMapping("/rates/currencies")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<?> getSupportedCurrencies(Principal principal) {
        try {
            log.debug("User {} requested supported currencies", principal.getName());

            SupportedCurrenciesResponse response = new SupportedCurrenciesResponse(
                    "USD", // base currency
                    java.util.List.of("GBP", "ZAR"), // target currencies
                    "These are the currently supported currency pairs for conversion"
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error retrieving supported currencies for user {}", principal.getName(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Failed to retrieve supported currencies", e.getMessage()));
        }
    }

    /**
     * Health check for rates service
     */
    @GetMapping("/rates/health")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<?> ratesHealth() {
        try {
            return ResponseEntity.ok().body(
                    new HealthResponse("Forex rates service is operational",
                            LocalDateTime.now(), "All systems functioning normally")
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(new ErrorResponse("Service health check failed", e.getMessage()));
        }
    }

    private record ErrorResponse(String error, String message) {}

    private record UpdateInitiatedResponse(String message, LocalDateTime timestamp) {}

    private record SupportedCurrenciesResponse(
            String baseCurrency,
            java.util.List<String> targetCurrencies,
            String description
    ) {}

    private record HealthResponse(String status, LocalDateTime timestamp, String details) {}
}
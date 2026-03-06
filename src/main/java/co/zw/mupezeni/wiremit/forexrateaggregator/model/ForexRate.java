/**
 * Created by tendaimupezeni for forexrateaggregator
 * Date: 8/14/25
 * Time: 7:50 PM
 */

package co.zw.mupezeni.wiremit.forexrateaggregator.model;


import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity representing aggregated forex rates with markup applied.
 * Includes indexes for optimized query performance on currency pair and timestamp lookups.
 */
@Entity
@Table(name = "forex_rates",
        indexes = {
                @Index(name = "idx_currency_pair", columnList = "currency_pair"),
                @Index(name = "idx_currency_pair_timestamp", columnList = "currency_pair, timestamp DESC"),
                @Index(name = "idx_timestamp", columnList = "timestamp DESC"),
                @Index(name = "idx_base_target", columnList = "base_currency, target_currency")
        })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ForexRate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Currency pair is required")
    @Size(min = 6, max = 7, message = "Currency pair must be in format 'USD-GBP'")
    @Column(name = "currency_pair", nullable = false)
    private String currencyPair;

    @NotNull(message = "Base currency is required")
    @Size(min = 3, max = 3, message = "Base currency must be 3 characters")
    @Column(name = "base_currency", nullable = false)
    private String baseCurrency;

    @NotNull(message = "Target currency is required")
    @Size(min = 3, max = 3, message = "Target currency must be 3 characters")
    @Column(name = "target_currency", nullable = false)
    private String targetCurrency;

    @NotNull(message = "Average rate is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Average rate must be positive")
    @Column(name = "average_rate", nullable = false, precision = 19, scale = 6)
    private BigDecimal averageRate;

    @NotNull(message = "Customer rate is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Customer rate must be positive")
    @Column(name = "customer_rate", nullable = false, precision = 19, scale = 6)
    private BigDecimal customerRate;

    @NotNull(message = "Markup is required")
    @DecimalMin(value = "0.0", message = "Markup must be non-negative")
    @Column(name = "markup", nullable = false, precision = 5, scale = 4)
    private BigDecimal markup;

    @Column(name = "api_sources", columnDefinition = "TEXT")
    private String apiSources;

    @Column(name = "source_count")
    private Integer sourceCount;

    @NotNull(message = "Timestamp is required")
    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
    }

    /**
     * Get formatted currency pair display name
     */
    public String getDisplayName() {
        return String.format("%s to %s", baseCurrency, targetCurrency);
    }

    /**
     * Calculate the markup amount
     */
    public BigDecimal getMarkupAmount() {
        return customerRate.subtract(averageRate);
    }
}
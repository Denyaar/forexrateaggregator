/**
 * Created by tendaimupezeni for forexrateaggregator
 * Date: 8/14/25
 * Time: 7:56 PM
 */

package co.zw.mupezeni.wiremit.forexrateaggregator.repository;

import co.zw.mupezeni.wiremit.forexrateaggregator.model.ForexRate;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Pageable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ForexRateRepository extends JpaRepository<ForexRate, Long> {

    Optional<ForexRate> findFirstByCurrencyPairOrderByTimestampDesc(String currencyPair);
    @Query("SELECT fr FROM ForexRate fr WHERE fr.timestamp = " +
            "(SELECT MAX(fr2.timestamp) FROM ForexRate fr2 WHERE fr2.currencyPair = fr.currencyPair)")
    List<ForexRate> findLatestRates();
    Page<ForexRate> findByCurrencyPairAndTimestampBetweenOrderByTimestampDesc(
            String currencyPair,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Pageable pageable
    );

}
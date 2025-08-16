/**
 * Created by tendaimupezeni for forexrateaggregator
 * Date: 8/14/25
 * Time: 7:55 PM
 */

package co.zw.mupezeni.wiremit.forexrateaggregator.repository;


import co.zw.mupezeni.wiremit.forexrateaggregator.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {


    Optional<User> findByUsername(String username);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);

    @Modifying
    @Query("UPDATE User u SET u.lastLogin = :lastLogin WHERE u.id = :userId")
    void updateLastLogin(@Param("userId") Long userId, @Param("lastLogin") LocalDateTime lastLogin);
}


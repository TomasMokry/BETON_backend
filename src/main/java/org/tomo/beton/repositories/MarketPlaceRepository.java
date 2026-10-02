package org.tomo.beton.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.tomo.beton.entities.MarketPlace;

import java.util.List;

public interface MarketPlaceRepository extends JpaRepository<MarketPlace, Long> {
    @Query("SELECT m FROM MarketPlace m WHERE m.active = true ORDER BY m.startDate DESC NULLS LAST, m.name")
    List<MarketPlace> findAllActive();

    @Query("SELECT m FROM MarketPlace m ORDER BY m.active DESC, m.startDate DESC NULLS LAST, m.name")
    List<MarketPlace> findAllSorted();

    @Modifying
    @Query("UPDATE User u SET u.currentMarketPlace = null WHERE u.currentMarketPlace = :marketPlace")
    void clearFromUsers(@Param("marketPlace") MarketPlace marketPlace);
}

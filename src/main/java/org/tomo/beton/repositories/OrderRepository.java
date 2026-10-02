package org.tomo.beton.repositories;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.tomo.beton.entities.MarketPlace;
import org.tomo.beton.entities.Order;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
    /*
     * Shared filter: customerId null = all customers;
     * filterMarket false = all market places, otherwise noMarket true = orders without a market place,
     * noMarket false = orders of marketPlaceId.
     */
    String FILTER = "(:customerId IS NULL OR o.customer.id = :customerId) AND " +
            "(:filterMarket = false OR (:noMarket = true AND m IS NULL) OR (:noMarket = false AND m.id = :marketPlaceId))";

    @EntityGraph(attributePaths = {"items.product", "marketPlace", "customer"})
    @Query("SELECT o FROM Order o LEFT JOIN o.marketPlace m WHERE " + FILTER)
    List<Order> findFiltered(@Param("customerId") Long customerId,
                             @Param("filterMarket") boolean filterMarket,
                             @Param("noMarket") boolean noMarket,
                             @Param("marketPlaceId") Long marketPlaceId);

    @EntityGraph(attributePaths = {"items.product", "marketPlace", "customer"})
    @Query("SELECT o FROM Order o WHERE o.id = :orderId")
    Optional<Order> getOrderWithItems(@Param("orderId") Long orderId);

    /** Rows of [marketPlaceId, marketPlaceName, method, orderCount, totalPrice] for the filtered orders. */
    @Query("SELECT m.id, m.name, o.method, COUNT(o), SUM(o.totalPrice) FROM Order o LEFT JOIN o.marketPlace m " +
            "WHERE " + FILTER + " GROUP BY m.id, m.name, o.method")
    List<Object[]> summarizeByMarketPlace(@Param("customerId") Long customerId,
                                          @Param("filterMarket") boolean filterMarket,
                                          @Param("noMarket") boolean noMarket,
                                          @Param("marketPlaceId") Long marketPlaceId);

    boolean existsByMarketPlace(MarketPlace marketPlace);
}

package org.tomo.beton.repositories;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.tomo.beton.entities.MarketPlace;
import org.tomo.beton.entities.Order;
import org.tomo.beton.entities.User;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
    @EntityGraph(attributePaths = {"items.product", "marketPlace"})
    @Query("SELECT o FROM Order o WHERE o.customer = :customer")
    List<Order> getOrdersByCustomer(@Param("customer") User customer);

    @EntityGraph(attributePaths = {"items.product", "marketPlace"})
    @Query("SELECT o FROM Order o WHERE o.customer = :customer AND o.marketPlace.id = :marketPlaceId")
    List<Order> getOrdersByCustomerAndMarketPlace(@Param("customer") User customer,
                                                  @Param("marketPlaceId") Long marketPlaceId);

    @EntityGraph(attributePaths = {"items.product", "marketPlace"})
    @Query("SELECT o FROM Order o WHERE o.customer = :customer AND o.marketPlace IS NULL")
    List<Order> getOrdersByCustomerWithoutMarketPlace(@Param("customer") User customer);

    @EntityGraph(attributePaths = {"items.product", "marketPlace"})
    @Query("SELECT o FROM Order o WHERE o.id = :orderId")
    Optional<Order> getOrderWithItems(@Param("orderId") Long orderId);

    /** Rows of [marketPlaceId, marketPlaceName, method, orderCount, totalPrice] for the customer's orders. */
    @Query("SELECT m.id, m.name, o.method, COUNT(o), SUM(o.totalPrice) FROM Order o LEFT JOIN o.marketPlace m " +
            "WHERE o.customer = :customer GROUP BY m.id, m.name, o.method")
    List<Object[]> summarizeByMarketPlace(@Param("customer") User customer);

    boolean existsByMarketPlace(MarketPlace marketPlace);
}

package org.tomo.beton.repositories;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.tomo.beton.entities.Product;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {

    @EntityGraph(attributePaths = "category")
    List<Product> findByCategoryId(Byte categoryId);

    @Query("select p from Product p")
    @EntityGraph(attributePaths = "category")
    List<Product> findAllWithCategory();
}

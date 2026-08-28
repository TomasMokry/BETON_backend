package org.tomo.beton.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.tomo.beton.entities.Category;

public interface CategoryRepository extends JpaRepository<Category, Byte> {
}

package org.tomo.beton.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.tomo.beton.entities.User;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByEmail(String email);

    Optional<User> findByEmail(String email);
}

package org.tomo.beton.service;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.tomo.beton.entities.User;
import org.tomo.beton.repositories.UserRepository;

@Service
@AllArgsConstructor
public class AuthService {

    private final UserRepository userRepository;

    public User getCurrentUser() {
        return userRepository.findById(1L).orElse(null);
    }
}

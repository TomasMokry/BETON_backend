package org.tomo.beton.service;

import lombok.AllArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.tomo.beton.entities.User;
import org.tomo.beton.excetions.UserNotFoundException;
import org.tomo.beton.repositories.UserRepository;

@Service
@AllArgsConstructor
public class AuthService {

    private final UserRepository userRepository;

    public User getCurrentUser() {
        // JwtAuthenticationFilter stores the user id (JWT subject) as the principal
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Long userId)) {
            throw new UserNotFoundException();
        }
        return userRepository.findById(userId).orElseThrow(UserNotFoundException::new);
    }
}

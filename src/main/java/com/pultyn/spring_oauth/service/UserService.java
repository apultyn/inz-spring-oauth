package com.pultyn.spring_oauth.service;

import com.pultyn.spring_oauth.exceptions.NotFoundException;
import com.pultyn.spring_oauth.model.UserEntity;
import com.pultyn.spring_oauth.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.apache.catalina.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;

    public UserEntity findUserById(Long userId) throws NotFoundException {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
    }

    @Transactional
    public UserEntity getOrCreateUser(Jwt jwt) {
        String keycloakId = jwt.getSubject();

        return userRepository.findByKeycloakId(keycloakId)
                .orElseGet(() -> {
                    String email = jwt.getClaimAsString("preferred_username");

                    UserEntity newUser = UserEntity.builder()
                            .email(email)
                            .keycloakId(keycloakId)
                            .build();
                    return userRepository.save(newUser);
                });
    }
}

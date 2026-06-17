package com.taskhive.service;

import com.taskhive.dto.UserRegistrationDto;
import com.taskhive.model.User;
import com.taskhive.model.UserGlobalRole;
import com.taskhive.repository.GlobalRoleRepository;
import com.taskhive.repository.UserGlobalRoleRepository;
import com.taskhive.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final GlobalRoleRepository globalRoleRepository;
    private final UserGlobalRoleRepository userGlobalRoleRepository;

    @Transactional
    public User register(UserRegistrationDto dto) {
        if (userRepository.findByEmail(dto.getEmail()).isPresent()) {
            throw new RuntimeException("Email already exists");
        }

        var user = User.builder()
                .email(dto.getEmail())
                .passwordHash(passwordEncoder.encode(dto.getPassword()))
                .isActive(true)
                .build();

        user = userRepository.save(user);
        assignRole(user, "USER");

        return user;
    }

    @Transactional(readOnly = true)
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Transactional(readOnly = true)
    public User getById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @Transactional
    public void deactivate(Long userId) {
        var user = getById(userId);
        user.setActive(false);
        userRepository.save(user);
    }

    @Transactional
    public void activate(Long userId) {
        var user = getById(userId);
        user.setActive(true);
        userRepository.save(user);
    }

    @Transactional
    public void assignRole(User user, String roleName) {
        var role = globalRoleRepository.findByRoleName(roleName)
                .orElseThrow(() -> new RuntimeException("Role not found: " + roleName));

        boolean alreadyHasRole = userGlobalRoleRepository.findByUserAndValidToIsNull(user)
                .stream()
                .anyMatch(ugr -> ugr.getGlobalRole().getRoleName().equals(roleName));

        if (!alreadyHasRole) {
            var userRole = UserGlobalRole.builder()
                    .user(user)
                    .globalRole(role)
                    .build();
            userGlobalRoleRepository.save(userRole);
        }
    }

    @Transactional
    public void revokeRole(Long userId, String roleName) {
        var user = getById(userId);
        userGlobalRoleRepository.findByUserAndValidToIsNull(user).stream()
                .filter(ugr -> ugr.getGlobalRole().getRoleName().equals(roleName))
                .forEach(ugr -> {
                    ugr.setValidTo(Instant.now());
                    userGlobalRoleRepository.save(ugr);
                });
    }

    @Transactional(readOnly = true)
    public List<UserGlobalRole> getActiveRoles(User user) {
        return userGlobalRoleRepository.findByUserAndValidToIsNull(user);
    }

    @Transactional(readOnly = true)
    public boolean hasRole(User user, String roleName) {
        return userGlobalRoleRepository.findByUserAndValidToIsNull(user).stream()
                .anyMatch(ugr -> ugr.getGlobalRole().getRoleName().equals(roleName));
    }
}

package com.example.MultiAdminProj;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    public User saveUser(User user) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findById(username)
                .orElseThrow(() -> new RuntimeException("Authenticated user not found"));

        Role roleToAssign = user.getRole();
        if (roleToAssign != null) {
            if ("STUDENT".equalsIgnoreCase(roleToAssign.getName())) {
                throw new IllegalArgumentException("Cannot create student user via this endpoint. Use /students/register or /students/add.");
            }

            if (!roleToAssign.getSchool().getId().equals(user.getSchool().getId())) {
                throw new IllegalArgumentException("Role must belong to the same school as the user");
            }

            Role existingRole = roleRepository.findByNameAndSchool(roleToAssign.getName(), user.getSchool())
                    .orElseThrow(() -> new IllegalArgumentException("Role not found: " + roleToAssign.getName()));

            if (existingRole.getLevel() > currentUser.getRole().getLevel()) {
                throw new SecurityException("Cannot assign a role with a higher privilege level");
            }

            Set<Permission> userPermissions = currentUser.getRole().getPermissions();
            if (existingRole.getPermissions() != null) {
                for (Permission permission : existingRole.getPermissions()) {
                    if (!userPermissions.contains(permission)) {
                        throw new SecurityException("Cannot assign permission " + permission + " that user does not have");
                    }
                }
            } else {
                throw new IllegalArgumentException("Role " + existingRole.getName() + " has no permissions defined");
            }

            user.setRole(existingRole);
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return userRepository.save(user);
    }

    public List<User> getAll() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findById(username)
                .orElseThrow(() -> new RuntimeException("Authenticated user not found"));

        return userRepository.findBySchoolAndRoleNameNot(currentUser.getSchool(), "STUDENT");
    }

    public void delete(String username) {
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findById(currentUsername)
                .orElseThrow(() -> new RuntimeException("Authenticated user not found"));

        User user = userRepository.findById(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));

        if (!user.getSchool().equals(currentUser.getSchool())) {
            throw new SecurityException("Cannot delete user from a different school");
        }
        userRepository.deleteById(username);
    }

    @Transactional
    public User updateUserRole(String username, Role role) {
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findById(currentUsername)
                .orElseThrow(() -> new RuntimeException("Authenticated user not found"));

        int currentUserRoleLevel = currentUser.getRole().getLevel();
        Set<Permission> userPermissions = currentUser.getRole().getPermissions();

        User user = userRepository.findById(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));

        // Prevent updating a user with equal or higher role level
        if (user.getRole().getLevel() >= currentUserRoleLevel) {
            throw new SecurityException("Cannot update a user with equal or higher privilege level");
        }

        if ("STUDENT".equalsIgnoreCase(role.getName())) {
            throw new IllegalArgumentException("Cannot assign STUDENT role via this endpoint. Use /students/register or /students/add.");
        }

        if (!role.getSchool().getId().equals(user.getSchool().getId())) {
            throw new IllegalArgumentException("Role must belong to the same school as the user");
        }

        Role existingRole = roleRepository.findByNameAndSchool(role.getName(), user.getSchool())
                .orElseThrow(() -> new IllegalArgumentException("Role not found: " + role.getName()));

        // Prevent assigning a role with equal or higher level
        if (existingRole.getLevel() >= currentUserRoleLevel) {
            throw new SecurityException("Cannot assign a role with equal or higher privilege level");
        }

        for (Permission permission : existingRole.getPermissions()) {
            if (!userPermissions.contains(permission)) {
                throw new SecurityException("Cannot assign role with permission " + permission + " that you do not have");
            }
        }

        user.setRole(existingRole);
        return userRepository.save(user);
    }

    public User updateOwnProfile(UpdateUserProfileRequest request) {
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findById(currentUsername)
                .orElseThrow(() -> new RuntimeException("Authenticated user not found"));

        if (request.getUsername() != null && !request.getUsername().isBlank()
                && !request.getUsername().equals(currentUsername)) {
            if (userRepository.existsById(request.getUsername())) {
                throw new IllegalArgumentException("Username already taken.");
            }
            userRepository.deleteById(currentUsername); // Remove old record (PK change)
            user.setUsername(request.getUsername());
        }

        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            user.setEmail(request.getEmail());
        }

        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        return userRepository.save(user);
    }
}
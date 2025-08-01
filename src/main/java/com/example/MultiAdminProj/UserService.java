package com.example.MultiAdminProj;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;

@Service
public class UserService {
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    public User saveUser(User user) {
        // Get the authenticated user's permissions
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findById(username)
                .orElseThrow(() -> new RuntimeException("Authenticated user not found"));

        Set<Permission> userPermissions = currentUser.getRole().getPermissions();

        // Handle role assignment (only existing roles allowed)
        Role roleToAssign = user.getRole();
        if (roleToAssign != null) {
            // Check if role exists
            Role existingRole = roleRepository.findById(roleToAssign.getName())
                    .orElseThrow(() -> new IllegalArgumentException("Role '" + roleToAssign.getName() + "' does not exist. Roles must be created through RoleController first."));

            // Check if the user has permission to assign all the permissions in the role
            for (Permission permission : existingRole.getPermissions()) {
                if (!userPermissions.contains(permission)) {
                    throw new SecurityException("Cannot assign role with permission " + permission + " that user does not have");
                }
            }

            // Set the role reference to the existing role
            user.setRole(existingRole);
        }

        // Encode password before saving
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        User savedUser = userRepository.save(user);

        return savedUser;
    }

    public List<User> getAll() {
        return userRepository.findAll();
    }



    public void delete(String username) {
        userRepository.deleteById(username);
    }

    @Transactional
    public User updateUserRole(String username, Role role) {
        // Get current authenticated user for permission checking
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findById(currentUsername)
                .orElseThrow(() -> new RuntimeException("Authenticated user not found"));

        Set<Permission> userPermissions = currentUser.getRole().getPermissions();

        // Find the target user
        User user = userRepository.findById(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));

        // Check if the role already exists (only existing roles allowed)
        Role existingRole = roleRepository.findById(role.getName())
                .orElseThrow(() -> new IllegalArgumentException("Role '" + role.getName() + "' does not exist. Roles must be created through RoleController first."));

        // Check if current user has permissions to assign all permissions in the existing role
        for (Permission permission : existingRole.getPermissions()) {
            if (!userPermissions.contains(permission)) {
                throw new SecurityException("Cannot assign existing role with permission " +
                        permission + " that you do not have");
            }
        }

        // Store old role name for logging
        String oldRoleName = user.getRole() != null ? user.getRole().getName() : "none";

        // Update the role
        user.setRole(existingRole);
        User updatedUser = userRepository.save(user);

        logger.info("Updated role for user {} from {} to {} with permissions: {}", 
                username, oldRoleName, existingRole.getName(), existingRole.getPermissions());
        return updatedUser;
    }
}
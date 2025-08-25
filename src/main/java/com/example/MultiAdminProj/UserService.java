package com.example.MultiAdminProj;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
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
        System.out.println("Authenticated user: " + username + ", School ID: " + currentUser.getSchool().getId());
        System.out.println("User school ID: " + user.getSchool().getId() + ", Role school ID: " + user.getRole().getSchool().getId());

        Role roleToAssign = user.getRole();
        if (roleToAssign != null) {
            if ("STUDENT".equalsIgnoreCase(roleToAssign.getName())) {
                throw new IllegalArgumentException("Cannot create student user via this endpoint. Use /students/register or /students/add.");
            }

            // Ensure the role belongs to the same school as the user
            if (!roleToAssign.getSchool().getId().equals(user.getSchool().getId())) {
                throw new IllegalArgumentException("Role must belong to the same school as the user");
            }

            // Fetch the existing role from the database
            Role existingRole = roleRepository.findByNameAndSchool(roleToAssign.getName(), user.getSchool())
                    .orElseThrow(() -> new IllegalArgumentException("Role not found: " + roleToAssign.getName()));

            // Skip permission check for admins (hierarchyLevel=0)
            if (currentUser.getHierarchyLevel() > 0) {
                // Check permissions of the existing role for non-admins
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
            }

            user.setRole(existingRole);
        }

        // Set hierarchy level: creator's level + 1
        user.setHierarchyLevel(currentUser.getHierarchyLevel() + 1);
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return userRepository.save(user);
    }

    public List<User> getAll() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findById(username)
                .orElseThrow(() -> new RuntimeException("Authenticated user not found"));

        List<String> rolesToExclude = "ADMIN".equalsIgnoreCase(currentUser.getRole().getName())
                ? Arrays.asList("STUDENT")
                : Arrays.asList("STUDENT", "ADMIN");

        return userRepository.findBySchoolAndRoleNameNotIn(currentUser.getSchool(), rolesToExclude);
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

        if (user.getHierarchyLevel() <= currentUser.getHierarchyLevel()) {
            throw new SecurityException("Cannot delete user with same or lower hierarchy level");
        }

        userRepository.deleteById(username);
    }

    @Transactional
    public User updateUserRole(String username, Role role) {
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findById(currentUsername)
                .orElseThrow(() -> new RuntimeException("Authenticated user not found"));

        User user = userRepository.findById(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));

        // Check hierarchy level: cannot update self or users with same/lower hierarchy level
        if (user.getHierarchyLevel() <= currentUser.getHierarchyLevel()) {
            throw new SecurityException("Cannot update user with same or lower hierarchy level");
        }

        if ("STUDENT".equalsIgnoreCase(role.getName())) {
            throw new IllegalArgumentException("Cannot assign STUDENT role via this endpoint. Use /students/register or /students/add.");
        }

        // Ensure the role belongs to the same school as the user
        if (!role.getSchool().getId().equals(user.getSchool().getId())) {
            throw new IllegalArgumentException("Role must belong to the same school as the user");
        }

        Role existingRole = roleRepository.findByNameAndSchool(role.getName(), user.getSchool())
                .orElseThrow(() -> new IllegalArgumentException("Role not found: " + role.getName()));

        // Prevent assigning a role that would imply a hierarchy level <= current user's
        // Skip for admins (hierarchyLevel=0)
        if (currentUser.getHierarchyLevel() > 0 && "ADMIN".equalsIgnoreCase(existingRole.getName())) {
            throw new SecurityException("Cannot assign ADMIN role as it requires hierarchy level 0");
        }

        // Skip permission check for admins (hierarchyLevel=0)
        if (currentUser.getHierarchyLevel() > 0) {
            Set<Permission> userPermissions = currentUser.getRole().getPermissions();
            for (Permission permission : existingRole.getPermissions()) {
                if (!userPermissions.contains(permission)) {
                    throw new SecurityException("Cannot assign existing role with permission " + permission + " that you do not have");
                }
            }
        }

        user.setRole(existingRole);
        return userRepository.save(user);
    }

    public User updateOwnProfile(UpdateUserProfileRequest request) {
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findById(currentUsername)
                .orElseThrow(() -> new RuntimeException("Authenticated user not found"));

        // Update username if provided, not blank, and different
        if (request.getUsername() != null && !request.getUsername().isBlank()
                && !request.getUsername().equals(currentUsername)) {
            if (userRepository.existsById(request.getUsername())) {
                throw new IllegalArgumentException("Username already taken.");
            }
            userRepository.deleteById(currentUsername); // Remove old record (PK change)
            user.setUsername(request.getUsername());
        }

        // Update email if provided and not blank
        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            user.setEmail(request.getEmail());
        }

        // Update password if provided and not blank
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        // Save user (with possible new username)
        return userRepository.save(user);
    }
}
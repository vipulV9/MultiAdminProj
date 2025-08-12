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

            // Check permissions of the existing role
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

        // Return only users from the same school
        return userRepository.findBySchool(currentUser.getSchool());
    }

    public void delete(String username) {
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findById(currentUsername)
                .orElseThrow(() -> new RuntimeException("Authenticated user not found"));

        User user = userRepository.findById(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));

        // Ensure the user belongs to the same school
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

        Set<Permission> userPermissions = currentUser.getRole().getPermissions();

        User user = userRepository.findById(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));

        if ("STUDENT".equalsIgnoreCase(role.getName())) {
            throw new IllegalArgumentException("Cannot assign STUDENT role via this endpoint. Use /students/register or /students/add.");
        }

        // Ensure the role belongs to the same school as the user
        if (!role.getSchool().equals(user.getSchool())) {
            throw new IllegalArgumentException("Role must belong to the same school as the user");
        }

        Role existingRole = roleRepository.findByNameAndSchool(role.getName(), user.getSchool())
                .orElseThrow(() -> new IllegalArgumentException("Role not found: " + role.getName()));
        for (Permission permission : existingRole.getPermissions()) {
            if (!userPermissions.contains(permission)) {
                throw new SecurityException("Cannot assign existing role with permission " + permission + " that you do not have");
            }
        }

        user.setRole(existingRole);
        return userRepository.save(user);
    }
}

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
    private StudentRepository studentRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    public User saveUser(User user) {
        // Get the authenticated user's permissions
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findById(username)
                .orElseThrow(() -> new RuntimeException("Authenticated user not found"));

        Set<Permission> userPermissions = currentUser.getRole().getPermissions();

        // Handle role creation/assignment
        Role roleToAssign = user.getRole();
        if (roleToAssign != null) {
            // Check if the user has permission to assign all the permissions in the role
            for (Permission permission : roleToAssign.getPermissions()) {
                if (!userPermissions.contains(permission)) {
                    throw new SecurityException("Cannot assign role with permission " + permission + " that user does not have");
                }
            }


            // Check if role already exists, if not create it
            Role existingRole = roleRepository.findById(roleToAssign.getName()).orElse(null);

            if (existingRole == null) {
                // Role doesn't exist, create it
                existingRole = roleRepository.save(roleToAssign);
                System.out.println("Created new role: " + existingRole.getName());
            } else {
                // Role exists, use the existing one
                System.out.println("Using existing role: " + existingRole.getName());
            }

            // Set the role reference to the saved/existing role
            user.setRole(existingRole);
        }

        // Encode password before saving
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        User savedUser = userRepository.save(user);

        // If the user has a student role, create a corresponding Student record
        if (roleToAssign != null && "STUDENT".equalsIgnoreCase(roleToAssign.getName())) {
            Student student = new Student();
            student.setRollNo(user.getUsername()); // Use username as roll number
            student.setName(user.getUsername());// Default name, can be updated later
            student.setAttendance("0");
            studentRepository.save(student);
        }

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

        // Check if the role already exists
        Role existingRole = roleRepository.findById(role.getName()).orElse(null);

        if (existingRole == null) {
            // Role doesn't exist, create it with the provided permissions
            // Check if current user has permissions to assign all permissions in the new role
            for (Permission permission : role.getPermissions()) {
                if (!userPermissions.contains(permission)) {
                    throw new SecurityException("Cannot create role with permission " + permission + " that you do not have");
                }
            }

            existingRole = roleRepository.save(role);
            System.out.println("Created new role: " + existingRole.getName());
        } else {
            // Role exists, use its existing permissions
            System.out.println("Using existing role: " + existingRole.getName() +
                    " with its existing permissions: " + existingRole.getPermissions());

            // Check if current user has permissions to assign all permissions in the existing role
            for (Permission permission : existingRole.getPermissions()) {
                if (!userPermissions.contains(permission)) {
                    throw new SecurityException("Cannot assign existing role with permission " +
                            permission + " that you do not have");
                }
            }
        }

        // Store old role name for logging
        String oldRoleName = user.getRole() != null ? user.getRole().getName() : "none";

        // Update the role
        user.setRole(existingRole);
        User updatedUser = userRepository.save(user);

        // If new role is student and old role wasn't, create student record
        if ("student".equalsIgnoreCase(existingRole.getName()) &&
                !"student".equalsIgnoreCase(oldRoleName)) {

            if (!studentRepository.existsById(username)) {
                Student newStudent = new Student();
                newStudent.setRollNo(username);
                newStudent.setName(username);
                studentRepository.save(newStudent);
                System.out.println("Created corresponding Student record for user: " + username);
            }
        }

        System.out.println("Updated role for user " + username + " from " + oldRoleName +
                " to " + existingRole.getName() + " with permissions: " + existingRole.getPermissions());
        return updatedUser;
    }


    @Transactional
    public void resetPassword(String username, String email, String newPassword) {
        User user = userRepository.findById(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Verify email matches
        if (!user.getEmail().equals(email)) {
            throw new RuntimeException("Email does not match records for this username");
        }

        // Set new password
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // Send email notification
        String subject = "Password Reset Confirmation";
        String body = "Hello " + username + ",\n\n" +
                "Your password has been reset successfully.\n\n" +
                "If you did not request this change, please contact support immediately.\n\n" +
                "Regards,\nTeam";

        emailService.sendEmail(email, subject, body);
    }
}
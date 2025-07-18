package com.example.MultiAdminProj;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

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
}
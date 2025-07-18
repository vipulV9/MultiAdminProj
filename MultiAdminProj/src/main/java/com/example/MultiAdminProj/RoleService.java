package com.example.MultiAdminProj;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
public class RoleService {
    @Autowired
    private RoleRepository roleRepo;

    @Autowired
    private UserRepository userRepository;

    public Role saveRole(Role role) {
        // Get the authenticated user's permissions
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findById(username)
                .orElseThrow(() -> new RuntimeException("Authenticated user not found"));

        Set<Permission> userPermissions = currentUser.getRole().getPermissions();

        // Check if the user has all the permissions they are trying to assign
        for (Permission permission : role.getPermissions()) {
            if (!userPermissions.contains(permission)) {
                throw new SecurityException("Cannot assign permission " + permission + " that user does not have");
            }
        }

        return roleRepo.save(role);
    }

    public List<Role> getAll() {
        return roleRepo.findAll();
    }

    public void delete(String name) {
        roleRepo.deleteById(name); // Changed from Integer.valueOf(name)
    }
}
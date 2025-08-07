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

    @Autowired
    private SchoolRepository schoolRepository;

    public Role saveRole(Role role) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findById(username)
                .orElseThrow(() -> new RuntimeException("Authenticated user not found"));

        // Ensure the role is associated with a school
        if (role.getSchool() == null || role.getSchool().getId() == null) {
            throw new IllegalArgumentException("Role must be associated with a school");
        }

        // Verify the school exists
        School school = schoolRepository.findById(role.getSchool().getId())
                .orElseThrow(() -> new IllegalArgumentException("School not found: " + role.getSchool().getId()));

        // Ensure the current user belongs to the same school as the role
        if (!school.equals(currentUser.getSchool())) {
            throw new SecurityException("Cannot create role for a different school");
        }

        // Check for existing role with the same name and school
        if (roleRepo.findByNameAndSchool(role.getName(), school).isPresent()) {
            throw new IllegalArgumentException("Role with name " + role.getName() + " already exists for this school");
        }

        Set<Permission> userPermissions = currentUser.getRole().getPermissions();

        for (Permission permission : role.getPermissions()) {
            if (!userPermissions.contains(permission)) {
                throw new SecurityException("Cannot assign permission " + permission + " that user does not have");
            }
        }

        // Set the school for the role
        role.setSchool(school);
        return roleRepo.save(role);
    }

    public Role updateRole(String roleName, Role updatedRole) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findById(username)
                .orElseThrow(() -> new RuntimeException("Authenticated user not found"));

        Role existingRole = roleRepo.findByNameAndSchool(roleName, currentUser.getSchool())
                .orElseThrow(() -> new IllegalArgumentException("Role not found: " + roleName));

        // Ensure user is updating a role within their own school
        if (!existingRole.getSchool().equals(currentUser.getSchool())) {
            throw new SecurityException("Cannot update role from a different school");
        }

        // Check if user has ROLE_UPDATE permission
        if (!currentUser.getRole().getPermissions().contains(Permission.ROLE_UPDATE)) {
            throw new SecurityException("You do not have permission to update roles");
        }

        // Ensure the user is not assigning permissions they don't have
        Set<Permission> userPermissions = currentUser.getRole().getPermissions();
        for (Permission permission : updatedRole.getPermissions()) {
            if (!userPermissions.contains(permission)) {
                throw new SecurityException("Cannot assign permission " + permission + " that user does not have");
            }
        }

        // Update the existing role
        existingRole.setPermissions(updatedRole.getPermissions());
        return roleRepo.save(existingRole);
    }


    public List<Role> getAll() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findById(username)
                .orElseThrow(() -> new RuntimeException("Authenticated user not found"));

        // Return only roles associated with the user's school
        return roleRepo.findBySchool(currentUser.getSchool());
    }

    public void delete(String name) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findById(username)
                .orElseThrow(() -> new RuntimeException("Authenticated user not found"));

        Role role = roleRepo.findByNameAndSchool(name, currentUser.getSchool())
                .orElseThrow(() -> new IllegalArgumentException("Role not found: " + name));

        // Ensure the role belongs to the user's school (redundant with findByNameAndSchool, but kept for clarity)
        if (!role.getSchool().equals(currentUser.getSchool())) {
            throw new SecurityException("Cannot delete role from a different school");
        }

        roleRepo.deleteById(role.getId());
    }
}

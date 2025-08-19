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

        if (role.getSchool() == null || role.getSchool().getId() == null) {
            throw new IllegalArgumentException("Role must be associated with a school");
        }

        School school = schoolRepository.findById(role.getSchool().getId())
                .orElseThrow(() -> new IllegalArgumentException("School not found: " + role.getSchool().getId()));

        if (!school.equals(currentUser.getSchool())) {
            throw new SecurityException("Cannot create role for a different school");
        }

        if (role.getLevel() > currentUser.getRole().getLevel()) {
            throw new SecurityException("Cannot create a role with a higher privilege level");
        }

        if (roleRepo.findByNameAndSchool(role.getName(), school).isPresent()) {
            throw new IllegalArgumentException("Role with name " + role.getName() + " already exists for this school");
        }

        Set<Permission> userPermissions = currentUser.getRole().getPermissions();
        for (Permission permission : role.getPermissions()) {
            if (!userPermissions.contains(permission)) {
                throw new SecurityException("Cannot assign permission " + permission + " that user does not have");
            }
        }

        role.setSchool(school);
        return roleRepo.save(role);
    }

    public Role updateRole(String roleName, Role updatedRole) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findById(username)
                .orElseThrow(() -> new RuntimeException("Authenticated user not found"));

        Role existingRole = roleRepo.findByNameAndSchool(roleName, currentUser.getSchool())
                .orElseThrow(() -> new IllegalArgumentException("Role not found: " + roleName));

        if (!existingRole.getSchool().equals(currentUser.getSchool())) {
            throw new SecurityException("Cannot update role from a different school");
        }

        if (updatedRole.getLevel() > currentUser.getRole().getLevel()) {
            throw new SecurityException("Cannot update role to a higher privilege level");
        }

        if (existingRole.getLevel() > currentUser.getRole().getLevel()) {
            throw new SecurityException("Cannot update a role with a higher privilege level");
        }

        if (!currentUser.getRole().getPermissions().contains(Permission.ROLE_CREATE)) {
            throw new SecurityException("You do not have permission to update roles");
        }

        Set<Permission> userPermissions = currentUser.getRole().getPermissions();
        for (Permission permission : updatedRole.getPermissions()) {
            if (!userPermissions.contains(permission)) {
                throw new SecurityException("Cannot assign permission " + permission + " that user does not have");
            }
        }

        existingRole.setPermissions(updatedRole.getPermissions());
        existingRole.setLevel(updatedRole.getLevel()); // Update level if provided
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

package com.example.MultiAdminProj;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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
                throw new SecurityException("Cannot assign permission" + permission + " that user does not have");
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

        if (!currentUser.getRole().getPermissions().contains(Permission.ROLE_CREATE)) {
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

        // Get all roles associated with the user's school
        List<Role> roles = roleRepo.findBySchool(currentUser.getSchool());

        // Filter roles based on user role
        String userRoleName = currentUser.getRole().getName();
        boolean isAdmin = "ADMIN".equalsIgnoreCase(userRoleName);

        roles = roles.stream()
                .filter(role -> !"STUDENT".equalsIgnoreCase(role.getName())) // Always exclude STUDENT
                .filter(role -> isAdmin || !"ADMIN".equalsIgnoreCase(role.getName())) // Exclude ADMIN for non-admins
                .filter(role -> !userRoleName.equalsIgnoreCase(role.getName())) // Exclude the user's own role
                .collect(Collectors.toList());

        return roles;
    }

    @Transactional
    public void delete(String name) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findById(username)
                .orElseThrow(() -> new RuntimeException("Authenticated user not found"));

        Role role = roleRepo.findByNameAndSchool(name, currentUser.getSchool())
                .orElseThrow(() -> new IllegalArgumentException("Role not found: " + name));

        // Prevent deletion of critical roles
        if ("ADMIN".equalsIgnoreCase(name) || "STUDENT".equalsIgnoreCase(name)) {
            throw new SecurityException("Cannot delete critical role: " + name);
        }

        // Ensure the role belongs to the user's school
        if (!role.getSchool().equals(currentUser.getSchool())) {
            throw new SecurityException("Cannot delete role from a different school");
        }

        // Check if the role is assigned to any users
        List<User> usersWithRole = userRepository.findByRole(role);
        if (!usersWithRole.isEmpty()) {
            // Check if any user with this role has equal or higher privilege
            for (User user : usersWithRole) {
                if (user.getHierarchyLevel() <= currentUser.getHierarchyLevel()) {
                    throw new SecurityException("Cannot delete role assigned to users with equal or higher privilege (hierarchy level: " + user.getHierarchyLevel() + ")");
                }
            }
            throw new IllegalStateException("Cannot delete role '" + name + "' because it is assigned to " + usersWithRole.size() + " user(s)");
        }

        // Log the deletion attempt
        System.out.println("User " + username + " deleted role " + name + " in school " + currentUser.getSchool().getId());

        roleRepo.deleteById(role.getId());
    }
}

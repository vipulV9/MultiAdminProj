package com.example.MultiAdminProj;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/roles")
public class RoleController {
    @Autowired
    private RoleService roleService;

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_CREATE')")
    public Role create(@Valid @RequestBody Role role) {
        return roleService.saveRole(role);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_READ')")
    public List<Role> getAll() {
        return roleService.getAll();
    }

    @PutMapping("/{name}")
    @PreAuthorize("hasAuthority('ROLE_UPDATE')")
    public Role update(@PathVariable String name, @Valid @RequestBody Role role) {
        role.setName(name); // Ensure the name matches the path variable
        return roleService.saveRole(role);
    }

    @DeleteMapping("/{name}")
    @PreAuthorize("hasAuthority('ROLE_DELETE')")
    public void delete(@PathVariable String name) {
        roleService.delete(name);
    }
}
package com.example.MultiAdminProj;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/roles")
public class RoleController {
    @Autowired
    private RoleService roleService;

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_CREATE')")
    public Role create(@RequestBody Role role) {
        return roleService.saveRole(role);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_READ')")
    public List<Role> getAll() {
        return roleService.getAll();
    }

    @DeleteMapping("/{name}")
    @PreAuthorize("hasAuthority('ROLE_DELETE')")
    public void delete(@PathVariable String name) {
        roleService.delete(name);
    }
}
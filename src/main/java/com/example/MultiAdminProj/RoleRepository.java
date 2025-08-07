package com.example.MultiAdminProj;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByNameAndSchool(String name, School school);
    List<Role> findBySchool(School school);
}
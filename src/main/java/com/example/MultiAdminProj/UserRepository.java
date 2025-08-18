package com.example.MultiAdminProj;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;


public interface UserRepository extends JpaRepository<User, String> {
    List<User> findBySchool(School school);
    boolean existsByEmail(String email);


    // New method to exclude users with a specific role name
    @Query("SELECT u FROM User u WHERE u.school = :school AND u.role.name != :roleName")
    List<User> findBySchoolAndRoleNameNot(School school, String roleName);
}
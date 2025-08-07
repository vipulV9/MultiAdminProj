package com.example.MultiAdminProj;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;


public interface UserRepository extends JpaRepository<User, String> {
    List<User> findBySchool(School school);
}
package com.example.MultiAdminProj;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StudentRepository extends JpaRepository<Student, String> {
    List<Student> findByApprovalStatus(String approvalStatus);
    List<Student> findBySchool(School school);
    List<Student> findByApprovalStatusAndSchool(String approvalStatus, School school);
}
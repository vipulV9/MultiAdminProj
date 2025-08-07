package com.example.MultiAdminProj;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface StudentRepository extends JpaRepository<Student, String> {
    List<Student> findByApprovalStatus(String approvalStatus);
    List<Student> findBySchool(School school);
    List<Student> findByApprovalStatusAndSchool(String approvalStatus, School school);
    @Query("SELECT s FROM Student s WHERE s.approvalStatus = 'APPROVED' AND s.school = :school")
    List<Student> findAllApprovedBySchool(School school);
}
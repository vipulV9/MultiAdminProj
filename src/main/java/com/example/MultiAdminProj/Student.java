package com.example.MultiAdminProj;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Student {
    @Id
    private String rollNo;

    private String name;
    private String email;
    private String classGrade;
    private String attendance;

    @ManyToOne
    private School school;

    private String approvalStatus;
}

package com.example.MultiAdminProj;

import jakarta.persistence.Column;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StudentRegistrationRequest {
    private String name;
    @Column(unique = true)
    private String email;
    private String classGrade;
    private Long schoolId;
}

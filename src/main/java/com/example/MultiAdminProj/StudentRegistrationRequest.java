package com.example.MultiAdminProj;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StudentRegistrationRequest {
    private String name;
    private String email;
    private String classGrade;
    private Long schoolId;
}
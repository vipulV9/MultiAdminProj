package com.example.MultiAdminProj;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Min;
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
    @NotBlank(message = "Roll number is required")
    private String rollNo;
    
    @NotBlank(message = "Name is required")
    private String name;
    
    @Min(value = 0, message = "Attendance must be non-negative")
    private Integer attendance = 0;
}

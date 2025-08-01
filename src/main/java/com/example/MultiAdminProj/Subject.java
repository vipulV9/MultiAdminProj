package com.example.MultiAdminProj;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Subject {
    @Id
    @NotBlank(message = "Subject code is required")
    private String code;
    
    @NotBlank(message = "Subject name is required")
    private String name;
}

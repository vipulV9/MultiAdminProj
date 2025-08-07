package com.example.MultiAdminProj;

import jakarta.persistence.*;
import lombok.*;

import java.security.PrivateKey;
import java.util.Set;




@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;  // Can be 'ADMIN', 'STUDENT', etc.

    @ElementCollection(fetch = FetchType.EAGER)
    @Enumerated(EnumType.STRING)
    private Set<Permission> permissions;

    @ManyToOne
    private School school;
}

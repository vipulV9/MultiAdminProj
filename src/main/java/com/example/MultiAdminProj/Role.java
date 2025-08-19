package com.example.MultiAdminProj;

import jakarta.persistence.*;
import lombok.*;

import java.security.PrivateKey;
import java.time.LocalDateTime;
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

    @Column(nullable = false, columnDefinition = "INT DEFAULT 0")
    private int level;

    @ElementCollection(fetch = FetchType.EAGER)
    @Enumerated(EnumType.STRING)
    private Set<Permission> permissions;

    @ManyToOne
    private School school;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}

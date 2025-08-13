package com.example.MultiAdminProj;

import lombok.Data;

@Data
public class UpdateUserProfileRequest {
    private String username;
    private String email;
    private String password;
}
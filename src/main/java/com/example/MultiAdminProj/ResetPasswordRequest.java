package com.example.MultiAdminProj;

import lombok.Data;

@Data
public class ResetPasswordRequest {
    private String username;
    private String email;
    private String newPassword;
}

package com.example.MultiAdminProj;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/users")
public class UserController {
    @Autowired
    private UserService userService;

    @Autowired
    private EmailService emailService;

    @PostMapping
    @PreAuthorize("hasAuthority('ADMIN_CREATE')")
    public User create(@Valid @RequestBody User user) {
        User savedUser = userService.saveUser(user);

        // Send email after successful creation (without password for security)
        String subject = "Account Created Successfully";
        String body = "Hello " + user.getUsername() + ",\n\nYour account has been created.\n" +
                "Username: " + user.getUsername() + "\n\n" +
                "Please contact your administrator for your password.\n\nRegards,\nTeam";

        emailService.sendEmail(user.getEmail(), subject, body);
        return savedUser;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ADMIN_READ')")
    public List<User> getAll() {
        return userService.getAll();
    }

    @DeleteMapping("/{username}")
    @PreAuthorize("hasAuthority('ADMIN_DELETE')")
    public void delete(@PathVariable String username) {
        userService.delete(username);
    }


    @PutMapping("/{username}")
    @PreAuthorize("hasAuthority('ADMIN_UPDATE')")
    public User updateUserRole(@Valid @RequestBody Role role, @PathVariable String username) {
        return userService.updateUserRole(username, role);
    }
}

package com.example.MultiAdminProj;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

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
    public User create(@RequestBody User user) {
        String rawPassword = user.getPassword();
        User savedUser = userService.saveUser(user);

        // Send email after successful creation
        String subject = "Account Created Successfully";
        String body = "Hello " + user.getUsername() + ",\n\nYour account has been created.\n" +
                "Username: " + user.getUsername() + "\nPassword: " + rawPassword + "\n\n" +
                "Please keep this information safe.\n\nRegards,\nTeam";

        emailService.sendEmail("vipulyadav2k@gmail.com", subject, body);

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
}
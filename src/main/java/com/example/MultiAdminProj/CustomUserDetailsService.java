package com.example.MultiAdminProj;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findById(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        if ("STUDENT".equalsIgnoreCase(user.getRole().getName())) {
            Student student = studentRepository.findById(username)
                    .orElseThrow(() -> new UsernameNotFoundException("Student not found: " + username));
            if (!"APPROVED".equals(student.getApprovalStatus())) {
                throw new UsernameNotFoundException("Student not approved: " + username);
            }
        }

        Long schoolId = user.getSchool() != null ? user.getSchool().getId() : null;

        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                user.getRole().getPermissions().stream()
                        .map(permission -> new SimpleGrantedAuthority(permission.name()))
                        .collect(Collectors.toList())
        ) {
            public Long getSchoolId() {
                return schoolId;
            }
        };
    }
}
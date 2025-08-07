package com.example.MultiAdminProj;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/students")
public class StudentController {
    @Autowired
    private StudentService studentService;

    @PostMapping("/register")
    public ResponseEntity<String> registerStudent(@RequestBody StudentRegistrationRequest request) {
        String username = studentService.registerStudent(request);
        return ResponseEntity.ok("Registration successful for " + username + ". Awaiting approval.");
    }

    @PostMapping("/add")
    @PreAuthorize("hasAuthority('STUDENT_CREATE')")
    public ResponseEntity<Student> addStudent(@RequestBody StudentRegistrationRequest request) {
        Student createdStudent = studentService.addStudent(request);
        return ResponseEntity.ok(createdStudent);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('STUDENT_READ')")
    public List<Student> getAll() {
        return studentService.getAll();
    }

    @GetMapping("/pending")
    @PreAuthorize("hasAuthority('STUDENT_READ')")
    public List<Student> getPendingStudents() {
        return studentService.getPendingStudents();
    }

    @PostMapping("/approve/{rollNo}")
    @PreAuthorize("hasAuthority('STUDENT_APPROVE')")
    public Student approveStudent(@PathVariable String rollNo) {
        return studentService.approveStudent(rollNo);
    }

    @PostMapping("/reject/{rollNo}")
    @PreAuthorize("hasAuthority('STUDENT_REJECT')")
    public Student rejectStudent(@PathVariable String rollNo) {
        return studentService.rejectStudent(rollNo);
    }

    @DeleteMapping("/{rollNo}")
    @PreAuthorize("hasAuthority('STUDENT_DELETE')")
    public void delete(@PathVariable String rollNo) {
        studentService.delete(rollNo);
    }

    @PutMapping("/{rollNo}")
    @PreAuthorize("hasAuthority('STUDENT_UPDATE')")
    public Student update(@PathVariable String rollNo, @RequestBody Student student) {
        return studentService.update(rollNo, student);
    }

    @GetMapping("/me")
    @PreAuthorize("hasAuthority('STUDENT_READ_OWN')")
    public Student getOwnProfile() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return studentService.findByRollNo(username);
    }

    @GetMapping("/me/attendance")
    @PreAuthorize("hasAuthority('STUDENT_VIEW_ATTENDANCE')")
    public String getOwnAttendance() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        Student student = studentService.findByRollNo(username);
        return student.getAttendance();
    }
}
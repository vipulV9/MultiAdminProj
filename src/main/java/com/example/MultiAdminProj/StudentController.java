package com.example.MultiAdminProj;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/students")
public class StudentController {
    @Autowired
    private StudentService studentService;


    @PostMapping
    @PreAuthorize("hasAuthority('STUDENT_CREATE')")
    public Student create(@Valid @RequestBody Student s) {
        return studentService.save(s);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('STUDENT_READ')")
    public List<Student> getAll() {
        return studentService.getAll();
    }

    @PutMapping("/{rollNo}")
    @PreAuthorize("hasAuthority('STUDENT_UPDATE')")
    public Student update(@PathVariable String rollNo, @Valid @RequestBody Student student) {
        student.setRollNo(rollNo); // Ensure the roll number matches the path variable
        return studentService.save(student);
    }

    @DeleteMapping("/{rollNo}")
    @PreAuthorize("hasAuthority('STUDENT_DELETE')")
    public void delete(@PathVariable String rollNo) {
        studentService.delete(rollNo);
    }
}
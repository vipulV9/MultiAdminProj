package com.example.MultiAdminProj;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/students")
public class StudentController {
    @Autowired
    private StudentService studentService;


    @PostMapping
    @PreAuthorize("hasAuthority('STUDENT_CREATE')")
    public Student create(@RequestBody Student s) {
        return studentService.save(s);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('STUDENT_READ')")
    public List<Student> getAll() {
        return studentService.getAll();
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

    @PutMapping("/{rollNo}/change-rollno")
    @PreAuthorize("hasAuthority('STUDENT_UPDATE')")
    public Student changeRollNo(@PathVariable String rollNo, @RequestBody Student student) {
        return studentService.changeRollNo(rollNo, student);
    }
}
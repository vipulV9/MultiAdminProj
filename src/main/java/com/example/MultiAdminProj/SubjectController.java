package com.example.MultiAdminProj;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/subjects")
public class SubjectController {
    @Autowired
    private SubjectService subjectService;

    @PostMapping
    @PreAuthorize("hasAuthority('SUBJECT_CREATE')")
    public Subject create(@Valid @RequestBody Subject s) {
        return subjectService.save(s);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('SUBJECT_READ')")
    public List<Subject> getAll() {
        return subjectService.getAll();
    }

    @PutMapping("/{code}")
    @PreAuthorize("hasAuthority('SUBJECT_UPDATE')")
    public Subject update(@PathVariable String code, @Valid @RequestBody Subject subject) {
        subject.setCode(code); // Ensure the code matches the path variable
        return subjectService.save(subject);
    }

    @DeleteMapping("/{code}")
    @PreAuthorize("hasAuthority('SUBJECT_DELETE')")
    public void delete(@PathVariable String code) {
        subjectService.delete(code);
    }
}

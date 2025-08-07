package com.example.MultiAdminProj;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/subjects")
public class SubjectController {
    @Autowired
    private SubjectService subjectService;

    @PostMapping
    @PreAuthorize("hasAuthority('SUBJECT_CREATE')")
    public Subject create(@RequestBody Subject s) {
        return subjectService.save(s);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('SUBJECT_READ')")
    public List<Subject> getAll() {
        return subjectService.getAll();
    }

    @DeleteMapping("/{code}")
    @PreAuthorize("hasAuthority('SUBJECT_DELETE')")
    public void delete(@PathVariable String code) {
        subjectService.delete(code);
    }

    @PutMapping("/{code}")
    @PreAuthorize("hasAuthority('SUBJECT_UPDATE')")
    public Subject update(@PathVariable String code, @RequestBody Subject subject) {
        return subjectService.update(code, subject);
    }

    @PutMapping("/{code}/change-code")
    @PreAuthorize("hasAuthority('SUBJECT_UPDATE')")
    public Subject changeCode(@PathVariable String code, @RequestBody Subject subject) {
        return subjectService.changeCode(code, subject);
    }
}
package com.example.MultiAdminProj;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/schools")
public class SchoolController {
    @Autowired
    private SchoolService schoolService;

    @PostMapping
    @PreAuthorize("hasAuthority('SCHOOL_CREATE')")
    public School create(@RequestBody School school) {
        return schoolService.save(school);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('SCHOOL_READ')")
    public List<School> getAll() {
        return schoolService.getAll();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('SCHOOL_DELETE')")
    public void delete(@PathVariable Long id) {
        schoolService.delete(id);
    }
}
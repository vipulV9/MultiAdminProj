package com.example.MultiAdminProj;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SchoolService {
    @Autowired
    private SchoolRepository schoolRepository;

    public School save(School school) {
        return schoolRepository.save(school);
    }

    public List<School> getAll() {
        return schoolRepository.findAll();
    }

    public void delete(Long id) {
        schoolRepository.deleteById(id);
    }
}
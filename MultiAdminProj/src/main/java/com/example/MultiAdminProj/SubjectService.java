package com.example.MultiAdminProj;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SubjectService {
    @Autowired
    private SubjectRepository subjectRepo;

    public Subject save(Subject s) {
        return subjectRepo.save(s);
    }

    public List<Subject> getAll() {
        return subjectRepo.findAll();
    }

    public void delete(String code) {
        subjectRepo.deleteById(code);
    }
}
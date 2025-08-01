package com.example.MultiAdminProj;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Transactional
    public Subject update(String code, Subject updatedSubject) {
        Subject existingSubject = subjectRepo.findById(code)
                .orElseThrow(() -> new RuntimeException("Subject not found with code: " + code));

        // Only update non-ID fields
        if (updatedSubject.getName() != null) {
            existingSubject.setName(updatedSubject.getName());
        }

        return subjectRepo.save(existingSubject);
    }

    @Transactional
    public Subject changeCode(String oldCode, Subject subject) {
        if (subject.getCode() == null || subject.getCode().isEmpty()) {
            throw new IllegalArgumentException("New subject code cannot be empty");
        }

        Subject existingSubject = subjectRepo.findById(oldCode)
                .orElseThrow(() -> new RuntimeException("Subject not found with code: " + oldCode));

        if (subjectRepo.existsById(subject.getCode())) {
            throw new IllegalArgumentException("Subject already exists with code: " + subject.getCode());
        }

        // Create new subject with new code but keep other data
        Subject newSubject = new Subject();
        newSubject.setCode(subject.getCode());
        newSubject.setName(subject.getName() != null ? subject.getName() : existingSubject.getName());

        // Save new subject
        Subject savedSubject = subjectRepo.save(newSubject);

        // Delete old subject
        subjectRepo.deleteById(oldCode);

        return savedSubject;
    }
}
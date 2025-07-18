package com.example.MultiAdminProj;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StudentService {
    @Autowired
    private StudentRepository studentRepo;

    @Autowired
    private UserRepository userRepository;


    public Student save(Student s) {
        return studentRepo.save(s);
    }

    public List<Student> getAll() {
        return studentRepo.findAll();
    }

    public void delete(String rollNo) {
        studentRepo.deleteById(rollNo);
    }
}

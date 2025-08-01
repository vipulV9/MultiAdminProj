package com.example.MultiAdminProj;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

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

    @Transactional
    public Student update(String rollNo, Student updatedStudent) {
        Student existingStudent = studentRepo.findById(rollNo)
                .orElseThrow(() -> new RuntimeException("Student not found with roll number: " + rollNo));

        // Only update non-ID fields
        if (updatedStudent.getName() != null) {
            existingStudent.setName(updatedStudent.getName());
        }

        if (updatedStudent.getAttendance() != null) {
            existingStudent.setAttendance(updatedStudent.getAttendance());
        }

        return studentRepo.save(existingStudent);
    }

    @Transactional
    public Student changeRollNo(String oldRollNo, Student student) {
        if (student.getRollNo() == null || student.getRollNo().isEmpty()) {
            throw new IllegalArgumentException("New roll number cannot be empty");
        }

        // Check if student exists with the old roll number
        Student existingStudent = studentRepo.findById(oldRollNo)
                .orElseThrow(() -> new RuntimeException("Student not found with roll number: " + oldRollNo));

        // Check if the new roll number is already taken
        if (studentRepo.existsById(student.getRollNo())) {
            throw new IllegalArgumentException("Student already exists with roll number: " + student.getRollNo());
        }

        // Check if there's a corresponding user
        Optional<User> userOptional = userRepository.findById(oldRollNo);

        // Create new student with new roll number but keep other data
        Student newStudent = new Student();
        newStudent.setRollNo(student.getRollNo());
        newStudent.setName(student.getName() != null ? student.getName() : existingStudent.getName());
        newStudent.setAttendance(student.getAttendance() != null ? student.getAttendance() : existingStudent.getAttendance());

        // Save new student
        Student savedStudent = studentRepo.save(newStudent);

        // If there's a corresponding user, update it too
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            // Create a new user with updated username
            User newUser = new User();
            newUser.setUsername(student.getRollNo());
            newUser.setPassword(user.getPassword());
            newUser.setEmail(user.getEmail());
            newUser.setRole(user.getRole());
            newUser.setCreatedAt(user.getCreatedAt()); // Preserve creation timestamp

            // Save the new user
            userRepository.save(newUser);

            // Delete the old user
            userRepository.delete(user);
        }

        // Delete old student record
        studentRepo.deleteById(oldRollNo);

        return savedStudent;
    }
}
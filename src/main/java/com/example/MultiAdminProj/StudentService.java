package com.example.MultiAdminProj;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Random;

@Service
public class StudentService {
    @Autowired
    private StudentRepository studentRepo;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SchoolRepository schoolRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Transactional
    public String registerStudent(StudentRegistrationRequest request) {
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findById(currentUsername)
                .orElseThrow(() -> new RuntimeException("Authenticated user not found"));

        School school = schoolRepository.findById(request.getSchoolId())
                .orElseThrow(() -> new IllegalArgumentException("School not found"));

        if (!school.equals(currentUser.getSchool())) {
            throw new SecurityException("Cannot register student to a different school");
        }

        String rollNo = generateUniqueRollNo(school.getId(), request.getClassGrade());
        String rawPassword = generateRandomPassword();

        User user = new User();
        user.setUsername(rollNo);
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(rawPassword));

        Role studentRole = roleRepository.findByNameAndSchool("STUDENT", school)
                .orElseThrow(() -> new RuntimeException("Student role not found for school: " + school.getName()));
        user.setRole(studentRole);

        user.setSchool(school); // Set school for the user
        Student student = new Student();
        student.setRollNo(rollNo);
        student.setName(request.getName());
        student.setEmail(request.getEmail());
        student.setClassGrade(request.getClassGrade());
        student.setSchool(school);
        student.setAttendance("0");
        student.setApprovalStatus("PENDING");

        userRepository.save(user);
        studentRepo.save(student);

        String subject = "Student Registration - Awaiting Approval";
        String body = String.format("Dear %s,\n\nYour registration is pending approval.\n" +
                        "Username/RollNo: %s\nPassword: %s\nSchool: %s\nClass: %s\n\n" +
                        "You will be notified once your registration is approved.\n\nRegards,\nTeam",
                request.getName(), rollNo, rawPassword, school.getName(), request.getClassGrade());
        emailService.sendEmail(request.getEmail(), subject, body);

        return rollNo;
    }

    @Transactional
    public Student addStudent(StudentRegistrationRequest request) {
        // Get the authenticated user
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findById(currentUsername)
                .orElseThrow(() -> new RuntimeException("Authenticated user not found"));

        // Retrieve the school from the request
        School school = schoolRepository.findById(request.getSchoolId())
                .orElseThrow(() -> new IllegalArgumentException("School not found"));

        // Check if the authenticated user belongs to the same school
        if (!school.equals(currentUser.getSchool())) {
            throw new SecurityException("Cannot add student to a different school");
        }

        // Validate class grade
        if (!school.getAvailableClasses().contains(request.getClassGrade())) {
            throw new IllegalArgumentException("Class " + request.getClassGrade() + " not available in this school");
        }

        // Generate roll number and password
        String rollNo = generateUniqueRollNo(school.getId(), request.getClassGrade());
        String rawPassword = generateRandomPassword();

        // Create User entity
        User user = new User();
        user.setUsername(rollNo);
        user.setSchool(school);
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(rawPassword));

        Role studentRole = roleRepository.findByNameAndSchool("STUDENT", school)
                .orElseThrow(() -> new RuntimeException("Student role not found for school: " + school.getName()));
        user.setRole(studentRole);

        // Create Student entity
        Student student = new Student();
        student.setRollNo(rollNo);
        student.setName(request.getName());
        student.setEmail(request.getEmail());
        student.setClassGrade(request.getClassGrade());
        student.setSchool(school);
        student.setAttendance("0");
        student.setApprovalStatus("APPROVED");

        // Save entities
        userRepository.save(user);
        studentRepo.save(student);

        // Send approval email
        emailService.sendEmail(student.getEmail(),
                "Registration Approved",
                String.format("Dear %s,\n\nYour registration has been approved.\n" +
                                "Username/RollNo: %s\n\nYou can now log in to the system.\n\nRegards,\nTeam",
                        student.getName(), rollNo));

        return student;
    }

    @Transactional
    public Student approveStudent(String rollNo) {
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findById(currentUsername)
                .orElseThrow(() -> new RuntimeException("Authenticated user not found"));

        Student student = studentRepo.findById(rollNo)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        if (!student.getSchool().equals(currentUser.getSchool())) {
            throw new SecurityException("Cannot approve student from a different school");
        }

        student.setApprovalStatus("APPROVED");
        studentRepo.save(student);

        emailService.sendEmail(student.getEmail(),
                "Registration Approved",
                String.format("Dear %s,\n\nYour registration has been approved.\n" +
                                "Username/RollNo: %s\n\nYou can now log in to the system.\n\nRegards,\nTeam",
                        student.getName(), rollNo));

        return student;
    }

    @Transactional
    public Student rejectStudent(String rollNo) {
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findById(currentUsername)
                .orElseThrow(() -> new RuntimeException("Authenticated user not found"));

        Student student = studentRepo.findById(rollNo)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        if (!student.getSchool().equals(currentUser.getSchool())) {
            throw new SecurityException("Cannot reject student from a different school");
        }

        student.setApprovalStatus("REJECTED");
        studentRepo.save(student);

        emailService.sendEmail(student.getEmail(),
                "Registration Rejected",
                String.format("Dear %s,\n\nYour registration has been rejected.\n" +
                                "Please contact support for more information.\n\nRegards,\nTeam",
                        student.getName()));

        return student;
    }


    @Transactional(readOnly = true)
    public List<Student> getAll() {
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findById(currentUsername)
                .orElseThrow(() -> new RuntimeException("Authenticated user not found"));
        return studentRepo.findAllApprovedBySchool(currentUser.getSchool());
    }

    public List<Student> getPendingStudents() {
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findById(currentUsername)
                .orElseThrow(() -> new RuntimeException("Authenticated user not found"));

        return studentRepo.findByApprovalStatusAndSchool("PENDING", currentUser.getSchool());
    }

    public void delete(String rollNo) {
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findById(currentUsername)
                .orElseThrow(() -> new RuntimeException("Authenticated user not found"));

        Student student = studentRepo.findById(rollNo)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        if (!student.getSchool().equals(currentUser.getSchool())) {
            throw new SecurityException("Cannot delete student from a different school");
        }

        studentRepo.deleteById(rollNo);
        userRepository.deleteById(rollNo);
    }

    @Transactional
    public Student update(String rollNo, Student updatedStudent) {
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findById(currentUsername)
                .orElseThrow(() -> new RuntimeException("Authenticated user not found"));

        Student existingStudent = studentRepo.findById(rollNo)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        if (!existingStudent.getSchool().equals(currentUser.getSchool())) {
            throw new SecurityException("Cannot update student from a different school");
        }

        if (updatedStudent.getName() != null) {
            existingStudent.setName(updatedStudent.getName());
        }
        if (updatedStudent.getEmail() != null) {
            existingStudent.setEmail(updatedStudent.getEmail());
            User user = userRepository.findById(rollNo)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            user.setEmail(updatedStudent.getEmail());
            userRepository.save(user);
        }
        if (updatedStudent.getClassGrade() != null) {
            School school = existingStudent.getSchool();
            if (!school.getAvailableClasses().contains(updatedStudent.getClassGrade())) {
                throw new IllegalArgumentException("Invalid class for this school");
            }
            existingStudent.setClassGrade(updatedStudent.getClassGrade());
        }
        if (updatedStudent.getAttendance() != null) {
            existingStudent.setAttendance(updatedStudent.getAttendance());
        }

        return studentRepo.save(existingStudent);
    }

    public Student findByRollNo(String rollNo) {
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findById(currentUsername)
                .orElseThrow(() -> new RuntimeException("Authenticated user not found"));

        Student student = studentRepo.findById(rollNo)
                .orElseThrow(() -> new RuntimeException("Student not found with roll number: " + rollNo));

        if (!student.getSchool().equals(currentUser.getSchool())) {
            throw new SecurityException("Cannot access student from a different school");
        }

        return student;
    }

    private String generateUniqueRollNo(Long schoolId, String classGrade) {
        String prefix = "S" + schoolId + "C" + classGrade;
        Random random = new Random();
        String rollNo;
        do {
            rollNo = prefix + String.format("%04d", random.nextInt(10000));
        } while (studentRepo.existsById(rollNo));
        return rollNo;
    }

    private String generateRandomPassword() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        Random random = new Random();
        StringBuilder password = new StringBuilder();
        for (int i = 0; i < 8; i++) {
            password.append(chars.charAt(random.nextInt(chars.length())));
        }
        return password.toString();
    }
}

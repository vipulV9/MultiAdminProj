package com.example.MultiAdminProj;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import jakarta.persistence.OptimisticLockException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
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
    public String registerStudent(Long schoolId, StudentRegistrationRequest request) {
        School school = schoolRepository.findById(schoolId)
                .orElseThrow(() -> new IllegalArgumentException("School not found with ID: " + schoolId));

        String rollNo = generateUniqueRollNo(school.getId(), request.getClassGrade());
        String rawPassword = generateRandomPassword();

        User user = new User();
        user.setUsername(rollNo);
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setHierarchyLevel(1000); // Set hierarchyLevel for student

        Role studentRole = roleRepository.findByNameAndSchool("STUDENT", school)
                .orElseThrow(() -> new RuntimeException("Student role not found for school: " + school.getName()));
        user.setRole(studentRole);

        user.setSchool(school);
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
                        "You will be notified once your registration is approved.\n\nRegards,\nTeam",
                request.getName());
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
            throw new IllegalArgumentException("Class " + request.getClassGrade() + " not available in this school. Valid classes: " + school.getAvailableClasses());
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
        user.setHierarchyLevel(1000); // Set hierarchyLevel for student

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
                                "Username/RollNo: %s\nPassword: %s\nSchool: %s\nClass: %s\n\n" +
                                "You can now log in to the system.\n\nRegards,\nTeam",
                        student.getName(), rollNo, rawPassword, school.getName(), request.getClassGrade()));

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

        try {
            // Generate a new password
            String rawPassword = generateRandomPassword();

            // Update the User entity with the new password
            User user = userRepository.findById(rollNo)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            user.setPassword(passwordEncoder.encode(rawPassword));
            userRepository.save(user);

            // Update student approval status
            student.setApprovalStatus("APPROVED");
            studentRepo.save(student);

            // Send approval email with the new password
            emailService.sendEmail(student.getEmail(),
                    "Registration Approved",
                    String.format("Dear %s,\n\nYour registration has been approved.\n" +
                                    "Username/RollNo: %s\nPassword: %s\n\n" +
                                    "You can now log in to the system.\n\nRegards,\nTeam",
                            student.getName(), rollNo, rawPassword));

            return student;
        } catch (OptimisticLockException e) {
            throw new RuntimeException("Failed to approve student due to concurrent modification. Please try again.");
        }
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

        try {
            student.setApprovalStatus("REJECTED");
            studentRepo.save(student);
            emailService.sendEmail(student.getEmail(),
                    "Registration Rejected",
                    String.format("Dear %s,\n\nYour registration has been rejected.\n" +
                                    "Please contact support for more information.\n\nRegards,\nTeam",
                            student.getName()));
            return student;
        } catch (OptimisticLockException e) {
            throw new RuntimeException("Failed to reject student due to concurrent modification. Please try again.");
        }
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


    @Transactional
    public List<BulkUploadResult.UploadRecord> bulkUploadStudents(Long schoolId, MultipartFile file) throws Exception {
        School school = schoolRepository.findById(schoolId)
                .orElseThrow(() -> new IllegalArgumentException("School not found with ID: " + schoolId));

        List<User> users = new ArrayList<>();
        List<Student> students = new ArrayList<>();
        List<BulkUploadResult.UploadRecord> results = new ArrayList<>();

        try (CSVReader csvReader = new CSVReader(new BufferedReader(new InputStreamReader(file.getInputStream())))) {
            String[] headers = csvReader.readNext(); // Read header row
            if (headers == null || headers.length < 3 || !Arrays.equals(headers, new String[]{"name", "email", "classGrade"})) {
                throw new IllegalArgumentException("CSV file must contain headers: name,email,classGrade");
            }

            String[] row;
            int rowNum = 1; // Track row number for error reporting
            while ((row = csvReader.readNext()) != null) {
                rowNum++;
                BulkUploadResult.UploadRecord record = new BulkUploadResult.UploadRecord();
                record.setRowNumber(rowNum);

                if (row.length < 3) {
                    record.setStatus("FAILED");
                    record.setMessage("Invalid row: fewer than 3 columns");
                    results.add(record);
                    continue;
                }

                String name = row[0].trim();
                String email = row[1].trim();
                String classGrade = row[2].trim();
                record.setName(name);
                record.setEmail(email);
                record.setClassGrade(classGrade);

                if (name.isEmpty() || email.isEmpty() || classGrade.isEmpty()) {
                    record.setStatus("FAILED");
                    record.setMessage("Empty name, email, or classGrade");
                    results.add(record);
                    continue;
                }

                if (!school.getAvailableClasses().contains(classGrade)) {
                    record.setStatus("FAILED");
                    record.setMessage("Class " + classGrade + " not available in this school");
                    results.add(record);
                    continue;
                }

                if (userRepository.existsByEmail(email)) {
                    record.setStatus("FAILED");
                    record.setMessage("Email already exists: " + email);
                    results.add(record);
                    continue;
                }

                try {
                    String rollNo = generateUniqueRollNo(school.getId(), classGrade);
                    String rawPassword = generateRandomPassword();

                    User user = new User();
                    user.setUsername(rollNo);
                    user.setEmail(email);
                    user.setPassword(passwordEncoder.encode(rawPassword));
                    user.setHierarchyLevel(1000); // Set hierarchyLevel for student
                    Role studentRole = roleRepository.findByNameAndSchool("STUDENT", school)
                            .orElseThrow(() -> new RuntimeException("Student role not found for school: " + school.getName()));
                    user.setRole(studentRole);
                    user.setSchool(school);
                    Student student = new Student();
                    student.setRollNo(rollNo);
                    student.setName(name);
                    student.setEmail(email);
                    student.setClassGrade(classGrade);
                    student.setSchool(school);
                    student.setAttendance("0");
                    student.setApprovalStatus("APPROVED");

                    users.add(user);
                    students.add(student);

                    record.setRollNo(rollNo);
                    record.setStatus("SUCCESS");
                    record.setMessage("Student added successfully");
                } catch (Exception e) {
                    record.setStatus("FAILED");
                    record.setMessage("Error processing student: " + e.getMessage());
                }
                results.add(record);
            }

            // Save all valid records
            try {
                userRepository.saveAll(users);
                studentRepo.saveAll(students);

                for (int i = 0; i < students.size(); i++) {
                    Student student = students.get(i);
                    User user = users.get(i);
                    BulkUploadResult.UploadRecord record = results.stream()
                            .filter(r -> r.getRollNo() != null && r.getRollNo().equals(student.getRollNo()))
                            .findFirst().orElse(null);

                    if (record != null && "SUCCESS".equals(record.getStatus())) {
                        String subject = "Student Registration - Approved";
                        String body = String.format("Dear %s,\n\nYour registration has been approved.\n" +
                                        "Username/RollNo: %s\nPassword: %s\nSchool: %s\nClass: %s\n\n" +
                                        "You can now log in to the system.\n\nRegards,\nTeam",
                                student.getName(), user.getUsername(), generateRandomPassword(), // Note: Regenerate or store password
                                school.getName(), student.getClassGrade());
                        emailService.sendEmail(student.getEmail(), subject, body);
                    }
                }
            } catch (Exception e) {
                // Mark all unsaved records as failed
                for (BulkUploadResult.UploadRecord record : results) {
                    if ("SUCCESS".equals(record.getStatus()) && users.stream().noneMatch(u -> u.getUsername().equals(record.getRollNo()))) {
                        record.setStatus("FAILED");
                        record.setMessage("Failed to save to database: " + e.getMessage());
                    }
                }
            }

            // Return only failed records
            return results.stream()
                    .filter(record -> "FAILED".equals(record.getStatus()))
                    .toList();
        } catch (CsvValidationException e) {
            throw new Exception("Error reading CSV file: " + e.getMessage());
        }
    }

}

package com.example.MultiAdminProj;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/students")
public class StudentController {
    @Autowired
    private StudentService studentService;

    @PostMapping("/register/{schoolId}")
    public ResponseEntity<String> registerStudent(@PathVariable Long schoolId, @RequestBody StudentRegistrationRequest request) {
        String username = studentService.registerStudent(schoolId, request);
        return ResponseEntity.ok("Registration successful for " + username + ". Awaiting approval.");
    }

    @PostMapping("/add")
    @PreAuthorize("hasAuthority('STUDENT_CREATE')")
    public ResponseEntity<Student> addStudent(@RequestBody StudentRegistrationRequest request) {
        Student createdStudent = studentService.addStudent(request);
        return ResponseEntity.ok(createdStudent);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('STUDENT_READ')")
    public List<Student> getAll() {
        return studentService.getAll();
    }

    @GetMapping("/pending")
    @PreAuthorize("hasAuthority('STUDENT_READ')")
    public List<Student> getPendingStudents() {
        return studentService.getPendingStudents();
    }

    @PostMapping("/approve/{rollNo}")
    @PreAuthorize("hasAuthority('STUDENT_APPROVEORREJECT')")
    public Student approveStudent(@PathVariable String rollNo) {
        return studentService.approveStudent(rollNo);
    }

    @PostMapping("/reject/{rollNo}")
    @PreAuthorize("hasAuthority('STUDENT_APPROVEORREJECT')")
    public Student rejectStudent(@PathVariable String rollNo) {
        return studentService.rejectStudent(rollNo);
    }

    @DeleteMapping("/{rollNo}")
    @PreAuthorize("hasAuthority('STUDENT_DELETE')")
    public void delete(@PathVariable String rollNo) {
        studentService.delete(rollNo);
    }


    @PutMapping("/{rollNo}")
    @PreAuthorize("hasAuthority('STUDENT_UPDATE')")
    public Student update(@PathVariable String rollNo, @RequestBody Student student) {
        return studentService.update(rollNo, student);
    }

    @GetMapping("/me")
    @PreAuthorize("hasAuthority('STUDENT_READ_OWN')")
    public Student getOwnProfile() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return studentService.findByRollNo(username);
    }

    @GetMapping("/me/attendance")
    @PreAuthorize("hasAuthority('STUDENT_VIEW_ATTENDANCE')")
    public String getOwnAttendance() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        Student student = studentService.findByRollNo(username);
        return student.getAttendance();
    }


    @PostMapping("/bulk-upload/{schoolId}")
    @PreAuthorize("hasAuthority('STUDENT_CREATE')")
    public ResponseEntity<Map<String, Object>> bulkUploadStudents(@PathVariable Long schoolId, @RequestParam("file") MultipartFile file) {
        try {
            List<BulkUploadResult.UploadRecord> failedRecords = studentService.bulkUploadStudents(schoolId, file);

            // Prepare response
            Map<String, Object> response = new HashMap<>();
            if (failedRecords.isEmpty()) {
                response.put("status", "success");
                response.put("message", "Bulk upload successful. All students registered and approved.");
            } else {
                response.put("status", "partial_success");
                response.put("message", "Bulk upload completed with some failures.");
                response.put("failedRecords", failedRecords);
            }

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "Bulk upload failed: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
}

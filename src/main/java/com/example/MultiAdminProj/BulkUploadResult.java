package com.example.MultiAdminProj;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BulkUploadResult {
    private List<UploadRecord> records;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UploadRecord {
        private int rowNumber;
        private String name;
        private String email;
        private String classGrade;
        private String rollNo;
        private String status; // SUCCESS or FAILED
        private String message;
    }
}
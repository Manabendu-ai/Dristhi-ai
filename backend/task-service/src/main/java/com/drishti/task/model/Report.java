package com.drishti.task.model;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "reports")
public class Report {

    @Id
    private String id;

    @Indexed
    private String citizenId;
    private String citizenName;

    // Location
    private double latitude;
    private double longitude;
    private String address;
    private String wardName;
    private int    wardNumber;
    private String zone; // North/South/East/West/Central

    // Image
    private String imageUrl;
    private String description;

    // Status
    @Indexed
    private ReportStatus status;

    // Assigned task
    private String taskId;
    private String assignedDriverId;

    // Timestamps
    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    public enum ReportStatus {
        REPORTED, ASSIGNED, IN_PROGRESS, COMPLETED, VERIFIED, DISPUTED, CLOSED
    }
}

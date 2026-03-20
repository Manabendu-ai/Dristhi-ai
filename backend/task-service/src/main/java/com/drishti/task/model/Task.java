package com.drishti.task.model;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "tasks")
public class Task {

    @Id
    private String id;

    private String reportId;
    private String driverId;
    private String driverName;

    private double latitude;
    private double longitude;
    private String wardName;
    private int    wardNumber;

    private TaskStatus status;
    private String     proofImageUrl;
    private String     driverNotes;

    private LocalDateTime assignedAt;
    private LocalDateTime completedAt;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    public enum TaskStatus {
        ASSIGNED, IN_PROGRESS, COMPLETED, VERIFIED, FAILED
    }
}

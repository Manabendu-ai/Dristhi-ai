package com.drishti.task.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CompleteTaskRequest {

    @NotBlank(message = "Proof image URL is required")
    private String proofImageUrl;

    @NotBlank(message = "Driver ID is required")
    private String driverId;

    private String driverNotes;

    // GNSS verification
    private Double currentLatitude;
    private Double currentLongitude;
}

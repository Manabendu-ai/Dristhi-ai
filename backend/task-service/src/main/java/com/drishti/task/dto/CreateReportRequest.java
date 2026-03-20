package com.drishti.task.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class CreateReportRequest {

    @NotBlank(message = "Citizen ID is required")
    private String citizenId;

    @NotBlank(message = "Citizen name is required")
    private String citizenName;

    @NotNull
    @DecimalMin(value = "12.7342", message = "Latitude outside Bengaluru bounds")
    @DecimalMax(value = "13.1739", message = "Latitude outside Bengaluru bounds")
    private Double latitude;

    @NotNull
    @DecimalMin(value = "77.3791", message = "Longitude outside Bengaluru bounds")
    @DecimalMax(value = "77.7840", message = "Longitude outside Bengaluru bounds")
    private Double longitude;

    @NotBlank(message = "Image URL is required")
    private String imageUrl;

    private String address;
    private String description;
}

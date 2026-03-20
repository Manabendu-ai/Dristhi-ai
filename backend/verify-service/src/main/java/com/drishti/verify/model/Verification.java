package com.drishti.verify.model;

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
@Document(collection = "verifications")
public class Verification {

    @Id
    private String id;

    @Indexed
    private String reportId;
    private String taskId;
    private String citizenId;

    // AI verdict
    private String  aiLabel;
    private double  aiConfidence;
    private double  aiCleanProb;
    private double  aiDirtyProb;
    private String  aiVerdict;
    private double  aiLatencyMs;
    private boolean aiVerified;

    // Citizen verdict
    private String  citizenVerdict;
    private String  citizenComment;
    private boolean citizenVerified;
    private LocalDateTime citizenVerifiedAt;

    // GNSS
    private boolean gnssVerified;
    private double  gnssDistanceMeters;

    // Trust Score breakdown
    private double aiScore;
    private double citizenScore;
    private double gnssScore;
    private double trustScore;
    private String trustLevel;

    // Conflict
    private boolean conflictDetected;
    private String  conflictReason;
    private String  conflictStatus;

    // Final
    private String finalVerdict;
    private VerificationStatus status;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    public enum VerificationStatus {
        AI_PENDING, AI_DONE, CITIZEN_PENDING,
        VERIFIED, DISPUTED, CONFLICT, CLOSED
    }
}

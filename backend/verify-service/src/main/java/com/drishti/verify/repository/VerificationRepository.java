package com.drishti.verify.repository;

import com.drishti.verify.model.Verification;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface VerificationRepository extends MongoRepository<Verification, String> {
    Optional<Verification> findByReportId(String reportId);
    Optional<Verification> findByTaskId(String taskId);
    List<Verification>     findByConflictDetectedTrue();
    List<Verification>     findByStatus(Verification.VerificationStatus status);
    List<Verification>     findByTrustScoreGreaterThan(double score);
}

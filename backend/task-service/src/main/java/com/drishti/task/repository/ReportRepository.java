package com.drishti.task.repository;

import com.drishti.task.model.Report;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReportRepository extends MongoRepository<Report, String> {

    List<Report> findByStatus(Report.ReportStatus status);
    List<Report> findByCitizenId(String citizenId);
    List<Report> findByWardName(String wardName);
    List<Report> findByWardNumber(int wardNumber);
    List<Report> findByZone(String zone);

    @Query("{ 'status': { $in: ['REPORTED', 'ASSIGNED', 'IN_PROGRESS'] } }")
    List<Report> findActiveReports();

    long countByWardName(String wardName);
    long countByStatus(Report.ReportStatus status);
}

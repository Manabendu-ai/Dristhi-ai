package com.drishti.task.service;

import com.drishti.task.dto.CompleteTaskRequest;
import com.drishti.task.dto.CreateReportRequest;
import com.drishti.task.model.Report;
import com.drishti.task.model.Task;
import com.drishti.task.repository.ReportRepository;
import com.drishti.task.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskService {

    private final ReportRepository    reportRepo;
    private final TaskRepository      taskRepo;
    private final BengaluruWardService wardService;

    // ── Create report ──────────────────────────────────────────────────────────
    public Report createReport(CreateReportRequest req) {
        if (!wardService.isInBengaluru(req.getLatitude(), req.getLongitude())) {
            throw new IllegalArgumentException(
                "Location (" + req.getLatitude() + ", " + req.getLongitude() +
                ") is outside BBMP Bengaluru service area");
        }

        BengaluruWardService.WardInfo ward =
            wardService.getWardInfo(req.getLatitude(), req.getLongitude());

        Report report = Report.builder()
            .citizenId(req.getCitizenId())
            .citizenName(req.getCitizenName())
            .latitude(req.getLatitude())
            .longitude(req.getLongitude())
            .address(req.getAddress())
            .wardName(ward.wardName())
            .wardNumber(ward.wardNumber())
            .zone(ward.zone())
            .imageUrl(req.getImageUrl())
            .description(req.getDescription())
            .status(Report.ReportStatus.REPORTED)
            .build();

        Report saved = reportRepo.save(report);
        log.info("New report created: {} in ward {} ({})",
                 saved.getId(), ward.wardName(), ward.zone());

        // Auto-assign to a driver immediately
        autoAssignDriver(saved);
        return reportRepo.findById(saved.getId()).orElse(saved);
    }

    // ── Auto-assign nearest driver ─────────────────────────────────────────────
    private void autoAssignDriver(Report report) {
        // In production: query driver locations from DB, find nearest
        // For hackathon: round-robin assignment from mock drivers
        String[] mockDrivers = {
            "driver_001", "driver_002", "driver_003", "driver_004", "driver_005"
        };
        String[] mockNames = {
            "Ravi Kumar", "Suresh Babu", "Anitha Rao", "Kiran Reddy", "Priya Nair"
        };

        int idx = (int)(Math.abs(report.getWardNumber()) % mockDrivers.length);

        Task task = Task.builder()
            .reportId(report.getId())
            .driverId(mockDrivers[idx])
            .driverName(mockNames[idx])
            .latitude(report.getLatitude())
            .longitude(report.getLongitude())
            .wardName(report.getWardName())
            .wardNumber(report.getWardNumber())
            .status(Task.TaskStatus.ASSIGNED)
            .assignedAt(LocalDateTime.now())
            .build();

        Task savedTask = taskRepo.save(task);

        report.setStatus(Report.ReportStatus.ASSIGNED);
        report.setTaskId(savedTask.getId());
        report.setAssignedDriverId(mockDrivers[idx]);
        reportRepo.save(report);

        log.info("Task {} assigned to driver {} for report {}",
                 savedTask.getId(), mockNames[idx], report.getId());
    }

    // ── Complete task ──────────────────────────────────────────────────────────
    public Task completeTask(String taskId, CompleteTaskRequest req) {
        Task task = taskRepo.findById(taskId)
            .orElseThrow(() -> new RuntimeException("Task not found: " + taskId));

        // GNSS verification — check driver is within 100m of task location
        if (req.getCurrentLatitude() != null && req.getCurrentLongitude() != null) {
            double dist = haversineMeters(
                req.getCurrentLatitude(), req.getCurrentLongitude(),
                task.getLatitude(),       task.getLongitude()
            );
            if (dist > 150) {
                throw new IllegalStateException(
                    "Driver is " + (int)dist + "m away from task location. " +
                    "Must be within 150m to complete task. " +
                    "GNSS geofence verification failed.");
            }
            log.info("GNSS verified: driver is {}m from task location", (int)dist);
        }

        task.setStatus(Task.TaskStatus.COMPLETED);
        task.setProofImageUrl(req.getProofImageUrl());
        task.setDriverNotes(req.getDriverNotes());
        task.setCompletedAt(LocalDateTime.now());
        Task saved = taskRepo.save(task);

        // Update report status
        reportRepo.findById(task.getReportId()).ifPresent(report -> {
            report.setStatus(Report.ReportStatus.COMPLETED);
            reportRepo.save(report);
        });

        return saved;
    }

    // ── Queries ────────────────────────────────────────────────────────────────
    public List<Report> getAllReports()                          { return reportRepo.findAll(); }
    public List<Report> getReportsByStatus(String status)       { return reportRepo.findByStatus(Report.ReportStatus.valueOf(status)); }
    public List<Report> getReportsByWard(String wardName)       { return reportRepo.findByWardName(wardName); }
    public List<Report> getReportsByCitizen(String citizenId)   { return reportRepo.findByCitizenId(citizenId); }
    public List<Report> getActiveReports()                      { return reportRepo.findActiveReports(); }
    public List<Task>   getTasksByDriver(String driverId)       { return taskRepo.findByDriverId(driverId); }
    public Report       getReportById(String id)                { return reportRepo.findById(id).orElseThrow(() -> new RuntimeException("Report not found")); }
    public Task         getTaskById(String id)                  { return taskRepo.findById(id).orElseThrow(() -> new RuntimeException("Task not found")); }

    public Map<String, Object> getDashboardStats() {
        return Map.of(
            "totalReports",    reportRepo.count(),
            "activeReports",   reportRepo.findActiveReports().size(),
            "completedToday",  reportRepo.countByStatus(Report.ReportStatus.VERIFIED),
            "reportsByStatus", Map.of(
                "REPORTED",    reportRepo.countByStatus(Report.ReportStatus.REPORTED),
                "ASSIGNED",    reportRepo.countByStatus(Report.ReportStatus.ASSIGNED),
                "COMPLETED",   reportRepo.countByStatus(Report.ReportStatus.COMPLETED),
                "VERIFIED",    reportRepo.countByStatus(Report.ReportStatus.VERIFIED),
                "DISPUTED",    reportRepo.countByStatus(Report.ReportStatus.DISPUTED)
            )
        );
    }

    private double haversineMeters(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371000;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat/2) * Math.sin(dLat/2)
                 + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                 * Math.sin(dLon/2) * Math.sin(dLon/2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
    }
public BengaluruWardService.WardInfo getWardInfoPublic(double lat, double lng) {
        if (!wardService.isInBengaluru(lat, lng)) return null;
        return wardService.getWardInfo(lat, lng);
    }
}

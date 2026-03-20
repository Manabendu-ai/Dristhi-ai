package com.drishti.task.controller;

import com.drishti.task.dto.CompleteTaskRequest;
import com.drishti.task.dto.CreateReportRequest;
import com.drishti.task.model.Report;
import com.drishti.task.model.Task;
import com.drishti.task.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "DRISHTI Task Service", description = "Garbage report and task management for BBMP Bengaluru")
public class TaskController {

    private final TaskService taskService;

    // ── Reports ────────────────────────────────────────────────────────────────
    @PostMapping("/reports")
    @Operation(summary = "Citizen submits a garbage report with GPS location")
    public ResponseEntity<?> createReport(@Valid @RequestBody CreateReportRequest req) {
        try {
            Report report = taskService.createReport(req);
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "success", true,
                "message", "Report created and driver assigned",
                "report",  report
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error",   e.getMessage()
            ));
        }
    }

    @GetMapping("/reports")
    @Operation(summary = "Get all reports with optional filters")
    public ResponseEntity<List<Report>> getReports(
        @RequestParam(required = false) String status,
        @RequestParam(required = false) String ward,
        @RequestParam(required = false) String citizenId
    ) {
        if (status   != null) return ResponseEntity.ok(taskService.getReportsByStatus(status));
        if (ward     != null) return ResponseEntity.ok(taskService.getReportsByWard(ward));
        if (citizenId!= null) return ResponseEntity.ok(taskService.getReportsByCitizen(citizenId));
        return ResponseEntity.ok(taskService.getAllReports());
    }

    @GetMapping("/reports/active")
    @Operation(summary = "Get all active reports (not yet verified)")
    public ResponseEntity<List<Report>> getActiveReports() {
        return ResponseEntity.ok(taskService.getActiveReports());
    }

    @GetMapping("/reports/{id}")
    @Operation(summary = "Get report by ID")
    public ResponseEntity<?> getReport(@PathVariable String id) {
        try {
            return ResponseEntity.ok(taskService.getReportById(id));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // ── Tasks ──────────────────────────────────────────────────────────────────
    @PatchMapping("/tasks/{taskId}/complete")
    @Operation(summary = "Driver marks task complete and uploads proof image")
    public ResponseEntity<?> completeTask(
        @PathVariable String taskId,
        @Valid @RequestBody CompleteTaskRequest req
    ) {
        try {
            Task task = taskService.completeTask(taskId, req);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Task completed successfully. Awaiting AI verification.",
                "task",    task
            ));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error",   e.getMessage()
            ));
        }
    }

    @GetMapping("/tasks/driver/{driverId}")
    @Operation(summary = "Get all tasks assigned to a driver")
    public ResponseEntity<List<Task>> getDriverTasks(@PathVariable String driverId) {
        return ResponseEntity.ok(taskService.getTasksByDriver(driverId));
    }

    @GetMapping("/tasks/{id}")
    @Operation(summary = "Get task by ID")
    public ResponseEntity<?> getTask(@PathVariable String id) {
        try {
            return ResponseEntity.ok(taskService.getTaskById(id));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // ── Dashboard ──────────────────────────────────────────────────────────────
    @GetMapping("/dashboard/stats")
    @Operation(summary = "Get governance dashboard statistics")
    public ResponseEntity<Map<String, Object>> getDashboardStats() {
        return ResponseEntity.ok(taskService.getDashboardStats());
    }

    // ── Ward check ─────────────────────────────────────────────────────────────
    @GetMapping("/location/verify")
    @Operation(summary = "Verify if GPS coordinates are within BBMP Bengaluru limits")
    public ResponseEntity<Map<String, Object>> verifyLocation(
        @RequestParam double lat,
        @RequestParam double lng
    ) {
        var wardInfo = taskService.getWardInfoPublic(lat, lng);
        return ResponseEntity.ok(Map.of(
            "inBengaluru", wardInfo != null,
            "wardName",    wardInfo != null ? wardInfo.wardName() : "Outside BBMP",
            "wardNumber",  wardInfo != null ? wardInfo.wardNumber() : 0,
            "zone",        wardInfo != null ? wardInfo.zone() : "Unknown",
            "coordinates", Map.of("lat", lat, "lng", lng)
        ));
    }
}

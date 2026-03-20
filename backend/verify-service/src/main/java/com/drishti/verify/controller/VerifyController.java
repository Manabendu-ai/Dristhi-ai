package com.drishti.verify.controller;

import com.drishti.verify.model.Verification;
import com.drishti.verify.service.VerifyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/verify")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "DRISHTI Verify Service", description = "Trust Score engine — AI + Citizen + GNSS verification")
public class VerifyController {

    private final VerifyService verifyService;

    @PostMapping("/ai")
    @Operation(summary = "Trigger AI verification for a completed cleanup task")
    public ResponseEntity<?> triggerAi(@RequestBody Map<String, Object> body) {
        try {
            Verification v = verifyService.triggerAiVerification(
                (String)  body.get("reportId"),
                (String)  body.get("taskId"),
                (String)  body.get("citizenId"),
                (String)  body.get("proofImageUrl"),
                body.get("gnssVerified") != null && (Boolean) body.get("gnssVerified"),
                body.get("gnssDistance") != null
                    ? ((Number) body.get("gnssDistance")).doubleValue() : 999.0
            );
            return ResponseEntity.ok(Map.of(
                "success",     true,
                "message",     "AI verification complete",
                "verification", v
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error",   e.getMessage()
            ));
        }
    }

    @PostMapping("/citizen")
    @Operation(summary = "Citizen submits their verdict on the cleanup")
    public ResponseEntity<?> citizenVerdict(@RequestBody Map<String, String> body) {
        try {
            Verification v = verifyService.submitCitizenVerdict(
                body.get("reportId"),
                body.get("verdict"),
                body.get("comment")
            );
            return ResponseEntity.ok(Map.of(
                "success",      true,
                "message",      "Citizen verdict recorded",
                "trustScore",   v.getTrustScore(),
                "trustLevel",   v.getTrustLevel(),
                "finalVerdict", v.getFinalVerdict(),
                "conflict",     v.isConflictDetected(),
                "verification", v
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error",   e.getMessage()
            ));
        }
    }

    @GetMapping("/report/{reportId}")
    @Operation(summary = "Get full verification + Trust Score for a report")
    public ResponseEntity<?> getVerification(@PathVariable String reportId) {
        try {
            return ResponseEntity.ok(verifyService.getVerification(reportId));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/conflicts")
    @Operation(summary = "Get all conflict cases for admin review")
    public ResponseEntity<List<Verification>> getConflicts() {
        return ResponseEntity.ok(verifyService.getConflicts());
    }

    @GetMapping("/stats")
    @Operation(summary = "Get verification statistics for governance dashboard")
    public ResponseEntity<Map<String, Object>> getStats() {
        return ResponseEntity.ok(verifyService.getStats());
    }

    @GetMapping("/all")
    @Operation(summary = "Get all verifications")
    public ResponseEntity<List<Verification>> getAll() {
        return ResponseEntity.ok(verifyService.getAll());
    }
}

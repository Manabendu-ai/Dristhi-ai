package com.drishti.verify.service;

import com.drishti.verify.model.Verification;
import com.drishti.verify.repository.VerificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class VerifyService {

    private final VerificationRepository verifyRepo;
    private final TrustScoreService      trustScoreService;
    private final RestTemplate           restTemplate;

    @Value("${ai.service.url}")
    private String aiServiceUrl;

    // ── Trigger AI verification ────────────────────────────────────────────────
    public Verification triggerAiVerification(
        String reportId,
        String taskId,
        String citizenId,
        String proofImageUrl,
        boolean gnssVerified,
        double  gnssDistance
    ) {
        // Check if already exists
        var existing = verifyRepo.findByReportId(reportId);
        Verification verification = existing.orElse(
            Verification.builder()
                .reportId(reportId)
                .taskId(taskId)
                .citizenId(citizenId)
                .gnssVerified(gnssVerified)
                .gnssDistanceMeters(gnssDistance)
                .status(Verification.VerificationStatus.AI_PENDING)
                .build()
        );

        // Call AI service
        try {
            Map<String, Object> aiResult = callAiService(proofImageUrl);

            verification.setAiLabel((String) aiResult.get("label"));
            verification.setAiConfidence(toDouble(aiResult.get("confidence")));
            verification.setAiCleanProb(toDouble(aiResult.get("clean_prob")));
            verification.setAiDirtyProb(toDouble(aiResult.get("dirty_prob")));
            verification.setAiVerdict((String) aiResult.get("verdict"));
            verification.setAiLatencyMs(toDouble(aiResult.get("latency_ms")));
            verification.setAiVerified(true);
            verification.setStatus(Verification.VerificationStatus.CITIZEN_PENDING);

            log.info("AI verdict for report {}: {} ({:.0f}% confidence)",
                reportId, verification.getAiLabel(),
                verification.getAiConfidence() * 100);

        } catch (Exception e) {
            log.error("AI service call failed: {}", e.getMessage());
            // Fallback — mark as uncertain
            verification.setAiLabel("uncertain");
            verification.setAiConfidence(0.5);
            verification.setAiVerdict("AI service unavailable — manual review needed");
            verification.setAiVerified(false);
            verification.setStatus(Verification.VerificationStatus.CITIZEN_PENDING);
        }

        // Compute initial Trust Score (without citizen input yet)
        TrustScoreService.TrustResult trust = trustScoreService.compute(
            verification.getAiLabel(),
            verification.getAiConfidence(),
            null,
            gnssVerified,
            gnssDistance
        );

        applyTrustResult(verification, trust);
        return verifyRepo.save(verification);
    }

    // ── Citizen submits verdict ────────────────────────────────────────────────
    public Verification submitCitizenVerdict(
        String reportId,
        String citizenVerdict,
        String citizenComment
    ) {
        Verification v = verifyRepo.findByReportId(reportId)
            .orElseThrow(() -> new RuntimeException("Verification not found for report: " + reportId));

        v.setCitizenVerdict(citizenVerdict);
        v.setCitizenComment(citizenComment);
        v.setCitizenVerified(true);
        v.setCitizenVerifiedAt(java.time.LocalDateTime.now());

        // Recompute Trust Score with citizen input
        TrustScoreService.TrustResult trust = trustScoreService.compute(
            v.getAiLabel(),
            v.getAiConfidence(),
            citizenVerdict,
            v.isGnssVerified(),
            v.getGnssDistanceMeters()
        );

        applyTrustResult(v, trust);

        // Set final verdict
        if (trust.conflictDetected) {
            v.setStatus(Verification.VerificationStatus.CONFLICT);
            v.setConflictDetected(true);
            v.setConflictReason(trust.conflictReason);
            v.setConflictStatus("PENDING_ADMIN_REVIEW");
            v.setFinalVerdict("DISPUTED — Admin review required");
            log.warn("CONFLICT detected for report {}: {}", reportId, trust.conflictReason);
        } else if (trust.trustScore >= 65) {
            v.setStatus(Verification.VerificationStatus.VERIFIED);
            v.setFinalVerdict("VERIFIED_CLEAN — Cleanup confirmed");
        } else {
            v.setStatus(Verification.VerificationStatus.DISPUTED);
            v.setFinalVerdict("DISPUTED — Low trust score");
        }

        return verifyRepo.save(v);
    }

    // ── Get Trust Score for a report ───────────────────────────────────────────
    public Verification getVerification(String reportId) {
        return verifyRepo.findByReportId(reportId)
            .orElseThrow(() -> new RuntimeException("No verification found for report: " + reportId));
    }

    public List<Verification> getConflicts() {
        return verifyRepo.findByConflictDetectedTrue();
    }

    public List<Verification> getAll() {
        return verifyRepo.findAll();
    }

    public Map<String, Object> getStats() {
        long total     = verifyRepo.count();
        long verified  = verifyRepo.findByStatus(Verification.VerificationStatus.VERIFIED).size();
        long conflicts = verifyRepo.findByConflictDetectedTrue().size();
        long disputed  = verifyRepo.findByStatus(Verification.VerificationStatus.DISPUTED).size();

        double avgTrust = verifyRepo.findAll().stream()
            .mapToDouble(Verification::getTrustScore)
            .average().orElse(0.0);

        return Map.of(
            "totalVerifications", total,
            "verified",           verified,
            "conflicts",          conflicts,
            "disputed",           disputed,
            "avgTrustScore",      Math.round(avgTrust * 10.0) / 10.0
        );
    }

    // ── Helpers ────────────────────────────────────────────────────────────────
    @SuppressWarnings("unchecked")
    private Map<String, Object> callAiService(String imageUrl) {
        String url = aiServiceUrl + "/predict/url";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, String>> req =
            new HttpEntity<>(Map.of("url", imageUrl), headers);
        ResponseEntity<Map> resp = restTemplate.postForEntity(url, req, Map.class);
        return resp.getBody();
    }

    private void applyTrustResult(Verification v, TrustScoreService.TrustResult t) {
        v.setAiScore(Math.round(t.aiScore * 10.0) / 10.0);
        v.setCitizenScore(Math.round(t.citizenScore * 10.0) / 10.0);
        v.setGnssScore(Math.round(t.gnssScore * 10.0) / 10.0);
        v.setTrustScore(Math.round(t.trustScore * 10.0) / 10.0);
        v.setTrustLevel(t.trustLevel);
        v.setConflictDetected(t.conflictDetected);
        v.setConflictReason(t.conflictReason);
    }

    private double toDouble(Object val) {
        if (val == null) return 0.0;
        if (val instanceof Number) return ((Number) val).doubleValue();
        try { return Double.parseDouble(val.toString()); } catch (Exception e) { return 0.0; }
    }
}

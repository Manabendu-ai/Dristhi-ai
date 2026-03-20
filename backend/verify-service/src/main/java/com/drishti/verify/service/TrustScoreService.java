package com.drishti.verify.service;

import org.springframework.stereotype.Service;

@Service
public class TrustScoreService {

    // Weights — must sum to 100
    private static final double AI_WEIGHT      = 40.0;
    private static final double CITIZEN_WEIGHT = 40.0;
    private static final double GNSS_WEIGHT    = 20.0;

    public static class TrustResult {
        public double aiScore;
        public double citizenScore;
        public double gnssScore;
        public double trustScore;
        public String trustLevel;
        public boolean conflictDetected;
        public String conflictReason;

        public TrustResult(double ai, double citizen, double gnss) {
            this.aiScore      = ai;
            this.citizenScore = citizen;
            this.gnssScore    = gnss;
            this.trustScore   = (ai * AI_WEIGHT / 100)
                              + (citizen * CITIZEN_WEIGHT / 100)
                              + (gnss * GNSS_WEIGHT / 100);
            this.trustLevel   = computeLevel(this.trustScore);
        }

        private String computeLevel(double score) {
            if (score >= 85) return "HIGH ✅";
            if (score >= 65) return "MEDIUM ⚠️";
            if (score >= 40) return "LOW ❌";
            return "VERY LOW 🚨";
        }
    }

    /**
     * Compute Trust Score from all three verification layers.
     *
     * @param aiLabel        "clean" or "dirty"
     * @param aiConfidence   0.0 - 1.0
     * @param citizenVerdict "CONFIRM_CLEAN" or "DISPUTE_DIRTY"
     * @param gnssVerified   whether driver was within geofence
     * @param gnssDistance   meters from task location
     */
    public TrustResult compute(
        String  aiLabel,
        double  aiConfidence,
        String  citizenVerdict,
        boolean gnssVerified,
        double  gnssDistance
    ) {
        // ── AI Score (0-100) ───────────────────────────────────────────────────
        double aiScore = 0.0;
        if (aiLabel != null) {
            // Base score from confidence
            aiScore = aiConfidence * 100;
            // Penalise low confidence
            if (aiConfidence < 0.6) aiScore *= 0.7;
        }

        // ── Citizen Score (0-100) ──────────────────────────────────────────────
        double citizenScore = 0.0;
        if (citizenVerdict != null) {
            switch (citizenVerdict) {
                case "CONFIRM_CLEAN" -> citizenScore = 100.0;
                case "DISPUTE_DIRTY" -> citizenScore = 0.0;
                case "UNCERTAIN"     -> citizenScore = 50.0;
                default              -> citizenScore = 50.0;
            }
        } else {
            // No citizen response yet — neutral
            citizenScore = 50.0;
        }

        // ── GNSS Score (0-100) ─────────────────────────────────────────────────
        double gnssScore;
        if (gnssVerified) {
            // Full score if within 50m, decreasing beyond
            if (gnssDistance <= 50)       gnssScore = 100.0;
            else if (gnssDistance <= 100) gnssScore = 85.0;
            else if (gnssDistance <= 150) gnssScore = 70.0;
            else                          gnssScore = 40.0;
        } else {
            gnssScore = 0.0;
        }

        TrustResult result = new TrustResult(aiScore, citizenScore, gnssScore);

        // ── Conflict detection ─────────────────────────────────────────────────
        if (aiLabel != null && citizenVerdict != null) {
            boolean aiSaysClean      = aiLabel.equals("clean");
            boolean citizenSaysClean = citizenVerdict.equals("CONFIRM_CLEAN");

            if (aiSaysClean != citizenSaysClean && aiConfidence > 0.7) {
                result.conflictDetected = true;
                result.conflictReason   = String.format(
                    "AI says %s (%.0f%% confidence) but citizen says %s",
                    aiLabel.toUpperCase(),
                    aiConfidence * 100,
                    citizenSaysClean ? "CLEAN" : "DIRTY"
                );
            }
        }

        return result;
    }
}

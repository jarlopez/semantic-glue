package edu.kth.wsglue.parsing.comparators;

public enum SemanticMatchingDegree {
    Exact(1.0),
    Subsumption(0.8),
    PlugIn(0.6),
    Structural(0.5),
    NotMatched(0.0);

    private Double score;

    SemanticMatchingDegree(Double scoreThresh) {
        score = scoreThresh;
    }

    public Double getScore() {
        return score;
    }

    public static SemanticMatchingDegree determineMatchingDegree(Double score) {
        if (score < Structural.getScore()) {
            return NotMatched;
        } else if (score < PlugIn.getScore()) {
            return Structural;
        } else if (score < Subsumption.getScore()) {
            return PlugIn;
        } else if (score < Exact.getScore()) {
            return Subsumption;
        } else if (score >= Exact.getScore()) {
            return Exact;
        }
        return NotMatched;
    }

}

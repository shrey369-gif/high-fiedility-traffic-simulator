package com.traffic.model;

public enum DriverBehavior {
    AGGRESSIVE(1.4, 0.5, 0.20, 1.3),
    NORMAL(1.0, 1.0, 0.05, 1.0),
    DEFENSIVE(0.7, 1.5, 0.01, 0.8);

    private final double speedMultiplier;
    private final double gapAcceptance;
    private final double signalViolationProbability;
    private final double overtakingAggression;

    DriverBehavior(double speedMultiplier, double gapAcceptance,
                   double signalViolationProbability, double overtakingAggression) {
        this.speedMultiplier = speedMultiplier;
        this.gapAcceptance = gapAcceptance;
        this.signalViolationProbability = signalViolationProbability;
        this.overtakingAggression = overtakingAggression;
    }

    public double getSpeedMultiplier() { return speedMultiplier; }
    public double getGapAcceptance() { return gapAcceptance; }
    public double getSignalViolationProbability() { return signalViolationProbability; }
    public double getOvertakingAggression() { return overtakingAggression; }

    public static DriverBehavior random() {
        double r = Math.random();
        if (r < 0.3) return AGGRESSIVE;
        if (r < 0.75) return NORMAL;
        return DEFENSIVE;
    }
}

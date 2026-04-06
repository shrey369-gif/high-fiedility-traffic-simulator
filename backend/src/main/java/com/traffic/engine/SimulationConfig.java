package com.traffic.engine;

public class SimulationConfig {
    private double trafficDensity;
    private boolean adaptiveSignals;
    private double simulationSpeed;
    private int maxVehicles;
    private double spawnRate;

    public SimulationConfig() {
        this.trafficDensity = 0.5;
        this.adaptiveSignals = false;
        this.simulationSpeed = 1.0;
        this.maxVehicles = 100;
        this.spawnRate = 0.3;
    }

    public double getTrafficDensity() { return trafficDensity; }
    public void setTrafficDensity(double trafficDensity) {
        this.trafficDensity = Math.max(0.0, Math.min(1.0, trafficDensity));
        this.maxVehicles = (int)(50 + trafficDensity * 150);
        this.spawnRate = 0.1 + trafficDensity * 0.5;
    }
    public boolean isAdaptiveSignals() { return adaptiveSignals; }
    public void setAdaptiveSignals(boolean adaptiveSignals) { this.adaptiveSignals = adaptiveSignals; }
    public double getSimulationSpeed() { return simulationSpeed; }
    public void setSimulationSpeed(double simulationSpeed) { this.simulationSpeed = Math.max(0.1, Math.min(5.0, simulationSpeed)); }
    public int getMaxVehicles() { return maxVehicles; }
    public double getSpawnRate() { return spawnRate; }

    public String toJson() {
        return String.format(
            "{\"trafficDensity\":%.2f,\"adaptiveSignals\":%b,\"simulationSpeed\":%.1f,\"maxVehicles\":%d,\"spawnRate\":%.2f}",
            trafficDensity, adaptiveSignals, simulationSpeed, maxVehicles, spawnRate
        );
    }
}

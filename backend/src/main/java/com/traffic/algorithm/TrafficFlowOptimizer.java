package com.traffic.algorithm;

import com.traffic.model.Road;
import com.traffic.model.Vehicle;
import java.util.List;

public class TrafficFlowOptimizer {

    private static final double CONGESTION_THRESHOLD = 0.6;

    public void optimizeFlow(List<Road> roads) {
        for (Road road : roads) {
            double density = road.getDensity();
            if (density > CONGESTION_THRESHOLD) {
                slowDownVehicles(road);
            } else if (density < 0.2) {
                allowSpeedUp(road);
            }
        }
    }

    private void slowDownVehicles(Road road) {
        double factor = Math.max(0.3, 1.0 - road.getDensity());
        for (Vehicle v : road.getVehicles()) {
            double maxSpeed = road.getSpeedLimit() * factor;
            if (v.getSpeed() > maxSpeed) {
                v.setSpeed(v.getSpeed() * 0.95);
            }
        }
    }

    private void allowSpeedUp(Road road) {
        for (Vehicle v : road.getVehicles()) {
            double targetSpeed = road.getSpeedLimit() * v.getBehavior().getSpeedMultiplier();
            if (v.getSpeed() < targetSpeed) {
                v.setSpeed(v.getSpeed() + (targetSpeed - v.getSpeed()) * 0.05);
            }
        }
    }

    public Road findAlternateRoute(List<Road> possibleRoutes) {
        Road best = null;
        double bestScore = Double.MAX_VALUE;
        for (Road road : possibleRoutes) {
            double score = road.getLength() * (1 + road.getDensity() * 10);
            if (score < bestScore) {
                bestScore = score;
                best = road;
            }
        }
        return best;
    }
}

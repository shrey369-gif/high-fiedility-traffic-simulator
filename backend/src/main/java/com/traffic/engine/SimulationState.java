package com.traffic.engine;

import com.traffic.model.*;
import java.util.List;

public class SimulationState {
    private final long tick;
    private final List<Vehicle> vehicles;
    private final List<Intersection> intersections;
    private final List<Road> roads;
    private int signalViolations;
    private int totalHonks;

    public SimulationState(long tick, List<Vehicle> vehicles, List<Intersection> intersections, List<Road> roads) {
        this.tick = tick;
        this.vehicles = vehicles;
        this.intersections = intersections;
        this.roads = roads;
        this.signalViolations = 0;
        this.totalHonks = 0;
    }

    public void incrementViolations() { signalViolations++; }
    public void incrementHonks() { totalHonks++; }

    public double getAverageSpeed() {
        if (vehicles.isEmpty()) return 0;
        return vehicles.stream().mapToDouble(Vehicle::getSpeed).average().orElse(0);
    }

    public double getCongestionIndex() {
        if (roads.isEmpty()) return 0;
        double totalDensity = roads.stream().mapToDouble(Road::getDensity).sum();
        return Math.min(1.0, totalDensity / roads.size() * 50);
    }

    public String toJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");

        sb.append("\"tick\":").append(tick).append(",");

        // Vehicles
        sb.append("\"vehicles\":[");
        for (int i = 0; i < vehicles.size(); i++) {
            Vehicle v = vehicles.get(i);
            if (i > 0) sb.append(",");
            sb.append(String.format(
                "{\"id\":%d,\"type\":\"%s\",\"x\":%.1f,\"y\":%.1f,\"speed\":%.1f,\"heading\":%.1f,\"behavior\":\"%s\",\"honking\":%b}",
                v.getId(), v.getType().name(), v.getX(), v.getY(), v.getSpeed(),
                v.getHeading(), v.getBehavior().name(), v.isHonking()
            ));
        }
        sb.append("],");

        // Signals
        sb.append("\"signals\":[");
        int si = 0;
        for (Intersection inter : intersections) {
            TrafficSignal sig = inter.getSignal();
            if (si > 0) sb.append(",");
            sb.append(String.format(
                "{\"id\":%d,\"x\":%.1f,\"y\":%.1f,\"state\":\"%s\",\"timeRemaining\":%d}",
                sig.getId(), inter.getX(), inter.getY(), sig.getState().name(), sig.getTimeRemaining()
            ));
            si++;
        }
        sb.append("],");

        // Roads
        sb.append("\"roads\":[");
        for (int i = 0; i < roads.size(); i++) {
            Road r = roads.get(i);
            if (i > 0) sb.append(",");
            sb.append(String.format(
                "{\"id\":%d,\"srcX\":%.1f,\"srcY\":%.1f,\"dstX\":%.1f,\"dstY\":%.1f,\"width\":%.1f}",
                r.getId(), r.getSource().getX(), r.getSource().getY(),
                r.getDestination().getX(), r.getDestination().getY(), r.getWidth()
            ));
        }
        sb.append("],");

        // Stats
        sb.append(String.format(
            "\"stats\":{\"vehicleCount\":%d,\"avgSpeed\":%.1f,\"congestionIndex\":%.2f,\"signalViolations\":%d,\"totalHonks\":%d}",
            vehicles.size(), getAverageSpeed(), getCongestionIndex(), signalViolations, totalHonks
        ));

        sb.append("}");
        return sb.toString();
    }

    public long getTick() { return tick; }
    public List<Vehicle> getVehicles() { return vehicles; }
    public List<Intersection> getIntersections() { return intersections; }
    public List<Road> getRoads() { return roads; }
    public int getSignalViolations() { return signalViolations; }
    public int getTotalHonks() { return totalHonks; }
}

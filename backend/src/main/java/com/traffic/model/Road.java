package com.traffic.model;

import java.util.ArrayList;
import java.util.List;

public class Road {
    private static int nextId = 1;
    private final int id;
    private final Intersection source;
    private final Intersection destination;
    private final double width;
    private final double speedLimit;
    private final double length;
    private final List<Vehicle> vehicles;

    public Road(Intersection source, Intersection destination, double width, double speedLimit) {
        this.id = nextId++;
        this.source = source;
        this.destination = destination;
        this.width = width;
        this.speedLimit = speedLimit;
        this.length = source.distanceTo(destination);
        this.vehicles = new ArrayList<>();
        source.addRoad(this);
        destination.addRoad(this);
    }

    public void addVehicle(Vehicle vehicle) {
        vehicles.add(vehicle);
    }

    public void removeVehicle(Vehicle vehicle) {
        vehicles.remove(vehicle);
    }

    public double getDensity() {
        if (length == 0) return 0;
        return vehicles.size() / (length * width);
    }

    public double getDirectionAngle() {
        double dx = destination.getX() - source.getX();
        double dy = destination.getY() - source.getY();
        return Math.toDegrees(Math.atan2(dy, dx));
    }

    public int getId() { return id; }
    public Intersection getSource() { return source; }
    public Intersection getDestination() { return destination; }
    public double getWidth() { return width; }
    public double getSpeedLimit() { return speedLimit; }
    public double getLength() { return length; }
    public List<Vehicle> getVehicles() { return vehicles; }
}

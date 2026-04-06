package com.traffic.model;

import java.util.ArrayList;
import java.util.List;

public class Intersection {
    private static int nextId = 1;
    private final int id;
    private final double x;
    private final double y;
    private final List<Road> connectedRoads;
    private final TrafficSignal signal;

    public Intersection(double x, double y) {
        this.id = nextId++;
        this.x = x;
        this.y = y;
        this.connectedRoads = new ArrayList<>();
        this.signal = new TrafficSignal(30, 30);
    }

    public Intersection(int id, double x, double y) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.connectedRoads = new ArrayList<>();
        this.signal = new TrafficSignal(30, 30);
    }

    public void addRoad(Road road) {
        connectedRoads.add(road);
    }

    public double distanceTo(Intersection other) {
        double dx = this.x - other.x;
        double dy = this.y - other.y;
        return Math.sqrt(dx * dx + dy * dy);
    }

    public int getId() { return id; }
    public double getX() { return x; }
    public double getY() { return y; }
    public List<Road> getConnectedRoads() { return connectedRoads; }
    public TrafficSignal getSignal() { return signal; }
}

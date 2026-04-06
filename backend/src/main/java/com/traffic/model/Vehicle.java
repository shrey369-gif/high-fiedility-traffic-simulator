package com.traffic.model;

public class Vehicle {
    private static int nextId = 1;
    private final int id;
    private final VehicleType type;
    private final DriverBehavior behavior;
    private double x;
    private double y;
    private double speed;
    private double heading;
    private double lateralOffset;
    private double progress;
    private Road currentRoad;
    private boolean honking;
    private int honkCooldown;

    public Vehicle(VehicleType type, DriverBehavior behavior, Road road) {
        this.id = nextId++;
        this.type = type;
        this.behavior = behavior;
        this.currentRoad = road;
        this.progress = 0;
        this.speed = type.getMinSpeed() + Math.random() * (type.getMaxSpeed() - type.getMinSpeed()) * 0.5;
        this.speed *= behavior.getSpeedMultiplier();
        this.lateralOffset = (Math.random() - 0.5) * (road.getWidth() - type.getWidth());
        this.heading = road.getDirectionAngle();
        this.honking = false;
        this.honkCooldown = 0;
        updatePosition();
        road.addVehicle(this);
    }

    public void update(double deltaTime) {
        double roadLength = currentRoad.getLength();
        if (roadLength == 0) return;

        progress += (speed * deltaTime) / roadLength;

        // Lane-less lateral drift (Indian-style)
        double drift = (Math.random() - 0.5) * 0.3 * behavior.getOvertakingAggression();
        double maxOffset = (currentRoad.getWidth() - type.getWidth()) / 2.0;
        lateralOffset = Math.max(-maxOffset, Math.min(maxOffset, lateralOffset + drift));

        // Honking logic
        if (honkCooldown > 0) {
            honkCooldown--;
            if (honkCooldown == 0) honking = false;
        }

        updatePosition();
    }

    private void updatePosition() {
        if (currentRoad == null) return;
        Intersection src = currentRoad.getSource();
        Intersection dst = currentRoad.getDestination();
        double dx = dst.getX() - src.getX();
        double dy = dst.getY() - src.getY();
        double len = currentRoad.getLength();

        x = src.getX() + dx * progress;
        y = src.getY() + dy * progress;

        if (len > 0) {
            double perpX = -dy / len;
            double perpY = dx / len;
            x += perpX * lateralOffset;
            y += perpY * lateralOffset;
        }

        heading = currentRoad.getDirectionAngle();
    }

    public void adjustSpeedForTraffic(double frontVehicleSpeed, double distance) {
        double safeDistance = type.getLength() * 2 * behavior.getGapAcceptance();
        if (distance < safeDistance) {
            speed = Math.max(0, frontVehicleSpeed * 0.8);
            if (behavior == DriverBehavior.AGGRESSIVE && !honking) {
                honking = true;
                honkCooldown = 30;
            }
        } else {
            double targetSpeed = currentRoad.getSpeedLimit() * behavior.getSpeedMultiplier();
            speed += (targetSpeed - speed) * 0.1;
        }
    }

    public boolean willViolateSignal() {
        return Math.random() < behavior.getSignalViolationProbability();
    }

    public boolean hasReachedEnd() {
        return progress >= 1.0;
    }

    public int getId() { return id; }
    public VehicleType getType() { return type; }
    public DriverBehavior getBehavior() { return behavior; }
    public double getX() { return x; }
    public double getY() { return y; }
    public double getSpeed() { return speed; }
    public void setSpeed(double speed) { this.speed = speed; }
    public double getHeading() { return heading; }
    public double getProgress() { return progress; }
    public void setProgress(double progress) { this.progress = progress; }
    public Road getCurrentRoad() { return currentRoad; }
    public void setCurrentRoad(Road road) {
        if (this.currentRoad != null) this.currentRoad.removeVehicle(this);
        this.currentRoad = road;
        this.progress = 0;
        this.lateralOffset = (Math.random() - 0.5) * (road.getWidth() - type.getWidth());
        this.heading = road.getDirectionAngle();
        road.addVehicle(this);
        updatePosition();
    }
    public boolean isHonking() { return honking; }
    public double getLateralOffset() { return lateralOffset; }
}

package com.traffic.model;

public enum VehicleType {
    CAR(4.5, 2.0, 60, 120),
    BIKE(2.0, 0.8, 40, 80),
    AUTO_RICKSHAW(3.0, 1.5, 40, 60),
    BUS(12.0, 2.5, 40, 80),
    TRUCK(10.0, 2.5, 35, 70);

    private final double length;
    private final double width;
    private final double minSpeed;
    private final double maxSpeed;

    VehicleType(double length, double width, double minSpeed, double maxSpeed) {
        this.length = length;
        this.width = width;
        this.minSpeed = minSpeed;
        this.maxSpeed = maxSpeed;
    }

    public double getLength() { return length; }
    public double getWidth() { return width; }
    public double getMinSpeed() { return minSpeed; }
    public double getMaxSpeed() { return maxSpeed; }
}

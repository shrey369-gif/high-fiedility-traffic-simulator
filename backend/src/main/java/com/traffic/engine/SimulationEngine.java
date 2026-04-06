package com.traffic.engine;

import com.traffic.algorithm.*;
import com.traffic.model.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class SimulationEngine {
    private final List<Intersection> intersections;
    private final List<Road> roads;
    private final List<Vehicle> vehicles;
    private final SimulationConfig config;
    private final AStarPathfinder pathfinder;
    private final TrafficFlowOptimizer flowOptimizer;
    private final AdaptiveSignalController signalController;
    private final Map<Integer, List<Road>> vehiclePaths;
    private final Map<Integer, Integer> vehiclePathIndex;

    private long tick;
    private volatile boolean running;
    private int totalViolations;
    private int totalHonks;
    private final List<double[]> speedHistory;

    public SimulationEngine() {
        this.intersections = new ArrayList<>();
        this.roads = new ArrayList<>();
        this.vehicles = new CopyOnWriteArrayList<>();
        this.config = new SimulationConfig();
        this.pathfinder = new AStarPathfinder();
        this.flowOptimizer = new TrafficFlowOptimizer();
        this.signalController = new AdaptiveSignalController();
        this.vehiclePaths = new HashMap<>();
        this.vehiclePathIndex = new HashMap<>();
        this.tick = 0;
        this.running = false;
        this.totalViolations = 0;
        this.totalHonks = 0;
        this.speedHistory = new ArrayList<>();
        buildDefaultNetwork();
    }

    private void buildDefaultNetwork() {
        // Create a realistic Indian road grid (800x600 canvas coordinates)
        Intersection i1 = new Intersection(100, 100);
        Intersection i2 = new Intersection(400, 100);
        Intersection i3 = new Intersection(700, 100);
        Intersection i4 = new Intersection(100, 300);
        Intersection i5 = new Intersection(400, 300);
        Intersection i6 = new Intersection(700, 300);
        Intersection i7 = new Intersection(100, 500);
        Intersection i8 = new Intersection(400, 500);
        Intersection i9 = new Intersection(700, 500);

        intersections.addAll(Arrays.asList(i1, i2, i3, i4, i5, i6, i7, i8, i9));

        // Horizontal roads
        roads.add(new Road(i1, i2, 30, 50));
        roads.add(new Road(i2, i3, 30, 50));
        roads.add(new Road(i4, i5, 40, 40));   // Wider main road
        roads.add(new Road(i5, i6, 40, 40));
        roads.add(new Road(i7, i8, 30, 50));
        roads.add(new Road(i8, i9, 30, 50));

        // Vertical roads
        roads.add(new Road(i1, i4, 30, 50));
        roads.add(new Road(i4, i7, 30, 50));
        roads.add(new Road(i2, i5, 35, 45));   // Slightly wider
        roads.add(new Road(i5, i8, 35, 45));
        roads.add(new Road(i3, i6, 30, 50));
        roads.add(new Road(i6, i9, 30, 50));

        // Diagonal shortcut (Indian-style narrow alley)
        roads.add(new Road(i1, i5, 20, 30));
        roads.add(new Road(i5, i9, 20, 30));
    }

    public void loadScenario(List<double[]> intersectionPositions, List<int[]> roadConnections) {
        intersections.clear();
        roads.clear();
        vehicles.clear();
        vehiclePaths.clear();
        vehiclePathIndex.clear();

        Map<Integer, Intersection> interMap = new HashMap<>();
        for (int i = 0; i < intersectionPositions.size(); i++) {
            double[] pos = intersectionPositions.get(i);
            Intersection inter = new Intersection(i + 1, pos[0], pos[1]);
            intersections.add(inter);
            interMap.put(i, inter);
        }

        for (int[] conn : roadConnections) {
            Intersection src = interMap.get(conn[0]);
            Intersection dst = interMap.get(conn[1]);
            double width = conn.length > 2 ? conn[2] : 30;
            if (src != null && dst != null) {
                roads.add(new Road(src, dst, width, 50));
            }
        }
    }

    public void step() {
        if (!running || roads.isEmpty()) return;
        tick++;
        double dt = 0.016 * config.getSimulationSpeed();

        // Update signals
        for (Intersection inter : intersections) {
            inter.getSignal().setAdaptive(config.isAdaptiveSignals());
            inter.getSignal().tick();
        }

        // Adaptive signal control
        if (config.isAdaptiveSignals()) {
            signalController.update(intersections);
        }

        // Spawn vehicles
        if (vehicles.size() < config.getMaxVehicles() && Math.random() < config.getSpawnRate() * dt * 10) {
            spawnVehicle();
        }

        // Update vehicles
        List<Vehicle> toRemove = new ArrayList<>();
        for (Vehicle vehicle : vehicles) {
            updateVehicle(vehicle, dt);
            if (vehicle.hasReachedEnd()) {
                if (!advanceToNextRoad(vehicle)) {
                    toRemove.add(vehicle);
                }
            }
        }

        // Remove vehicles that completed their journey
        for (Vehicle v : toRemove) {
            v.getCurrentRoad().removeVehicle(v);
            vehicles.remove(v);
            vehiclePaths.remove(v.getId());
            vehiclePathIndex.remove(v.getId());
        }

        // Optimize flow
        flowOptimizer.optimizeFlow(roads);

        // Record history
        if (tick % 60 == 0) {
            double avgSpeed = vehicles.isEmpty() ? 0 :
                vehicles.stream().mapToDouble(Vehicle::getSpeed).average().orElse(0);
            double congestion = getState().getCongestionIndex();
            speedHistory.add(new double[]{tick, avgSpeed, congestion, vehicles.size()});
            if (speedHistory.size() > 100) speedHistory.remove(0);
        }
    }

    private void spawnVehicle() {
        if (roads.isEmpty()) return;

        // Pick random edge intersections as start and end
        List<Intersection> edgeIntersections = new ArrayList<>(intersections);
        Intersection start = edgeIntersections.get((int)(Math.random() * edgeIntersections.size()));
        Intersection goal = edgeIntersections.get((int)(Math.random() * edgeIntersections.size()));
        if (start.getId() == goal.getId()) return;

        // Find path
        List<Road> path = pathfinder.findPath(start, goal, roads);
        if (path.isEmpty()) {
            // Fallback: just put on a random road
            Road road = roads.get((int)(Math.random() * roads.size()));
            VehicleType type = randomVehicleType();
            DriverBehavior behavior = DriverBehavior.random();
            Vehicle vehicle = new Vehicle(type, behavior, road);
            vehicles.add(vehicle);
            return;
        }

        VehicleType type = randomVehicleType();
        DriverBehavior behavior = DriverBehavior.random();
        Vehicle vehicle = new Vehicle(type, behavior, path.get(0));
        vehicles.add(vehicle);
        vehiclePaths.put(vehicle.getId(), path);
        vehiclePathIndex.put(vehicle.getId(), 0);
    }

    private VehicleType randomVehicleType() {
        double r = Math.random();
        if (r < 0.25) return VehicleType.CAR;
        if (r < 0.50) return VehicleType.BIKE;
        if (r < 0.70) return VehicleType.AUTO_RICKSHAW;
        if (r < 0.85) return VehicleType.BUS;
        return VehicleType.TRUCK;
    }

    private void updateVehicle(Vehicle vehicle, double dt) {
        // Check signal at destination intersection
        Road road = vehicle.getCurrentRoad();
        Intersection destInter = road.getDestination();
        TrafficSignal signal = destInter.getSignal();

        if (vehicle.getProgress() > 0.85) {
            if (signal.getState() == TrafficSignal.SignalState.RED || signal.getState() == TrafficSignal.SignalState.YELLOW) {
                if (!vehicle.willViolateSignal()) {
                    // Stop at red
                    double stopSpeed = vehicle.getSpeed() * 0.85;
                    vehicle.setSpeed(Math.max(0, stopSpeed));
                    if (vehicle.getProgress() > 0.95) {
                        vehicle.setProgress(0.95);
                    }
                } else {
                    totalViolations++;
                }
            }
        }

        // Check for vehicles ahead
        for (Vehicle other : road.getVehicles()) {
            if (other.getId() != vehicle.getId() && other.getProgress() > vehicle.getProgress()) {
                double dist = (other.getProgress() - vehicle.getProgress()) * road.getLength();
                vehicle.adjustSpeedForTraffic(other.getSpeed(), dist);
                if (vehicle.isHonking()) totalHonks++;
                break;
            }
        }

        vehicle.update(dt);
    }

    private boolean advanceToNextRoad(Vehicle vehicle) {
        List<Road> path = vehiclePaths.get(vehicle.getId());
        Integer idx = vehiclePathIndex.get(vehicle.getId());

        if (path == null || idx == null) {
            // No path — pick a random connected road
            Road current = vehicle.getCurrentRoad();
            Intersection dest = current.getDestination();
            List<Road> options = dest.getConnectedRoads();
            if (options.isEmpty()) return false;

            Road next = options.get((int)(Math.random() * options.size()));
            vehicle.setCurrentRoad(next);
            return true;
        }

        int nextIdx = idx + 1;
        if (nextIdx >= path.size()) {
            return false; // Journey complete
        }

        vehicle.setCurrentRoad(path.get(nextIdx));
        vehiclePathIndex.put(vehicle.getId(), nextIdx);
        return true;
    }

    public SimulationState getState() {
        SimulationState state = new SimulationState(tick, new ArrayList<>(vehicles), intersections, roads);
        return state;
    }

    public String getInsightsJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"totalViolations\":").append(totalViolations).append(",");
        sb.append("\"totalHonks\":").append(totalHonks).append(",");
        sb.append("\"vehicleCount\":").append(vehicles.size()).append(",");

        // Vehicle type distribution
        int cars = 0, bikes = 0, autos = 0, buses = 0, trucks = 0;
        for (Vehicle v : vehicles) {
            switch (v.getType()) {
                case CAR: cars++; break;
                case BIKE: bikes++; break;
                case AUTO_RICKSHAW: autos++; break;
                case BUS: buses++; break;
                case TRUCK: trucks++; break;
            }
        }
        sb.append(String.format("\"distribution\":{\"cars\":%d,\"bikes\":%d,\"autos\":%d,\"buses\":%d,\"trucks\":%d},", cars, bikes, autos, buses, trucks));

        // Speed history
        sb.append("\"history\":[");
        for (int i = 0; i < speedHistory.size(); i++) {
            double[] entry = speedHistory.get(i);
            if (i > 0) sb.append(",");
            sb.append(String.format("{\"tick\":%.0f,\"avgSpeed\":%.1f,\"congestion\":%.2f,\"vehicles\":%.0f}", entry[0], entry[1], entry[2], entry[3]));
        }
        sb.append("]");

        sb.append("}");
        return sb.toString();
    }

    public void start() {
        running = true;
    }

    public void stop() {
        running = false;
    }

    public boolean isRunning() { return running; }
    public SimulationConfig getConfig() { return config; }
    public long getTick() { return tick; }
    public List<Intersection> getIntersections() { return intersections; }
    public List<Road> getRoads() { return roads; }
    public List<Vehicle> getVehicles() { return vehicles; }
}

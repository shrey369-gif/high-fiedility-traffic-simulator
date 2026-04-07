package com.traffic.scenario;

import java.util.*;

/**
 * Core scenario generation engine.
 * Takes a parsed road network and augments it with Indian road complexities:
 * - Road conditions (potholes, rough patches)
 * - Obstacles (barricades, construction zones, lane closures)
 * - Mixed traffic (cars, bikes, auto-rickshaws, buses, trucks)
 * - Seed-based reproducibility
 */
public class ScenarioBuilder {

    private Random random;

    // ===== SCENARIO CONFIGURATION =====
    public static class ScenarioConfig {
        public double trafficDensity = 0.5;        // 0.0 - 1.0
        public double roadConditionSeverity = 0.3;  // 0.0 - 1.0
        public boolean enableConstructionZones = true;
        public boolean enableLaneClosures = true;
        public boolean enablePotholes = true;
        public boolean enableBarricades = true;
        public long seed = -1;                      // -1 = random seed
        public String cityPreset = "delhi";

        public static ScenarioConfig fromJson(String json) {
            ScenarioConfig cfg = new ScenarioConfig();
            cfg.trafficDensity = getDouble(json, "trafficDensity", 0.5);
            cfg.roadConditionSeverity = getDouble(json, "roadConditionSeverity", 0.3);
            cfg.enableConstructionZones = getBool(json, "enableConstructionZones", true);
            cfg.enableLaneClosures = getBool(json, "enableLaneClosures", true);
            cfg.enablePotholes = getBool(json, "enablePotholes", true);
            cfg.enableBarricades = getBool(json, "enableBarricades", true);
            cfg.seed = (long) getDouble(json, "seed", -1);
            cfg.cityPreset = getString(json, "cityPreset", "delhi");
            return cfg;
        }

        private static double getDouble(String json, String key, double def) {
            String s = "\"" + key + "\"";
            int idx = json.indexOf(s);
            if (idx < 0) return def;
            idx = json.indexOf(":", idx) + 1;
            while (idx < json.length() && json.charAt(idx) == ' ') idx++;
            int end = idx;
            while (end < json.length() && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '.' || json.charAt(end) == '-')) end++;
            try { return Double.parseDouble(json.substring(idx, end)); }
            catch (Exception e) { return def; }
        }

        private static boolean getBool(String json, String key, boolean def) {
            String s = "\"" + key + "\"";
            int idx = json.indexOf(s);
            if (idx < 0) return def;
            idx = json.indexOf(":", idx) + 1;
            while (idx < json.length() && json.charAt(idx) == ' ') idx++;
            if (json.startsWith("true", idx)) return true;
            if (json.startsWith("false", idx)) return false;
            return def;
        }

        private static String getString(String json, String key, String def) {
            String s = "\"" + key + "\":\"";
            int idx = json.indexOf(s);
            if (idx < 0) return def;
            idx += s.length();
            int end = json.indexOf("\"", idx);
            if (end < 0) return def;
            return json.substring(idx, end);
        }
    }

    // ===== GENERATED SCENARIO OUTPUT =====
    public static class GeneratedScenario {
        public List<ScenarioRoad> roads = new ArrayList<>();
        public List<ScenarioVehicle> vehicles = new ArrayList<>();
        public List<ScenarioObstacle> obstacles = new ArrayList<>();
        public List<ScenarioCondition> conditions = new ArrayList<>();
        public List<ScenarioIntersection> intersections = new ArrayList<>();
        public ScenarioConfig config;
        public long usedSeed;

        public String toJson() {
            StringBuilder sb = new StringBuilder();
            sb.append("{");

            // Config
            sb.append("\"seed\":").append(usedSeed).append(",");
            sb.append("\"cityPreset\":\"").append(config.cityPreset).append("\",");

            // Intersections
            sb.append("\"intersections\":[");
            for (int i = 0; i < intersections.size(); i++) {
                if (i > 0) sb.append(",");
                intersections.get(i).appendJson(sb);
            }
            sb.append("],");

            // Roads
            sb.append("\"roads\":[");
            for (int i = 0; i < roads.size(); i++) {
                if (i > 0) sb.append(",");
                roads.get(i).appendJson(sb);
            }
            sb.append("],");

            // Vehicles
            sb.append("\"vehicles\":[");
            for (int i = 0; i < vehicles.size(); i++) {
                if (i > 0) sb.append(",");
                vehicles.get(i).appendJson(sb);
            }
            sb.append("],");

            // Obstacles
            sb.append("\"obstacles\":[");
            for (int i = 0; i < obstacles.size(); i++) {
                if (i > 0) sb.append(",");
                obstacles.get(i).appendJson(sb);
            }
            sb.append("],");

            // Conditions
            sb.append("\"conditions\":[");
            for (int i = 0; i < conditions.size(); i++) {
                if (i > 0) sb.append(",");
                conditions.get(i).appendJson(sb);
            }
            sb.append("]");

            sb.append("}");
            return sb.toString();
        }
    }

    // ===== DATA CLASSES =====

    public static class ScenarioIntersection {
        public int id;
        public double x, y;
        public boolean isJunction;
        public boolean hasSignal;

        public void appendJson(StringBuilder sb) {
            sb.append(String.format(Locale.US, "{\"id\":%d,\"x\":%.1f,\"y\":%.1f,\"isJunction\":%b,\"hasSignal\":%b}",
                id, x, y, isJunction, hasSignal));
        }
    }

    public static class ScenarioRoad {
        public int id;
        public int srcId, dstId;
        public double width, speedLimit, length;
        public int lanes;
        public String name;
        public double srcX, srcY, dstX, dstY;

        public void appendJson(StringBuilder sb) {
            sb.append(String.format(Locale.US,
                "{\"id\":%d,\"src\":%d,\"dst\":%d,\"width\":%.1f,\"speedLimit\":%.1f,\"length\":%.1f,\"lanes\":%d,\"name\":\"%s\",\"srcX\":%.1f,\"srcY\":%.1f,\"dstX\":%.1f,\"dstY\":%.1f}",
                id, srcId, dstId, width, speedLimit, length, lanes,
                name != null ? name.replace("\"", "") : "Road",
                srcX, srcY, dstX, dstY));
        }
    }

    public static class ScenarioVehicle {
        public int id;
        public String type; // CAR, BIKE, AUTO_RICKSHAW, BUS, TRUCK
        public String behavior; // AGGRESSIVE, NORMAL, DEFENSIVE
        public double x, y, speed, heading;
        public int roadId;

        public void appendJson(StringBuilder sb) {
            sb.append(String.format(Locale.US,
                "{\"id\":%d,\"type\":\"%s\",\"behavior\":\"%s\",\"x\":%.1f,\"y\":%.1f,\"speed\":%.1f,\"heading\":%.1f,\"roadId\":%d}",
                id, type, behavior, x, y, speed, heading, roadId));
        }
    }

    public static class ScenarioObstacle {
        public int id;
        public String type; // BARRICADE, CONSTRUCTION_ZONE, LANE_CLOSURE, PARKED_VEHICLE
        public double x, y, width, height;
        public int roadId;
        public double roadProgress; // 0.0-1.0 along the road

        public void appendJson(StringBuilder sb) {
            sb.append(String.format(Locale.US,
                "{\"id\":%d,\"type\":\"%s\",\"x\":%.1f,\"y\":%.1f,\"width\":%.1f,\"height\":%.1f,\"roadId\":%d,\"roadProgress\":%.2f}",
                id, type, x, y, width, height, roadId, roadProgress));
        }
    }

    public static class ScenarioCondition {
        public int id;
        public String type; // POTHOLE, ROUGH_PATCH, SPEED_BREAKER, WATERLOGGING
        public double x, y, radius;
        public double severity; // 0.0-1.0
        public int roadId;
        public double roadProgress;

        public void appendJson(StringBuilder sb) {
            sb.append(String.format(Locale.US,
                "{\"id\":%d,\"type\":\"%s\",\"x\":%.1f,\"y\":%.1f,\"radius\":%.1f,\"severity\":%.2f,\"roadId\":%d,\"roadProgress\":%.2f}",
                id, type, x, y, radius, severity, roadId, roadProgress));
        }
    }

    // ===== MAIN BUILD METHOD =====

    public GeneratedScenario build(OSMParser.ParsedMap map, ScenarioConfig config) {
        long seed = config.seed >= 0 ? config.seed : System.currentTimeMillis();
        this.random = new Random(seed);

        GeneratedScenario scenario = new GeneratedScenario();
        scenario.config = config;
        scenario.usedSeed = seed;

        // 1. Convert parsed intersections to scenario intersections
        Map<Integer, ScenarioIntersection> interMap = new HashMap<>();
        for (OSMParser.ParsedIntersection pi : map.intersections) {
            ScenarioIntersection si = new ScenarioIntersection();
            si.id = pi.id;
            si.x = pi.canvasX;
            si.y = pi.canvasY;
            si.isJunction = pi.isJunction;
            si.hasSignal = pi.isJunction && random.nextDouble() < 0.6;
            interMap.put(pi.id, si);
            scenario.intersections.add(si);
        }

        // 2. Convert parsed roads to scenario roads
        int roadId = 1;
        for (OSMParser.ParsedRoad pr : map.roads) {
            ScenarioRoad sr = new ScenarioRoad();
            sr.id = roadId++;

            // Resolve source/destination by index or node ID
            ScenarioIntersection src = null, dst = null;
            if (pr.srcIndex >= 0 && pr.srcIndex < scenario.intersections.size()) {
                src = scenario.intersections.get(pr.srcIndex);
            }
            if (pr.dstIndex >= 0 && pr.dstIndex < scenario.intersections.size()) {
                dst = scenario.intersections.get(pr.dstIndex);
            }

            // Fallback: find by osmNodeId
            if (src == null || dst == null) {
                for (OSMParser.ParsedIntersection pi : map.intersections) {
                    if (pi.osmNodeId == pr.srcNodeId && src == null) src = interMap.get(pi.id);
                    if (pi.osmNodeId == pr.dstNodeId && dst == null) dst = interMap.get(pi.id);
                }
            }

            if (src == null || dst == null) continue;

            sr.srcId = src.id;
            sr.dstId = dst.id;
            sr.srcX = src.x;
            sr.srcY = src.y;
            sr.dstX = dst.x;
            sr.dstY = dst.y;
            sr.width = pr.width;
            sr.speedLimit = pr.speedLimit;
            sr.lanes = pr.lanes;
            sr.name = pr.name;
            sr.length = Math.sqrt(Math.pow(dst.x - src.x, 2) + Math.pow(dst.y - src.y, 2));
            scenario.roads.add(sr);
        }

        // 3. Generate road conditions
        if (config.enablePotholes) {
            generateRoadConditions(scenario, config);
        }

        // 4. Generate obstacles
        generateObstacles(scenario, config);

        // 5. Generate traffic
        generateTraffic(scenario, config);

        return scenario;
    }

    // ===== ROAD CONDITION GENERATOR =====

    private void generateRoadConditions(GeneratedScenario scenario, ScenarioConfig config) {
        int condId = 1;
        for (ScenarioRoad road : scenario.roads) {
            double severity = config.roadConditionSeverity;

            // Potholes: Poisson-distributed along road
            int numPotholes = poissonRandom(severity * 4);
            for (int i = 0; i < numPotholes; i++) {
                ScenarioCondition cond = new ScenarioCondition();
                cond.id = condId++;
                cond.type = "POTHOLE";
                cond.roadId = road.id;
                cond.roadProgress = 0.1 + random.nextDouble() * 0.8;
                cond.severity = 0.3 + random.nextDouble() * 0.7 * severity;
                cond.radius = 2 + random.nextDouble() * 5;
                // Calculate position
                cond.x = road.srcX + (road.dstX - road.srcX) * cond.roadProgress;
                cond.y = road.srcY + (road.dstY - road.srcY) * cond.roadProgress;
                // Add lateral offset
                double perpX = -(road.dstY - road.srcY) / Math.max(road.length, 1);
                double perpY = (road.dstX - road.srcX) / Math.max(road.length, 1);
                double offset = (random.nextDouble() - 0.5) * road.width * 0.6;
                cond.x += perpX * offset;
                cond.y += perpY * offset;
                scenario.conditions.add(cond);
            }

            // Rough patches
            if (random.nextDouble() < severity * 0.5) {
                ScenarioCondition cond = new ScenarioCondition();
                cond.id = condId++;
                cond.type = "ROUGH_PATCH";
                cond.roadId = road.id;
                cond.roadProgress = 0.2 + random.nextDouble() * 0.6;
                cond.severity = 0.4 + random.nextDouble() * 0.6 * severity;
                cond.radius = road.width * 0.4 + random.nextDouble() * road.width * 0.3;
                cond.x = road.srcX + (road.dstX - road.srcX) * cond.roadProgress;
                cond.y = road.srcY + (road.dstY - road.srcY) * cond.roadProgress;
                scenario.conditions.add(cond);
            }

            // Speed breakers
            if (random.nextDouble() < severity * 0.3) {
                ScenarioCondition cond = new ScenarioCondition();
                cond.id = condId++;
                cond.type = "SPEED_BREAKER";
                cond.roadId = road.id;
                cond.roadProgress = 0.3 + random.nextDouble() * 0.4;
                cond.severity = 0.5;
                cond.radius = road.width * 0.5;
                cond.x = road.srcX + (road.dstX - road.srcX) * cond.roadProgress;
                cond.y = road.srcY + (road.dstY - road.srcY) * cond.roadProgress;
                scenario.conditions.add(cond);
            }
        }
    }

    // ===== OBSTACLE GENERATOR =====

    private void generateObstacles(GeneratedScenario scenario, ScenarioConfig config) {
        int obstId = 1;
        List<ScenarioRoad> eligibleRoads = new ArrayList<>(scenario.roads);

        // Construction zones
        if (config.enableConstructionZones) {
            int numZones = 1 + (int)(config.roadConditionSeverity * 3);
            Collections.shuffle(eligibleRoads, random);
            for (int i = 0; i < Math.min(numZones, eligibleRoads.size()); i++) {
                ScenarioRoad road = eligibleRoads.get(i);
                ScenarioObstacle obs = new ScenarioObstacle();
                obs.id = obstId++;
                obs.type = "CONSTRUCTION_ZONE";
                obs.roadId = road.id;
                obs.roadProgress = 0.3 + random.nextDouble() * 0.4;
                obs.width = road.width * 0.6;
                obs.height = 15 + random.nextDouble() * 25;
                obs.x = road.srcX + (road.dstX - road.srcX) * obs.roadProgress;
                obs.y = road.srcY + (road.dstY - road.srcY) * obs.roadProgress;
                scenario.obstacles.add(obs);
            }
        }

        // Barricades
        if (config.enableBarricades) {
            int numBarricades = (int)(config.roadConditionSeverity * 5);
            Collections.shuffle(eligibleRoads, random);
            for (int i = 0; i < Math.min(numBarricades, eligibleRoads.size()); i++) {
                ScenarioRoad road = eligibleRoads.get(i);
                ScenarioObstacle obs = new ScenarioObstacle();
                obs.id = obstId++;
                obs.type = "BARRICADE";
                obs.roadId = road.id;
                obs.roadProgress = 0.2 + random.nextDouble() * 0.6;
                obs.width = road.width * 0.3;
                obs.height = 3;
                obs.x = road.srcX + (road.dstX - road.srcX) * obs.roadProgress;
                obs.y = road.srcY + (road.dstY - road.srcY) * obs.roadProgress;
                // Offset to side of road
                double perpX = -(road.dstY - road.srcY) / Math.max(road.length, 1);
                double perpY = (road.dstX - road.srcX) / Math.max(road.length, 1);
                double side = random.nextBoolean() ? 1 : -1;
                obs.x += perpX * road.width * 0.3 * side;
                obs.y += perpY * road.width * 0.3 * side;
                scenario.obstacles.add(obs);
            }
        }

        // Lane closures
        if (config.enableLaneClosures) {
            int numClosures = (int)(config.roadConditionSeverity * 3);
            Collections.shuffle(eligibleRoads, random);
            for (int i = 0; i < Math.min(numClosures, eligibleRoads.size()); i++) {
                ScenarioRoad road = eligibleRoads.get(i);
                if (road.lanes < 2) continue;
                ScenarioObstacle obs = new ScenarioObstacle();
                obs.id = obstId++;
                obs.type = "LANE_CLOSURE";
                obs.roadId = road.id;
                obs.roadProgress = 0.15 + random.nextDouble() * 0.7;
                obs.width = road.width / road.lanes;
                obs.height = 30 + random.nextDouble() * 50;
                obs.x = road.srcX + (road.dstX - road.srcX) * obs.roadProgress;
                obs.y = road.srcY + (road.dstY - road.srcY) * obs.roadProgress;
                scenario.obstacles.add(obs);
            }
        }

        // Parked vehicles (always present in Indian roads)
        int numParked = 2 + (int)(config.trafficDensity * 6);
        Collections.shuffle(eligibleRoads, random);
        for (int i = 0; i < Math.min(numParked, eligibleRoads.size()); i++) {
            ScenarioRoad road = eligibleRoads.get(i);
            ScenarioObstacle obs = new ScenarioObstacle();
            obs.id = obstId++;
            obs.type = "PARKED_VEHICLE";
            obs.roadId = road.id;
            obs.roadProgress = random.nextDouble();
            obs.width = 2 + random.nextDouble() * 3;
            obs.height = 4 + random.nextDouble() * 4;
            obs.x = road.srcX + (road.dstX - road.srcX) * obs.roadProgress;
            obs.y = road.srcY + (road.dstY - road.srcY) * obs.roadProgress;
            // Push to edge of road
            double perpX = -(road.dstY - road.srcY) / Math.max(road.length, 1);
            double perpY = (road.dstX - road.srcX) / Math.max(road.length, 1);
            double side = random.nextBoolean() ? 1 : -1;
            obs.x += perpX * road.width * 0.4 * side;
            obs.y += perpY * road.width * 0.4 * side;
            scenario.obstacles.add(obs);
        }
    }

    // ===== TRAFFIC GENERATOR =====

    private void generateTraffic(GeneratedScenario scenario, ScenarioConfig config) {
        int vehId = 1;
        int totalVehicles = (int)(config.trafficDensity * scenario.roads.size() * 4);
        totalVehicles = Math.max(5, Math.min(totalVehicles, 200));

        // Indian traffic distribution
        String[] types = {"CAR", "BIKE", "AUTO_RICKSHAW", "BUS", "TRUCK"};
        double[] weights = {0.25, 0.30, 0.20, 0.15, 0.10};
        String[] behaviors = {"AGGRESSIVE", "NORMAL", "DEFENSIVE"};
        double[] behaviorWeights = {0.30, 0.45, 0.25};

        for (int i = 0; i < totalVehicles; i++) {
            ScenarioRoad road = scenario.roads.get(random.nextInt(scenario.roads.size()));
            ScenarioVehicle veh = new ScenarioVehicle();
            veh.id = vehId++;
            veh.type = weightedChoice(types, weights);
            veh.behavior = weightedChoice(behaviors, behaviorWeights);
            veh.roadId = road.id;

            double progress = random.nextDouble();
            veh.x = road.srcX + (road.dstX - road.srcX) * progress;
            veh.y = road.srcY + (road.dstY - road.srcY) * progress;

            // Add lateral offset (lane-less driving)
            double perpX = -(road.dstY - road.srcY) / Math.max(road.length, 1);
            double perpY = (road.dstX - road.srcX) / Math.max(road.length, 1);
            double lateralOffset = (random.nextDouble() - 0.5) * road.width * 0.7;
            veh.x += perpX * lateralOffset;
            veh.y += perpY * lateralOffset;

            veh.heading = Math.toDegrees(Math.atan2(road.dstY - road.srcY, road.dstX - road.srcX));
            veh.speed = getBaseSpeed(veh.type) * (0.5 + random.nextDouble() * 0.5);

            scenario.vehicles.add(veh);
        }
    }

    private double getBaseSpeed(String type) {
        switch (type) {
            case "CAR": return 40 + random.nextDouble() * 40;
            case "BIKE": return 30 + random.nextDouble() * 50;
            case "AUTO_RICKSHAW": return 25 + random.nextDouble() * 25;
            case "BUS": return 20 + random.nextDouble() * 30;
            case "TRUCK": return 20 + random.nextDouble() * 25;
            default: return 30;
        }
    }

    // ===== UTILITY =====

    private int poissonRandom(double lambda) {
        double L = Math.exp(-lambda);
        int k = 0;
        double p = 1.0;
        do {
            k++;
            p *= random.nextDouble();
        } while (p > L);
        return k - 1;
    }

    private String weightedChoice(String[] options, double[] weights) {
        double r = random.nextDouble();
        double cumulative = 0;
        for (int i = 0; i < options.length; i++) {
            cumulative += weights[i];
            if (r < cumulative) return options[i];
        }
        return options[options.length - 1];
    }
}

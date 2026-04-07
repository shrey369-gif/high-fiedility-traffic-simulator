package com.traffic.scenario;

import java.util.*;

/**
 * Parses OpenStreetMap (OSM) XML data to extract road networks.
 * Converts geographic coordinates to canvas-space coordinates.
 * 
 * Supports: nodes, ways with highway tags, lane data extraction.
 */
public class OSMParser {

    // Parsed node: id -> [lat, lon]
    private final Map<Long, double[]> nodes = new HashMap<>();
    // Parsed roads
    private final List<ParsedRoad> parsedRoads = new ArrayList<>();
    // Parsed intersections (nodes referenced by 2+ ways)
    private final List<ParsedIntersection> parsedIntersections = new ArrayList<>();

    // Canvas dimensions for coordinate mapping
    private static final double CANVAS_W = 800.0;
    private static final double CANVAS_H = 600.0;

    /**
     * Parse OSM XML string and extract road network.
     */
    public ParsedMap parse(String osmXml) {
        nodes.clear();
        parsedRoads.clear();
        parsedIntersections.clear();

        // Phase 1: Extract all nodes
        parseNodes(osmXml);

        // Phase 2: Extract ways (roads)
        parseWays(osmXml);

        // Phase 3: Identify intersections (nodes used by multiple ways)
        identifyIntersections();

        // Phase 4: Convert to canvas coordinates
        normalizeCoordinates();

        return new ParsedMap(
            new ArrayList<>(parsedIntersections),
            new ArrayList<>(parsedRoads)
        );
    }

    /**
     * Generate a default demo map (Connaught Place, Delhi style grid).
     */
    public ParsedMap generateDemoMap(String cityPreset) {
        nodes.clear();
        parsedRoads.clear();
        parsedIntersections.clear();

        switch (cityPreset.toLowerCase()) {
            case "mumbai":
                generateMumbaiNetwork();
                break;
            case "bangalore":
                generateBangaloreNetwork();
                break;
            case "delhi":
            default:
                generateDelhiNetwork();
                break;
        }

        return new ParsedMap(
            new ArrayList<>(parsedIntersections),
            new ArrayList<>(parsedRoads)
        );
    }

    private void parseNodes(String xml) {
        int searchFrom = 0;
        while (true) {
            int nodeStart = xml.indexOf("<node", searchFrom);
            if (nodeStart < 0) break;
            int nodeEnd = xml.indexOf(">", nodeStart);
            if (nodeEnd < 0) break;

            String nodeTag = xml.substring(nodeStart, nodeEnd + 1);
            long id = parseLongAttr(nodeTag, "id");
            double lat = parseDoubleAttr(nodeTag, "lat");
            double lon = parseDoubleAttr(nodeTag, "lon");

            if (id > 0 && !Double.isNaN(lat) && !Double.isNaN(lon)) {
                nodes.put(id, new double[]{lat, lon});
            }
            searchFrom = nodeEnd + 1;
        }
    }

    private void parseWays(String xml) {
        int searchFrom = 0;
        while (true) {
            int wayStart = xml.indexOf("<way", searchFrom);
            if (wayStart < 0) break;
            int wayEnd = xml.indexOf("</way>", wayStart);
            if (wayEnd < 0) break;

            String wayBlock = xml.substring(wayStart, wayEnd + 6);

            // Check if it's a highway
            if (!isHighway(wayBlock)) {
                searchFrom = wayEnd + 6;
                continue;
            }

            // Extract node references
            List<Long> nodeRefs = new ArrayList<>();
            int refSearch = 0;
            while (true) {
                int ndIdx = wayBlock.indexOf("<nd", refSearch);
                if (ndIdx < 0) break;
                int ndEnd = wayBlock.indexOf("/>", ndIdx);
                if (ndEnd < 0) break;
                String ndTag = wayBlock.substring(ndIdx, ndEnd + 2);
                long ref = parseLongAttr(ndTag, "ref");
                if (ref > 0) nodeRefs.add(ref);
                refSearch = ndEnd + 2;
            }

            // Extract road properties
            String highwayType = getTagValue(wayBlock, "highway");
            int lanes = parseIntTag(wayBlock, "lanes", getDefaultLanes(highwayType));
            double width = getDefaultWidth(highwayType);
            double speedLimit = getDefaultSpeedLimit(highwayType);
            String name = getTagValue(wayBlock, "name");
            if (name == null) name = highwayType;

            // Create road segments between consecutive nodes
            for (int i = 0; i < nodeRefs.size() - 1; i++) {
                long srcId = nodeRefs.get(i);
                long dstId = nodeRefs.get(i + 1);
                if (nodes.containsKey(srcId) && nodes.containsKey(dstId)) {
                    parsedRoads.add(new ParsedRoad(srcId, dstId, width, speedLimit, lanes, name));
                }
            }

            searchFrom = wayEnd + 6;
        }
    }

    private void identifyIntersections() {
        // Count how many roads reference each node
        Map<Long, Integer> nodeRefCount = new HashMap<>();
        Set<Long> allRoadNodes = new LinkedHashSet<>();

        for (ParsedRoad road : parsedRoads) {
            allRoadNodes.add(road.srcNodeId);
            allRoadNodes.add(road.dstNodeId);
            nodeRefCount.merge(road.srcNodeId, 1, Integer::sum);
            nodeRefCount.merge(road.dstNodeId, 1, Integer::sum);
        }

        // Nodes used by 2+ roads OR at endpoints are intersections
        int id = 1;
        for (long nodeId : allRoadNodes) {
            double[] coords = nodes.get(nodeId);
            if (coords != null) {
                int refCount = nodeRefCount.getOrDefault(nodeId, 0);
                boolean isJunction = refCount >= 2;
                parsedIntersections.add(new ParsedIntersection(
                    id++, nodeId, coords[0], coords[1], isJunction
                ));
            }
        }
    }

    private void normalizeCoordinates() {
        if (parsedIntersections.isEmpty()) return;

        double minLat = Double.MAX_VALUE, maxLat = -Double.MAX_VALUE;
        double minLon = Double.MAX_VALUE, maxLon = -Double.MAX_VALUE;

        for (ParsedIntersection pi : parsedIntersections) {
            minLat = Math.min(minLat, pi.lat);
            maxLat = Math.max(maxLat, pi.lat);
            minLon = Math.min(minLon, pi.lon);
            maxLon = Math.max(maxLon, pi.lon);
        }

        double latRange = maxLat - minLat;
        double lonRange = maxLon - minLon;
        if (latRange == 0) latRange = 1;
        if (lonRange == 0) lonRange = 1;

        double padding = 60;
        double drawW = CANVAS_W - 2 * padding;
        double drawH = CANVAS_H - 2 * padding;

        for (ParsedIntersection pi : parsedIntersections) {
            pi.canvasX = padding + ((pi.lon - minLon) / lonRange) * drawW;
            pi.canvasY = padding + ((maxLat - pi.lat) / latRange) * drawH; // Flip Y
        }
    }

    // --- Demo city networks ---

    private void generateDelhiNetwork() {
        // Connaught Place inspired radial + grid layout
        double cx = 400, cy = 300;
        int id = 1;

        // Central hub
        parsedIntersections.add(new ParsedIntersection(id++, 0, 0, 0, true, cx, cy));

        // Inner ring (6 points)
        double r1 = 120;
        for (int i = 0; i < 6; i++) {
            double angle = Math.toRadians(i * 60);
            parsedIntersections.add(new ParsedIntersection(id++, 0, 0, 0, true,
                cx + r1 * Math.cos(angle), cy + r1 * Math.sin(angle)));
        }

        // Outer ring (6 points)
        double r2 = 240;
        for (int i = 0; i < 6; i++) {
            double angle = Math.toRadians(i * 60 + 30);
            parsedIntersections.add(new ParsedIntersection(id++, 0, 0, 0, true,
                cx + r2 * Math.cos(angle), cy + r2 * Math.sin(angle)));
        }

        // Connect center to inner ring
        for (int i = 1; i <= 6; i++) {
            parsedRoads.add(new ParsedRoad(0, 0, 35, 40, 2, "Radial Road " + i, 0, i));
        }
        // Inner ring connections
        for (int i = 1; i <= 6; i++) {
            parsedRoads.add(new ParsedRoad(0, 0, 30, 40, 2, "Inner Ring", i, (i % 6) + 1));
        }
        // Inner to outer
        for (int i = 0; i < 6; i++) {
            parsedRoads.add(new ParsedRoad(0, 0, 25, 50, 3, "Arterial Road", i + 1, i + 7));
            parsedRoads.add(new ParsedRoad(0, 0, 25, 50, 3, "Arterial Road", (i % 6) + 1 + 1 > 6 ? 1 : (i % 6) + 2, i + 7));
        }
        // Outer ring connections
        for (int i = 7; i <= 12; i++) {
            parsedRoads.add(new ParsedRoad(0, 0, 20, 50, 2, "Outer Ring", i, (i - 7 + 1) % 6 + 7));
        }
    }

    private void generateMumbaiNetwork() {
        // Linear coastal road + grid pattern
        int id = 1;
        // Vertical spine (Marine Drive style)
        for (int i = 0; i < 6; i++) {
            parsedIntersections.add(new ParsedIntersection(id++, 0, 0, 0, true,
                200, 80 + i * 90));
        }
        // Parallel spine
        for (int i = 0; i < 6; i++) {
            parsedIntersections.add(new ParsedIntersection(id++, 0, 0, 0, true,
                450, 80 + i * 90));
        }
        // Eastern connections
        for (int i = 0; i < 4; i++) {
            parsedIntersections.add(new ParsedIntersection(id++, 0, 0, 0, true,
                650, 120 + i * 120));
        }

        // Western spine roads
        for (int i = 0; i < 5; i++) {
            parsedRoads.add(new ParsedRoad(0, 0, 40, 50, 3, "Western Express", i, i + 1));
        }
        // Central spine roads
        for (int i = 6; i < 11; i++) {
            parsedRoads.add(new ParsedRoad(0, 0, 35, 40, 2, "SV Road", i, i + 1));
        }
        // Cross connections
        for (int i = 0; i < 6; i++) {
            parsedRoads.add(new ParsedRoad(0, 0, 25, 30, 2, "Link Road", i, i + 6));
        }
        // Eastern links
        parsedRoads.add(new ParsedRoad(0, 0, 30, 40, 2, "Eastern Freeway", 6, 12));
        parsedRoads.add(new ParsedRoad(0, 0, 30, 40, 2, "Eastern Freeway", 8, 13));
        parsedRoads.add(new ParsedRoad(0, 0, 30, 40, 2, "Eastern Freeway", 10, 14));
        parsedRoads.add(new ParsedRoad(0, 0, 30, 40, 2, "Eastern Freeway", 11, 15));
    }

    private void generateBangaloreNetwork() {
        // Outer ring road + central grid
        int id = 1;
        double cx = 400, cy = 300;

        // Central grid (4x3)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 4; col++) {
                parsedIntersections.add(new ParsedIntersection(id++, 0, 0, 0, true,
                    200 + col * 150, 150 + row * 150));
            }
        }

        // Horizontal roads
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int srcIdx = row * 4 + col;
                int dstIdx = row * 4 + col + 1;
                double w = row == 1 ? 40 : 25; // MG Road wider
                parsedRoads.add(new ParsedRoad(0, 0, w, 40, 2, row == 1 ? "MG Road" : "Cross Street", srcIdx, dstIdx));
            }
        }
        // Vertical roads
        for (int col = 0; col < 4; col++) {
            for (int row = 0; row < 2; row++) {
                int srcIdx = row * 4 + col;
                int dstIdx = (row + 1) * 4 + col;
                parsedRoads.add(new ParsedRoad(0, 0, 30, 40, 2, "Cross Road", srcIdx, dstIdx));
            }
        }
        // Diagonal shortcuts
        parsedRoads.add(new ParsedRoad(0, 0, 20, 30, 1, "Narrow Lane", 0, 5));
        parsedRoads.add(new ParsedRoad(0, 0, 20, 30, 1, "Narrow Lane", 3, 6));
        parsedRoads.add(new ParsedRoad(0, 0, 20, 30, 1, "Narrow Lane", 5, 10));
        parsedRoads.add(new ParsedRoad(0, 0, 20, 30, 1, "Narrow Lane", 6, 11));
    }

    // --- Attribute parsers ---

    private long parseLongAttr(String tag, String attr) {
        String search = attr + "=\"";
        int idx = tag.indexOf(search);
        if (idx < 0) return -1;
        idx += search.length();
        int end = tag.indexOf("\"", idx);
        if (end < 0) return -1;
        try { return Long.parseLong(tag.substring(idx, end)); }
        catch (NumberFormatException e) { return -1; }
    }

    private double parseDoubleAttr(String tag, String attr) {
        String search = attr + "=\"";
        int idx = tag.indexOf(search);
        if (idx < 0) return Double.NaN;
        idx += search.length();
        int end = tag.indexOf("\"", idx);
        if (end < 0) return Double.NaN;
        try { return Double.parseDouble(tag.substring(idx, end)); }
        catch (NumberFormatException e) { return Double.NaN; }
    }

    private boolean isHighway(String wayBlock) {
        return wayBlock.contains("k=\"highway\"");
    }

    private String getTagValue(String block, String key) {
        String search = "k=\"" + key + "\" v=\"";
        int idx = block.indexOf(search);
        if (idx < 0) return null;
        idx += search.length();
        int end = block.indexOf("\"", idx);
        if (end < 0) return null;
        return block.substring(idx, end);
    }

    private int parseIntTag(String block, String key, int def) {
        String val = getTagValue(block, key);
        if (val == null) return def;
        try { return Integer.parseInt(val); }
        catch (NumberFormatException e) { return def; }
    }

    private double getDefaultWidth(String type) {
        if (type == null) return 25;
        switch (type) {
            case "motorway": case "trunk": return 40;
            case "primary": return 35;
            case "secondary": return 30;
            case "tertiary": return 25;
            case "residential": return 20;
            case "service": return 15;
            default: return 25;
        }
    }

    private double getDefaultSpeedLimit(String type) {
        if (type == null) return 40;
        switch (type) {
            case "motorway": return 80;
            case "trunk": return 60;
            case "primary": return 50;
            case "secondary": return 40;
            case "tertiary": return 35;
            case "residential": return 25;
            default: return 40;
        }
    }

    private int getDefaultLanes(String type) {
        if (type == null) return 2;
        switch (type) {
            case "motorway": return 4;
            case "trunk": return 3;
            case "primary": return 3;
            case "secondary": return 2;
            default: return 2;
        }
    }

    // --- Inner data classes ---

    public static class ParsedMap {
        public final List<ParsedIntersection> intersections;
        public final List<ParsedRoad> roads;

        public ParsedMap(List<ParsedIntersection> intersections, List<ParsedRoad> roads) {
            this.intersections = intersections;
            this.roads = roads;
        }
    }

    public static class ParsedIntersection {
        public final int id;
        public final long osmNodeId;
        public final double lat;
        public final double lon;
        public final boolean isJunction;
        public double canvasX;
        public double canvasY;

        public ParsedIntersection(int id, long osmNodeId, double lat, double lon, boolean isJunction) {
            this.id = id;
            this.osmNodeId = osmNodeId;
            this.lat = lat;
            this.lon = lon;
            this.isJunction = isJunction;
        }

        public ParsedIntersection(int id, long osmNodeId, double lat, double lon, boolean isJunction,
                                   double canvasX, double canvasY) {
            this(id, osmNodeId, lat, lon, isJunction);
            this.canvasX = canvasX;
            this.canvasY = canvasY;
        }
    }

    public static class ParsedRoad {
        public final long srcNodeId;
        public final long dstNodeId;
        public final double width;
        public final double speedLimit;
        public final int lanes;
        public final String name;
        // Index-based references for demo maps
        public int srcIndex = -1;
        public int dstIndex = -1;

        public ParsedRoad(long srcNodeId, long dstNodeId, double width, double speedLimit, int lanes, String name) {
            this.srcNodeId = srcNodeId;
            this.dstNodeId = dstNodeId;
            this.width = width;
            this.speedLimit = speedLimit;
            this.lanes = lanes;
            this.name = name;
        }

        public ParsedRoad(long srcNodeId, long dstNodeId, double width, double speedLimit, int lanes, String name,
                          int srcIndex, int dstIndex) {
            this(srcNodeId, dstNodeId, width, speedLimit, lanes, name);
            this.srcIndex = srcIndex;
            this.dstIndex = dstIndex;
        }
    }
}

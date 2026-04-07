package com.traffic.scenario;

import com.sun.net.httpserver.*;
import com.traffic.util.JsonUtil;
import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 * HTTP handler for scenario generation endpoints.
 * Registers: /api/scenario/generate, /api/scenario/export, /api/scenario/osm-parse
 */
public class ScenarioGeneratorHandler {

    private final OSMParser osmParser = new OSMParser();
    private final ScenarioBuilder scenarioBuilder = new ScenarioBuilder();
    private final MatlabExportService exportService = new MatlabExportService();

    // Store last generated scenario for export
    private ScenarioBuilder.GeneratedScenario lastScenario;

    public void registerEndpoints(HttpServer server) {
        server.createContext("/api/scenario/generate", this::handleGenerate);
        server.createContext("/api/scenario/export", this::handleExport);
        server.createContext("/api/scenario/osm-parse", this::handleOsmParse);
    }

    private void addCorsHeaders(HttpExchange exchange) {
        Headers headers = exchange.getResponseHeaders();
        headers.add("Access-Control-Allow-Origin", "*");
        headers.add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        headers.add("Access-Control-Allow-Headers", "Content-Type");
    }

    private void sendJson(HttpExchange exchange, int code, String json) throws IOException {
        addCorsHeaders(exchange);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) { os.write(bytes); }
    }

    private void sendText(HttpExchange exchange, int code, String text, String contentType) throws IOException {
        addCorsHeaders(exchange);
        exchange.getResponseHeaders().add("Content-Type", contentType);
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) { os.write(bytes); }
    }

    /**
     * POST /api/scenario/generate
     * Body: { trafficDensity, roadConditionSeverity, enableConstructionZones, ... , cityPreset, osmData? }
     * Returns: generated scenario JSON
     */
    private void handleGenerate(HttpExchange exchange) throws IOException {
        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            addCorsHeaders(exchange); exchange.sendResponseHeaders(204, -1); return;
        }
        if (!"POST".equals(exchange.getRequestMethod())) {
            sendJson(exchange, 405, JsonUtil.errorJson("POST required")); return;
        }

        try {
            String body = JsonUtil.readRequestBody(exchange.getRequestBody());
            ScenarioBuilder.ScenarioConfig config = ScenarioBuilder.ScenarioConfig.fromJson(body);

            // Check if OSM data was provided
            String osmData = extractOsmData(body);
            OSMParser.ParsedMap map;

            if (osmData != null && !osmData.isEmpty()) {
                map = osmParser.parse(osmData);
            } else {
                map = osmParser.generateDemoMap(config.cityPreset);
            }

            ScenarioBuilder.GeneratedScenario scenario = scenarioBuilder.build(map, config);
            lastScenario = scenario;

            sendJson(exchange, 200, scenario.toJson());
        } catch (Exception e) {
            sendJson(exchange, 500, JsonUtil.errorJson("Generation failed: " + e.getMessage()));
        }
    }

    /**
     * POST /api/scenario/export
     * Body: { format: "matlab" | "roadrunner" | "json", scenario?: {...} }
     * Returns: exported file content
     */
    private void handleExport(HttpExchange exchange) throws IOException {
        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            addCorsHeaders(exchange); exchange.sendResponseHeaders(204, -1); return;
        }
        if (!"POST".equals(exchange.getRequestMethod())) {
            sendJson(exchange, 405, JsonUtil.errorJson("POST required")); return;
        }

        try {
            String body = JsonUtil.readRequestBody(exchange.getRequestBody());
            String format = JsonUtil.getStringValue(body, "format");
            if (format == null) format = "matlab";

            ScenarioBuilder.GeneratedScenario scenario = lastScenario;
            if (scenario == null) {
                sendJson(exchange, 400, JsonUtil.errorJson("No scenario generated yet. Call /generate first."));
                return;
            }

            switch (format) {
                case "matlab":
                    String mScript = exportService.exportToMatlabScript(scenario);
                    sendJson(exchange, 200, "{\"format\":\"matlab\",\"filename\":\"indian_scenario.m\",\"content\":" +
                        escapeJsonString(mScript) + "}");
                    break;
                case "roadrunner":
                    String rrScene = exportService.exportToRoadRunnerScene(scenario);
                    sendJson(exchange, 200, "{\"format\":\"roadrunner\",\"filename\":\"scene.rrscene\",\"content\":" +
                        escapeJsonString(rrScene) + "}");
                    break;
                case "json":
                default:
                    sendJson(exchange, 200, "{\"format\":\"json\",\"filename\":\"scenario.json\",\"content\":" +
                        escapeJsonString(scenario.toJson()) + "}");
                    break;
            }
        } catch (Exception e) {
            sendJson(exchange, 500, JsonUtil.errorJson("Export failed: " + e.getMessage()));
        }
    }

    /**
     * POST /api/scenario/osm-parse
     * Body: { osmXml: "..." }
     * Returns: parsed road network summary
     */
    private void handleOsmParse(HttpExchange exchange) throws IOException {
        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            addCorsHeaders(exchange); exchange.sendResponseHeaders(204, -1); return;
        }
        if (!"POST".equals(exchange.getRequestMethod())) {
            sendJson(exchange, 405, JsonUtil.errorJson("POST required")); return;
        }

        try {
            String body = JsonUtil.readRequestBody(exchange.getRequestBody());
            String osmXml = extractOsmData(body);

            if (osmXml == null || osmXml.isEmpty()) {
                sendJson(exchange, 400, JsonUtil.errorJson("No osmData provided"));
                return;
            }

            OSMParser.ParsedMap map = osmParser.parse(osmXml);
            sendJson(exchange, 200, String.format(
                "{\"status\":\"parsed\",\"intersections\":%d,\"roads\":%d}",
                map.intersections.size(), map.roads.size()));
        } catch (Exception e) {
            sendJson(exchange, 500, JsonUtil.errorJson("Parse failed: " + e.getMessage()));
        }
    }

    private String extractOsmData(String json) {
        String key = "\"osmData\":\"";
        int idx = json.indexOf(key);
        if (idx < 0) return null;
        idx += key.length();
        int end = json.indexOf("\"", idx);
        while (end > 0 && json.charAt(end - 1) == '\\') {
            end = json.indexOf("\"", end + 1);
        }
        if (end < 0) return null;
        return json.substring(idx, end).replace("\\n", "\n").replace("\\\"", "\"");
    }

    private String escapeJsonString(String s) {
        StringBuilder sb = new StringBuilder("\"");
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default: sb.append(c);
            }
        }
        sb.append("\"");
        return sb.toString();
    }
}

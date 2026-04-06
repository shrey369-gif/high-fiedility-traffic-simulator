package com.traffic.server;

import com.sun.net.httpserver.*;
import com.traffic.engine.SimulationEngine;
import com.traffic.util.JsonUtil;
import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;

public class HttpSimServer {
    private final HttpServer server;
    private final SimulationEngine engine;
    private ScheduledExecutorService simulationExecutor;
    private final CopyOnWriteArrayList<OutputStream> sseClients;

    public HttpSimServer(int port, SimulationEngine engine) throws IOException {
        this.engine = engine;
        this.sseClients = new CopyOnWriteArrayList<>();
        this.server = HttpServer.create(new InetSocketAddress(port), 0);

        server.createContext("/api/simulation/start", this::handleStart);
        server.createContext("/api/simulation/stop", this::handleStop);
        server.createContext("/api/simulation/stream", this::handleStream);
        server.createContext("/api/simulation/config", this::handleConfig);
        server.createContext("/api/simulation/state", this::handleState);
        server.createContext("/api/scenario/create", this::handleScenarioCreate);
        server.createContext("/api/insights/stats", this::handleInsights);
        server.createContext("/api/health", this::handleHealth);

        server.setExecutor(Executors.newFixedThreadPool(10));
    }

    public void start() {
        server.start();
        System.out.println("Traffic Simulation Server running on port " + server.getAddress().getPort());
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
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private void handleStart(HttpExchange exchange) throws IOException {
        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            addCorsHeaders(exchange);
            exchange.sendResponseHeaders(204, -1);
            return;
        }
        engine.start();
        startSimulationLoop();
        sendJson(exchange, 200, JsonUtil.statusJson("running"));
    }

    private void handleStop(HttpExchange exchange) throws IOException {
        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            addCorsHeaders(exchange);
            exchange.sendResponseHeaders(204, -1);
            return;
        }
        engine.stop();
        stopSimulationLoop();
        sendJson(exchange, 200, JsonUtil.statusJson("stopped"));
    }

    private void handleStream(HttpExchange exchange) throws IOException {
        addCorsHeaders(exchange);
        Headers headers = exchange.getResponseHeaders();
        headers.add("Content-Type", "text/event-stream");
        headers.add("Cache-Control", "no-cache");
        headers.add("Connection", "keep-alive");
        exchange.sendResponseHeaders(200, 0);

        OutputStream os = exchange.getResponseBody();
        sseClients.add(os);

        // Keep connection alive
        try {
            while (true) {
                Thread.sleep(1000);
                os.write(":keepalive\n\n".getBytes(StandardCharsets.UTF_8));
                os.flush();
            }
        } catch (Exception e) {
            sseClients.remove(os);
            try { os.close(); } catch (Exception ignored) {}
        }
    }

    private void handleConfig(HttpExchange exchange) throws IOException {
        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            addCorsHeaders(exchange);
            exchange.sendResponseHeaders(204, -1);
            return;
        }
        if ("POST".equals(exchange.getRequestMethod())) {
            String body = JsonUtil.readRequestBody(exchange.getRequestBody());
            double density = JsonUtil.getDoubleValue(body, "density", -1);
            if (density >= 0) engine.getConfig().setTrafficDensity(density);
            boolean adaptive = JsonUtil.getBooleanValue(body, "adaptiveSignals", engine.getConfig().isAdaptiveSignals());
            engine.getConfig().setAdaptiveSignals(adaptive);
            double speed = JsonUtil.getDoubleValue(body, "speed", -1);
            if (speed > 0) engine.getConfig().setSimulationSpeed(speed);
            sendJson(exchange, 200, engine.getConfig().toJson());
        } else {
            sendJson(exchange, 200, engine.getConfig().toJson());
        }
    }

    private void handleState(HttpExchange exchange) throws IOException {
        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            addCorsHeaders(exchange);
            exchange.sendResponseHeaders(204, -1);
            return;
        }
        sendJson(exchange, 200, engine.getState().toJson());
    }

    private void handleScenarioCreate(HttpExchange exchange) throws IOException {
        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            addCorsHeaders(exchange);
            exchange.sendResponseHeaders(204, -1);
            return;
        }
        if ("POST".equals(exchange.getRequestMethod())) {
            String body = JsonUtil.readRequestBody(exchange.getRequestBody());
            // Parse intersections and roads from JSON body
            // Simple format: { "intersections": [[x,y],...], "roads": [[src,dst,width],...] }
            try {
                var intersectionPositions = parseArrayOfDoubleArrays(body, "intersections");
                var roadConnections = parseArrayOfIntArrays(body, "roads");
                engine.stop();
                stopSimulationLoop();
                engine.loadScenario(intersectionPositions, roadConnections);
                sendJson(exchange, 200, JsonUtil.statusJson("scenario_loaded"));
            } catch (Exception e) {
                sendJson(exchange, 400, JsonUtil.errorJson("Invalid scenario format: " + e.getMessage()));
            }
        } else {
            sendJson(exchange, 405, JsonUtil.errorJson("POST required"));
        }
    }

    private void handleInsights(HttpExchange exchange) throws IOException {
        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            addCorsHeaders(exchange);
            exchange.sendResponseHeaders(204, -1);
            return;
        }
        sendJson(exchange, 200, engine.getInsightsJson());
    }

    private void handleHealth(HttpExchange exchange) throws IOException {
        addCorsHeaders(exchange);
        sendJson(exchange, 200, "{\"status\":\"ok\",\"running\":" + engine.isRunning() + ",\"tick\":" + engine.getTick() + "}");
    }

    private void startSimulationLoop() {
        if (simulationExecutor != null && !simulationExecutor.isShutdown()) return;
        simulationExecutor = Executors.newSingleThreadScheduledExecutor();
        simulationExecutor.scheduleAtFixedRate(() -> {
            try {
                engine.step();
                broadcastState();
            } catch (Exception e) {
                System.err.println("Simulation error: " + e.getMessage());
            }
        }, 0, 16, TimeUnit.MILLISECONDS); // ~60 FPS
    }

    private void stopSimulationLoop() {
        if (simulationExecutor != null) {
            simulationExecutor.shutdown();
            simulationExecutor = null;
        }
    }

    private void broadcastState() {
        if (sseClients.isEmpty()) return;
        String json = engine.getState().toJson();
        String event = "data: " + json + "\n\n";
        byte[] bytes = event.getBytes(StandardCharsets.UTF_8);

        for (OutputStream os : sseClients) {
            try {
                os.write(bytes);
                os.flush();
            } catch (Exception e) {
                sseClients.remove(os);
            }
        }
    }

    // Simple JSON array parsers
    private java.util.List<double[]> parseArrayOfDoubleArrays(String json, String key) {
        java.util.List<double[]> result = new java.util.ArrayList<>();
        String search = "\"" + key + "\"";
        int idx = json.indexOf(search);
        if (idx < 0) return result;
        idx = json.indexOf("[", idx);
        if (idx < 0) return result;
        idx++; // skip outer [

        while (idx < json.length()) {
            idx = json.indexOf("[", idx);
            if (idx < 0) break;
            int end = json.indexOf("]", idx);
            if (end < 0) break;
            String inner = json.substring(idx + 1, end).trim();
            String[] parts = inner.split(",");
            if (parts.length >= 2) {
                result.add(new double[]{
                    Double.parseDouble(parts[0].trim()),
                    Double.parseDouble(parts[1].trim())
                });
            }
            idx = end + 1;
            if (idx < json.length() && json.charAt(idx) == ']') break;
        }
        return result;
    }

    private java.util.List<int[]> parseArrayOfIntArrays(String json, String key) {
        java.util.List<int[]> result = new java.util.ArrayList<>();
        String search = "\"" + key + "\"";
        int idx = json.indexOf(search);
        if (idx < 0) return result;
        idx = json.indexOf("[", idx);
        if (idx < 0) return result;
        idx++;

        while (idx < json.length()) {
            idx = json.indexOf("[", idx);
            if (idx < 0) break;
            int end = json.indexOf("]", idx);
            if (end < 0) break;
            String inner = json.substring(idx + 1, end).trim();
            String[] parts = inner.split(",");
            int[] arr = new int[parts.length];
            for (int i = 0; i < parts.length; i++) {
                arr[i] = Integer.parseInt(parts[i].trim());
            }
            result.add(arr);
            idx = end + 1;
            if (idx < json.length() && json.charAt(idx) == ']') break;
        }
        return result;
    }
}

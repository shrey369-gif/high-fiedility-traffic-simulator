package com.traffic;

import com.traffic.engine.SimulationEngine;
import com.traffic.server.HttpSimServer;

public class Main {
    public static void main(String[] args) {
        try {
            int port = 8080;
            if (args.length > 0) {
                try { port = Integer.parseInt(args[0]); } catch (NumberFormatException e) { }
            }

            System.out.println("╔══════════════════════════════════════════════════╗");
            System.out.println("║  Indian Traffic Simulation Engine v1.0           ║");
            System.out.println("║  Accelerating High-Fidelity Road Network Modeling║");
            System.out.println("╚══════════════════════════════════════════════════╝");
            System.out.println();

            SimulationEngine engine = new SimulationEngine();
            System.out.println("Simulation engine initialized.");
            System.out.println("Road network: " + engine.getIntersections().size() + " intersections, " + engine.getRoads().size() + " roads");

            HttpSimServer server = new HttpSimServer(port, engine);
            server.start();

            System.out.println();
            System.out.println("API Endpoints:");
            System.out.println("  GET  /api/simulation/start   - Start simulation");
            System.out.println("  GET  /api/simulation/stop    - Stop simulation");
            System.out.println("  GET  /api/simulation/stream  - SSE event stream");
            System.out.println("  POST /api/simulation/config  - Update config");
            System.out.println("  GET  /api/simulation/state   - Current state");
            System.out.println("  POST /api/scenario/create    - Load scenario");
            System.out.println("  GET  /api/insights/stats     - Traffic insights");
            System.out.println("  GET  /api/health             - Health check");
            System.out.println();
            System.out.println("Server ready. Waiting for connections...");

        } catch (Exception e) {
            System.err.println("Failed to start server: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}

"use client";
import { useState, useEffect, useRef, useCallback } from "react";
import { motion } from "framer-motion";
import dynamic from "next/dynamic";
import { startSimulation, stopSimulation, updateConfig, createSSEConnection } from "@/lib/api";

const SimulationCanvas = dynamic(() => import("@/components/SimulationCanvas"), { ssr: false });

export default function DashboardPage() {
  const [simData, setSimData] = useState(null);
  const [isRunning, setIsRunning] = useState(false);
  const [density, setDensity] = useState(0.5);
  const [adaptiveSignals, setAdaptiveSignals] = useState(false);
  const [simSpeed, setSimSpeed] = useState(1.0);
  const [connected, setConnected] = useState(false);
  const [error, setError] = useState(null);
  const sseRef = useRef(null);
  const [canvasSize, setCanvasSize] = useState({ w: 800, h: 600 });

  useEffect(() => {
    const update = () => {
      const w = Math.min(window.innerWidth - 420, 900);
      const h = Math.min(window.innerHeight - 120, 600);
      setCanvasSize({ w: Math.max(400, w), h: Math.max(300, h) });
    };
    update();
    window.addEventListener("resize", update);
    return () => window.removeEventListener("resize", update);
  }, []);

  const connectSSE = useCallback(() => {
    if (sseRef.current) sseRef.current.close();
    sseRef.current = createSSEConnection(
      (data) => {
        setSimData(data);
        setConnected(true);
        setError(null);
      },
      () => {
        setConnected(false);
        setError("Connection lost. Is the backend running on port 8080?");
      }
    );
  }, []);

  useEffect(() => {
    return () => {
      if (sseRef.current) sseRef.current.close();
    };
  }, []);

  const handleStart = async () => {
    try {
      setError(null);
      await startSimulation();
      setIsRunning(true);
      connectSSE();
    } catch {
      setError("Failed to start. Make sure the Java backend is running on port 8080.");
    }
  };

  const handleStop = async () => {
    try {
      await stopSimulation();
      setIsRunning(false);
      if (sseRef.current) sseRef.current.close();
    } catch {
      setError("Failed to stop simulation.");
    }
  };

  const handleDensityChange = async (val) => {
    setDensity(val);
    try { await updateConfig({ density: val }); } catch {}
  };

  const handleSpeedChange = async (val) => {
    setSimSpeed(val);
    try { await updateConfig({ speed: val }); } catch {}
  };

  const handleAdaptiveToggle = async () => {
    const newVal = !adaptiveSignals;
    setAdaptiveSignals(newVal);
    try { await updateConfig({ adaptiveSignals: newVal }); } catch {}
  };

  const stats = simData?.stats || {};

  return (
    <div className="min-h-screen pt-20 px-6 pb-6 grid-bg">
      <div className="max-w-[1400px] mx-auto">
        {/* Header */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.6 }}
          className="mb-6"
        >
          <h1
            className="text-3xl md:text-4xl font-bold tracking-[-0.03em]"
            style={{ fontFamily: "var(--font-heading)" }}
          >
            Simulation{" "}
            <span style={{ color: "var(--accent)" }}>Dashboard</span>
          </h1>
          <p className="text-sm mt-1" style={{ color: "var(--muted)" }}>
            Real-time traffic simulation with live controls and analytics
          </p>
        </motion.div>

        <div className="flex flex-col lg:flex-row gap-6">
          {/* Canvas */}
          <motion.div
            initial={{ opacity: 0, scale: 0.98 }}
            animate={{ opacity: 1, scale: 1 }}
            transition={{ duration: 0.6, delay: 0.1 }}
            className="flex-1 canvas-container"
          >
            <SimulationCanvas data={simData} width={canvasSize.w} height={canvasSize.h} />
          </motion.div>

          {/* Controls Panel */}
          <motion.div
            initial={{ opacity: 0, x: 30 }}
            animate={{ opacity: 1, x: 0 }}
            transition={{ duration: 0.6, delay: 0.2 }}
            className="w-full lg:w-[340px] space-y-4"
          >
            {/* Connection Status */}
            <div
              className="gradient-border rounded-xl p-4 flex items-center gap-3"
            >
              <div
                className="w-2.5 h-2.5 rounded-full"
                style={{
                  background: connected ? "#44FF44" : error ? "#FF4444" : "#888",
                  boxShadow: connected ? "0 0 8px #44FF44" : "none",
                }}
              />
              <span className="text-xs" style={{ color: "var(--muted)", fontFamily: "var(--font-heading)" }}>
                {connected ? "Connected to backend" : error || "Not connected"}
              </span>
            </div>

            {/* Start/Stop */}
            <button
              onClick={isRunning ? handleStop : handleStart}
              className="w-full py-3.5 rounded-xl font-semibold text-sm transition-all hover:scale-[1.02] active:scale-[0.98]"
              style={{
                background: isRunning ? "var(--surface-2)" : "var(--accent)",
                color: isRunning ? "var(--muted)" : "#fff",
                border: isRunning ? "1px solid var(--border)" : "none",
                fontFamily: "var(--font-heading)",
                boxShadow: isRunning ? "none" : "0 0 20px var(--accent-glow)",
              }}
            >
              {isRunning ? "■  Stop Simulation" : "▶  Start Simulation"}
            </button>

            {/* Density Slider */}
            <div className="gradient-border rounded-xl p-5">
              <label className="flex justify-between items-center mb-3">
                <span className="text-sm font-medium" style={{ fontFamily: "var(--font-heading)" }}>
                  Traffic Density
                </span>
                <span className="text-xs px-2 py-0.5 rounded-md" style={{ background: "var(--surface-2)", color: "var(--accent)" }}>
                  {(density * 100).toFixed(0)}%
                </span>
              </label>
              <input
                type="range"
                min="0"
                max="1"
                step="0.05"
                value={density}
                onChange={(e) => handleDensityChange(parseFloat(e.target.value))}
                className="w-full h-1.5 rounded-full appearance-none cursor-pointer"
                style={{
                  background: `linear-gradient(to right, var(--accent) 0%, var(--accent) ${density * 100}%, var(--surface-3) ${density * 100}%, var(--surface-3) 100%)`,
                  accentColor: "var(--accent)",
                }}
              />
              <div className="flex justify-between text-xs mt-1" style={{ color: "var(--muted)" }}>
                <span>Low</span><span>High</span>
              </div>
            </div>

            {/* Simulation Speed */}
            <div className="gradient-border rounded-xl p-5">
              <label className="flex justify-between items-center mb-3">
                <span className="text-sm font-medium" style={{ fontFamily: "var(--font-heading)" }}>
                  Sim Speed
                </span>
                <span className="text-xs px-2 py-0.5 rounded-md" style={{ background: "var(--surface-2)", color: "var(--accent)" }}>
                  {simSpeed.toFixed(1)}x
                </span>
              </label>
              <input
                type="range"
                min="0.1"
                max="5"
                step="0.1"
                value={simSpeed}
                onChange={(e) => handleSpeedChange(parseFloat(e.target.value))}
                className="w-full h-1.5 rounded-full appearance-none cursor-pointer"
                style={{
                  background: `linear-gradient(to right, var(--accent) 0%, var(--accent) ${(simSpeed / 5) * 100}%, var(--surface-3) ${(simSpeed / 5) * 100}%, var(--surface-3) 100%)`,
                  accentColor: "var(--accent)",
                }}
              />
            </div>

            {/* Signal Mode */}
            <div className="gradient-border rounded-xl p-5 flex items-center justify-between">
              <span className="text-sm font-medium" style={{ fontFamily: "var(--font-heading)" }}>
                Adaptive Signals
              </span>
              <button
                onClick={handleAdaptiveToggle}
                className="relative w-12 h-6 rounded-full transition-colors"
                style={{
                  background: adaptiveSignals ? "var(--accent)" : "var(--surface-3)",
                }}
              >
                <motion.div
                  layout
                  className="absolute top-0.5 w-5 h-5 rounded-full bg-white"
                  style={{ left: adaptiveSignals ? "calc(100% - 22px)" : "2px" }}
                  transition={{ type: "spring", stiffness: 500, damping: 30 }}
                />
              </button>
            </div>

            {/* Stats */}
            <div className="grid grid-cols-2 gap-3">
              {[
                { label: "Vehicles", value: stats.vehicleCount ?? "—", icon: "🚗" },
                { label: "Avg Speed", value: stats.avgSpeed ? `${stats.avgSpeed.toFixed(1)}` : "—", unit: "km/h", icon: "⚡" },
                { label: "Congestion", value: stats.congestionIndex ? `${(stats.congestionIndex * 100).toFixed(0)}` : "—", unit: "%", icon: "🔥" },
                { label: "Violations", value: stats.signalViolations ?? "—", icon: "🚨" },
              ].map((s, i) => (
                <motion.div
                  key={s.label}
                  initial={{ opacity: 0, y: 10 }}
                  animate={{ opacity: 1, y: 0 }}
                  transition={{ delay: 0.4 + i * 0.05 }}
                  className="gradient-border rounded-xl p-4 pulse-glow"
                >
                  <div className="text-lg mb-1">{s.icon}</div>
                  <div className="text-xl font-bold" style={{ fontFamily: "var(--font-heading)", color: "var(--accent)" }}>
                    {s.value}
                    {s.unit && <span className="text-xs ml-0.5" style={{ color: "var(--muted)" }}>{s.unit}</span>}
                  </div>
                  <div className="text-xs mt-0.5" style={{ color: "var(--muted)" }}>{s.label}</div>
                </motion.div>
              ))}
            </div>

            {/* Legend */}
            <div className="gradient-border rounded-xl p-4">
              <span className="text-xs font-medium mb-2 block" style={{ fontFamily: "var(--font-heading)", color: "var(--muted)" }}>
                Vehicle Legend
              </span>
              <div className="grid grid-cols-2 gap-y-1.5 gap-x-4">
                {[
                  { type: "Car", color: "#ffffff" },
                  { type: "Bike", color: "#6C5CE7" },
                  { type: "Auto", color: "#FFD93D" },
                  { type: "Bus", color: "#FF6B6B" },
                  { type: "Truck", color: "#4ECDC4" },
                ].map((v) => (
                  <div key={v.type} className="flex items-center gap-2">
                    <div className="w-3 h-2 rounded-sm" style={{ background: v.color }} />
                    <span className="text-xs" style={{ color: "var(--muted)" }}>{v.type}</span>
                  </div>
                ))}
              </div>
            </div>
          </motion.div>
        </div>
      </div>
    </div>
  );
}

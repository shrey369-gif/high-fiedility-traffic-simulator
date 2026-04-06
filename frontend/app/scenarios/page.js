"use client";
import { useState, useRef, useEffect, useCallback } from "react";
import { motion } from "framer-motion";
import { createScenario } from "@/lib/api";

export default function ScenariosPage() {
  const canvasRef = useRef(null);
  const [intersections, setIntersections] = useState([]);
  const [roads, setRoads] = useState([]);
  const [mode, setMode] = useState("intersection"); // intersection | road
  const [selectedIntersection, setSelectedIntersection] = useState(null);
  const [roadWidth, setRoadWidth] = useState(30);
  const [message, setMessage] = useState(null);
  const [canvasSize, setCanvasSize] = useState({ w: 800, h: 600 });

  useEffect(() => {
    const update = () => {
      setCanvasSize({
        w: Math.min(window.innerWidth - 400, 900),
        h: Math.min(window.innerHeight - 140, 600),
      });
    };
    update();
    window.addEventListener("resize", update);
    return () => window.removeEventListener("resize", update);
  }, []);

  const draw = useCallback(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const ctx = canvas.getContext("2d");
    const dpr = window.devicePixelRatio || 1;
    canvas.width = canvasSize.w * dpr;
    canvas.height = canvasSize.h * dpr;
    ctx.scale(dpr, dpr);
    canvas.style.width = canvasSize.w + "px";
    canvas.style.height = canvasSize.h + "px";

    // Background
    ctx.fillStyle = "#0a0a0a";
    ctx.fillRect(0, 0, canvasSize.w, canvasSize.h);

    // Grid
    ctx.strokeStyle = "rgba(108, 92, 231, 0.04)";
    ctx.lineWidth = 1;
    for (let x = 0; x < canvasSize.w; x += 40) {
      ctx.beginPath();
      ctx.moveTo(x, 0);
      ctx.lineTo(x, canvasSize.h);
      ctx.stroke();
    }
    for (let y = 0; y < canvasSize.h; y += 40) {
      ctx.beginPath();
      ctx.moveTo(0, y);
      ctx.lineTo(canvasSize.w, y);
      ctx.stroke();
    }

    // Draw roads
    for (const road of roads) {
      const src = intersections[road.src];
      const dst = intersections[road.dst];
      if (!src || !dst) continue;

      // Road shadow
      ctx.strokeStyle = "rgba(108, 92, 231, 0.1)";
      ctx.lineWidth = road.width + 4;
      ctx.lineCap = "round";
      ctx.beginPath();
      ctx.moveTo(src.x, src.y);
      ctx.lineTo(dst.x, dst.y);
      ctx.stroke();

      // Road surface
      ctx.strokeStyle = "#1a1a1a";
      ctx.lineWidth = road.width;
      ctx.beginPath();
      ctx.moveTo(src.x, src.y);
      ctx.lineTo(dst.x, dst.y);
      ctx.stroke();

      // Center dashes
      ctx.strokeStyle = "rgba(255,255,255,0.08)";
      ctx.lineWidth = 1;
      ctx.setLineDash([6, 10]);
      ctx.beginPath();
      ctx.moveTo(src.x, src.y);
      ctx.lineTo(dst.x, dst.y);
      ctx.stroke();
      ctx.setLineDash([]);

      // Width label
      ctx.fillStyle = "var(--muted)";
      ctx.font = "10px Inter";
      const mx = (src.x + dst.x) / 2;
      const my = (src.y + dst.y) / 2;
      ctx.fillStyle = "#666";
      ctx.fillText(`${road.width}m`, mx + 5, my - 5);
    }

    // Draw intersections
    for (let i = 0; i < intersections.length; i++) {
      const inter = intersections[i];
      const isSelected = selectedIntersection === i;

      // Glow
      if (isSelected) {
        const gradient = ctx.createRadialGradient(inter.x, inter.y, 3, inter.x, inter.y, 20);
        gradient.addColorStop(0, "rgba(108, 92, 231, 0.3)");
        gradient.addColorStop(1, "transparent");
        ctx.fillStyle = gradient;
        ctx.fillRect(inter.x - 20, inter.y - 20, 40, 40);
      }

      // Outer ring
      ctx.beginPath();
      ctx.arc(inter.x, inter.y, isSelected ? 10 : 8, 0, Math.PI * 2);
      ctx.fillStyle = isSelected ? "var(--accent)" : "#333";
      ctx.fill();

      // Inner dot
      ctx.beginPath();
      ctx.arc(inter.x, inter.y, 4, 0, Math.PI * 2);
      ctx.fillStyle = isSelected ? "#fff" : "#666";
      ctx.fill();

      // Label
      ctx.fillStyle = "#888";
      ctx.font = "bold 11px 'Space Grotesk', sans-serif";
      ctx.fillText(`I${i + 1}`, inter.x + 14, inter.y + 4);
    }

    // Mode hint
    ctx.fillStyle = "#333";
    ctx.font = "12px Inter";
    if (mode === "intersection") {
      ctx.fillText("Click to place intersections", 12, canvasSize.h - 12);
    } else {
      ctx.fillText(
        selectedIntersection !== null
          ? "Click another intersection to connect"
          : "Click an intersection to start a road",
        12,
        canvasSize.h - 12
      );
    }
  }, [intersections, roads, selectedIntersection, mode, canvasSize]);

  useEffect(() => {
    draw();
  }, [draw]);

  const handleCanvasClick = (e) => {
    const rect = canvasRef.current.getBoundingClientRect();
    const x = e.clientX - rect.left;
    const y = e.clientY - rect.top;

    if (mode === "intersection") {
      setIntersections((prev) => [...prev, { x, y }]);
      setMessage(`Intersection I${intersections.length + 1} placed at (${Math.round(x)}, ${Math.round(y)})`);
    } else {
      // Find nearest intersection
      let nearest = -1;
      let minDist = 25;
      for (let i = 0; i < intersections.length; i++) {
        const dx = intersections[i].x - x;
        const dy = intersections[i].y - y;
        const dist = Math.sqrt(dx * dx + dy * dy);
        if (dist < minDist) {
          minDist = dist;
          nearest = i;
        }
      }

      if (nearest >= 0) {
        if (selectedIntersection === null) {
          setSelectedIntersection(nearest);
          setMessage(`Selected I${nearest + 1}. Click another intersection to connect.`);
        } else if (nearest !== selectedIntersection) {
          setRoads((prev) => [...prev, { src: selectedIntersection, dst: nearest, width: roadWidth }]);
          setMessage(`Road created: I${selectedIntersection + 1} → I${nearest + 1}`);
          setSelectedIntersection(null);
        }
      }
    }
  };

  const handleSave = async () => {
    if (intersections.length < 2 || roads.length < 1) {
      setMessage("Need at least 2 intersections and 1 road.");
      return;
    }
    try {
      const scenario = {
        intersections: intersections.map((i) => [i.x, i.y]),
        roads: roads.map((r) => [r.src, r.dst, r.width]),
      };
      await createScenario(scenario);
      setMessage("✓ Scenario loaded into simulation engine!");
    } catch {
      setMessage("Failed to save. Is the backend running?");
    }
  };

  const handleClear = () => {
    setIntersections([]);
    setRoads([]);
    setSelectedIntersection(null);
    setMessage("Canvas cleared.");
  };

  return (
    <div className="min-h-screen pt-20 px-6 pb-6 grid-bg">
      <div className="max-w-[1400px] mx-auto">
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
            Scenario{" "}
            <span style={{ color: "var(--accent)" }}>Builder</span>
          </h1>
          <p className="text-sm mt-1" style={{ color: "var(--muted)" }}>
            Design custom road networks by placing intersections and connecting them with roads
          </p>
        </motion.div>

        <div className="flex flex-col lg:flex-row gap-6">
          {/* Canvas */}
          <motion.div
            initial={{ opacity: 0, scale: 0.98 }}
            animate={{ opacity: 1, scale: 1 }}
            transition={{ duration: 0.6, delay: 0.1 }}
            className="flex-1 canvas-container cursor-crosshair"
          >
            <canvas
              ref={canvasRef}
              onClick={handleCanvasClick}
              style={{ width: canvasSize.w, height: canvasSize.h, borderRadius: 12, display: "block" }}
            />
          </motion.div>

          {/* Tools Panel */}
          <motion.div
            initial={{ opacity: 0, x: 30 }}
            animate={{ opacity: 1, x: 0 }}
            transition={{ duration: 0.6, delay: 0.2 }}
            className="w-full lg:w-[320px] space-y-4"
          >
            {/* Mode selector */}
            <div className="gradient-border rounded-xl p-4">
              <span
                className="text-xs font-medium block mb-3 uppercase tracking-widest"
                style={{ color: "var(--muted)", fontFamily: "var(--font-heading)" }}
              >
                Drawing Mode
              </span>
              <div className="flex gap-2">
                {["intersection", "road"].map((m) => (
                  <button
                    key={m}
                    onClick={() => { setMode(m); setSelectedIntersection(null); }}
                    className="flex-1 py-2.5 rounded-lg text-sm font-medium transition-all capitalize"
                    style={{
                      background: mode === m ? "var(--accent)" : "var(--surface-2)",
                      color: mode === m ? "#fff" : "var(--muted)",
                      fontFamily: "var(--font-heading)",
                      boxShadow: mode === m ? "0 0 15px var(--accent-glow)" : "none",
                    }}
                  >
                    {m === "intersection" ? "📍 Intersection" : "🛣️ Road"}
                  </button>
                ))}
              </div>
            </div>

            {/* Road Width */}
            <div className="gradient-border rounded-xl p-5">
              <label className="flex justify-between items-center mb-3">
                <span className="text-sm font-medium" style={{ fontFamily: "var(--font-heading)" }}>
                  Road Width
                </span>
                <span className="text-xs px-2 py-0.5 rounded-md" style={{ background: "var(--surface-2)", color: "var(--accent)" }}>
                  {roadWidth}m
                </span>
              </label>
              <input
                type="range"
                min="15"
                max="60"
                step="5"
                value={roadWidth}
                onChange={(e) => setRoadWidth(parseInt(e.target.value))}
                className="w-full h-1.5 rounded-full appearance-none cursor-pointer"
                style={{
                  background: `linear-gradient(to right, var(--accent) 0%, var(--accent) ${((roadWidth - 15) / 45) * 100}%, var(--surface-3) ${((roadWidth - 15) / 45) * 100}%, var(--surface-3) 100%)`,
                }}
              />
            </div>

            {/* Stats */}
            <div className="gradient-border rounded-xl p-5 space-y-3">
              <div className="flex justify-between">
                <span className="text-sm" style={{ color: "var(--muted)" }}>Intersections</span>
                <span className="text-sm font-bold" style={{ color: "var(--accent)", fontFamily: "var(--font-heading)" }}>
                  {intersections.length}
                </span>
              </div>
              <div className="flex justify-between">
                <span className="text-sm" style={{ color: "var(--muted)" }}>Roads</span>
                <span className="text-sm font-bold" style={{ color: "var(--accent)", fontFamily: "var(--font-heading)" }}>
                  {roads.length}
                </span>
              </div>
            </div>

            {/* Actions */}
            <button
              onClick={handleSave}
              className="w-full py-3.5 rounded-xl font-semibold text-sm transition-all hover:scale-[1.02] active:scale-[0.98]"
              style={{
                background: "var(--accent)",
                color: "#fff",
                fontFamily: "var(--font-heading)",
                boxShadow: "0 0 20px var(--accent-glow)",
              }}
            >
              💾  Load into Simulator
            </button>

            <button
              onClick={handleClear}
              className="w-full py-3 rounded-xl font-semibold text-sm transition-all hover:bg-white/5"
              style={{
                border: "1px solid var(--border)",
                color: "var(--muted)",
                fontFamily: "var(--font-heading)",
              }}
            >
              🗑️  Clear Canvas
            </button>

            {/* Instructions */}
            <div className="gradient-border rounded-xl p-5">
              <span
                className="text-xs font-medium block mb-3 uppercase tracking-widest"
                style={{ color: "var(--muted)", fontFamily: "var(--font-heading)" }}
              >
                How to use
              </span>
              <ol className="text-xs space-y-2" style={{ color: "var(--muted)" }}>
                <li>1. Select <strong style={{ color: "#fff" }}>Intersection</strong> mode and click to place nodes</li>
                <li>2. Switch to <strong style={{ color: "#fff" }}>Road</strong> mode</li>
                <li>3. Click two intersections to connect them</li>
                <li>4. Adjust road width as needed</li>
                <li>5. Click <strong style={{ color: "#fff" }}>Load into Simulator</strong> to use</li>
              </ol>
            </div>

            {/* Message */}
            {message && (
              <motion.div
                initial={{ opacity: 0, y: 10 }}
                animate={{ opacity: 1, y: 0 }}
                className="text-xs p-3 rounded-lg"
                style={{ background: "var(--surface-2)", color: "var(--accent)" }}
              >
                {message}
              </motion.div>
            )}
          </motion.div>
        </div>
      </div>
    </div>
  );
}

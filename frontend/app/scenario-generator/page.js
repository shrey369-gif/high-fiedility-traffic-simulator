"use client";
import { useState, useRef, useEffect, useCallback } from "react";
import { motion } from "framer-motion";
import { generateScenario, exportScenario, parseOSM, createScenario } from "@/lib/api";

const CITIES = ["Delhi", "Mumbai", "Bangalore"];

export default function ScenarioGeneratorPage() {
  const canvasRef = useRef(null);
  const [loading, setLoading] = useState(false);
  const [scenario, setScenario] = useState(null);
  const [osmData, setOsmData] = useState("");
  const [message, setMessage] = useState(null);
  
  const [config, setConfig] = useState({
    cityPreset: "delhi",
    trafficDensity: 0.5,
    roadConditionSeverity: 0.3,
    enableConstructionZones: true,
    enableLaneClosures: true,
    enablePotholes: true,
    enableBarricades: true,
    seed: -1
  });

  const [canvasSize, setCanvasSize] = useState({ w: 800, h: 600 });

  useEffect(() => {
    const update = () => {
      setCanvasSize({
        w: Math.min(window.innerWidth - 450, 900),
        h: Math.min(window.innerHeight - 150, 600),
      });
    };
    update();
    window.addEventListener("resize", update);
    return () => window.removeEventListener("resize", update);
  }, []);

  const drawScenario = useCallback(() => {
    const canvas = canvasRef.current;
    if (!canvas || !scenario) return;
    const ctx = canvas.getContext("2d");
    const dpr = window.devicePixelRatio || 1;
    canvas.width = canvasSize.w * dpr;
    canvas.height = canvasSize.h * dpr;
    ctx.scale(dpr, dpr);
    canvas.style.width = canvasSize.w + "px";
    canvas.style.height = canvasSize.h + "px";

    ctx.fillStyle = "#0a0a0a";
    ctx.fillRect(0, 0, canvasSize.w, canvasSize.h);

    // Draw grid
    ctx.strokeStyle = "rgba(108, 92, 231, 0.04)";
    ctx.lineWidth = 1;
    for (let x = 0; x < canvasSize.w; x += 40) {
      ctx.beginPath(); ctx.moveTo(x, 0); ctx.lineTo(x, canvasSize.h); ctx.stroke();
    }
    for (let y = 0; y < canvasSize.h; y += 40) {
      ctx.beginPath(); ctx.moveTo(0, y); ctx.lineTo(canvasSize.w, y); ctx.stroke();
    }

    if (!scenario.roads) return;

    // Draw roads
    for (const r of scenario.roads) {
      ctx.strokeStyle = "#1a1a1a";
      ctx.lineWidth = r.width;
      ctx.lineCap = "round";
      ctx.beginPath();
      ctx.moveTo(r.srcX, r.srcY);
      ctx.lineTo(r.dstX, r.dstY);
      ctx.stroke();

      ctx.strokeStyle = "rgba(255,255,255,0.08)";
      ctx.lineWidth = 1;
      ctx.setLineDash([6, 10]);
      ctx.beginPath();
      ctx.moveTo(r.srcX, r.srcY);
      ctx.lineTo(r.dstX, r.dstY);
      ctx.stroke();
      ctx.setLineDash([]);
    }

    // Draw intersections
    for (const i of scenario.intersections) {
      ctx.beginPath();
      ctx.arc(i.x, i.y, 6, 0, Math.PI * 2);
      ctx.fillStyle = "#333";
      ctx.fill();
    }

    // Draw conditions
    for (const c of (scenario.conditions || [])) {
      ctx.beginPath();
      ctx.arc(c.x, c.y, Math.max(3, c.radius), 0, Math.PI * 2);
      if (c.type === "POTHOLE") ctx.fillStyle = "rgba(120, 80, 50, 0.8)";
      else if (c.type === "SPEED_BREAKER") ctx.fillStyle = "rgba(200, 150, 50, 0.6)";
      else ctx.fillStyle = "rgba(100, 100, 100, 0.5)";
      ctx.fill();
    }

    // Draw obstacles
    for (const o of (scenario.obstacles || [])) {
      ctx.save();
      ctx.translate(o.x, o.y);
      if (o.type === "BARRICADE" || o.type === "CONSTRUCTION_ZONE") {
        ctx.fillStyle = "repeating-linear-gradient(45deg, #FFaa00, #FFaa00 5px, #000 5px, #000 10px)";
        ctx.fillRect(-o.width/2, -o.height/2, o.width, o.height);
        ctx.strokeStyle = "#FFaa00";
        ctx.strokeRect(-o.width/2, -o.height/2, o.width, o.height);
      } else if (o.type === "LANE_CLOSURE") {
        ctx.fillStyle = "rgba(255, 50, 50, 0.3)";
        ctx.fillRect(-o.width/2, -o.height/2, o.width, o.height);
      } else {
        ctx.fillStyle = "#555";
        ctx.fillRect(-o.width/2, -o.height/2, o.width, o.height);
      }
      ctx.restore();
    }

    // Draw vehicles
    for (const v of (scenario.vehicles || [])) {
      ctx.save();
      ctx.translate(v.x, v.y);
      ctx.rotate(v.heading * Math.PI / 180);
      
      let w = 2, h = 4.5, color = "#fff";
      if (v.type === "BIKE") { w = 1; h = 2; color = "#aaa"; }
      else if (v.type === "AUTO_RICKSHAW") { w = 1.5; h = 3; color = "#ddaa00"; }
      else if (v.type === "BUS") { w = 2.5; h = 12; color = "#4488ff"; }
      else if (v.type === "TRUCK") { w = 2.5; h = 10; color = "#888"; }
      
      ctx.fillStyle = color;
      ctx.fillRect(-w/2, -h/2, w, h);
      ctx.restore();
    }

  }, [scenario, canvasSize]);

  useEffect(() => { drawScenario(); }, [drawScenario]);

  const handleGenerate = async () => {
    setLoading(true);
    setMessage(null);
    try {
      const payload = { ...config, osmData };
      const res = await generateScenario(payload);
      if (res.error) throw new Error(res.error);
      setScenario(res);
      setMessage(`✓ Scenario generated (Seed: ${res.seed}) | ${res.vehicles?.length || 0} vehicles, ${res.conditions?.length || 0} conditions`);
    } catch (err) {
      setMessage(`❌ Error: ${err.message}`);
    } finally {
      setLoading(false);
    }
  };

  const handleExport = async (format) => {
    setMessage(null);
    try {
      const res = await exportScenario(format);
      if (res.error) throw new Error(res.error);
      
      // trigger download
      const blob = new Blob([res.content], { type: 'text/plain' });
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = res.filename;
      a.click();
      window.URL.revokeObjectURL(url);
      
      setMessage(`✓ Downloaded ${res.filename}`);
    } catch (err) {
      setMessage(`❌ Export Error: ${err.message}`);
    }
  };

  const handleLoadSim = async () => {
      setMessage(null);
      if (!scenario) return;
      try {
          // Format for existing ScenarioBuilder (just roads and intersections)
          let simpleFormat = {
              intersections: scenario.intersections.map(i => [i.x, i.y]),
              roads: scenario.roads.map(r => [r.srcId, r.dstId, r.width])
          };
          
          await createScenario(simpleFormat);
          setMessage("✓ Loaded into Real-Time Simulator!");
      } catch (err) {
          setMessage(`❌ Sim Load Error: ${err.message}`);
      }
  }

  return (
    <div className="min-h-screen pt-20 px-6 pb-6 grid-bg">
      <div className="max-w-[1500px] mx-auto">
        <motion.div initial={{ opacity: 0, y: 10 }} animate={{ opacity: 1, y: 0 }} className="mb-6 flex justify-between items-end">
          <div>
            <h1 className="text-3xl md:text-4xl font-bold tracking-[-0.03em]" style={{ fontFamily: "var(--font-heading)" }}>
              Scenario <span style={{ color: "var(--accent)" }}>Generator</span>
            </h1>
            <p className="text-sm mt-1" style={{ color: "var(--muted)" }}>
              Procedurally generate high-fidelity Indian road scenarios for MATLAB & Simulink
            </p>
          </div>
          <div className="flex gap-2">
            {CITIES.map(c => (
              <button key={c} 
                onClick={() => setConfig({...config, cityPreset: c.toLowerCase()})}
                className={`px-4 py-1.5 rounded-full text-xs font-semibold ${config.cityPreset === c.toLowerCase() ? 'bg-[var(--accent)] text-white' : 'bg-[#222] text-[#aaa]'}`}>
                {c}
              </button>
            ))}
          </div>
        </motion.div>

        <div className="flex flex-col xl:flex-row gap-6">
          <motion.div initial={{ opacity: 0, x: -20 }} animate={{ opacity: 1, x: 0 }} className="w-full xl:w-[400px] flex flex-col gap-4">
            
            <div className="gradient-border rounded-xl p-5">
               <span className="text-xs font-medium block mb-3 uppercase tracking-widest text-[#aaa]">Map Data</span>
               <textarea 
                 className="w-full bg-[#111] border border-[#333] rounded-lg p-3 text-xs text-[#ccc] font-mono h-[80px]"
                 placeholder="Paste OSM XML data here (or leave blank to use city preset)..."
                 value={osmData}
                 onChange={e => setOsmData(e.target.value)}
               />
            </div>

            <div className="gradient-border rounded-xl p-5">
              <span className="text-xs font-medium block mb-4 uppercase tracking-widest text-[#aaa]">Parameters</span>
              
              <div className="space-y-5">
                <div>
                  <div className="flex justify-between mb-2">
                    <label className="text-xs font-medium">Traffic Density</label>
                    <span className="text-xs text-[var(--accent)]">{Math.round(config.trafficDensity * 100)}%</span>
                  </div>
                  <input type="range" min="0" max="1" step="0.1" value={config.trafficDensity} onChange={e => setConfig({...config, trafficDensity: parseFloat(e.target.value)})} className="w-full h-1 bg-[#333] rounded-full appearance-none" />
                </div>

                <div>
                  <div className="flex justify-between mb-2">
                    <label className="text-xs font-medium">Condition Severity</label>
                    <span className="text-xs text-[var(--accent)]">{Math.round(config.roadConditionSeverity * 100)}%</span>
                  </div>
                  <input type="range" min="0" max="1" step="0.1" value={config.roadConditionSeverity} onChange={e => setConfig({...config, roadConditionSeverity: parseFloat(e.target.value)})} className="w-full h-1 bg-[#333] rounded-full appearance-none" />
                </div>
                
                <div className="pt-2 border-t border-[#222] grid grid-cols-2 gap-3">
                    <label className="flex items-center gap-2 text-xs text-[#aaa]">
                       <input type="checkbox" checked={config.enablePotholes} onChange={e => setConfig({...config, enablePotholes: e.target.checked})} /> Potholes
                    </label>
                    <label className="flex items-center gap-2 text-xs text-[#aaa]">
                       <input type="checkbox" checked={config.enableBarricades} onChange={e => setConfig({...config, enableBarricades: e.target.checked})} /> Barricades
                    </label>
                    <label className="flex items-center gap-2 text-xs text-[#aaa]">
                       <input type="checkbox" checked={config.enableLaneClosures} onChange={e => setConfig({...config, enableLaneClosures: e.target.checked})} /> Lane Closures
                    </label>
                    <label className="flex items-center gap-2 text-xs text-[#aaa]">
                       <input type="checkbox" checked={config.enableConstructionZones} onChange={e => setConfig({...config, enableConstructionZones: e.target.checked})} /> Work Zones
                    </label>
                </div>
              </div>
            </div>

            <button 
              onClick={handleGenerate} 
              disabled={loading}
              className="w-full py-4 rounded-xl font-bold text-sm bg-[var(--accent)] text-white shadow-[0_0_20px_rgba(108,92,231,0.3)] hover:scale-[1.02] transition-transform disabled:opacity-50">
              {loading ? "Generating..." : "⚡ Generate Scenario"}
            </button>

            {message && (
                <div className="text-xs p-3 rounded-lg bg-[var(--surface-2)] text-[var(--accent)] font-medium">
                    {message}
                </div>
            )}

          </motion.div>

          <motion.div initial={{ opacity: 0, scale: 0.98 }} animate={{ opacity: 1, scale: 1 }} className="flex-1 flex flex-col gap-4">
            <div className="canvas-container bg-[#080808] border border-[#222] rounded-xl overflow-hidden flex items-center justify-center">
               {!scenario && !loading && <span className="absolute text-[#444] text-sm">Click Generate to preview</span>}
               {loading && <span className="absolute text-[var(--accent)] text-sm animate-pulse">Processing Road Network...</span>}
               <canvas ref={canvasRef} style={{ display: "block" }} />
            </div>

            {scenario && (
               <div className="flex gap-3">
                  <button onClick={() => handleExport("matlab")} className="flex-1 py-3 bg-[#181818] border border-[#333] hover:border-[var(--accent)] rounded-lg text-sm text-[#eee] transition-colors">
                     Download MATLAB (.m)
                  </button>
                  <button onClick={() => handleExport("roadrunner")} className="flex-1 py-3 bg-[#181818] border border-[#333] hover:border-[var(--accent)] rounded-lg text-sm text-[#eee] transition-colors">
                     Download RoadRunner
                  </button>
                  <button onClick={() => handleExport("json")} className="flex-1 py-3 bg-[#181818] border border-[#333] hover:border-[var(--accent)] rounded-lg text-sm text-[#eee] transition-colors">
                     Download JSON
                  </button>
                  <button onClick={handleLoadSim} className="flex-1 py-3 bg-[#2d1b54] border border-[var(--accent)] rounded-lg text-sm text-white font-semibold transition-colors">
                     ▶ Play in Simulator
                  </button>
               </div>
            )}
          </motion.div>
        </div>
      </div>
    </div>
  );
}

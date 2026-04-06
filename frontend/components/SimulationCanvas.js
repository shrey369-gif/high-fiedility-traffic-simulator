"use client";
import { useRef, useEffect, useCallback } from "react";

const VEHICLE_COLORS = {
  CAR: "#ffffff",
  BIKE: "#6C5CE7",
  AUTO_RICKSHAW: "#FFD93D",
  BUS: "#FF6B6B",
  TRUCK: "#4ECDC4",
};

const SIGNAL_COLORS = {
  RED: "#FF4444",
  YELLOW: "#FFD93D",
  GREEN: "#44FF44",
};

export default function SimulationCanvas({ data, width = 800, height = 600, isHero = false }) {
  const canvasRef = useRef(null);
  const prevDataRef = useRef(null);
  const animFrameRef = useRef(null);

  const draw = useCallback((ctx, simData) => {
    const w = ctx.canvas.width;
    const h = ctx.canvas.height;
    const scaleX = w / 800;
    const scaleY = h / 600;

    // Clear with dark background
    ctx.fillStyle = "#0a0a0a";
    ctx.fillRect(0, 0, w, h);

    // Draw grid
    ctx.strokeStyle = "rgba(108, 92, 231, 0.04)";
    ctx.lineWidth = 1;
    for (let x = 0; x < w; x += 60 * scaleX) {
      ctx.beginPath();
      ctx.moveTo(x, 0);
      ctx.lineTo(x, h);
      ctx.stroke();
    }
    for (let y = 0; y < h; y += 60 * scaleY) {
      ctx.beginPath();
      ctx.moveTo(0, y);
      ctx.lineTo(w, y);
      ctx.stroke();
    }

    if (!simData) {
      drawPlaceholder(ctx, w, h);
      return;
    }

    // Draw roads
    if (simData.roads) {
      for (const road of simData.roads) {
        const sx = road.srcX * scaleX;
        const sy = road.srcY * scaleY;
        const dx = road.dstX * scaleX;
        const dy = road.dstY * scaleY;
        const rw = road.width * scaleX;

        // Road shadow
        ctx.strokeStyle = "rgba(108, 92, 231, 0.08)";
        ctx.lineWidth = rw + 4;
        ctx.lineCap = "round";
        ctx.beginPath();
        ctx.moveTo(sx, sy);
        ctx.lineTo(dx, dy);
        ctx.stroke();

        // Road surface
        ctx.strokeStyle = "#1a1a1a";
        ctx.lineWidth = rw;
        ctx.beginPath();
        ctx.moveTo(sx, sy);
        ctx.lineTo(dx, dy);
        ctx.stroke();

        // Center dashes
        ctx.strokeStyle = "rgba(255, 255, 255, 0.08)";
        ctx.lineWidth = 1;
        ctx.setLineDash([8 * scaleX, 12 * scaleX]);
        ctx.beginPath();
        ctx.moveTo(sx, sy);
        ctx.lineTo(dx, dy);
        ctx.stroke();
        ctx.setLineDash([]);
      }
    }

    // Draw signals
    if (simData.signals) {
      for (const signal of simData.signals) {
        const sx = signal.x * scaleX;
        const sy = signal.y * scaleY;
        const color = SIGNAL_COLORS[signal.state] || "#888";

        // Glow
        const gradient = ctx.createRadialGradient(sx, sy, 2, sx, sy, 25 * scaleX);
        gradient.addColorStop(0, color + "60");
        gradient.addColorStop(1, "transparent");
        ctx.fillStyle = gradient;
        ctx.fillRect(sx - 25 * scaleX, sy - 25 * scaleY, 50 * scaleX, 50 * scaleY);

        // Signal dot
        ctx.beginPath();
        ctx.arc(sx, sy, 5 * scaleX, 0, Math.PI * 2);
        ctx.fillStyle = color;
        ctx.fill();

        // Outer ring
        ctx.beginPath();
        ctx.arc(sx, sy, 8 * scaleX, 0, Math.PI * 2);
        ctx.strokeStyle = color + "60";
        ctx.lineWidth = 1.5;
        ctx.stroke();
      }
    }

    // Draw vehicles
    if (simData.vehicles) {
      for (const v of simData.vehicles) {
        const vx = v.x * scaleX;
        const vy = v.y * scaleY;
        const color = VEHICLE_COLORS[v.type] || "#fff";
        const angle = (v.heading * Math.PI) / 180;

        ctx.save();
        ctx.translate(vx, vy);
        ctx.rotate(angle);

        // Vehicle size based on type
        let vw, vh;
        switch (v.type) {
          case "BIKE":
            vw = 4 * scaleX; vh = 2 * scaleY;
            break;
          case "AUTO_RICKSHAW":
            vw = 6 * scaleX; vh = 4 * scaleY;
            break;
          case "BUS":
            vw = 16 * scaleX; vh = 5 * scaleY;
            break;
          case "TRUCK":
            vw = 14 * scaleX; vh = 5 * scaleY;
            break;
          default: // CAR
            vw = 8 * scaleX; vh = 4 * scaleY;
        }

        // Vehicle glow
        ctx.shadowColor = color;
        ctx.shadowBlur = 6;

        // Vehicle body
        ctx.fillStyle = color;
        ctx.beginPath();
        ctx.roundRect(-vw / 2, -vh / 2, vw, vh, 2);
        ctx.fill();

        // Headlight
        ctx.fillStyle = "rgba(255,255,255,0.7)";
        ctx.beginPath();
        ctx.arc(vw / 2 - 1, -vh / 4, 1.2, 0, Math.PI * 2);
        ctx.fill();
        ctx.beginPath();
        ctx.arc(vw / 2 - 1, vh / 4, 1.2, 0, Math.PI * 2);
        ctx.fill();

        ctx.shadowBlur = 0;

        // Honking indicator
        if (v.honking) {
          ctx.strokeStyle = "rgba(255, 255, 100, 0.5)";
          ctx.lineWidth = 1;
          for (let i = 1; i <= 3; i++) {
            ctx.beginPath();
            ctx.arc(0, 0, (4 + i * 4) * scaleX, -0.3, 0.3);
            ctx.stroke();
          }
        }

        ctx.restore();
      }
    }

    // Stats overlay (only for hero)
    if (isHero && simData.stats) {
      ctx.fillStyle = "rgba(0,0,0,0.6)";
      ctx.fillRect(w - 160, 10, 150, 70);
      ctx.strokeStyle = "rgba(108, 92, 231, 0.3)";
      ctx.strokeRect(w - 160, 10, 150, 70);
      ctx.fillStyle = "#888";
      ctx.font = `${10 * scaleX}px Inter, sans-serif`;
      ctx.fillText(`Vehicles: ${simData.stats.vehicleCount}`, w - 150, 30);
      ctx.fillText(`Avg Speed: ${simData.stats.avgSpeed?.toFixed(1)} km/h`, w - 150, 48);
      ctx.fillText(`Congestion: ${(simData.stats.congestionIndex * 100)?.toFixed(0)}%`, w - 150, 66);
    }
  }, [isHero]);

  const drawPlaceholder = (ctx, w, h) => {
    // Animated placeholder when no data
    const time = Date.now() / 1000;

    // Draw demo roads
    const demoRoads = [
      [100, 100, 700, 100], [100, 300, 700, 300], [100, 500, 700, 500],
      [100, 100, 100, 500], [400, 100, 400, 500], [700, 100, 700, 500],
    ];
    const scaleX = w / 800;
    const scaleY = h / 600;

    for (const [x1, y1, x2, y2] of demoRoads) {
      ctx.strokeStyle = "#1a1a1a";
      ctx.lineWidth = 20 * scaleX;
      ctx.lineCap = "round";
      ctx.beginPath();
      ctx.moveTo(x1 * scaleX, y1 * scaleY);
      ctx.lineTo(x2 * scaleX, y2 * scaleY);
      ctx.stroke();

      ctx.strokeStyle = "rgba(255,255,255,0.05)";
      ctx.lineWidth = 1;
      ctx.setLineDash([6, 10]);
      ctx.beginPath();
      ctx.moveTo(x1 * scaleX, y1 * scaleY);
      ctx.lineTo(x2 * scaleX, y2 * scaleY);
      ctx.stroke();
      ctx.setLineDash([]);
    }

    // Animated demo vehicles
    for (let i = 0; i < 12; i++) {
      const roadIdx = i % 6;
      const [x1, y1, x2, y2] = demoRoads[roadIdx];
      const progress = ((time * 0.15 + i * 0.15) % 1);
      const x = (x1 + (x2 - x1) * progress) * scaleX;
      const y = (y1 + (y2 - y1) * progress) * scaleY;
      const colors = ["#ffffff", "#6C5CE7", "#FFD93D", "#FF6B6B", "#4ECDC4"];

      ctx.fillStyle = colors[i % colors.length];
      ctx.shadowColor = colors[i % colors.length];
      ctx.shadowBlur = 8;
      ctx.beginPath();
      ctx.roundRect(x - 4, y - 2, 8, 4, 2);
      ctx.fill();
      ctx.shadowBlur = 0;
    }

    // Draw signal dots
    const signalPositions = [[100, 100], [400, 100], [700, 100], [100, 300], [400, 300], [700, 300], [100, 500], [400, 500], [700, 500]];
    for (let i = 0; i < signalPositions.length; i++) {
      const [px, py] = signalPositions[i];
      const phase = Math.floor(time + i) % 3;
      const color = phase === 0 ? "#44FF44" : phase === 1 ? "#FFD93D" : "#FF4444";
      ctx.beginPath();
      ctx.arc(px * scaleX, py * scaleY, 4 * scaleX, 0, Math.PI * 2);
      ctx.fillStyle = color;
      ctx.fill();
    }
  };

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;

    const ctx = canvas.getContext("2d");
    const dpr = window.devicePixelRatio || 1;
    canvas.width = width * dpr;
    canvas.height = height * dpr;
    ctx.scale(dpr, dpr);
    canvas.style.width = width + "px";
    canvas.style.height = height + "px";

    const animate = () => {
      draw(ctx, data);
      if (!data) {
        animFrameRef.current = requestAnimationFrame(animate);
      }
    };

    animate();

    return () => {
      if (animFrameRef.current) cancelAnimationFrame(animFrameRef.current);
    };
  }, [data, width, height, draw]);

  useEffect(() => {
    if (data) {
      const canvas = canvasRef.current;
      if (!canvas) return;
      const ctx = canvas.getContext("2d");
      draw(ctx, data);
    }
  }, [data, draw]);

  return (
    <canvas
      ref={canvasRef}
      className="block"
      style={{ width, height, borderRadius: 12 }}
    />
  );
}

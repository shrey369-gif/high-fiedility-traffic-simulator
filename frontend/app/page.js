"use client";
import { motion, useScroll, useTransform } from "framer-motion";
import { useRef, useState, useEffect } from "react";
import Link from "next/link";
import dynamic from "next/dynamic";

const SimulationCanvas = dynamic(() => import("@/components/SimulationCanvas"), { ssr: false });

const staggerContainer = {
  hidden: {},
  show: { transition: { staggerChildren: 0.12, delayChildren: 0.3 } },
};

const fadeUp = {
  hidden: { opacity: 0, y: 40 },
  show: { opacity: 1, y: 0, transition: { duration: 0.8, ease: [0.22, 1, 0.36, 1] } },
};

const features = [
  {
    icon: "🛣️",
    title: "Lane-Less Traffic",
    desc: "Realistic Indian driving behavior with lateral weaving, overtaking from any side, and dynamic gap acceptance.",
  },
  {
    icon: "🚦",
    title: "Adaptive Signals",
    desc: "Queue-proportional green time allocation that responds to real-time traffic density at intersections.",
  },
  {
    icon: "🗺️",
    title: "A* Pathfinding",
    desc: "Density-aware routing that finds optimal paths through the road network, avoiding congested segments.",
  },
  {
    icon: "📊",
    title: "Real-Time Analytics",
    desc: "Live metrics including congestion index, speed distributions, signal violations, and flow throughput.",
  },
];

const vehicleTypes = [
  { name: "Cars", pct: 25, color: "#ffffff" },
  { name: "Bikes", pct: 25, color: "#6C5CE7" },
  { name: "Auto-Rickshaws", pct: 20, color: "#FFD93D" },
  { name: "Buses", pct: 15, color: "#FF6B6B" },
  { name: "Trucks", pct: 15, color: "#4ECDC4" },
];

export default function LandingPage() {
  const heroRef = useRef(null);
  const { scrollYProgress } = useScroll({ target: heroRef, offset: ["start start", "end start"] });
  const heroY = useTransform(scrollYProgress, [0, 1], [0, 200]);
  const heroOpacity = useTransform(scrollYProgress, [0, 0.5], [1, 0]);
  const [canvasSize, setCanvasSize] = useState({ w: 800, h: 500 });

  useEffect(() => {
    const update = () => {
      setCanvasSize({
        w: Math.min(window.innerWidth - 48, 1000),
        h: Math.min(window.innerHeight * 0.5, 500),
      });
    };
    update();
    window.addEventListener("resize", update);
    return () => window.removeEventListener("resize", update);
  }, []);

  return (
    <div className="grid-bg">
      {/* Hero Section */}
      <section ref={heroRef} className="relative min-h-screen flex flex-col items-center justify-center px-6 pt-20 overflow-hidden">
        {/* Gradient orbs */}
        <div className="absolute top-1/4 left-1/4 w-96 h-96 bg-[var(--accent)] rounded-full opacity-5 blur-[120px] pointer-events-none" />
        <div className="absolute bottom-1/4 right-1/4 w-96 h-96 bg-purple-500 rounded-full opacity-5 blur-[120px] pointer-events-none" />

        <motion.div
          style={{ y: heroY, opacity: heroOpacity }}
          className="relative z-10 text-center max-w-5xl mx-auto"
        >
          <motion.div variants={staggerContainer} initial="hidden" animate="show">
            <motion.div variants={fadeUp} className="mb-6">
              <span
                className="inline-block px-4 py-1.5 text-xs tracking-widest uppercase rounded-full border"
                style={{
                  borderColor: "var(--accent)",
                  color: "var(--accent)",
                  background: "rgba(108, 92, 231, 0.08)",
                  fontFamily: "var(--font-heading)",
                }}
              >
                Real-Time Simulation Engine
              </span>
            </motion.div>

            <motion.h1
              variants={fadeUp}
              className="text-5xl md:text-7xl lg:text-8xl font-bold leading-[0.9] tracking-[-0.04em] mb-6"
              style={{ fontFamily: "var(--font-heading)" }}
            >
              Simulating the{" "}
              <span className="glow-text" style={{ color: "var(--accent)" }}>
                Chaos
              </span>
              <br />
              of Indian Roads
            </motion.h1>

            <motion.p
              variants={fadeUp}
              className="text-lg md:text-xl max-w-2xl mx-auto mb-10 leading-relaxed"
              style={{ color: "var(--muted)" }}
            >
              Accelerating high-fidelity road network modeling with realistic lane-less
              traffic, adaptive signals, and real-time analytics.
            </motion.p>

            <motion.div variants={fadeUp} className="flex gap-4 justify-center">
              <Link
                href="/dashboard"
                className="group relative px-8 py-3.5 font-semibold text-sm rounded-xl overflow-hidden transition-transform hover:scale-105"
                style={{ fontFamily: "var(--font-heading)" }}
              >
                <div
                  className="absolute inset-0 glow transition-opacity group-hover:opacity-100 opacity-80"
                  style={{ background: "var(--accent)" }}
                />
                <span className="relative z-10 text-white">Launch Simulator</span>
              </Link>
              <Link
                href="/insights"
                className="px-8 py-3.5 font-semibold text-sm rounded-xl border transition-all hover:bg-white/5"
                style={{
                  borderColor: "var(--border)",
                  color: "var(--muted)",
                  fontFamily: "var(--font-heading)",
                }}
              >
                View Insights →
              </Link>
            </motion.div>
          </motion.div>
        </motion.div>

        {/* Hero Canvas */}
        <motion.div
          initial={{ opacity: 0, y: 60 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 1.2, delay: 0.6, ease: [0.22, 1, 0.36, 1] }}
          className="relative z-10 mt-12 canvas-container"
          style={{ maxWidth: 1000 }}
        >
          <SimulationCanvas width={canvasSize.w} height={canvasSize.h} data={null} isHero={true} />
          <div className="absolute inset-0 bg-gradient-to-t from-black/60 via-transparent to-transparent pointer-events-none rounded-xl" />
        </motion.div>

        {/* Scroll indicator */}
        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 0.4 }}
          transition={{ delay: 2, duration: 1 }}
          className="absolute bottom-8 left-1/2 -translate-x-1/2"
        >
          <motion.div
            animate={{ y: [0, 8, 0] }}
            transition={{ repeat: Infinity, duration: 1.8, ease: "easeInOut" }}
          >
            <svg width="20" height="30" viewBox="0 0 20 30" fill="none" stroke="var(--muted)" strokeWidth="2">
              <rect x="1" y="1" width="18" height="28" rx="9" />
              <circle cx="10" cy="8" r="2" fill="var(--muted)" />
            </svg>
          </motion.div>
        </motion.div>
      </section>

      {/* Features Section */}
      <section className="py-32 px-6">
        <div className="max-w-6xl mx-auto">
          <motion.div
            initial={{ opacity: 0, y: 40 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true, margin: "-100px" }}
            transition={{ duration: 0.8 }}
            className="text-center mb-20"
          >
            <h2
              className="text-4xl md:text-5xl font-bold tracking-[-0.03em] mb-4"
              style={{ fontFamily: "var(--font-heading)" }}
            >
              Engineered for{" "}
              <span style={{ color: "var(--accent)" }}>Realism</span>
            </h2>
            <p style={{ color: "var(--muted)" }} className="text-lg max-w-xl mx-auto">
              Every detail models real Indian traffic patterns — from auto-rickshaws weaving between buses to probabilistic signal violations.
            </p>
          </motion.div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            {features.map((f, i) => (
              <motion.div
                key={f.title}
                initial={{ opacity: 0, y: 30 }}
                whileInView={{ opacity: 1, y: 0 }}
                viewport={{ once: true }}
                transition={{ duration: 0.6, delay: i * 0.1 }}
                className="gradient-border rounded-2xl p-8 group hover:bg-[var(--surface-2)] transition-colors cursor-default"
              >
                <div className="text-4xl mb-4">{f.icon}</div>
                <h3
                  className="text-xl font-bold mb-2 tracking-tight"
                  style={{ fontFamily: "var(--font-heading)" }}
                >
                  {f.title}
                </h3>
                <p style={{ color: "var(--muted)" }} className="text-sm leading-relaxed">
                  {f.desc}
                </p>
              </motion.div>
            ))}
          </div>
        </div>
      </section>

      {/* Vehicle Types Section */}
      <section className="py-24 px-6">
        <div className="max-w-4xl mx-auto">
          <motion.div
            initial={{ opacity: 0, y: 40 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true, margin: "-100px" }}
            transition={{ duration: 0.8 }}
            className="text-center mb-16"
          >
            <h2
              className="text-4xl md:text-5xl font-bold tracking-[-0.03em] mb-4"
              style={{ fontFamily: "var(--font-heading)" }}
            >
              Mixed Traffic
            </h2>
            <p style={{ color: "var(--muted)" }} className="text-lg">
              5 distinct vehicle types with unique physics and behavior profiles
            </p>
          </motion.div>

          <div className="space-y-4">
            {vehicleTypes.map((v, i) => (
              <motion.div
                key={v.name}
                initial={{ opacity: 0, x: -30 }}
                whileInView={{ opacity: 1, x: 0 }}
                viewport={{ once: true }}
                transition={{ duration: 0.5, delay: i * 0.08 }}
                className="flex items-center gap-4"
              >
                <span className="w-32 text-sm font-medium" style={{ fontFamily: "var(--font-heading)" }}>
                  {v.name}
                </span>
                <div className="flex-1 h-8 rounded-full overflow-hidden" style={{ background: "var(--surface-2)" }}>
                  <motion.div
                    initial={{ width: 0 }}
                    whileInView={{ width: `${v.pct}%` }}
                    viewport={{ once: true }}
                    transition={{ duration: 1.2, delay: i * 0.1, ease: [0.22, 1, 0.36, 1] }}
                    className="h-full rounded-full"
                    style={{ background: v.color, opacity: 0.8 }}
                  />
                </div>
                <span className="w-12 text-right text-sm" style={{ color: "var(--muted)" }}>
                  {v.pct}%
                </span>
              </motion.div>
            ))}
          </div>
        </div>
      </section>

      {/* CTA Section */}
      <section className="py-32 px-6">
        <motion.div
          initial={{ opacity: 0, scale: 0.95 }}
          whileInView={{ opacity: 1, scale: 1 }}
          viewport={{ once: true, margin: "-100px" }}
          transition={{ duration: 0.8 }}
          className="max-w-3xl mx-auto text-center"
        >
          <h2
            className="text-4xl md:text-6xl font-bold tracking-[-0.03em] mb-6"
            style={{ fontFamily: "var(--font-heading)" }}
          >
            Ready to simulate?
          </h2>
          <p className="text-lg mb-10" style={{ color: "var(--muted)" }}>
            Launch the dashboard. Build custom scenarios. Analyze traffic flow. All in real-time.
          </p>
          <Link
            href="/dashboard"
            className="group relative inline-block px-10 py-4 font-semibold text-sm rounded-xl overflow-hidden transition-transform hover:scale-105"
            style={{ fontFamily: "var(--font-heading)" }}
          >
            <div
              className="absolute inset-0 glow transition-opacity group-hover:opacity-100 opacity-80"
              style={{ background: "var(--accent)" }}
            />
            <span className="relative z-10 text-white text-base">Open Dashboard →</span>
          </Link>
        </motion.div>
      </section>

      {/* Footer */}
      <footer className="border-t py-8 px-6" style={{ borderColor: "var(--border)" }}>
        <div className="max-w-6xl mx-auto flex items-center justify-between">
          <span className="text-xs" style={{ color: "var(--muted)" }}>
            Indian Traffic Simulation Engine — Accelerating High-Fidelity Road Network Modeling
          </span>
          <span className="text-xs" style={{ color: "var(--muted)" }}>
            Built with Java + Next.js
          </span>
        </div>
      </footer>
    </div>
  );
}

"use client";
import { useState, useEffect, useCallback } from "react";
import { motion } from "framer-motion";
import { getInsights } from "@/lib/api";
import {
  LineChart, Line, BarChart, Bar, AreaChart, Area,
  XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, Cell
} from "recharts";

const ACCENT = "#6C5CE7";
const COLORS = ["#ffffff", "#6C5CE7", "#FFD93D", "#FF6B6B", "#4ECDC4"];

const CustomTooltip = ({ active, payload, label }) => {
  if (!active || !payload?.length) return null;
  return (
    <div className="rounded-lg px-3 py-2 text-xs" style={{ background: "#111", border: "1px solid #222" }}>
      <p className="font-medium mb-1" style={{ fontFamily: "var(--font-heading)" }}>{label}</p>
      {payload.map((p, i) => (
        <p key={i} style={{ color: p.color }}>
          {p.name}: {typeof p.value === "number" ? p.value.toFixed(1) : p.value}
        </p>
      ))}
    </div>
  );
};

export default function InsightsPage() {
  const [insights, setInsights] = useState(null);
  const [error, setError] = useState(null);
  const [autoRefresh, setAutoRefresh] = useState(true);

  const fetchData = useCallback(async () => {
    try {
      const data = await getInsights();
      setInsights(data);
      setError(null);
    } catch {
      setError("Cannot connect to backend. Make sure the Java server is running on port 8080.");
    }
  }, []);

  useEffect(() => {
    fetchData();
    if (autoRefresh) {
      const interval = setInterval(fetchData, 2000);
      return () => clearInterval(interval);
    }
  }, [fetchData, autoRefresh]);

  const history = insights?.history || [];
  const distribution = insights?.distribution
    ? [
        { name: "Cars", value: insights.distribution.cars, color: COLORS[0] },
        { name: "Bikes", value: insights.distribution.bikes, color: COLORS[1] },
        { name: "Autos", value: insights.distribution.autos, color: COLORS[2] },
        { name: "Buses", value: insights.distribution.buses, color: COLORS[3] },
        { name: "Trucks", value: insights.distribution.trucks, color: COLORS[4] },
      ]
    : [];

  return (
    <div className="min-h-screen pt-20 px-6 pb-6 grid-bg">
      <div className="max-w-[1200px] mx-auto">
        {/* Header */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.6 }}
          className="mb-8 flex items-end justify-between"
        >
          <div>
            <h1
              className="text-3xl md:text-4xl font-bold tracking-[-0.03em]"
              style={{ fontFamily: "var(--font-heading)" }}
            >
              Traffic{" "}
              <span style={{ color: "var(--accent)" }}>Insights</span>
            </h1>
            <p className="text-sm mt-1" style={{ color: "var(--muted)" }}>
              Real-time traffic analytics and efficiency metrics
            </p>
          </div>
          <button
            onClick={() => setAutoRefresh(!autoRefresh)}
            className="px-4 py-2 rounded-lg text-xs font-medium transition-all"
            style={{
              background: autoRefresh ? "rgba(108, 92, 231, 0.1)" : "var(--surface-2)",
              color: autoRefresh ? "var(--accent)" : "var(--muted)",
              border: `1px solid ${autoRefresh ? "var(--accent)" : "var(--border)"}`,
              fontFamily: "var(--font-heading)",
            }}
          >
            {autoRefresh ? "● Live" : "○ Paused"}
          </button>
        </motion.div>

        {error && (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            className="mb-6 p-4 rounded-xl text-sm"
            style={{ background: "rgba(255, 68, 68, 0.1)", border: "1px solid rgba(255, 68, 68, 0.2)", color: "#FF6B6B" }}
          >
            {error}
          </motion.div>
        )}

        {/* Summary Cards */}
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-8">
          {[
            { label: "Total Vehicles", value: insights?.vehicleCount ?? "—", icon: "🚗" },
            { label: "Signal Violations", value: insights?.totalViolations ?? "—", icon: "🚨" },
            { label: "Total Honks", value: insights?.totalHonks ?? "—", icon: "📢" },
            {
              label: "Avg Speed",
              value: history.length > 0 ? `${history[history.length - 1].avgSpeed.toFixed(1)}` : "—",
              unit: "km/h",
              icon: "⚡",
            },
          ].map((s, i) => (
            <motion.div
              key={s.label}
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: i * 0.08 }}
              className="gradient-border rounded-xl p-5"
            >
              <div className="text-2xl mb-2">{s.icon}</div>
              <div
                className="text-2xl font-bold"
                style={{ fontFamily: "var(--font-heading)", color: ACCENT }}
              >
                {s.value}
                {s.unit && <span className="text-xs ml-1" style={{ color: "var(--muted)" }}>{s.unit}</span>}
              </div>
              <div className="text-xs mt-1" style={{ color: "var(--muted)" }}>
                {s.label}
              </div>
            </motion.div>
          ))}
        </div>

        {/* Charts Grid */}
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-8">
          {/* Speed Over Time */}
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.3 }}
            className="gradient-border rounded-xl p-6"
          >
            <h3
              className="text-sm font-bold mb-4 tracking-tight"
              style={{ fontFamily: "var(--font-heading)" }}
            >
              Average Speed Over Time
            </h3>
            <div style={{ height: 250 }}>
              {history.length > 1 ? (
                <ResponsiveContainer width="100%" height="100%">
                  <AreaChart data={history}>
                    <defs>
                      <linearGradient id="speedGradient" x1="0" y1="0" x2="0" y2="1">
                        <stop offset="5%" stopColor={ACCENT} stopOpacity={0.3} />
                        <stop offset="95%" stopColor={ACCENT} stopOpacity={0} />
                      </linearGradient>
                    </defs>
                    <CartesianGrid strokeDasharray="3 3" stroke="#1a1a1a" />
                    <XAxis dataKey="tick" stroke="#444" tick={{ fontSize: 10 }} />
                    <YAxis stroke="#444" tick={{ fontSize: 10 }} />
                    <Tooltip content={<CustomTooltip />} />
                    <Area type="monotone" dataKey="avgSpeed" stroke={ACCENT} fill="url(#speedGradient)" name="Speed (km/h)" strokeWidth={2} />
                  </AreaChart>
                </ResponsiveContainer>
              ) : (
                <div className="h-full flex items-center justify-center text-sm" style={{ color: "var(--muted)" }}>
                  Start the simulation to see speed data
                </div>
              )}
            </div>
          </motion.div>

          {/* Congestion Over Time */}
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.4 }}
            className="gradient-border rounded-xl p-6"
          >
            <h3
              className="text-sm font-bold mb-4 tracking-tight"
              style={{ fontFamily: "var(--font-heading)" }}
            >
              Congestion Index Over Time
            </h3>
            <div style={{ height: 250 }}>
              {history.length > 1 ? (
                <ResponsiveContainer width="100%" height="100%">
                  <LineChart data={history}>
                    <CartesianGrid strokeDasharray="3 3" stroke="#1a1a1a" />
                    <XAxis dataKey="tick" stroke="#444" tick={{ fontSize: 10 }} />
                    <YAxis stroke="#444" tick={{ fontSize: 10 }} domain={[0, 1]} />
                    <Tooltip content={<CustomTooltip />} />
                    <Line type="monotone" dataKey="congestion" stroke="#FF6B6B" name="Congestion" strokeWidth={2} dot={false} />
                  </LineChart>
                </ResponsiveContainer>
              ) : (
                <div className="h-full flex items-center justify-center text-sm" style={{ color: "var(--muted)" }}>
                  Start the simulation to see congestion data
                </div>
              )}
            </div>
          </motion.div>

          {/* Vehicle Distribution */}
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.5 }}
            className="gradient-border rounded-xl p-6"
          >
            <h3
              className="text-sm font-bold mb-4 tracking-tight"
              style={{ fontFamily: "var(--font-heading)" }}
            >
              Vehicle Type Distribution
            </h3>
            <div style={{ height: 250 }}>
              {distribution.some((d) => d.value > 0) ? (
                <ResponsiveContainer width="100%" height="100%">
                  <BarChart data={distribution}>
                    <CartesianGrid strokeDasharray="3 3" stroke="#1a1a1a" />
                    <XAxis dataKey="name" stroke="#444" tick={{ fontSize: 10 }} />
                    <YAxis stroke="#444" tick={{ fontSize: 10 }} />
                    <Tooltip content={<CustomTooltip />} />
                    <Bar dataKey="value" name="Count" radius={[4, 4, 0, 0]}>
                      {distribution.map((entry, index) => (
                        <Cell key={index} fill={entry.color} fillOpacity={0.8} />
                      ))}
                    </Bar>
                  </BarChart>
                </ResponsiveContainer>
              ) : (
                <div className="h-full flex items-center justify-center text-sm" style={{ color: "var(--muted)" }}>
                  Start the simulation to see vehicle distribution
                </div>
              )}
            </div>
          </motion.div>

          {/* Vehicle Count Over Time */}
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.6 }}
            className="gradient-border rounded-xl p-6"
          >
            <h3
              className="text-sm font-bold mb-4 tracking-tight"
              style={{ fontFamily: "var(--font-heading)" }}
            >
              Active Vehicles Over Time
            </h3>
            <div style={{ height: 250 }}>
              {history.length > 1 ? (
                <ResponsiveContainer width="100%" height="100%">
                  <AreaChart data={history}>
                    <defs>
                      <linearGradient id="vehicleGradient" x1="0" y1="0" x2="0" y2="1">
                        <stop offset="5%" stopColor="#4ECDC4" stopOpacity={0.3} />
                        <stop offset="95%" stopColor="#4ECDC4" stopOpacity={0} />
                      </linearGradient>
                    </defs>
                    <CartesianGrid strokeDasharray="3 3" stroke="#1a1a1a" />
                    <XAxis dataKey="tick" stroke="#444" tick={{ fontSize: 10 }} />
                    <YAxis stroke="#444" tick={{ fontSize: 10 }} />
                    <Tooltip content={<CustomTooltip />} />
                    <Area type="monotone" dataKey="vehicles" stroke="#4ECDC4" fill="url(#vehicleGradient)" name="Vehicles" strokeWidth={2} />
                  </AreaChart>
                </ResponsiveContainer>
              ) : (
                <div className="h-full flex items-center justify-center text-sm" style={{ color: "var(--muted)" }}>
                  Start the simulation to see vehicle count data
                </div>
              )}
            </div>
          </motion.div>
        </div>
      </div>
    </div>
  );
}

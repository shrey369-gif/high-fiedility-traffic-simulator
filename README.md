# Indian Traffic Simulation — Project README

**Accelerating High-Fidelity Road Network Modeling for Indian Traffic Simulations**

A Java-based traffic simulation engine with a visually stunning Next.js web interface that demonstrates realistic Indian traffic behavior.

---

## 🏗️ Architecture

```
┌─────────────────────┐         SSE/REST          ┌─────────────────────────┐
│   Java Backend      │◄──────────────────────────►│   Next.js Frontend      │
│   (Port 8080)       │                            │   (Port 3000)           │
│                     │                            │                         │
│  SimulationEngine   │  GET /api/simulation/start │  Landing Page           │
│  A* Pathfinding     │  GET /api/simulation/stop  │  Simulation Dashboard   │
│  Adaptive Signals   │  GET /api/simulation/stream│  Scenario Builder       │
│  Traffic Flow Opt.  │  POST /api/simulation/config│ Insights Panel         │
│  Vehicle Physics    │  POST /api/scenario/create │  Canvas Renderer        │
│  Signal Control     │  GET /api/insights/stats   │  Recharts Analytics     │
└─────────────────────┘                            └─────────────────────────┘
```

## 🚀 Quick Start

### 1. Start the Java Backend
```bash
cd backend
compile.bat        # Compiles all Java files
run.bat            # Starts server on port 8080
```

### 2. Start the Next.js Frontend
```bash
cd frontend
npm install        # First time only
npm run dev        # Starts on port 3000
```

### 3. Open Browser
Navigate to `http://localhost:3000`

---

## ⚙️ Indian Traffic Behaviors Modeled

| Behavior | Implementation |
|----------|---------------|
| Lane-less driving | Vehicles position laterally within road width, not in discrete lanes |
| Overtaking | Aggressive drivers overtake from any side |
| Honking | Abstracted as a "pressure" metric influencing nearby vehicles |
| Signal violations | Probabilistic (aggressive drivers: ~20% violation rate) |
| Mixed vehicle sizes | Auto-rickshaws weave between buses/trucks |
| Density clustering | Vehicles bunch at intersections |

## 🚗 Vehicle Types

| Type | Size (L×W) | Speed Range |
|------|-----------|-------------|
| Car | 4.5×2.0m | 60-120 km/h |
| Bike | 2.0×0.8m | 40-80 km/h |
| Auto-Rickshaw | 3.0×1.5m | 40-60 km/h |
| Bus | 12.0×2.5m | 40-80 km/h |
| Truck | 10.0×2.5m | 35-70 km/h |

## 🧮 Algorithms

- **A\* Pathfinding** — Density-aware routing over intersection graph
- **Traffic Flow Optimization** — Heuristic speed control based on road congestion
- **Adaptive Signal Control** — Queue-proportional green time allocation

## 🎨 Design

- Monochromatic black/white/gray palette
- Electric purple accent (#6C5CE7)
- Space Grotesk + Inter typography
- Framer Motion animations
- Canvas 2D vehicle rendering

## 📊 Tech Stack

| Layer | Technology |
|-------|-----------|
| Backend | Java 25 (pure, no dependencies) |
| Server | com.sun.net.httpserver.HttpServer |
| Frontend | Next.js 16, React 19 |
| Styling | Tailwind CSS v4 |
| Animations | Framer Motion |
| Charts | Recharts |
| Rendering | HTML5 Canvas 2D |

const API_BASE = "http://localhost:8080";

export async function startSimulation() {
  const res = await fetch(`${API_BASE}/api/simulation/start`);
  return res.json();
}

export async function stopSimulation() {
  const res = await fetch(`${API_BASE}/api/simulation/stop`);
  return res.json();
}

export async function getSimulationState() {
  const res = await fetch(`${API_BASE}/api/simulation/state`);
  return res.json();
}

export async function updateConfig(config) {
  const res = await fetch(`${API_BASE}/api/simulation/config`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(config),
  });
  return res.json();
}

export async function createScenario(scenario) {
  const res = await fetch(`${API_BASE}/api/scenario/create`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(scenario),
  });
  return res.json();
}

export async function getInsights() {
  const res = await fetch(`${API_BASE}/api/insights/stats`);
  return res.json();
}

export async function getHealth() {
  const res = await fetch(`${API_BASE}/api/health`);
  return res.json();
}

export function createSSEConnection(onMessage, onError) {
  const eventSource = new EventSource(`${API_BASE}/api/simulation/stream`);
  
  eventSource.onmessage = (event) => {
    try {
      const data = JSON.parse(event.data);
      onMessage(data);
    } catch (e) {
      // skip non-JSON messages (keepalive)
    }
  };

  eventSource.onerror = (err) => {
    if (onError) onError(err);
  };

  return eventSource;
}

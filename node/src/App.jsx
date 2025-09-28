import { useEffect, useState } from "react";

export default function App() {
  const [status, setStatus] = useState("carregando...");

  useEffect(() => {
    fetch("http://localhost:3000/health")
      .then(r => r.json())
      .then(j => setStatus(JSON.stringify(j)))
      .catch(e => setStatus("erro: " + e.message));
  }, []);

  return (
    <div style={{ fontFamily: "sans-serif", padding: 24 }}>
      <h1>React + Vite ✅</h1>
      <p>API Node /health → {status}</p>
    </div>
  );
}

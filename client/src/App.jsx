import { useEffect, useRef, useState } from "react";

const OBSTACLES = [
  { x: 5, y: 2, raio: 1.2, descricao: "Zona industrial" },
  { x: 4, y: 5, raio: 1.0, descricao: "Aeroporto" }
];

export default function App() {
  const canvasRef = useRef(null);
  const [saida, setSaida] = useState(null);
  const [erro, setErro] = useState("");
  const [rotaAtual, setRotaAtual] = useState([]);
  const [tituloMapa, setTituloMapa] = useState("Mapa");

  const payload = {
    drones: [{ id: "D1", capacidadeKg: 5, autonomiaKm: 20 }],
    pedidos: [
      { x: 3, y: 4, pesoKg: 2, prioridade: "ALTA" },
      { x: 7, y: 1, pesoKg: 1, prioridade: "MEDIA" },
      { x: 6, y: 6, pesoKg: 2.5, prioridade: "ALTA" }
    ],
    obstaculos: OBSTACLES
  };

  const desenharRota = (rota, titulo = "Rota", obstaculos = []) => {
    const cvs = canvasRef.current;
    if (!cvs) return;
    const ctx = cvs.getContext("2d");
    const w = (cvs.width = 600);
    const h = (cvs.height = 500);

    ctx.fillStyle = "#fafafa";
    ctx.fillRect(0, 0, w, h);

    ctx.strokeStyle = "#eee";
    for (let x = 40; x < w; x += 40) {
      ctx.beginPath();
      ctx.moveTo(x, 0);
      ctx.lineTo(x, h);
      ctx.stroke();
    }
    for (let y = 40; y < h; y += 40) {
      ctx.beginPath();
      ctx.moveTo(0, y);
      ctx.lineTo(w, y);
      ctx.stroke();
    }

    ctx.fillStyle = "#000";
    ctx.font = "16px system-ui";
    ctx.fillText(titulo, 10, 24);

    const obsList = Array.isArray(obstaculos) ? obstaculos : [];

    const xs = Array.isArray(rota) ? rota.map((p) => p[0]) : [];
    const ys = Array.isArray(rota) ? rota.map((p) => p[1]) : [];
    obsList.forEach((obs) => {
      const r = Number(obs.raio ?? 0);
      xs.push((obs.x ?? 0) + r, (obs.x ?? 0) - r);
      ys.push((obs.y ?? 0) + r, (obs.y ?? 0) - r);
    });

    const minX = xs.length ? Math.min(...xs, 0) : 0;
    const maxX = xs.length ? Math.max(...xs, 0) : 0;
    const minY = ys.length ? Math.min(...ys, 0) : 0;
    const maxY = ys.length ? Math.max(...ys, 0) : 0;

    const pad = 40;
    const escalaX = (w - 2 * pad) / Math.max(1, maxX - minX);
    const escalaY = (h - 2 * pad) / Math.max(1, maxY - minY);
    const toScreen = ([x, y]) => [
      pad + (x - minX) * escalaX,
      h - (pad + (y - minY) * escalaY)
    ];

    if (!Array.isArray(rota) || rota.length < 2) {
      ctx.fillStyle = "#d22";
      ctx.beginPath();
      ctx.arc(40, h - 40, 6, 0, Math.PI * 2);
      ctx.fill();
      ctx.fillStyle = "#000";
      ctx.fillText("Sem rota (DEPOSITO)", 60, h - 36);
    } else {
      ctx.beginPath();
      rota.forEach((p, i) => {
        const [sx, sy] = toScreen(p);
        if (i === 0) ctx.moveTo(sx, sy);
        else ctx.lineTo(sx, sy);
      });
      ctx.strokeStyle = "#333";
      ctx.lineWidth = 2;
      ctx.stroke();

      rota.forEach((p, i) => {
        const [sx, sy] = toScreen(p);
        ctx.beginPath();
        ctx.arc(sx, sy, 5, 0, Math.PI * 2);
        ctx.fillStyle = i === 0 || i === rota.length - 1 ? "#d22" : "#06c";
        ctx.fill();
        ctx.fillStyle = "#000";
        ctx.font = "12px system-ui";
        ctx.fillText(`${p[0]},${p[1]}`, sx + 8, sy - 8);
      });
    }

    obsList.forEach((obs) => {
      const raio = Number(obs.raio ?? 0);
      if (raio <= 0) return;
      const [cx, cy] = toScreen([obs.x ?? 0, obs.y ?? 0]);
      const raioPx = raio * (escalaX + escalaY) * 0.5;
      ctx.beginPath();
      ctx.fillStyle = "rgba(200, 0, 0, 0.18)";
      ctx.strokeStyle = "#a00";
      ctx.lineWidth = 1.5;
      ctx.arc(cx, cy, raioPx, 0, Math.PI * 2);
      ctx.fill();
      ctx.stroke();
    });

    ctx.fillStyle = "#000";
    ctx.font = "13px system-ui";
    ctx.fillText("Vermelho = Deposito | Azul = Entregas | Circulos = Obstaculos", 10, h - 16);
  };

  useEffect(() => {
    desenharRota(rotaAtual, tituloMapa, payload.obstaculos);
  }, [rotaAtual, tituloMapa, payload.obstaculos]);

  const formatNumber = (value, decimals = 2) => {
    const num = Number(value);
    return Number.isFinite(num) ? num.toFixed(decimals) : "-";
  };

  function desenharTeste() {
    const rotaFake = [[0, 0], [3, 4], [7, 1], [6, 6], [0, 0]];
    setErro("");
    setSaida(null);
    setTituloMapa("Rota (teste offline)");
    setRotaAtual(rotaFake);
  }

  async function planejarApi() {
    setErro("");
    setSaida(null);
    setTituloMapa("Carregando rota...");
    setRotaAtual([]);
    try {
      const resp = await fetch("/api/planejar", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(payload)
      });
      const json = await resp.json();

      if (!resp.ok) throw new Error(json?.erro || "Falha na API");

      setSaida(json);
      const viagensResp = Array.isArray(json?.viagens) ? json.viagens : [];
      const primeiraRota = Array.isArray(viagensResp[0]?.rota)
        ? viagensResp[0].rota
        : Array.isArray(json?.rota)
        ? json.rota
        : [];

      setTituloMapa(viagensResp.length > 0 ? "Rota (viagem 1)" : "Rota (API)");
      setRotaAtual(Array.isArray(primeiraRota) ? primeiraRota : []);
    } catch (e) {
      console.error(e);
      setSaida(null);
      setErro(e.message);
      setTituloMapa("Erro ao buscar rota");
      setRotaAtual([]);
    }
  }

  const viagens = Array.isArray(saida?.viagens) ? saida.viagens : [];
  const bateriaSolta = !viagens.length && saida?.bateria
    ? [{
        idDrone: saida?.idDrone ?? saida?.drone?.id,
        rota: Array.isArray(saida?.rota) ? saida.rota : [],
        distanciaKm: saida?.distanciaKm,
        pesoTotalKg: saida?.pesoTotalKg,
        tempoHoras: saida?.tempoHoras ?? saida?.bateria?.tempoTotalHoras,
        bateria: saida.bateria
      }]
    : [];
  const viagensParaExibir = viagens.length ? viagens : bateriaSolta;
  const tempoEntregaTotal = viagensParaExibir.reduce(
    (total, viagem) => total + (Number(viagem?.tempoHoras) || Number(viagem?.bateria?.tempoTotalHoras) || 0),
    0
  );

  return (
    <div style={{ fontFamily: "system-ui", padding: 20 }}>
      <h1>Planejamento de Drones</h1>
      <div style={{ display: "flex", gap: 8, marginBottom: 12 }}>
        <button type="button" onClick={desenharTeste}>Desenhar teste (sem API)</button>
        <button type="button" onClick={planejarApi}>Planejar (API)</button>
      </div>
      {erro && <p style={{ color: "red" }}>Erro: {erro}</p>}

      <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 16 }}>
        <pre style={{ background: "black", padding: 12, height: 280, overflow: "auto", color: "#0f0" }}>
          Entrada:
          {"\n"}
          {JSON.stringify(payload, null, 2)}
        </pre>
        <pre style={{ background: "black", padding: 12, height: 280, overflow: "auto", color: "#0f0" }}>
          Saida:
          {"\n"}
          {saida ? JSON.stringify(saida, null, 2) : "Sem resultado"}
        </pre>
      </div>

      <div style={{ marginTop: 12 }}>
        <strong>Obstaculos configurados:</strong>
        <ul>
          {payload.obstaculos.map((obs, index) => (
            <li key={`obs-${index}`}>
              {obs.descricao ?? `Obstaculo ${index + 1}`} - Centro ({obs.x}, {obs.y}) | Raio {obs.raio}
            </li>
          ))}
        </ul>
      </div>

      {viagensParaExibir.length > 0 && (
        <div style={{ marginTop: 16, display: "grid", gap: 12 }}>
          <h3>Viagens e simulacao de bateria</h3>
          <p style={{ margin: "0 0 8px" }}>
            Tempo total estimado de entrega: {formatNumber(tempoEntregaTotal)} h
          </p>
          {viagensParaExibir.map((viagem, index) => {
            const bateria = viagem?.bateria;
            const rota = Array.isArray(viagem?.rota) ? viagem.rota : [];
            const tempoHoras = Number(viagem?.tempoHoras) || Number(bateria?.tempoTotalHoras);
            return (
              <div
                key={`viagem-${index}`}
                style={{
                  border: "1px solid #ddd",
                  borderRadius: 6,
                  padding: 12,
                  background: "#fdfdfd"
                }}
              >
                <div
                  style={{
                    display: "flex",
                    justifyContent: "space-between",
                    alignItems: "center",
                    gap: 12,
                    flexWrap: "wrap"
                  }}
                >
                  <strong>
                    Viagem {index + 1} - Drone {viagem?.idDrone ?? viagem?.drone?.id ?? "N/D"}
                  </strong>
                  {rota.length > 0 && (
                    <button
                      type="button"
                      onClick={() => {
                        setTituloMapa(`Rota (viagem ${index + 1})`);
                        setRotaAtual(rota);
                      }}
                    >
                      Ver rota no mapa
                    </button>
                  )}
                </div>
                <p style={{ margin: "8px 0 4px" }}>
                  Peso total: {formatNumber(viagem?.pesoTotalKg)} kg | Distancia planejada: {formatNumber(viagem?.distanciaKm)} km | Tempo estimado: {formatNumber(tempoHoras)} h
                </p>
                {bateria ? (
                  <>
                    <p style={{ margin: "4px 0" }}>
                      Distancia simulada: {formatNumber(bateria.distanciaTotalKm)} km | Tempo simulado: {formatNumber(bateria.tempoTotalHoras)} h | {bateria.rotaCompleta ? "Bateria suficiente" : "Bateria insuficiente"}
                    </p>
                    <div style={{ overflowX: "auto" }}>
                      <table style={{ width: "100%", borderCollapse: "collapse" }}>
                        <thead>
                          <tr style={{ background: "#ececec" }}>
                            <th style={{ textAlign: "left", padding: "6px", borderBottom: "1px solid #ccc" }}>#</th>
                            <th style={{ textAlign: "left", padding: "6px", borderBottom: "1px solid #ccc" }}>Dist. segmento (km)</th>
                            <th style={{ textAlign: "left", padding: "6px", borderBottom: "1px solid #ccc" }}>Dist. acumulada (km)</th>
                            <th style={{ textAlign: "left", padding: "6px", borderBottom: "1px solid #ccc" }}>Tempo segmento (h)</th>
                            <th style={{ textAlign: "left", padding: "6px", borderBottom: "1px solid #ccc" }}>Tempo acumulado (h)</th>
                            <th style={{ textAlign: "left", padding: "6px", borderBottom: "1px solid #ccc" }}>Bateria restante (%)</th>
                          </tr>
                        </thead>
                        <tbody>
                          {Array.isArray(bateria.passos) && bateria.passos.length > 0 ? (
                            bateria.passos.map((passo, passoIndex) => (
                              <tr key={`passo-${passoIndex}`}>
                                <td style={{ padding: "6px", borderBottom: "1px solid #eee" }}>{passoIndex + 1}</td>
                                <td style={{ padding: "6px", borderBottom: "1px solid #eee" }}>{formatNumber(passo?.distanciaSegmentoKm)}</td>
                                <td style={{ padding: "6px", borderBottom: "1px solid #eee" }}>{formatNumber(passo?.distanciaAcumuladaKm)}</td>
                                <td style={{ padding: "6px", borderBottom: "1px solid #eee" }}>{formatNumber(passo?.tempoSegmentoHoras)}</td>
                                <td style={{ padding: "6px", borderBottom: "1px solid #eee" }}>{formatNumber(passo?.tempoAcumuladoHoras)}</td>
                                <td style={{ padding: "6px", borderBottom: "1px solid #eee" }}>{formatNumber(passo?.cargaRestantePercentual)}</td>
                              </tr>
                            ))
                          ) : (
                            <tr>
                              <td colSpan={6} style={{ padding: "6px", borderBottom: "1px solid #eee" }}>
                                Nenhum passo de bateria informado.
                              </td>
                            </tr>
                          )}
                        </tbody>
                      </table>
                    </div>
                  </>
                ) : (
                  <p style={{ marginTop: 8 }}>Simulacao de bateria nao disponivel para esta viagem.</p>
                )}
              </div>
            );
          })}
        </div>
      )}

      <h3 style={{ marginTop: 16 }}>{tituloMapa}</h3>
      <canvas
        ref={canvasRef}
        width={600}
        height={500}
        style={{ border: "1px solid #ddd", background: "white" }}
      />
    </div>
  );
}

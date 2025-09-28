function distance(a, b) {
  const dx = (a?.[0] ?? 0) - (b?.[0] ?? 0);
  const dy = (a?.[1] ?? 0) - (b?.[1] ?? 0);
  return Math.hypot(dx, dy);
}

function distancePointToSegment(a, b, p) {
  const ax = a?.[0] ?? 0;
  const ay = a?.[1] ?? 0;
  const bx = b?.[0] ?? 0;
  const by = b?.[1] ?? 0;
  const px = p?.x ?? 0;
  const py = p?.y ?? 0;

  const dx = bx - ax;
  const dy = by - ay;
  if (dx === 0 && dy === 0) {
    return Math.hypot(ax - px, ay - py);
  }

  let t = ((px - ax) * dx + (py - ay) * dy) / (dx * dx + dy * dy);
  t = Math.max(0, Math.min(1, t));
  const projX = ax + t * dx;
  const projY = ay + t * dy;
  return Math.hypot(projX - px, projY - py);
}

function intersects(obstacle, a, b) {
  if (!obstacle) return false;
  const raio = Number(obstacle.raio ?? obstacle.r) || 0;
  if (raio <= 0) return false;
  const dist = distancePointToSegment(a, b, obstacle);
  return dist < raio;
}

function detour(obstacle, origem, destino) {
  const ax = origem?.[0] ?? 0;
  const ay = origem?.[1] ?? 0;
  const bx = destino?.[0] ?? 0;
  const by = destino?.[1] ?? 0;

  const dx = bx - ax;
  const dy = by - ay;
  const comprimento = Math.hypot(dx, dy);
  if (comprimento === 0) {
    return [];
  }

  let px = -dy / comprimento;
  let py = dx / comprimento;
  const midX = (ax + bx) * 0.5;
  const midY = (ay + by) * 0.5;
  const dot = (midX - (obstacle.x ?? 0)) * px + (midY - (obstacle.y ?? 0)) * py;
  const sinal = dot >= 0 ? 1 : -1;
  px *= sinal;
  py *= sinal;

  const clear = (Number(obstacle.raio ?? obstacle.r) || 0) + 0.5;
  const ponto1 = [ax + px * clear, ay + py * clear];
  const ponto2 = [bx + px * clear, by + py * clear];
  return [ponto1, ponto2];
}

function aplicarObstaculos(rota = [], obstaculos = []) {
  if (!Array.isArray(rota) || rota.length < 2 || !Array.isArray(obstaculos) || !obstaculos.length) {
    return rota;
  }

  const ajustada = [];
  for (let i = 0; i < rota.length - 1; i += 1) {
    const origem = rota[i];
    const destino = rota[i + 1];
    ajustada.push(origem);

    for (const obstaculo of obstaculos) {
      if (intersects(obstaculo, origem, destino)) {
        const desvios = detour(obstaculo, origem, destino);
        for (const ponto of desvios) {
          const ultimo = ajustada[ajustada.length - 1];
          if (ponto && (ultimo?.[0] !== ponto[0] || ultimo?.[1] !== ponto[1])) {
            ajustada.push(ponto);
          }
        }
        break;
      }
    }
  }
  ajustada.push(rota[rota.length - 1]);
  return ajustada;
}

module.exports = {
  distance,
  aplicarObstaculos
};

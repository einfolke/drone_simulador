const DEFAULT_SPEED_KMH = 40;

function distance(a, b) {
  const dx = (a?.[0] ?? 0) - (b?.[0] ?? 0);
  const dy = (a?.[1] ?? 0) - (b?.[1] ?? 0);
  return Math.hypot(dx, dy);
}

function resolveDroneMetrics(drone = {}) {
  const autonomiaKm = Number(drone.autonomiaKm ?? drone.distanciaPorCarga);
  const capacidadeKg = Number(drone.capacidadeKg ?? drone.capacidadePorPeso);
  const tempoOperacao = Number(drone.tempoVooHoras ?? drone.tempoDeVooPorCarga);

  const distanciaMax = Number.isFinite(autonomiaKm) && autonomiaKm > 0
    ? autonomiaKm
    : Math.max(capacidadeKg || 0, 1);
  const tempoMaximo = Number.isFinite(tempoOperacao) && tempoOperacao > 0
    ? tempoOperacao
    : distanciaMax / DEFAULT_SPEED_KMH;
  const velocidade = tempoMaximo > 0 ? distanciaMax / tempoMaximo : DEFAULT_SPEED_KMH;

  return {
    distanciaMax,
    tempoMaximo,
    velocidade
  };
}

function simulateBattery(drone = {}, rota = []) {
  if (!Array.isArray(rota) || rota.length < 2) {
    return { passos: [], distanciaTotalKm: 0, tempoTotalHoras: 0, rotaCompleta: true };
  }

  const { distanciaMax, velocidade } = resolveDroneMetrics(drone);

  let distanciaAcumulada = 0;
  let tempoAcumulado = 0;
  let cargaRestante = 100;
  let completou = true;
  const passos = [];

  for (let i = 0; i < rota.length - 1; i += 1) {
    const origem = rota[i];
    const destino = rota[i + 1];
    const distanciaSegmento = distance(origem, destino);
    distanciaAcumulada += distanciaSegmento;

    const consumoPercentual = distanciaMax > 0 ? (distanciaSegmento / distanciaMax) * 100 : 0;
    const cargaAposSegmento = Math.max(0, cargaRestante - consumoPercentual);

    const tempoSegmentoHoras = velocidade > 0 ? distanciaSegmento / velocidade : 0;
    tempoAcumulado += tempoSegmentoHoras;

    passos.push({
      distanciaSegmentoKm: distanciaSegmento,
      distanciaAcumuladaKm: distanciaAcumulada,
      tempoSegmentoHoras,
      tempoAcumuladoHoras: tempoAcumulado,
      cargaRestantePercentual: cargaAposSegmento
    });

    cargaRestante = cargaAposSegmento;
    if (cargaRestante <= 0) {
      completou = i === rota.length - 2;
      if (!completou) {
        break;
      }
    }
  }

  return {
    passos,
    distanciaTotalKm: distanciaAcumulada,
    tempoTotalHoras: tempoAcumulado,
    rotaCompleta: completou
  };
}

module.exports = {
  simulateBattery
};


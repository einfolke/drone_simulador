const express = require('express');
const cors = require('cors');
const dronesDao = require('./dao/dronesDao');
const entregasDao = require('./dao/entregasDao');
const { simulateBattery } = require('./utils/batterySimulator');
const { aplicarObstaculos } = require('./utils/obstacles');

const JAVA_API_URL = (process.env.JAVA_API_URL || 'http://localhost:8080').replace(/\/$/, '');
const JAVA_API_ENABLED = String(process.env.JAVA_API_ENABLED || 'true').toLowerCase() === 'true';

function buildStubResponse(body = {}) {
  const { drones = [], pedidos = [], obstaculos = [] } = body;
  const baseRota = [[0, 0], ...pedidos.map((p) => [Number(p.x) || 0, Number(p.y) || 0]), [0, 0]];
  const rota = aplicarObstaculos(baseRota, obstaculos);
  const principalDrone = drones[0] || {};
  const simulacaoBateria = simulateBattery(principalDrone, rota);
  const pesoTotal = pedidos.reduce((soma, pedido) => soma + (Number(pedido.pesoKg) || 0), 0);

  return {
    origem: 'stub',
    recebido: { drones, pedidos, obstaculos },
    tempoTotalEntregaHoras: simulacaoBateria.tempoTotalHoras,
    viagens: [
      {
        idDrone: principalDrone.id ?? principalDrone.identificador ?? 'DX',
        pesoTotalKg: pesoTotal,
        distanciaKm: simulacaoBateria.distanciaTotalKm,
        tempoHoras: simulacaoBateria.tempoTotalHoras,
        idsPedidos: pedidos.map((_, index) => index + 1),
        rota,
        bateria: simulacaoBateria
      }
    ]
  };
}

const app = express();
app.use(cors());
app.use(express.json({ limit: '1mb' }));

app.get('/health', (_req, res) => res.json({ ok: true }));

app.get('/api/drones', async (_req, res) => {
  try {
    const drones = await dronesDao.listDrones();
    res.json(drones);
  } catch (error) {
    console.error('Erro ao listar drones:', error);
    res.status(500).json({ erro: 'Falha ao listar drones' });
  }
});

app.post('/api/drones', async (req, res) => {
  const identificador = typeof req.body?.identificador === 'string' ? req.body.identificador.trim() : '';
  const capacidadeKg = Number(req.body?.capacidadeKg);
  const autonomiaKm = Number(req.body?.autonomiaKm);
  const status = req.body?.status;

  if (!identificador || Number.isNaN(capacidadeKg) || Number.isNaN(autonomiaKm)) {
    return res.status(400).json({ erro: 'identificador, capacidadeKg e autonomiaKm sao obrigatorios' });
  }

  try {
    const drone = await dronesDao.createDrone({ identificador, capacidadeKg, autonomiaKm, status });
    res.status(201).json(drone);
  } catch (error) {
    console.error('Erro ao cadastrar drone:', error);
    if (error.code === '23505') {
      return res.status(409).json({ erro: 'identificador ja cadastrado' });
    }
    res.status(500).json({ erro: 'Falha ao cadastrar drone' });
  }
});

app.get('/api/entregas', async (_req, res) => {
  try {
    const entregas = await entregasDao.listEntregas();
    res.json(entregas);
  } catch (error) {
    console.error('Erro ao listar entregas:', error);
    res.status(500).json({ erro: 'Falha ao listar entregas' });
  }
});

app.post('/api/entregas', async (req, res) => {
  const pedidoId = typeof req.body?.pedidoId === 'string' && req.body.pedidoId.trim()
    ? req.body.pedidoId.trim()
    : (typeof req.body?.codigo === 'string' ? req.body.codigo.trim() : '');
  const pesoKg = Number(req.body?.pesoKg);
  const origemX = Number(req.body?.origemX);
  const origemY = Number(req.body?.origemY);
  const destinoX = Number(req.body?.destinoX);
  const destinoY = Number(req.body?.destinoY);
  const prioridade = Number(req.body?.prioridade);
  const droneId = req.body?.droneId != null ? Number(req.body.droneId) : null;
  const status = req.body?.status;

  if (!pedidoId || [pesoKg, origemX, origemY, destinoX, destinoY].some(Number.isNaN) || Number.isNaN(prioridade)) {
    return res.status(400).json({
      erro: 'pedidoId, pesoKg, origemX, origemY, destinoX, destinoY e prioridade sao obrigatorios'
    });
  }

  try {
    const entrega = await entregasDao.createEntrega({
      pedidoId,
      pesoKg,
      origemX,
      origemY,
      destinoX,
      destinoY,
      prioridade,
      droneId: Number.isNaN(droneId) ? null : droneId,
      status
    });
    res.status(201).json(entrega);
  } catch (error) {
    console.error('Erro ao cadastrar entrega:', error);
    if (error.code === '23505') {
      return res.status(409).json({ erro: 'pedidoId ja cadastrado' });
    }
    res.status(500).json({ erro: 'Falha ao cadastrar entrega' });
  }
});

app.post('/api/planejar', async (req, res) => {
  const payload = req.body ?? {};
  const useStubOnly = !JAVA_API_ENABLED;

  if (useStubOnly) {
    return res.json(buildStubResponse(payload));
  }

  try {
    const response = await fetch(`${JAVA_API_URL}/api/planejar`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    });

    const raw = await response.text();
    let parsed;
    try {
      parsed = JSON.parse(raw);
    } catch (parseError) {
      parsed = raw;
    }

    if (!response.ok) {
      console.warn(`API Java retornou status ${response.status}; usando stub.`);
      return res.json({ ...buildStubResponse(payload), origem: 'stub-fallback', erroJava: parsed });
    }

    return res.status(response.status).json(parsed);
  } catch (error) {
    console.error('Erro ao chamar API Java, usando stub:', error);
    return res.json({ ...buildStubResponse(payload), origem: 'stub-fallback', erroJava: error.message });
  }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => console.log(`API Node em http://localhost:${PORT}`));



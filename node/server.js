require('dotenv').config();

const express = require('express');
const cors = require('cors');
const { spawn } = require('child_process');
const path = require('path');
const fs = require('fs');
const dronesDao = require('./dao/dronesDao');
const entregasDao = require('./dao/entregasDao');
const { simulateBattery } = require('./utils/batterySimulator');
const { aplicarObstaculos } = require('./utils/obstacles');

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

const JAR_PATH = path.resolve(__dirname, '../java/target/SEU-ARTEFATO-jar-with-dependencies.jar');

app.post('/api/planejar', (req, res) => {
  console.log('POST /api/planejar recebido');

  if (!fs.existsSync(JAR_PATH)) {
    console.warn('JAR nao encontrado em:', JAR_PATH, ' - retornando STUB para teste.');
    const { drones = [], pedidos = [], obstaculos = [] } = req.body || {};
    const baseRota = [[0, 0], ...(pedidos.map(p => [Number(p.x) || 0, Number(p.y) || 0])), [0, 0]];
    const rota = aplicarObstaculos(baseRota, obstaculos);
    const principalDrone = drones[0] || {};
    const simulacaoBateria = simulateBattery(principalDrone, rota);
    const pesoTotal = pedidos.reduce((s, p) => s + (Number(p.pesoKg) || 0), 0);
    const viagemStub = {
      idDrone: principalDrone.id ?? 'DX',
      pesoTotalKg: pesoTotal,
      distanciaKm: simulacaoBateria.distanciaTotalKm,
      tempoHoras: simulacaoBateria.tempoTotalHoras,
      idsPedidos: pedidos.map((_, i) => i + 1),
      rota,
      bateria: simulacaoBateria
    };
    return res.json({
      origem: 'stub',
      recebido: { drones, pedidos, obstaculos },
      tempoTotalEntregaHoras: simulacaoBateria.tempoTotalHoras,
      viagens: [viagemStub]
    });
  }

  const child = spawn('java', ['-jar', JAR_PATH], { stdio: ['pipe', 'pipe', 'pipe'] });
  child.stdin.write(JSON.stringify(req.body));
  child.stdin.end();

  let out = '';
  let err = '';
  child.stdout.on('data', (chunk) => { out += chunk.toString(); });
  child.stderr.on('data', (chunk) => { err += chunk.toString(); });
  child.on('close', (code) => {
    if (code !== 0) {
      console.error('Java stderr:', err);
      return res.status(500).json({ erro: 'Falha ao executar Java', detalhes: err });
    }
    try {
      const json = JSON.parse(out);
      if (!Number.isFinite(Number(json.tempoTotalEntregaHoras))) {
        const viagensCalc = Array.isArray(json?.viagens) ? json.viagens : [];
        const totalTempo = viagensCalc.reduce((acc, viagem) => {
          const tempo = Number(viagem?.tempoHoras ?? viagem?.bateria?.tempoTotalHoras);
          return acc + (Number.isFinite(tempo) ? tempo : 0);
        }, 0);
        if (viagensCalc.length && Number.isFinite(totalTempo)) {
          json.tempoTotalEntregaHoras = totalTempo;
        }
      }
      res.json(json);
    } catch (e) {
      console.error('Saida invalida do Java:', out, e);
      res.status(500).json({ erro: 'Saida invalida do Java', raw: out });
    }
  });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => console.log(`API Node em http://localhost:${PORT}`));












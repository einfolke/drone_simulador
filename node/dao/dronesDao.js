const pool = require('../db/pool');

const mapDrone = (row) => ({
  id: row.id,
  identificador: row.identificador,
  capacidadeKg: Number(row.capacidade_kg),
  autonomiaKm: Number(row.autonomia_km),
  status: row.status
});

async function createDrone({ identificador, capacidadeKg, autonomiaKm, status = 'DISPONIVEL' }) {
  const result = await pool.query(
    `INSERT INTO drone (identificador, capacidade_kg, autonomia_km, status)
     VALUES ($1, $2, $3, $4)
     RETURNING id, identificador, capacidade_kg, autonomia_km, status`,
    [identificador, capacidadeKg, autonomiaKm, status]
  );
  return mapDrone(result.rows[0]);
}

async function listDrones() {
  const result = await pool.query(
    'SELECT id, identificador, capacidade_kg, autonomia_km, status FROM drone ORDER BY identificador'
  );
  return result.rows.map(mapDrone);
}

module.exports = {
  createDrone,
  listDrones
};

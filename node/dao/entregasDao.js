const pool = require('../db/pool');

const mapEntrega = (row) => ({
  id: row.id,
  pedidoId: row.pedido_id,
  pesoKg: Number(row.peso_kg),
  origemX: row.origem_x,
  origemY: row.origem_y,
  destinoX: row.destino_x,
  destinoY: row.destino_y,
  prioridade: row.prioridade,
  droneId: row.drone_id,
  status: row.status,
  criadoEm: row.criado_em
});

async function createEntrega({
  pedidoId,
  pesoKg,
  origemX,
  origemY,
  destinoX,
  destinoY,
  prioridade,
  droneId = null,
  status = 'PENDENTE'
}) {
  const result = await pool.query(
    `INSERT INTO entrega (
       pedido_id, peso_kg, origem_x, origem_y,
       destino_x, destino_y, prioridade, drone_id, status
     )
     VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9)
     RETURNING id, pedido_id, peso_kg, origem_x, origem_y, destino_x, destino_y,
               prioridade, drone_id, status, criado_em`,
    [pedidoId, pesoKg, origemX, origemY, destinoX, destinoY, prioridade, droneId, status]
  );
  return mapEntrega(result.rows[0]);
}

async function listEntregas() {
  const result = await pool.query(
    `SELECT id, pedido_id, peso_kg, origem_x, origem_y, destino_x, destino_y,
            prioridade, drone_id, status, criado_em
       FROM entrega
       ORDER BY criado_em DESC`
  );
  return result.rows.map(mapEntrega);
}

module.exports = {
  createEntrega,
  listEntregas
};

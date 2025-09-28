CREATE TABLE IF NOT EXISTS drone (
  id SERIAL PRIMARY KEY,
  identificador VARCHAR(50) UNIQUE NOT NULL,
  capacidade_kg NUMERIC(6,2) NOT NULL,
  autonomia_km NUMERIC(6,2) NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'DISPONIVEL'
);

CREATE TABLE IF NOT EXISTS entrega (
  id SERIAL PRIMARY KEY,
  pedido_id VARCHAR(50) NOT NULL,
  peso_kg NUMERIC(6,2) NOT NULL,
  origem_x INT NOT NULL,
  origem_y INT NOT NULL,
  destino_x INT NOT NULL,
  destino_y INT NOT NULL,
  prioridade INT NOT NULL DEFAULT 0,
  drone_id INT REFERENCES drone(id),
  criado_em TIMESTAMP NOT NULL DEFAULT now(),
  status VARCHAR(20) NOT NULL DEFAULT 'PENDENTE'
);

CREATE INDEX IF NOT EXISTS idx_entrega_prioridade
  ON entrega (prioridade DESC, criado_em ASC);

INSERT INTO drone (identificador, capacidade_kg, autonomia_km, status)
VALUES
    ('D1', 5.00, 20.00, 'DISPONIVEL'),
    ('D2', 7.50, 35.00, 'DISPONIVEL')
ON CONFLICT (identificador) DO NOTHING;

INSERT INTO entrega (
    pedido_id, peso_kg, origem_x, origem_y,
    destino_x, destino_y, prioridade, status
)
VALUES
    ('P1', 2.50, 0, 0, 3, 4, 2, 'PENDENTE'),
    ('P2', 1.25, 0, 0, 7, 1, 1, 'PENDENTE')
ON CONFLICT (pedido_id) DO NOTHING;

package com.drone.simulador.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import com.drone.simulador.dao.model.EntregaRecord;

public class EntregaDao {

    private static final String INSERT_SQL = """
        INSERT INTO entrega (
            pedido_id, peso_kg, origem_x, origem_y,
            destino_x, destino_y, prioridade, drone_id, status
        )
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, COALESCE(?, 'PENDENTE'))
        RETURNING id, pedido_id, peso_kg, origem_x, origem_y, destino_x, destino_y,
                  prioridade, drone_id, status, criado_em
        """;

    private static final String LIST_SQL = """
        SELECT id, pedido_id, peso_kg, origem_x, origem_y, destino_x, destino_y,
               prioridade, drone_id, status, criado_em
          FROM entrega
         ORDER BY criado_em DESC
        """;

    private final DataSource dataSource;

    public EntregaDao(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public EntregaRecord inserir(
        String pedidoId,
        double pesoKg,
        int origemX,
        int origemY,
        int destinoX,
        int destinoY,
        int prioridade,
        Long droneId,
        String status
    ) throws SQLException {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(INSERT_SQL)) {

            ps.setString(1, pedidoId);
            ps.setDouble(2, pesoKg);
            ps.setInt(3, origemX);
            ps.setInt(4, origemY);
            ps.setInt(5, destinoX);
            ps.setInt(6, destinoY);
            ps.setInt(7, prioridade);
            if (droneId == null) {
                ps.setNull(8, java.sql.Types.BIGINT);
            } else {
                ps.setLong(8, droneId);
            }
            ps.setString(9, status);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return map(rs);
                }
                throw new SQLException("Falha ao inserir entrega: retorno vazio");
            }
        }
    }

    public List<EntregaRecord> listar() throws SQLException {
        List<EntregaRecord> entregas = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(LIST_SQL);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                entregas.add(map(rs));
            }
        }
        return entregas;
    }

    private EntregaRecord map(ResultSet rs) throws SQLException {
        LocalDateTime criadoEm = rs.getObject("criado_em", LocalDateTime.class);
        return new EntregaRecord(
            rs.getLong("id"),
            rs.getString("pedido_id"),
            rs.getDouble("peso_kg"),
            rs.getInt("origem_x"),
            rs.getInt("origem_y"),
            rs.getInt("destino_x"),
            rs.getInt("destino_y"),
            rs.getInt("prioridade"),
            rs.getObject("drone_id", Long.class),
            rs.getString("status"),
            criadoEm
        );
    }
}



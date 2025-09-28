package com.drone.simulador.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import com.drone.simulador.dao.model.DroneRecord;

public class DroneDao {

    private static final String INSERT_SQL = """
        INSERT INTO drone (identificador, capacidade_kg, autonomia_km, status)
        VALUES (?, ?, ?, COALESCE(?, 'DISPONIVEL'))
        RETURNING id, identificador, capacidade_kg, autonomia_km, status
        """;

    private static final String LIST_SQL = """
        SELECT id, identificador, capacidade_kg, autonomia_km, status
          FROM drone
         ORDER BY identificador
        """;

    private final DataSource dataSource;

    public DroneDao(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public DroneRecord inserir(String identificador, double capacidadeKg, double autonomiaKm, String status) throws SQLException {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(INSERT_SQL)) {
            ps.setString(1, identificador);
            ps.setDouble(2, capacidadeKg);
            ps.setDouble(3, autonomiaKm);
            ps.setString(4, status);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return map(rs);
                }
                throw new SQLException("Falha ao inserir drone: retorno vazio");
            }
        }
    }

    public List<DroneRecord> listar() throws SQLException {
        List<DroneRecord> drones = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(LIST_SQL);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                drones.add(map(rs));
            }
        }
        return drones;
    }

    private DroneRecord map(ResultSet rs) throws SQLException {
        return new DroneRecord(
            rs.getLong("id"),
            rs.getString("identificador"),
            rs.getDouble("capacidade_kg"),
            rs.getDouble("autonomia_km"),
            rs.getString("status")
        );
    }
}

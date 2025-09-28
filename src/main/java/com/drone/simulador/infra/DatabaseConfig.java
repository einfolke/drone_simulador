package com.drone.simulador.infra;

import java.util.Objects;
import java.util.Properties;

import javax.sql.DataSource;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public final class DatabaseConfig {

    private static final HikariDataSource DATA_SOURCE = createDataSource();

    private DatabaseConfig() {
    }

    private static HikariDataSource createDataSource() {
        Properties props = new Properties();
        props.setProperty("jdbcUrl", getEnv("JDBC_URL", "jdbc:postgresql://localhost:5432/drones"));
        props.setProperty("username", getEnv("JDBC_USER", "app"));
        props.setProperty("password", getEnv("JDBC_PASSWORD", "secret"));
        props.setProperty("maximumPoolSize", "5");
        props.setProperty("minimumIdle", "1");
        props.setProperty("connectionTimeout", "10000");

        HikariConfig config = new HikariConfig(props);
        config.setPoolName("drone-simulador-pool");
        return new HikariDataSource(config);
    }

    private static String getEnv(String key, String defaultValue) {
        return Objects.requireNonNullElse(System.getenv(key), defaultValue);
    }

    public static DataSource getDataSource() {
        return DATA_SOURCE;
    }
}

package com.drone.simulador.dao.model;

public record DroneRecord(
    Long id,
    String identificador,
    double capacidadeKg,
    double autonomiaKm,
    String status
) {}

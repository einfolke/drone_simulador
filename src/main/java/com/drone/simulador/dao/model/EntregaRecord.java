package com.drone.simulador.dao.model;

import java.time.LocalDateTime;

public record EntregaRecord(
    Long id,
    String pedidoId,
    double pesoKg,
    int origemX,
    int origemY,
    int destinoX,
    int destinoY,
    int prioridade,
    Long droneId,
    String status,
    LocalDateTime criadoEm
) {}

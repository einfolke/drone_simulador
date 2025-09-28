package com.drone.simulador.dto;

import java.util.List;

public record ViagemDTO(
    String idDrone,
    double pesoTotalEmKg,
    double distanciaEmKm,
    List<Long> idsPedidos,
    List<double[]> rota
) {
}

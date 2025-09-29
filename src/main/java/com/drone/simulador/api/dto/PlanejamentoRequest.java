package com.drone.simulador.api.dto;

import java.util.List;

public record PlanejamentoRequest(
    List<DronePayload> drones,
    List<PedidoPayload> pedidos,
    List<ObstaculoPayload> obstaculos
) {
    public record DronePayload(
        String id,
        Double capacidadeKg,
        Double capacidadePorPeso,
        Double autonomiaKm,
        Double distanciaPorCarga,
        Double tempoVooHoras,
        Double tempoDeVooPorCarga
    ) {}

    public record PedidoPayload(
        Double x,
        Double y,
        Double pesoKg,
        String prioridade,
        String tempoChegada
    ) {}

    public record ObstaculoPayload(Double x, Double y, Double raio) {}
}

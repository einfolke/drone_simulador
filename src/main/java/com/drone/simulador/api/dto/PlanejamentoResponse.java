package com.drone.simulador.api.dto;

import java.util.List;

public record PlanejamentoResponse(
    String origem,
    double tempoTotalEntregaHoras,
    List<ViagemResponse> viagens
) {
    public record ViagemResponse(
        String idDrone,
        double pesoTotalKg,
        double distanciaKm,
        double tempoHoras,
        List<Long> idsPedidos,
        List<double[]> rota,
        BatteryResponse bateria
    ) {}

    public record BatteryResponse(
        double distanciaTotalKm,
        double tempoTotalHoras,
        boolean rotaCompleta,
        List<BatteryPassoResponse> passos
    ) {}

    public record BatteryPassoResponse(
        double distanciaSegmentoKm,
        double distanciaAcumuladaKm,
        double tempoSegmentoHoras,
        double tempoAcumuladoHoras,
        double cargaRestantePercentual
    ) {}
}

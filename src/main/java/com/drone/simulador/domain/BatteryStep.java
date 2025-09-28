package com.drone.simulador.domain;

public record BatteryStep(
    double distanciaSegmentoKm,
    double distanciaAcumuladaKm,
    double tempoSegmentoHoras,
    double tempoAcumuladoHoras,
    double cargaRestantePercentual
) {
}

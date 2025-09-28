package com.drone.simulador.domain;

import java.util.List;

public record DroneBatterySimulation(
    Drone drone,
    List<BatteryStep> passos,
    double distanciaTotalKm,
    double tempoTotalHoras,
    boolean rotaCompleta
) {
}

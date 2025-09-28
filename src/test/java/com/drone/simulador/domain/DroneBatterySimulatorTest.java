package com.drone.simulador.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DroneBatterySimulatorTest {

    private static final double EPS = 1e-6;

    @Test
    @DisplayName("Simulador reduz bateria proporcionalmente a distancia")
    void simularReduzBateriaPorDistancia() {
        Drone drone = new Drone("D1", 5.0, 2.0); // distancia maxima = 10 km
        List<Partida> rota = List.of(
            Partida.DEPOSITO,
            new Partida(3, 4), // 5 km
            Partida.DEPOSITO    // 5 km
        );

        DroneBatterySimulator simulator = new DroneBatterySimulator();
        DroneBatterySimulation simulacao = simulator.simular(drone, rota);

        assertTrue(simulacao.rotaCompleta());
        assertEquals(10.0, simulacao.distanciaTotalKm(), EPS);
        assertEquals(2.0, simulacao.passos().size());
        BatteryStep step1 = simulacao.passos().get(0);
        assertEquals(50.0, step1.cargaRestantePercentual(), EPS);
        BatteryStep step2 = simulacao.passos().get(1);
        assertEquals(0.0, step2.cargaRestantePercentual(), EPS);
    }

    @Test
    @DisplayName("Simulador interrompe rota quando bateria acaba")
    void simularInterrompeQuandoBateriaAcaba() {
        Drone drone = new Drone("D1", 5.0, 1.0); // distancia maxima = 5 km
        List<Partida> rota = List.of(
            Partida.DEPOSITO,
            new Partida(4, 0), // 4 km
            new Partida(8, 0), // +4 km (total 8 km)
            new Partida(10, 0)
        );

        DroneBatterySimulator simulator = new DroneBatterySimulator();
        DroneBatterySimulation simulacao = simulator.simular(drone, rota);

        assertFalse(simulacao.rotaCompleta());
        assertEquals(8.0, simulacao.distanciaTotalKm(), EPS);
        assertEquals(2, simulacao.passos().size());
        assertEquals(20.0, simulacao.passos().get(0).cargaRestantePercentual(), EPS);
        assertEquals(0.0, simulacao.passos().get(1).cargaRestantePercentual(), EPS);
    }
}

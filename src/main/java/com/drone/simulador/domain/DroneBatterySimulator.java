package com.drone.simulador.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class DroneBatterySimulator {

    public DroneBatterySimulation simular(Drone drone, List<Partida> rota) {
        Objects.requireNonNull(drone, "Drone nao pode ser nulo");
        Objects.requireNonNull(rota, "Rota nao pode ser nula");
        if (rota.size() < 2) {
            return new DroneBatterySimulation(drone, List.of(), 0.0, 0.0, true);
        }

        double capacidadeDistancia = Math.max(1e-6, drone.getDistanciaPorCarga());
        double tempoMaximo = drone.getTempoDeVooPorCarga();

        double distanciaAcumulada = 0.0;
        double tempoAcumulado = 0.0;
        double cargaRestante = 100.0;
        boolean completou = true;

        List<BatteryStep> passos = new ArrayList<>();

        for (int i = 0; i < rota.size() - 1; i++) {
            Partida origem = rota.get(i);
            Partida destino = rota.get(i + 1);
            double distanciaSegmento = origem.distanceTo(destino);
            distanciaAcumulada += distanciaSegmento;

            double consumoPercentual = (distanciaSegmento / capacidadeDistancia) * 100.0;
            double cargaAposSegmento = Math.max(0.0, cargaRestante - consumoPercentual);

            double tempoSegmentoHoras = tempoMaximo * (distanciaSegmento / capacidadeDistancia);
            tempoAcumulado += tempoSegmentoHoras;

            passos.add(new BatteryStep(
                distanciaSegmento,
                distanciaAcumulada,
                tempoSegmentoHoras,
                tempoAcumulado,
                cargaAposSegmento
            ));

            cargaRestante = cargaAposSegmento;
            if (cargaRestante <= 0.0) {
                completou = i == rota.size() - 2;
                if (!completou) {
                    break;
                }
            }
        }

        return new DroneBatterySimulation(
            drone,
            List.copyOf(passos),
            distanciaAcumulada,
            tempoAcumulado,
            completou
        );
    }

    public DroneBatterySimulation simular(Viagem viagem) {
        Objects.requireNonNull(viagem, "Viagem nao pode ser nula");
        return simular(viagem.getDrone(), viagem.getRota());
    }
}

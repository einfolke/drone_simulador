package com.drone.simulador;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.drone.simulador.domain.AgendarDrone;
import com.drone.simulador.domain.Drone;
import com.drone.simulador.domain.DroneBatterySimulator;
import com.drone.simulador.domain.Obstaculo;
import com.drone.simulador.domain.Partida;
import com.drone.simulador.domain.Pedido;
import com.drone.simulador.domain.Prioridade;
import com.drone.simulador.domain.Viagem;

public class Main {
    public static void main(String[] args) {
        com.drone.simulador.infra.MigrationService.migrate();

        List<Drone> drones = List.of(
            new Drone("DroneA", 5.0, 20.0),
            new Drone("DroneB", 5.0, 20.0)
        );

        LocalDateTime base = LocalDateTime.now().withHour(8).withMinute(0).withSecond(0).withNano(0);
        List<Pedido> pedidos = List.of(
            new Pedido(new Partida(3, 4), 2.0, Prioridade.ALTA, base.plusMinutes(5)),
            new Pedido(new Partida(7, 1), 1.0, Prioridade.MEDIA, base.plusMinutes(10)),
            new Pedido(new Partida(6, 6), 2.5, Prioridade.ALTA, base.plusMinutes(12)),
            new Pedido(new Partida(10, 0), 1.5, Prioridade.BAIXA, base.plusMinutes(20))
        );

        List<Obstaculo> obstaculos = List.of(
            new Obstaculo(5, 2, 1.2),
            new Obstaculo(4, 5, 1.0)
        );

        AgendarDrone agendador = new AgendarDrone();
        List<Viagem> viagens = agendador.planejar(drones, new ArrayList<>(pedidos), obstaculos);

        System.out.println("=== PLANO DE VIAGENS ===");
        int i = 1;
        for (Viagem viagem : viagens) {
            System.out.printf(
                "Viagem %d - Drone=%s | Peso=%.2fkg | Distancia=%.2fkm | Tempo=%.2fh%n",
                i++,
                viagem.getDrone().getId(),
                viagem.getPesoTotalKg(),
                viagem.getDistanciaKm(),
                viagem.getTempoHoras()
            );
            viagem.getPedidos().forEach(
                pedido -> System.out.println("  - Pedido " + pedido.getId() + " (chegada: " + pedido.getTempoChegada() + ")")
            );

            var simulador = new DroneBatterySimulator();
            var resultado = simulador.simular(viagem);
            resultado.passos().forEach(pass -> System.out.printf(
                "    Segmento: %.2f km | Distancia acumulada: %.2f km | Bateria: %.1f%%%n",
                pass.distanciaSegmentoKm(),
                pass.distanciaAcumuladaKm(),
                pass.cargaRestantePercentual()
            ));
            if (!resultado.rotaCompleta()) {
                System.out.println("    Atencao: bateria insuficiente para completar a rota.");
            }
        }
    }
}

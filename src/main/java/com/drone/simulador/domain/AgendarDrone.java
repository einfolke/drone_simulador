package com.drone.simulador.domain;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.Queue;

public class AgendarDrone {

    private static final Comparator<Pedido> ORDENACAO_POR_PRIORIDADE_E_CHEGADA =
        Comparator.comparingInt((Pedido p) -> p.getPrioridade().getPeso()).reversed()
            .thenComparing(Pedido::getTempoChegada);

    public List<Viagem> planejar(List<Drone> drones, List<Pedido> pedidos) {
        return planejar(drones, pedidos, List.of());
    }

    public List<Viagem> planejar(List<Drone> drones, List<Pedido> pedidos, List<Obstaculo> obstaculos) {
        Objects.requireNonNull(drones, "Lista de drones nao pode ser nula");
        Objects.requireNonNull(pedidos, "Lista de pedidos nao pode ser nula");
        PriorityQueue<Pedido> fila = new PriorityQueue<>(ORDENACAO_POR_PRIORIDADE_E_CHEGADA);
        fila.addAll(pedidos);

        List<Viagem> viagens = new ArrayList<>();
        int idxDrone = 0;

        while (!fila.isEmpty()) {
            Drone droneAtual = drones.get(idxDrone);
            idxDrone = (idxDrone + 1) % drones.size();

            Viagem viagem = new Viagem(droneAtual);

            while (!fila.isEmpty() && viagem.cabe(fila.peek())) {
                viagem.adicionarPedido(fila.poll());
            }

            if (viagem.getPedidos().isEmpty()) {
                Pedido pedido = fila.peek();
                if (pedido != null && pedido.getPesoEmKg() <= droneAtual.getCapacidadePorPeso()) {
                    viagem.adicionarPedido(fila.poll());
                } else {
                    fila.poll();
                }
            }

            List<Partida> rota = ajustarPorAutonomia(viagem, fila, obstaculos);
            double distancia = CalcularDistanciaPorTrajeto.gerar(rota);
            double tempoHoras = calcularTempoHoras(distancia, viagem.getDrone());
            viagem.definirRota(rota, distancia, tempoHoras);

            viagens.add(viagem);
        }

        return viagens;
    }

    private List<Partida> ajustarPorAutonomia(Viagem viagem, Queue<Pedido> restantes, List<Obstaculo> obstaculos) {
        while (true) {
            if (viagem.getPedidos().isEmpty()) {
                return List.of(Partida.DEPOSITO, Partida.DEPOSITO);
            }

            var rota = CalcularDistanciaPorTrajeto.buildRoute(viagem.getPedidos(), obstaculos);
            double distanciaTotal = CalcularDistanciaPorTrajeto.gerar(rota);

            if (distanciaTotal <= viagem.getDrone().getDistanciaPorCarga()) {
                return rota;
            }

            Pedido remover = selecionarMenosCritico(viagem.getPedidos());
            if (remover == null) {
                return rota;
            }

            viagem.removerPedido(remover);
            restantes.add(remover);
        }
    }

    private double calcularTempoHoras(double distanciaTotal, Drone drone) {
        double capacidadeDistancia = drone.getDistanciaPorCarga();
        double tempoDisponivel = drone.getTempoDeVooPorCarga();
        if (capacidadeDistancia > 0 && tempoDisponivel > 0) {
            return (distanciaTotal / capacidadeDistancia) * tempoDisponivel;
        }
        double velocidadePadrao = 40.0;
        return distanciaTotal / velocidadePadrao;
    }

    private Pedido selecionarMenosCritico(List<Pedido> pedidos) {
        return pedidos.stream()
            .sorted((a, b) -> {
                int prioridadeComparada = Integer.compare(a.getPrioridade().getPeso(), b.getPrioridade().getPeso());
                if (prioridadeComparada != 0) {
                    return prioridadeComparada;
                }
                double distanciaA = Partida.DEPOSITO.distanceTo(a.getLocalizacao());
                double distanciaB = Partida.DEPOSITO.distanceTo(b.getLocalizacao());
                return Double.compare(distanciaB, distanciaA);
            })
            .findFirst()
            .orElse(null);
    }
}

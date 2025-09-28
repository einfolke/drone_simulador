package com.drone.simulador.domain;

import java.time.LocalDateTime;
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

    private static final int LIMITE_CANDIDATOS_COMBINACAO = 12;
    private static final double TOLERANCIA = 1e-6;

    public List<Viagem> planejar(List<Drone> drones, List<Pedido> pedidos) {
        return planejar(drones, pedidos, List.of());
    }

    public List<Viagem> planejar(List<Drone> drones, List<Pedido> pedidos, List<Obstaculo> obstaculos) {
        Objects.requireNonNull(drones, "Lista de drones nao pode ser nula");
        Objects.requireNonNull(pedidos, "Lista de pedidos nao pode ser nula");
        List<Obstaculo> obstaculosSeguros = obstaculos == null ? List.of() : obstaculos;
        if (drones.isEmpty()) {
            return List.of();
        }

        PriorityQueue<Pedido> fila = new PriorityQueue<>(ORDENACAO_POR_PRIORIDADE_E_CHEGADA);
        fila.addAll(pedidos);

        List<Viagem> viagens = new ArrayList<>();
        int idxDrone = 0;

        while (!fila.isEmpty()) {
            Drone droneAtual = drones.get(idxDrone);
            idxDrone = (idxDrone + 1) % drones.size();

            Viagem viagem = new Viagem(droneAtual);
            List<Pedido> selecionados = selecionarMelhorCombinacao(droneAtual, fila, obstaculosSeguros);

            if (selecionados.isEmpty()) {
                Pedido pedido = fila.poll();
                if (pedido != null) {
                    viagem.adicionarPedido(pedido);
                }
            } else {
                for (Pedido pedido : selecionados) {
                    if (fila.remove(pedido)) {
                        viagem.adicionarPedido(pedido);
                    }
                }
            }

            if (viagem.getPedidos().isEmpty()) {
                continue;
            }

            List<Partida> rota = ajustarPorAutonomia(viagem, fila, obstaculosSeguros);
            double distancia = CalcularDistanciaPorTrajeto.gerar(rota);
            double tempoHoras = calcularTempoHoras(distancia, viagem.getDrone());
            viagem.definirRota(rota, distancia, tempoHoras);

            viagens.add(viagem);
        }

        return viagens;
    }

    private List<Pedido> selecionarMelhorCombinacao(Drone drone, PriorityQueue<Pedido> fila, List<Obstaculo> obstaculos) {
        if (fila.isEmpty()) {
            return List.of();
        }

        List<Pedido> ordenados = new ArrayList<>(fila);
        ordenados.sort(ORDENACAO_POR_PRIORIDADE_E_CHEGADA);
        int limite = Math.min(LIMITE_CANDIDATOS_COMBINACAO, ordenados.size());
        List<Pedido> candidatos = new ArrayList<>(ordenados.subList(0, limite));

        MelhorCombinacao melhor = new MelhorCombinacao();
        explorarCombinacoes(candidatos, 0, new ArrayList<>(), 0.0, drone, obstaculos, melhor);
        return melhor.obterPedidos();
    }

    private void explorarCombinacoes(
        List<Pedido> candidatos,
        int indice,
        List<Pedido> corrente,
        double pesoAtual,
        Drone drone,
        List<Obstaculo> obstaculos,
        MelhorCombinacao melhor
    ) {
        if (indice >= candidatos.size()) {
            avaliarCombinacao(corrente, drone, obstaculos, melhor);
            return;
        }

        Pedido pedido = candidatos.get(indice);

        double novoPeso = pesoAtual + pedido.getPesoEmKg();
        if (novoPeso <= drone.getCapacidadePorPeso() + TOLERANCIA) {
            corrente.add(pedido);
            explorarCombinacoes(candidatos, indice + 1, corrente, novoPeso, drone, obstaculos, melhor);
            corrente.remove(corrente.size() - 1);
        }

        explorarCombinacoes(candidatos, indice + 1, corrente, pesoAtual, drone, obstaculos, melhor);
    }

    private void avaliarCombinacao(List<Pedido> combinacao, Drone drone, List<Obstaculo> obstaculos, MelhorCombinacao melhor) {
        if (combinacao.isEmpty()) {
            return;
        }

        double pesoTotal = combinacao.stream().mapToDouble(Pedido::getPesoEmKg).sum();
        if (pesoTotal > drone.getCapacidadePorPeso() + TOLERANCIA) {
            return;
        }

        List<Partida> rota = CalcularDistanciaPorTrajeto.buildRoute(combinacao, obstaculos);
        double distancia = CalcularDistanciaPorTrajeto.gerar(rota);
        double alcanceMaximo = drone.getDistanciaPorCarga();
        if (alcanceMaximo > 0 && distancia > alcanceMaximo + TOLERANCIA) {
            return;
        }

        int prioridadeTotal = combinacao.stream()
            .mapToInt(p -> p.getPrioridade().getPeso())
            .sum();
        double pesoUso = pesoTotal / drone.getCapacidadePorPeso();
        double alcanceUso = alcanceMaximo == 0 ? 0.0 : distancia / alcanceMaximo;
        LocalDateTime chegadaMaisAntiga = combinacao.stream()
            .map(Pedido::getTempoChegada)
            .min(LocalDateTime::compareTo)
            .orElse(null);

        CombinationRanking ranking = new CombinationRanking(
            prioridadeTotal,
            combinacao.size(),
            pesoUso,
            alcanceUso,
            chegadaMaisAntiga,
            distancia
        );

        if (melhor.deveAtualizar(ranking)) {
            melhor.atualizar(List.copyOf(combinacao), ranking);
        }
    }

    private List<Partida> ajustarPorAutonomia(Viagem viagem, Queue<Pedido> restantes, List<Obstaculo> obstaculos) {
        while (true) {
            if (viagem.getPedidos().isEmpty()) {
                return List.of(Partida.DEPOSITO, Partida.DEPOSITO);
            }

            var rota = CalcularDistanciaPorTrajeto.buildRoute(viagem.getPedidos(), obstaculos);
            double distanciaTotal = CalcularDistanciaPorTrajeto.gerar(rota);
            double autonomiaDisponivel = viagem.getDrone().getDistanciaPorCarga();

            if (distanciaTotal <= autonomiaDisponivel + TOLERANCIA) {
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

    private static final class MelhorCombinacao {
        private List<Pedido> pedidos = List.of();
        private CombinationRanking ranking;

        boolean deveAtualizar(CombinationRanking candidata) {
            if (candidata == null) {
                return false;
            }
            if (ranking == null) {
                return true;
            }
            return candidata.compareTo(ranking) < 0;
        }

        void atualizar(List<Pedido> novosPedidos, CombinationRanking novaRanking) {
            this.pedidos = novosPedidos;
            this.ranking = novaRanking;
        }

        List<Pedido> obterPedidos() {
            return pedidos;
        }
    }

    private static final class CombinationRanking implements Comparable<CombinationRanking> {
        private final int prioridadeTotal;
        private final int quantidadePedidos;
        private final double pesoUso;
        private final double alcanceUso;
        private final LocalDateTime chegadaMaisAntiga;
        private final double distancia;

        CombinationRanking(
            int prioridadeTotal,
            int quantidadePedidos,
            double pesoUso,
            double alcanceUso,
            LocalDateTime chegadaMaisAntiga,
            double distancia
        ) {
            this.prioridadeTotal = prioridadeTotal;
            this.quantidadePedidos = quantidadePedidos;
            this.pesoUso = pesoUso;
            this.alcanceUso = alcanceUso;
            this.chegadaMaisAntiga = chegadaMaisAntiga;
            this.distancia = distancia;
        }

        @Override
        public int compareTo(CombinationRanking outro) {
            int prioridade = Integer.compare(outro.prioridadeTotal, prioridadeTotal);
            if (prioridade != 0) {
                return prioridade;
            }
            int quantidade = Integer.compare(outro.quantidadePedidos, quantidadePedidos);
            if (quantidade != 0) {
                return quantidade;
            }
            int pesoComparado = Double.compare(outro.pesoUso, pesoUso);
            if (pesoComparado != 0) {
                return pesoComparado;
            }
            int alcanceComparado = Double.compare(outro.alcanceUso, alcanceUso);
            if (alcanceComparado != 0) {
                return alcanceComparado;
            }
            if (chegadaMaisAntiga != null && outro.chegadaMaisAntiga != null && !chegadaMaisAntiga.equals(outro.chegadaMaisAntiga)) {
                return chegadaMaisAntiga.isBefore(outro.chegadaMaisAntiga) ? -1 : 1;
            }
            return Double.compare(distancia, outro.distancia);
        }
    }
}

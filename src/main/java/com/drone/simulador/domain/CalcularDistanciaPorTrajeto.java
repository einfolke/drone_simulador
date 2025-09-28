package com.drone.simulador.domain;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public final class CalcularDistanciaPorTrajeto {
    private CalcularDistanciaPorTrajeto() {
    }

    public static List<Partida> buildRoute(List<Pedido> pedidos) {
        return buildRoute(pedidos, List.of());
    }

    public static List<Partida> buildRoute(List<Pedido> pedidos, List<Obstaculo> obstaculos) {
        List<Pedido> pedidosSeguros = pedidos == null ? List.of() : pedidos;

        List<Partida> clientes = pedidosSeguros.stream()
            .map(Pedido::getLocalizacao)
            .filter(Objects::nonNull)
            .collect(Collectors.toCollection(ArrayList::new));

        List<Partida> rotaBase = new ArrayList<>();
        Partida deposito = Partida.DEPOSITO;
        if (deposito == null) {
            throw new IllegalStateException("Deposito nao definido");
        }

        Partida atual = deposito;
        rotaBase.add(atual);

        while (!clientes.isEmpty()) {
            Partida proximo = nearest(atual, clientes);
            if (proximo == null) {
                break;
            }
            rotaBase.add(proximo);
            clientes.remove(proximo);
            atual = proximo;
        }

        rotaBase.add(deposito);
        return ajustarRotaParaObstaculos(rotaBase, obstaculos);
    }

    private static List<Partida> ajustarRotaParaObstaculos(List<Partida> rotaBase, List<Obstaculo> obstaculos) {
        if (rotaBase.size() < 2 || obstaculos == null || obstaculos.isEmpty()) {
            return rotaBase;
        }
        List<Partida> rotaAjustada = new LinkedList<>();
        for (int i = 0; i < rotaBase.size() - 1; i++) {
            Partida origem = rotaBase.get(i);
            Partida destino = rotaBase.get(i + 1);
            rotaAjustada.add(origem);

            for (Obstaculo obstaculo : obstaculos) {
                if (obstaculo == null) {
                    continue;
                }
                if (obstaculo.intersectaSegmento(origem, destino)) {
                    Partida[] desvios = obstaculo.gerarDesvio(origem, destino);
                    for (Partida desvio : desvios) {
                        if (desvio != null && !rotaAjustada.isEmpty()) {
                            Partida ultimo = rotaAjustada.get(rotaAjustada.size() - 1);
                            if (!ultimo.equals(desvio)) {
                                rotaAjustada.add(desvio);
                            }
                        }
                    }
                    break;
                }
            }
        }
        rotaAjustada.add(rotaBase.get(rotaBase.size() - 1));
        return List.copyOf(rotaAjustada);
    }

    private static Partida nearest(Partida origem, List<Partida> candidatos) {
        if (origem == null) {
            throw new IllegalArgumentException("Ponto de origem nao pode ser nulo");
        }
        if (candidatos == null || candidatos.isEmpty()) {
            return null;
        }

        Partida melhor = null;
        double menorDistancia = Double.MAX_VALUE;
        for (Partida candidato : candidatos) {
            if (candidato == null) {
                continue;
            }
            double distancia = origem.distanceTo(candidato);
            if (distancia < menorDistancia) {
                menorDistancia = distancia;
                melhor = candidato;
            }
        }
        return melhor;
    }

    public static double gerar(List<Partida> rota) {
        if (rota == null || rota.size() < 2) {
            return 0.0;
        }
        double distanciaTotal = 0.0;
        for (int i = 0; i < rota.size() - 1; i++) {
            Partida origem = rota.get(i);
            Partida destino = rota.get(i + 1);
            if (origem == null || destino == null) {
                throw new IllegalArgumentException("Rota contem pontos nulos nas posicoes " + i + " ou " + (i + 1));
            }
            distanciaTotal += origem.distanceTo(destino);
        }
        return distanciaTotal;
    }
}

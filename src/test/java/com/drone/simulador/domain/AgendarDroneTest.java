package com.drone.simulador.domain;

import static junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AgendarDroneTest {

    @Test
    @DisplayName("planejar distribui pedidos respeitando capacidade e garante rota com deposito")
    void planejarDistribuiPedidosRespeitandoCapacidade() {
        var agendador = new AgendarDrone();
        var drones = List.of(
            new Drone("D1", 5.0, 10.0),
            new Drone("D2", 5.0, 10.0)
        );

        var pedidoAlta = new Pedido(new Partida(2, 2), 3.0, Prioridade.ALTA);
        var pedidoMedia = new Pedido(new Partida(4, 3), 3.0, Prioridade.MEDIA);
        var pedidoBaixa = new Pedido(new Partida(1, 5), 2.0, Prioridade.BAIXA);

        var pedidos = new ArrayList<>(List.of(pedidoAlta, pedidoMedia, pedidoBaixa));

        var viagens = agendador.planejar(drones, pedidos);

        assertEquals(2, viagens.size(), "deveria gerar viagens suficientes para respeitar peso");
        viagens.forEach(viagem -> {
            assertTrue(viagem.getPesoTotalKg() <= viagem.getDrone().getCapacidadePorPeso());
            var rota = viagem.getRota();
            assertTrue(!rota.isEmpty() && rota.get(0).equals(Partida.DEPOSITO), "rota deve iniciar no deposito");
            assertTrue(!rota.isEmpty() && rota.get(rota.size() - 1).equals(Partida.DEPOSITO), "rota deve finalizar no deposito");
            assertTrue(viagem.getTempoHoras() >= 0, "tempo precisa ser calculado");
        });
    }

    @Test
    @DisplayName("planejar remove pedidos de menor prioridade quando distancia excede autonomia")
    void planejarRespeitaAutonomiaRemovendoMenorPrioridade() {
        var agendador = new AgendarDrone();
        var droneUnico = new Drone("D1", 10.0, 2.0); // distancia maxima = 20km
        var drones = List.of(droneUnico);

        var pedidoPrioritario = new Pedido(new Partida(6, 0), 4.0, Prioridade.ALTA);
        var pedidoMenor = new Pedido(new Partida(12, 0), 4.0, Prioridade.BAIXA);

        var pedidos = new ArrayList<>(List.of(pedidoPrioritario, pedidoMenor));

        var viagens = agendador.planejar(drones, pedidos);

        assertEquals(2, viagens.size(), "espera duas viagens: prioridade alta primeiro e restante depois");

        var primeiraViagem = viagens.get(0);
        assertTrue(primeiraViagem.getPedidos().contains(pedidoPrioritario));
        assertTrue(!primeiraViagem.getPedidos().contains(pedidoMenor));
        assertTrue(primeiraViagem.getDistanciaKm() <= droneUnico.getDistanciaPorCarga());

        var segundaViagem = viagens.get(1);
        assertTrue(segundaViagem.getPedidos().contains(pedidoMenor));
    }

    @Test
    @DisplayName("planejar insere desvios quando ha obstaculos na rota")
    void planejarConsideraObstaculos() {
        var agendador = new AgendarDrone();
        var drones = List.of(new Drone("D1", 5.0, 10.0));
        var pedidos = new ArrayList<>(List.of(new Pedido(new Partida(10, 0), 1.0, Prioridade.ALTA)));
        var obstaculos = List.of(new Obstaculo(5, 0, 1.0));

        var viagens = agendador.planejar(drones, pedidos, obstaculos);

        assertEquals(1, viagens.size());
        var rota = viagens.get(0).getRota();
        assertTrue(rota.size() > 3, "rota deve conter pontos extras para desvio");
        boolean possuiDesvioSuperior = rota.stream().anyMatch(p -> Math.abs(p.y() - 1.5) < 1e-6);
        assertTrue(possuiDesvioSuperior, "espera ponto de desvio acima do obstaculo");
        assertTrue(viagens.get(0).getTempoHoras() > 0, "tempo deve ser maior que zero");
    }
}

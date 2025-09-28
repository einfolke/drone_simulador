package com.drone.simulador.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class Viagem {
    private final Drone drone;
    private final List<Pedido> pedidos = new ArrayList<>();
    private double pesoTotalKg = 0.0;
    private double distanciaKm = 0.0;
    private double tempoHoras = 0.0;
    private List<Partida> rota = List.of();

    public Viagem(Drone drone) {
        this.drone = Objects.requireNonNull(drone, "Drone nao pode ser nulo");
    }

    public Drone getDrone() {
        return drone;
    }

    public List<Pedido> getPedidos() {
        return Collections.unmodifiableList(pedidos);
    }

    public double getPesoTotalKg() {
        return pesoTotalKg;
    }

    public double getDistanciaKm() {
        return distanciaKm;
    }

    public double getTempoHoras() {
        return tempoHoras;
    }

    public List<Partida> getRota() {
        return rota;
    }

    public boolean cabe(Pedido pedido) {
        Objects.requireNonNull(pedido, "Pedido nao pode ser nulo");
        return pesoTotalKg + pedido.getPesoEmKg() <= drone.getCapacidadePorPeso();
    }

    public void adicionarPedido(Pedido pedido) {
        pedidos.add(Objects.requireNonNull(pedido, "Pedido nao pode ser nulo"));
        pesoTotalKg += pedido.getPesoEmKg();
    }

    public void removerPedido(Pedido pedido) {
        if (pedidos.remove(pedido)) {
            pesoTotalKg -= pedido.getPesoEmKg();
        }
    }

    public void definirRota(List<Partida> novaRota, double distancia, double tempo) {
        rota = List.copyOf(Objects.requireNonNull(novaRota, "Rota nao pode ser nula"));
        distanciaKm = distancia;
        tempoHoras = tempo;
    }
}

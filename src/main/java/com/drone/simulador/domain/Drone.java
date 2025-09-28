package com.drone.simulador.domain;

public class Drone {
    private final String id;
    private final double capacidadePorPeso;
    private final double tempoDeVooPorCarga;

    public Drone(String id, double capacidadePorPeso, double tempoDeVooPorCarga) {
        if (capacidadePorPeso <= 0 || tempoDeVooPorCarga <= 0) {
            throw new IllegalArgumentException("Capacidade e tempo de voo devem ser positivos");
        }
        this.id = id;
        this.capacidadePorPeso = capacidadePorPeso;
        this.tempoDeVooPorCarga = tempoDeVooPorCarga;
    }

    public String getId() {
        return id;
    }

    public double getCapacidadePorPeso() {
        return capacidadePorPeso;
    }

    public double getTempoDeVooPorCarga() {
        return tempoDeVooPorCarga;
    }

    public double getDistanciaPorCarga() {
        return capacidadePorPeso * tempoDeVooPorCarga;
    }
}

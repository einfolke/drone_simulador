package com.drone.simulador.domain;

public enum Prioridade {
    ALTA(3),
    MEDIA(2),
    BAIXA(1);

    private final int peso;

    Prioridade(int peso) {
        this.peso = peso;
    }

    public int getPeso() {
        return peso;
    }
}

package com.drone.simulador.domain;

public record Partida(double x, double y) {
    public static final Partida DEPOSITO = new Partida(0, 0);

    public double distanceTo(Partida outro) {
        if (outro == null) {
            throw new IllegalArgumentException("Ponto destino nao pode ser nulo");
        }
        double dx = x - outro.x;
        double dy = y - outro.y;
        return Math.sqrt(dx * dx + dy * dy);
    }
}

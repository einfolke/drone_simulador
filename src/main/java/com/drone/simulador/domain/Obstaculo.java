package com.drone.simulador.domain;

public record Obstaculo(double x, double y, double raio) {

    private static final double MARGEM = 0.5;

    public boolean intersectaSegmento(Partida a, Partida b) {
        double distancia = distanciaPontoParaSegmento(a, b, x, y);
        return distancia < raio;
    }

    public Partida[] gerarDesvio(Partida origem, Partida destino) {
        double dx = destino.x() - origem.x();
        double dy = destino.y() - origem.y();
        double comprimento = Math.hypot(dx, dy);
        if (comprimento == 0.0) {
            return new Partida[0];
        }

        double px = -dy / comprimento;
        double py = dx / comprimento;

        double midX = (origem.x() + destino.x()) * 0.5;
        double midY = (origem.y() + destino.y()) * 0.5;
        double dot = (midX - x) * px + (midY - y) * py;
        double sinal = dot >= 0 ? 1.0 : -1.0;
        px *= sinal;
        py *= sinal;

        double clearance = raio + MARGEM;

        Partida ponto1 = new Partida(origem.x() + px * clearance, origem.y() + py * clearance);
        Partida ponto2 = new Partida(destino.x() + px * clearance, destino.y() + py * clearance);
        return new Partida[] { ponto1, ponto2 };
    }

    private static double distanciaPontoParaSegmento(Partida a, Partida b, double px, double py) {
        double ax = a.x();
        double ay = a.y();
        double bx = b.x();
        double by = b.y();

        double dx = bx - ax;
        double dy = by - ay;
        if (dx == 0.0 && dy == 0.0) {
            return Math.hypot(ax - px, ay - py);
        }

        double t = ((px - ax) * dx + (py - ay) * dy) / (dx * dx + dy * dy);
        t = Math.max(0.0, Math.min(1.0, t));
        double projX = ax + t * dx;
        double projY = ay + t * dy;
        double difX = projX - px;
        double difY = projY - py;
        return Math.hypot(difX, difY);
    }
}


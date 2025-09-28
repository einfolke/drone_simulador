package com.drone.simulador.domain;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicLong;

public class Pedido {
    private static final AtomicLong SEQUENCIA = new AtomicLong(1);

    private final long id;
    private final Partida localizacao;
    private final double pesoEmKg;
    private final Prioridade prioridade;
    private final LocalDateTime tempoChegada;

    public Pedido(Partida localizacao, double pesoEmKg, Prioridade prioridade) {
        this(localizacao, pesoEmKg, prioridade, LocalDateTime.now());
    }

    public Pedido(Partida localizacao, double pesoEmKg, Prioridade prioridade, LocalDateTime tempoChegada) {
        this.id = SEQUENCIA.getAndIncrement();
        this.localizacao = localizacao;
        this.pesoEmKg = pesoEmKg;
        this.prioridade = prioridade;
        this.tempoChegada = tempoChegada == null ? LocalDateTime.now() : tempoChegada;
    }

    public long getId() {
        return id;
    }

    public Partida getLocalizacao() {
        return localizacao;
    }

    public double getPesoEmKg() {
        return pesoEmKg;
    }

    public Prioridade getPrioridade() {
        return prioridade;
    }

    public LocalDateTime getTempoChegada() {
        return tempoChegada;
    }
}

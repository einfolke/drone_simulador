package com.drone.simulador.dto;

import java.util.List;

public record SaidaDTO(List<ViagemDTO> viagens) {

    public static class ViagemDTO {
        public String idDrone;
        public double pesoTotalEmKg;
        public double distanciaEmKm;
        public List<Long> idsPedidos;
        public List<double[]> rota;
    }
}

package com.drone.simulador.dto;

import java.util.List;

public record EntradaDTO(List<DroneDTO> drones, List<PedidoDTO> pedidos) {

    public static class DroneDTO {
        public String id;
        public double capacidadePorPeso;
        public double tempoDeVooPorCarga;
    }

    public static class PedidoDTO {
        public double x;
        public double y;
        public double pesoKg;
        public String prioridade;
    }
}

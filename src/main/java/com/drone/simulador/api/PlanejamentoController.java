package com.drone.simulador.api;

import com.drone.simulador.api.dto.PlanejamentoRequest;
import com.drone.simulador.api.dto.PlanejamentoResponse;
import com.drone.simulador.api.dto.PlanejamentoResponse.BatteryPassoResponse;
import com.drone.simulador.api.dto.PlanejamentoResponse.BatteryResponse;
import com.drone.simulador.api.dto.PlanejamentoResponse.ViagemResponse;
import com.drone.simulador.dao.DroneDao;
import com.drone.simulador.dao.EntregaDao;
import com.drone.simulador.dao.model.DroneRecord;
import com.drone.simulador.dao.model.EntregaRecord;
import com.drone.simulador.domain.AgendarDrone;
import com.drone.simulador.domain.BatteryStep;
import com.drone.simulador.domain.Drone;
import com.drone.simulador.domain.DroneBatterySimulation;
import com.drone.simulador.domain.DroneBatterySimulator;
import com.drone.simulador.domain.Obstaculo;
import com.drone.simulador.domain.Partida;
import com.drone.simulador.domain.Pedido;
import com.drone.simulador.domain.Prioridade;
import com.drone.simulador.domain.Viagem;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;

public final class PlanejamentoController implements HttpHandler {
    private final ObjectMapper mapper;
    private final AgendarDrone agendador;
    private final DroneBatterySimulator batterySimulator;
    private final DroneDao droneDao;
    private final EntregaDao entregaDao;

    public PlanejamentoController(ObjectMapper mapper, DataSource dataSource) {
        this(mapper, new AgendarDrone(), new DroneBatterySimulator(), new DroneDao(dataSource), new EntregaDao(dataSource));
    }

    public PlanejamentoController(
        ObjectMapper mapper,
        AgendarDrone agendador,
        DroneBatterySimulator batterySimulator,
        DroneDao droneDao,
        EntregaDao entregaDao
    ) {
        this.mapper = Objects.requireNonNull(mapper, "mapper nao pode ser nulo");
        this.agendador = Objects.requireNonNull(agendador, "agendador nao pode ser nulo");
        this.batterySimulator = Objects.requireNonNull(batterySimulator, "batterySimulator nao pode ser nulo");
        this.droneDao = Objects.requireNonNull(droneDao, "droneDao nao pode ser nulo");
        this.entregaDao = Objects.requireNonNull(entregaDao, "entregaDao nao pode ser nulo");
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try (exchange) {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                writeJson(exchange, 405, new ErrorResponse("Metodo nao permitido", "Use POST"));
                return;
            }

            PlanejamentoRequest requestBody;
            try (InputStream body = exchange.getRequestBody()) {
                requestBody = mapper.readValue(body, PlanejamentoRequest.class);
            } catch (JsonProcessingException e) {
                writeJson(exchange, 400, new ErrorResponse("JSON invalido", Optional.ofNullable(e.getOriginalMessage()).orElse(e.getMessage())));
                return;
            }

            try {
                PlanejamentoResponse response = processar(requestBody);
                writeJson(exchange, 200, response);
            } catch (IllegalArgumentException e) {
                writeJson(exchange, 422, new ErrorResponse("Dados invalidos", e.getMessage()));
            } catch (SQLException e) {
                e.printStackTrace();
                writeJson(exchange, 500, new ErrorResponse("Erro no banco de dados", e.getMessage()));
            } catch (Exception e) {
                e.printStackTrace();
                writeJson(exchange, 500, new ErrorResponse("Erro interno", e.getMessage()));
            }
        }
    }

    private PlanejamentoResponse processar(PlanejamentoRequest request) throws SQLException {
        persistPayload(request);

        List<DroneRecord> droneRecords = droneDao.listar();
        if (droneRecords.isEmpty()) {
            throw new IllegalArgumentException("Nenhum drone cadastrado");
        }
        List<EntregaRecord> entregaRecords = entregaDao.listar();
        if (entregaRecords.isEmpty()) {
            throw new IllegalArgumentException("Nenhum pedido cadastrado");
        }

        List<Drone> drones = droneRecords.stream().map(this::toDrone).toList();
        Map<Long, Long> pedidoIdMap = new HashMap<>();
        List<Pedido> pedidos = entregaRecords.stream()
            .map(record -> toPedido(record, pedidoIdMap))
            .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);

        List<Obstaculo> obstaculos = Optional.ofNullable(request.obstaculos()).orElse(List.of()).stream()
            .map(this::toObstaculo)
            .toList();

        List<Viagem> viagens = agendador.planejar(drones, new ArrayList<>(pedidos), obstaculos);
        double tempoTotal = viagens.stream().mapToDouble(Viagem::getTempoHoras).sum();
        List<ViagemResponse> viagensResponse = viagens.stream()
            .map(viagem -> toResponse(viagem, pedidoIdMap))
            .toList();

        return new PlanejamentoResponse("postgres", tempoTotal, viagensResponse);
    }

    private void persistPayload(PlanejamentoRequest request) throws SQLException {
        List<PlanejamentoRequest.DronePayload> dronesPayload = Optional.ofNullable(request.drones()).orElse(List.of());
        for (PlanejamentoRequest.DronePayload drone : dronesPayload) {
            persistDrone(drone);
        }

        List<PlanejamentoRequest.PedidoPayload> pedidosPayload = Optional.ofNullable(request.pedidos()).orElse(List.of());
        for (PlanejamentoRequest.PedidoPayload pedido : pedidosPayload) {
            persistPedido(pedido);
        }
    }

    private void persistDrone(PlanejamentoRequest.DronePayload payload) throws SQLException {
        if (payload == null) {
            return;
        }
        String identificador = Optional.ofNullable(payload.id()).map(String::trim)
            .filter(s -> !s.isEmpty())
            .orElseThrow(() -> new IllegalArgumentException("Drone sem identificador"));

        double capacidade = firstPositive(payload.capacidadeKg(), payload.capacidadePorPeso())
            .orElseThrow(() -> new IllegalArgumentException("Drone " + identificador + " precisa de capacidade"));

        Optional<Double> autonomiaInformada = firstPositive(payload.autonomiaKm(), payload.distanciaPorCarga());
        Optional<Double> tempoInformado = firstPositive(payload.tempoVooHoras(), payload.tempoDeVooPorCarga());
        double autonomia = autonomiaInformada.orElseGet(() -> tempoInformado
            .map(tempo -> capacidade * tempo)
            .orElseThrow(() -> new IllegalArgumentException("Drone " + identificador + " precisa de autonomia ou tempo de voo")));

        try {
            droneDao.inserir(identificador, capacidade, autonomia, null);
        } catch (SQLException e) {
            if (!isUniqueViolation(e)) {
                throw e;
            }
        }
    }

    private void persistPedido(PlanejamentoRequest.PedidoPayload payload) throws SQLException {
        if (payload == null) {
            return;
        }
        double peso = Optional.ofNullable(payload.pesoKg())
            .filter(value -> value > 0)
            .orElseThrow(() -> new IllegalArgumentException("Pedido sem peso valido"));

        int destinoX = toCoordinate(payload.x(), "x");
        int destinoY = toCoordinate(payload.y(), "y");
        int prioridade = prioridadeToInt(payload.prioridade());

        String pedidoId = gerarPedidoId(payload);
        try {
            entregaDao.inserir(pedidoId, peso, 0, 0, destinoX, destinoY, prioridade, null, null);
        } catch (SQLException e) {
            if (!isUniqueViolation(e)) {
                throw e;
            }
        }
    }

    private String gerarPedidoId(PlanejamentoRequest.PedidoPayload payload) {
        String base = String.format(
            Locale.ROOT,
            "%.4f:%.4f:%.4f:%s:%s",
            Optional.ofNullable(payload.x()).orElse(0.0),
            Optional.ofNullable(payload.y()).orElse(0.0),
            Optional.ofNullable(payload.pesoKg()).orElse(0.0),
            Optional.ofNullable(payload.prioridade()).orElse(""),
            Optional.ofNullable(payload.tempoChegada()).orElse("")
        );
        return "REQ-" + UUID.nameUUIDFromBytes(base.getBytes(StandardCharsets.UTF_8));
    }

    private int prioridadeToInt(String prioridadeBruta) {
        String valor = Optional.ofNullable(prioridadeBruta).map(String::trim)
            .filter(s -> !s.isEmpty())
            .orElseThrow(() -> new IllegalArgumentException("Pedido sem prioridade"));
        return switch (valor.toUpperCase(Locale.ROOT)) {
            case "ALTA" -> 3;
            case "MEDIA" -> 2;
            case "BAIXA" -> 1;
            default -> throw new IllegalArgumentException("Prioridade invalida: " + valor);
        };
    }

    private int toCoordinate(Double valor, String campo) {
        double resolved = Optional.ofNullable(valor)
            .orElseThrow(() -> new IllegalArgumentException("Pedido sem coordenada " + campo));
        if (!Double.isFinite(resolved)) {
            throw new IllegalArgumentException("Coordenada " + campo + " invalida");
        }
        long arredondado = Math.round(resolved);
        if (arredondado < Integer.MIN_VALUE || arredondado > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Coordenada " + campo + " fora do intervalo suportado");
        }
        return (int) arredondado;
    }

    private Drone toDrone(DroneRecord record) {
        double capacidade = record.capacidadeKg();
        double autonomia = record.autonomiaKm();
        double tempo = autonomia > 0 ? autonomia / Math.max(capacidade, 1e-6) : 1.0;
        return new Drone(record.identificador(), capacidade, tempo);
    }

    private Pedido toPedido(EntregaRecord record, Map<Long, Long> pedidoIdMap) {
        Prioridade prioridade = switch (record.prioridade()) {
            case 3 -> Prioridade.ALTA;
            case 2 -> Prioridade.MEDIA;
            default -> Prioridade.BAIXA;
        };
        LocalDateTime chegada = record.criadoEm();
        Pedido pedido = chegada != null
            ? new Pedido(new Partida(record.destinoX(), record.destinoY()), record.pesoKg(), prioridade, chegada)
            : new Pedido(new Partida(record.destinoX(), record.destinoY()), record.pesoKg(), prioridade);
        pedidoIdMap.put(pedido.getId(), record.id());
        return pedido;
    }

    private ViagemResponse toResponse(Viagem viagem, Map<Long, Long> pedidoIdMap) {
        List<Long> idsPedidos = viagem.getPedidos().stream()
            .map(pedido -> pedidoIdMap.getOrDefault(pedido.getId(), pedido.getId()))
            .toList();
        List<double[]> rota = viagem.getRota().stream()
            .map(p -> new double[] { p.x(), p.y() })
            .toList();
        DroneBatterySimulation simulacao = batterySimulator.simular(viagem);
        BatteryResponse bateria = toBatteryResponse(simulacao);
        return new ViagemResponse(
            viagem.getDrone().getId(),
            viagem.getPesoTotalKg(),
            viagem.getDistanciaKm(),
            viagem.getTempoHoras(),
            idsPedidos,
            rota,
            bateria
        );
    }

    private BatteryResponse toBatteryResponse(DroneBatterySimulation simulacao) {
        List<BatteryPassoResponse> passos = simulacao.passos().stream()
            .map(this::toBatteryStep)
            .toList();
        return new BatteryResponse(
            simulacao.distanciaTotalKm(),
            simulacao.tempoTotalHoras(),
            simulacao.rotaCompleta(),
            passos
        );
    }

    private BatteryPassoResponse toBatteryStep(BatteryStep passo) {
        return new BatteryPassoResponse(
            passo.distanciaSegmentoKm(),
            passo.distanciaAcumuladaKm(),
            passo.tempoSegmentoHoras(),
            passo.tempoAcumuladoHoras(),
            passo.cargaRestantePercentual()
        );
    }

    private Obstaculo toObstaculo(PlanejamentoRequest.ObstaculoPayload payload) {
        if (payload == null) {
            throw new IllegalArgumentException("Obstaculo nao pode ser nulo");
        }
        double x = Optional.ofNullable(payload.x()).orElseThrow(() -> new IllegalArgumentException("Obstaculo sem x"));
        double y = Optional.ofNullable(payload.y()).orElseThrow(() -> new IllegalArgumentException("Obstaculo sem y"));
        double raio = Optional.ofNullable(payload.raio()).orElseThrow(() -> new IllegalArgumentException("Obstaculo sem raio"));
        if (raio <= 0) {
            throw new IllegalArgumentException("Obstaculo com raio invalido: " + raio);
        }
        return new Obstaculo(x, y, raio);
    }

    private Optional<Double> firstPositive(Double... valores) {
        if (valores == null) {
            return Optional.empty();
        }
        for (Double valor : valores) {
            if (valor != null && valor > 0) {
                return Optional.of(valor);
            }
        }
        return Optional.empty();
    }

    private boolean isUniqueViolation(SQLException e) {
        return "23505".equals(e.getSQLState());
    }

    private void writeJson(HttpExchange exchange, int status, Object body) throws IOException {
        byte[] payload = mapper.writeValueAsBytes(body);
        Headers headers = exchange.getResponseHeaders();
        headers.set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(status, payload.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(payload);
        }
    }

    private record ErrorResponse(String erro, String detalhes) {}
}


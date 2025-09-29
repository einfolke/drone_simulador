import { useEffect, useMemo, useRef, useState } from "react";
import {
  Alert,
  AppShell,
  Badge,
  Box,
  Button,
  Card,
  Container,
  Divider,
  Grid,
  Group,
  List,
  Paper,
  ScrollArea,
  SimpleGrid,
  Stack,
  Table,
  Text,
  ThemeIcon,
  Title
} from "@mantine/core";
import { notifications } from "@mantine/notifications";
import {
  IconAlertCircle,
  IconDrone,
  IconMap,
  IconPlayerPlay,
  IconRoute,
  IconSparkles
} from "@tabler/icons-react";

const OBSTACLES = [
  { x: 5, y: 2, raio: 1.2, descricao: "Zona industrial" },
  { x: 4, y: 5, raio: 1.0, descricao: "Aeroporto" }
];

export default function App() {
  const canvasRef = useRef(null);
  const payload = useMemo(
    () => ({
      drones: [{ id: "D1", capacidadeKg: 5, autonomiaKm: 20 }],
      pedidos: [
        { x: 3, y: 4, pesoKg: 2, prioridade: "ALTA" },
        { x: 7, y: 1, pesoKg: 1, prioridade: "MEDIA" },
        { x: 6, y: 6, pesoKg: 2.5, prioridade: "ALTA" }
      ],
      obstaculos: OBSTACLES
    }),
    []
  );

  const [saida, setSaida] = useState(null);
  const [erro, setErro] = useState("");
  const [rotaAtual, setRotaAtual] = useState([]);
  const [tituloMapa, setTituloMapa] = useState("Mapa");

  const desenharRota = (rota, titulo = "Rota", obstaculos = []) => {
    const cvs = canvasRef.current;
    if (!cvs) return;
    const ctx = cvs.getContext("2d");
    const w = (cvs.width = 600);
    const h = (cvs.height = 500);

    ctx.fillStyle = "#fafafa";
    ctx.fillRect(0, 0, w, h);

    ctx.strokeStyle = "#eee";
    for (let x = 40; x < w; x += 40) {
      ctx.beginPath();
      ctx.moveTo(x, 0);
      ctx.lineTo(x, h);
      ctx.stroke();
    }
    for (let y = 40; y < h; y += 40) {
      ctx.beginPath();
      ctx.moveTo(0, y);
      ctx.lineTo(w, y);
      ctx.stroke();
    }

    ctx.fillStyle = "#000";
    ctx.font = "16px system-ui";
    ctx.fillText(titulo, 10, 24);

    const obsList = Array.isArray(obstaculos) ? obstaculos : [];

    const xs = Array.isArray(rota) ? rota.map((p) => p[0]) : [];
    const ys = Array.isArray(rota) ? rota.map((p) => p[1]) : [];
    obsList.forEach((obs) => {
      const r = Number(obs.raio ?? 0);
      xs.push((obs.x ?? 0) + r, (obs.x ?? 0) - r);
      ys.push((obs.y ?? 0) + r, (obs.y ?? 0) - r);
    });

    const minX = xs.length ? Math.min(...xs, 0) : 0;
    const maxX = xs.length ? Math.max(...xs, 0) : 0;
    const minY = ys.length ? Math.min(...ys, 0) : 0;
    const maxY = ys.length ? Math.max(...ys, 0) : 0;

    const pad = 40;
    const escalaX = (w - 2 * pad) / Math.max(1, maxX - minX);
    const escalaY = (h - 2 * pad) / Math.max(1, maxY - minY);
    const toScreen = ([x, y]) => [
      pad + (x - minX) * escalaX,
      h - (pad + (y - minY) * escalaY)
    ];

    if (!Array.isArray(rota) || rota.length < 2) {
      ctx.fillStyle = "#d22";
      ctx.beginPath();
      ctx.arc(40, h - 40, 6, 0, Math.PI * 2);
      ctx.fill();
      ctx.fillStyle = "#000";
      ctx.fillText("Sem rota (DEPOSITO)", 60, h - 36);
    } else {
      ctx.beginPath();
      rota.forEach((p, i) => {
        const [sx, sy] = toScreen(p);
        if (i === 0) ctx.moveTo(sx, sy);
        else ctx.lineTo(sx, sy);
      });
      ctx.strokeStyle = "#333";
      ctx.lineWidth = 2;
      ctx.stroke();

      rota.forEach((p, i) => {
        const [sx, sy] = toScreen(p);
        ctx.beginPath();
        ctx.arc(sx, sy, 5, 0, Math.PI * 2);
        ctx.fillStyle = i === 0 || i === rota.length - 1 ? "#d22" : "#06c";
        ctx.fill();
        ctx.fillStyle = "#000";
        ctx.font = "12px system-ui";
        ctx.fillText(`${p[0]},${p[1]}`, sx + 8, sy - 8);
      });
    }

    obsList.forEach((obs) => {
      const raio = Number(obs.raio ?? 0);
      if (raio <= 0) return;
      const [cx, cy] = toScreen([obs.x ?? 0, obs.y ?? 0]);
      const raioPx = raio * (escalaX + escalaY) * 0.5;
      ctx.beginPath();
      ctx.fillStyle = "rgba(200, 0, 0, 0.18)";
      ctx.strokeStyle = "#a00";
      ctx.lineWidth = 1.5;
      ctx.arc(cx, cy, raioPx, 0, Math.PI * 2);
      ctx.fill();
      ctx.stroke();
    });

    ctx.fillStyle = "#000";
    ctx.font = "13px system-ui";
    ctx.fillText("Vermelho = Deposito | Azul = Entregas | Circulos = Obstaculos", 10, h - 16);
  };

  useEffect(() => {
    desenharRota(rotaAtual, tituloMapa, payload.obstaculos);
  }, [rotaAtual, tituloMapa, payload]);

  const formatNumber = (value, decimals = 2) => {
    const num = Number(value);
    return Number.isFinite(num) ? num.toFixed(decimals) : "-";
  };

  function desenharTeste() {
    const rotaFake = [[0, 0], [3, 4], [7, 1], [6, 6], [0, 0]];
    setErro("");
    setSaida(null);
    setTituloMapa("Rota (teste offline)");
    setRotaAtual(rotaFake);
    notifications.show({
      color: "indigo",
      title: "Simulação offline",
      message: "Rota de teste desenhada no mapa.",
      icon: <IconSparkles size={18} />
    });
  }

  async function planejarApi() {
    setErro("");
    setSaida(null);
    setTituloMapa("Carregando rota...");
    setRotaAtual([]);
    try {
      const resp = await fetch("/api/planejar", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(payload)
      });
      const json = await resp.json();

      if (!resp.ok) throw new Error(json?.erro || "Falha na API");

      setSaida(json);
      const viagensResp = Array.isArray(json?.viagens) ? json.viagens : [];
      const primeiraRota = viagensResp.length
        ? Array.isArray(viagensResp[0]?.rota)
          ? viagensResp[0].rota
          : []
        : Array.isArray(json?.rota)
          ? json.rota
          : [];

      setTituloMapa(viagensResp.length > 0 ? "Rota (viagem 1)" : "Rota (API)");
      setRotaAtual(primeiraRota);
      notifications.show({
        color: "green",
        title: "Planejamento concluído",
        message: "Dados recebidos da API com sucesso.",
        icon: <IconDrone size={18} />
      });
    } catch (e) {
      console.error(e);
      setSaida(null);
      setErro(e.message);
      setTituloMapa("Erro ao buscar rota");
      setRotaAtual([]);
      notifications.show({
        color: "red",
        title: "Erro ao planejar",
        message: e.message,
        icon: <IconAlertCircle size={18} />
      });
    }
  }

  const viagens = Array.isArray(saida?.viagens) ? saida.viagens : [];
  const bateriaSolta =
    !viagens.length && saida?.bateria
      ? [
          {
            idDrone: saida?.idDrone ?? saida?.drone?.id,
            rota: Array.isArray(saida?.rota) ? saida.rota : [],
            distanciaKm: saida?.distanciaKm,
            pesoTotalKg: saida?.pesoTotalKg,
            tempoHoras: saida?.tempoHoras ?? saida?.bateria?.tempoTotalHoras,
            bateria: saida.bateria
          }
        ]
      : [];
  const viagensParaExibir = viagens.length ? viagens : bateriaSolta;
  const tempoEntregaTotal = viagensParaExibir.reduce(
    (total, viagem) =>
      total + (Number(viagem?.tempoHoras) || Number(viagem?.bateria?.tempoTotalHoras) || 0),
    0
  );

  const overviewCards = [
    {
      title: "Drones ativos",
      value: payload.drones.length,
      description: "Capacidade total " +
        formatNumber(
          payload.drones.reduce((acc, drone) => acc + Number(drone.capacidadeKg || 0), 0),
          1
        ) +
        " kg",
      color: "blue"
    },
    {
      title: "Pedidos na fila",
      value: payload.pedidos.length,
      description: "Prioridade alta " +
        payload.pedidos.filter((pedido) => pedido.prioridade === "ALTA").length,
      color: "teal"
    },
    {
      title: "Obstáculos mapeados",
      value: payload.obstaculos.length,
      description: "Inclui zonas sensíveis",
      color: "orange"
    }
  ];

  return (
    <AppShell padding="lg">
      <AppShell.Header>
        <Container size="xl" py="sm">
          <Group justify="space-between" align="flex-start">
            <Stack gap={4}>
              <Group gap="xs">
                <ThemeIcon variant="light" color="blue" radius="md" size="lg">
                  <IconDrone size={20} />
                </ThemeIcon>
                <Title order={2}>Planejamento de Drones</Title>
              </Group>
              <Text size="sm" c="dimmed">
                Defina cargas, calcule rotas e visualize restrições do espaço aéreo em tempo real.
              </Text>
            </Stack>
            <Group>
              <Button variant="light" leftSection={<IconSparkles size={16} />} onClick={desenharTeste}>
                Simular offline
              </Button>
              <Button leftSection={<IconPlayerPlay size={16} />} onClick={planejarApi}>
                Planejar com API
              </Button>
            </Group>
          </Group>
        </Container>
      </AppShell.Header>

      <AppShell.Main>
        <Container size="xl" py="xl">
          <Stack gap="xl">
            {erro && (
              <Alert
                variant="light"
                color="red"
                title="Erro ao consultar API"
                icon={<IconAlertCircle size={18} />}
              >
                {erro}
              </Alert>
            )}

            <SimpleGrid cols={{ base: 1, sm: 3 }} spacing="md">
              {overviewCards.map((card) => (
                <Card key={card.title} withBorder radius="md" shadow="sm">
                  <Stack gap="xs">
                    <Text size="xs" c="dimmed">
                      {card.title}
                    </Text>
                    <Title order={3}>{card.value}</Title>
                    <Text size="sm" c="dimmed">
                      {card.description}
                    </Text>
                    <Divider my="xs" />
                    <Badge color={card.color} variant="light" radius="sm">
                      Atualizado automaticamente
                    </Badge>
                  </Stack>
                </Card>
              ))}
            </SimpleGrid>

            <Grid gutter="md">
              <Grid.Col span={{ base: 12, md: 6 }}>
                <Card withBorder radius="md" shadow="sm">
                  <Group justify="space-between" mb="sm">
                    <Title order={4}>Payload de entrada</Title>
                    <Badge color="gray" variant="light">
                      JSON
                    </Badge>
                  </Group>
                  <ScrollArea h={260} type="auto">
                    <Paper
                      radius="sm"
                      p="sm"
                      withBorder
                      bg="dark.9"
                      c="green.4"
                      style={{ fontFamily: "monospace", fontSize: 12 }}
                      component="pre"
                    >
                      {JSON.stringify(payload, null, 2)}
                    </Paper>
                  </ScrollArea>
                </Card>
              </Grid.Col>
              <Grid.Col span={{ base: 12, md: 6 }}>
                <Card withBorder radius="md" shadow="sm">
                  <Group justify="space-between" mb="sm">
                    <Title order={4}>Retorno da API</Title>
                    <Badge color="green" variant="light">
                      Em tempo real
                    </Badge>
                  </Group>
                  <ScrollArea h={260} type="auto">
                    <Paper
                      radius="sm"
                      p="sm"
                      withBorder
                      bg="dark.9"
                      c="green.4"
                      style={{ fontFamily: "monospace", fontSize: 12 }}
                      component="pre"
                    >
                      {saida ? JSON.stringify(saida, null, 2) : "Sem resultado"}
                    </Paper>
                  </ScrollArea>
                </Card>
              </Grid.Col>
            </Grid>

            <Card withBorder radius="md" shadow="sm">
              <Group justify="space-between" mb="md">
                <Group gap="xs">
                  <ThemeIcon color="orange" variant="light">
                    <IconAlertCircle size={18} />
                  </ThemeIcon>
                  <Title order={4}>Obstáculos configurados</Title>
                </Group>
                <Badge color="orange" variant="light">
                  {payload.obstaculos.length} zonas
                </Badge>
              </Group>
              <List spacing="xs" size="sm">
                {payload.obstaculos.map((obs, index) => (
                  <List.Item
                    key={`obs-${index}`}
                    icon={
                      <ThemeIcon size={24} radius="xl" color="orange" variant="light">
                        <IconMap size={16} />
                      </ThemeIcon>
                    }
                  >
                    <Text fw={500}>{obs.descricao ?? `Obstáculo ${index + 1}`}</Text>
                    <Text size="sm" c="dimmed">
                      Centro ({obs.x}, {obs.y}) · Raio {obs.raio} km
                    </Text>
                  </List.Item>
                ))}
              </List>
            </Card>

            {viagensParaExibir.length > 0 && (
              <Card withBorder radius="md" shadow="sm">
                <Stack gap="md">
                  <Group justify="space-between" align="center">
                    <Title order={3}>Viagens e simulação de bateria</Title>
                    <Text size="sm" c="dimmed">
                      Tempo total estimado de entrega: {formatNumber(tempoEntregaTotal)} h
                    </Text>
                  </Group>
                  <Divider />
                  <Stack gap="md">
                    {viagensParaExibir.map((viagem, index) => {
                      const bateria = viagem?.bateria;
                      const rota = Array.isArray(viagem?.rota) ? viagem.rota : [];
                      const tempoHoras =
                        Number(viagem?.tempoHoras) || Number(bateria?.tempoTotalHoras);
                      const bateriaOk = bateria ? bateria.rotaCompleta : false;

                      return (
                        <Card key={`viagem-${index}`} withBorder padding="md" radius="md" shadow="xs">
                          <Stack gap="sm">
                            <Group justify="space-between" align="flex-start">
                              <Stack gap={4}>
                                <Group gap="xs">
                                  <Badge color="blue" variant="light">
                                    Viagem {index + 1}
                                  </Badge>
                                  <Badge color="indigo" variant="light">
                                    Drone {viagem?.idDrone ?? viagem?.drone?.id ?? "N/D"}
                                  </Badge>
                                  <Badge color={bateriaOk ? "green" : "red"} variant="light">
                                    {bateriaOk ? "Bateria suficiente" : "Bateria insuficiente"}
                                  </Badge>
                                </Group>
                                <Text size="sm" c="dimmed">
                                  Peso total {formatNumber(viagem?.pesoTotalKg)} kg · Distância {" "}
                                  {formatNumber(viagem?.distanciaKm)} km · Tempo estimado {formatNumber(tempoHoras)} h
                                </Text>
                              </Stack>
                              {rota.length > 0 && (
                                <Button
                                  variant="light"
                                  size="xs"
                                  leftSection={<IconRoute size={14} />}
                                  onClick={() => {
                                    setTituloMapa(`Rota (viagem ${index + 1})`);
                                    setRotaAtual(rota);
                                  }}
                                >
                                  Ver rota no mapa
                                </Button>
                              )}
                            </Group>

                            {bateria ? (
                              <Stack gap="xs">
                                <Text size="sm" fw={500}>
                                  Simulação de bateria
                                </Text>
                                <ScrollArea h={200} type="auto">
                                  <Table highlightOnHover withRowBorders>
                                    <Table.Thead>
                                      <Table.Tr>
                                        <Table.Th>#</Table.Th>
                                        <Table.Th>Dist. segmento (km)</Table.Th>
                                        <Table.Th>Dist. acumulada (km)</Table.Th>
                                        <Table.Th>Tempo segmento (h)</Table.Th>
                                        <Table.Th>Tempo acumulado (h)</Table.Th>
                                        <Table.Th>Bateria restante (%)</Table.Th>
                                      </Table.Tr>
                                    </Table.Thead>
                                    <Table.Tbody>
                                      {Array.isArray(bateria.passos) && bateria.passos.length > 0 ? (
                                        bateria.passos.map((passo, passoIndex) => (
                                          <Table.Tr key={`passo-${passoIndex}`}>
                                            <Table.Td>{passoIndex + 1}</Table.Td>
                                            <Table.Td>{formatNumber(passo?.distanciaSegmentoKm)}</Table.Td>
                                            <Table.Td>{formatNumber(passo?.distanciaAcumuladaKm)}</Table.Td>
                                            <Table.Td>{formatNumber(passo?.tempoSegmentoHoras)}</Table.Td>
                                            <Table.Td>{formatNumber(passo?.tempoAcumuladoHoras)}</Table.Td>
                                            <Table.Td>{formatNumber(passo?.cargaRestantePercentual)}</Table.Td>
                                          </Table.Tr>
                                        ))
                                      ) : (
                                        <Table.Tr>
                                          <Table.Td colSpan={6}>
                                            Nenhum passo de bateria informado.
                                          </Table.Td>
                                        </Table.Tr>
                                      )}
                                    </Table.Tbody>
                                  </Table>
                                </ScrollArea>
                              </Stack>
                            ) : (
                              <Alert variant="light" color="yellow" icon={<IconAlertCircle size={16} />}>
                                Simulação de bateria não disponível para esta viagem.
                              </Alert>
                            )}
                          </Stack>
                        </Card>
                      );
                    })}
                  </Stack>
                </Stack>
              </Card>
            )}

            <Card withBorder radius="md" shadow="sm">
              <Group justify="space-between" mb="sm">
                <Title order={3}>{tituloMapa}</Title>
                <Badge variant="light" color="gray">
                  Visualização de rota
                </Badge>
              </Group>
              <Paper withBorder radius="md" p="sm">
                <Box
                  component="canvas"
                  ref={canvasRef}
                  width={600}
                  height={500}
                  style={{ width: "100%", maxWidth: "100%", borderRadius: 8, background: "white" }}
                />
              </Paper>
            </Card>
          </Stack>
        </Container>
      </AppShell.Main>
    </AppShell>
  );
}



package org.aiacon.simuladordemobilidadeurbana.visualization;

import org.aiacon.simuladordemobilidadeurbana.model.Edge;
import org.aiacon.simuladordemobilidadeurbana.model.Graph;
import org.aiacon.simuladordemobilidadeurbana.model.Node;
import org.aiacon.simuladordemobilidadeurbana.model.TrafficLight;
import org.aiacon.simuladordemobilidadeurbana.model.Vehicle;
import org.aiacon.simuladordemobilidadeurbana.model.CustomLinkedList;
import org.aiacon.simuladordemobilidadeurbana.model.LightPhase;
import org.aiacon.simuladordemobilidadeurbana.simulation.Simulator;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Point2D;
import javafx.scene.Scene;
import javafx.scene.Group;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import org.aiacon.simuladordemobilidadeurbana.simulation.Statistics;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

/**
 * Classe responsável por visualizar o grafo e a simulação usando JavaFX.
 * Ela estende {@code javafx.application.Application} para gerenciar a interface gráfica
 * e a atualização dinâmica dos elementos da simulação, como veículos e semáforos.
 */
public class Visualizer extends Application {

    private static final double LARGURA_TELA = 1000;
    private static final double ALTURA_TELA = 750;
    private static final double MARGEM_TELA = 50;
    private static final double ANGULO_ROTACAO_GRAUS = 0;

    private double minLat, maxLat, minLon, maxLon;
    private double centroLat, centroLon;
    private double escalaX, escalaY;
    private boolean transformacaoCalculada = false;
    private Text statsText;
    private Graph graph;
    private Simulator simulator;
    private Text congestionText;

    private Pane pane;
    private Map<String, Group> trafficLightNodeVisuals;
    private Map<String, Circle> regularNodeVisuals;
    private Map<String, ImageView> vehicleVisuals;

    private Image carroImage;

    private volatile boolean running = true;

    /**
     * Classe interna para representar o visual de um semáforo, incluindo seus indicadores.
     */
    private static class TrafficLightDisplay {
        Rectangle nsIndicator; // Indicador para Norte-Sul
        Rectangle ewIndicator; // Indicador para Leste-Oeste

        /**
         * Construtor para {@code TrafficLightDisplay}.
         * @param ns O retângulo que representa o indicador Norte-Sul.
         * @param ew O retângulo que representa o indicador Leste-Oeste.
         */
        TrafficLightDisplay(Rectangle ns, Rectangle ew) {
            this.nsIndicator = ns;
            this.ewIndicator = ew;
        }
    }
    private Map<String, TrafficLightDisplay> lightVisualsMap;

    /**
     * Construtor para a classe {@code Visualizer}.
     * @param graph O objeto {@code Graph} que representa a estrutura da cidade.
     * @param simulator O objeto {@code Simulator} que contém o estado atual da simulação.
     */
    public Visualizer(Graph graph, Simulator simulator) {
        this.graph = graph;
        this.simulator = simulator;
        this.trafficLightNodeVisuals = new HashMap<>();
        this.regularNodeVisuals = new HashMap<>();
        this.lightVisualsMap = new HashMap<>();
        this.vehicleVisuals = new HashMap<>();
    }

    /**
     * Construtor padrão para a classe {@code Visualizer}.
     * Este construtor é principalmente para uso interno do JavaFX, mas requer que
     * {@code setGraph} e {@code setSimulator} sejam chamados antes de {@code start}.
     */
    public Visualizer() {
        this.trafficLightNodeVisuals = new HashMap<>();
        this.regularNodeVisuals = new HashMap<>();
        this.lightVisualsMap = new HashMap<>();
        this.vehicleVisuals = new HashMap<>();
    }

    /**
     * O método de entrada principal para a aplicação JavaFX do visualizador.
     * Este método é chamado após o sistema estar pronto para a aplicação.
     *
     * @param primaryStage O palco principal para esta aplicação, onde a cena da aplicação pode ser definida.
     */
    @Override
    public void start(Stage primaryStage) {
        if (this.graph == null || this.simulator == null) {
            System.err.println("Visualizer: Graph ou Simulator não inicializado. Encerrando UI.");
            Pane errorPane = new Pane(new Text("Erro Crítico: Dados da simulação não carregados para o Visualizer."));
            Scene errorScene = new Scene(errorPane, 450, 100);
            primaryStage.setTitle("Erro de Inicialização do Visualizer");
            primaryStage.setScene(errorScene);
            primaryStage.show();
            return;
        }

        this.pane = new Pane();
        pane.setStyle("-fx-background-color: #D3D3D3;");

        statsText = new Text(10, ALTURA_TELA - 10, "Estatísticas: Carregando...");
        pane.getChildren().add(statsText);
        congestionText = new Text(10, ALTURA_TELA - 10, "Congestionamento: N/A");
        pane.getChildren().add(congestionText);

        this.carroImage = new Image(getClass().getResourceAsStream("/carros/carroofc.png"));

        calcularParametrosDeTransformacao();
        desenharElementosEstaticos();

        Scene scene = new Scene(pane, LARGURA_TELA, ALTURA_TELA);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Simulador de Mobilidade Urbana AIACON");
        primaryStage.show();

        primaryStage.setOnCloseRequest(event -> {
            running = false;
            System.out.println("Visualizer: Solicitação de fechamento recebida.");
            if (simulator != null) {
                simulator.stopSimulation();
            }
        });

        Thread updateThread = new Thread(() -> {
            while (running) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    if (!running) break;
                    System.err.println("Visualizer: update thread interrompida: " + e.getMessage());
                    Thread.currentThread().interrupt();
                }
                if (running) {
                    Platform.runLater(this::atualizarElementosDinamicos);
                }
            }
            System.out.println("Visualizer: Update thread finalizada.");
        });
        updateThread.setDaemon(true);
        updateThread.setName("VisualizerUpdateThread");
        updateThread.start();
    }

    /**
     * Calcula os parâmetros de transformação para mapear coordenadas geográficas
     * para coordenadas de tela.
     */
    private void calcularParametrosDeTransformacao() {
        if (graph.getNodes() == null || graph.getNodes().isEmpty()) {
            System.err.println("Visualizer: Nenhum nó no grafo para calcular transformação. Usando defaults.");
            minLat = -5.12; maxLat = -5.06; minLon = -42.84; maxLon = -42.78;
        } else {
            minLat = Double.MAX_VALUE; maxLat = -Double.MAX_VALUE;
            minLon = Double.MAX_VALUE; maxLon = -Double.MAX_VALUE;
            for (Node node : graph.getNodes()) {
                if (node == null) continue;
                if (node.getLatitude() < minLat) minLat = node.getLatitude();
                if (node.getLatitude() > maxLat) maxLat = node.getLatitude();
                if (node.getLongitude() < minLon) minLon = node.getLongitude();
                if (node.getLongitude() > maxLon) maxLon = node.getLongitude();
            }
        }
        if (Math.abs(maxLat - minLat) < 0.00001) { maxLat = minLat + 0.001; minLat = minLat - 0.001;}
        if (Math.abs(maxLon - minLon) < 0.00001) { maxLon = minLon + 0.001; minLon = minLon - 0.001;}

        centroLat = (minLat + maxLat) / 2.0;
        centroLon = (minLon + maxLon) / 2.0;
        double deltaLonGeo = maxLon - minLon;
        double deltaLatGeo = maxLat - minLat;
        double larguraDesenhoUtil = LARGURA_TELA - 2 * MARGEM_TELA;
        double alturaDesenhoUtil = ALTURA_TELA - 2 * MARGEM_TELA;
        double escalaPotencialX = (deltaLonGeo == 0) ? larguraDesenhoUtil : larguraDesenhoUtil / deltaLonGeo;
        double escalaPotencialY = (deltaLatGeo == 0) ? alturaDesenhoUtil : alturaDesenhoUtil / deltaLatGeo;
        escalaX = Math.min(escalaPotencialX, escalaPotencialY);
        escalaY = escalaX;
        transformacaoCalculada = true;
    }

    /**
     * Transforma coordenadas geográficas (latitude, longitude) em coordenadas de tela (X, Y).
     * @param latGeo A latitude geográfica.
     * @param lonGeo A longitude geográfica.
     * @return Um objeto {@code Point2D} contendo as coordenadas de tela.
     */
    private Point2D transformarCoordenadas(double latGeo, double lonGeo) {
        if (!transformacaoCalculada) {
            calcularParametrosDeTransformacao();
            if(!transformacaoCalculada) {
                return new Point2D(LARGURA_TELA / 2, ALTURA_TELA / 2);
            }
        }
        double lonRel = lonGeo - centroLon;
        double latRel = latGeo - centroLat;
        double xTela = lonRel * escalaX + LARGURA_TELA / 2;
        double yTela = latRel * (-escalaY) + ALTURA_TELA / 2;
        return new Point2D(xTela, yTela);
    }

    /**
     * Desenha os elementos estáticos do grafo, como nós e arestas, na tela.
     */
    private void desenharElementosEstaticos() {
        pane.getChildren().clear();
        trafficLightNodeVisuals.clear();
        regularNodeVisuals.clear();
        lightVisualsMap.clear();

        if (graph.getEdges() != null) {
            for (Edge edge : graph.getEdges()) {
                if (edge == null) continue;
                Node sourceNode = graph.getNode(edge.getSource());
                Node targetNode = graph.getNode(edge.getDestination());
                if (sourceNode != null && targetNode != null) {
                    Point2D p1 = transformarCoordenadas(sourceNode.getLatitude(), sourceNode.getLongitude());
                    Point2D p2 = transformarCoordenadas(targetNode.getLatitude(), targetNode.getLongitude());
                    Line line = new Line(p1.getX(), p1.getY(), p2.getX(), p2.getY());
                    line.setStroke(Color.BLACK);
                    line.setStrokeWidth(5.0);
                    pane.getChildren().add(line);

                    Line dashedLine = new Line(p1.getX(), p1.getY(), p2.getX(), p2.getY());
                    dashedLine.setStroke(Color.YELLOW);
                    dashedLine.setStrokeWidth(0.5);
                    dashedLine.getStrokeDashArray().addAll(10.0, 10.0);
                    dashedLine.setStrokeLineCap(javafx.scene.shape.StrokeLineCap.ROUND);

                    pane.getChildren().add(dashedLine);
                }
            }
        }

        if (graph.getNodes() != null) {
            for (Node node : graph.getNodes()) {
                if (node == null) continue;
                Point2D p = transformarCoordenadas(node.getLatitude(), node.getLongitude());
                TrafficLight tl = simulator.getTrafficLight(node.getId());

                if (tl != null) {
                    Group trafficLightGroup = new Group();

                    Circle baseCircle = new Circle(p.getX(), p.getY(), 5, Color.DARKSLATEGRAY);
                    baseCircle.setStroke(Color.BLACK);
                    baseCircle.setStrokeWidth(0.5);
                    trafficLightGroup.getChildren().add(baseCircle);

                    Rectangle nsIndicator = new Rectangle(p.getX() - 2, p.getY() - 8, 4, 16);
                    nsIndicator.setFill(Color.GRAY);
                    nsIndicator.setStroke(Color.BLACK);
                    nsIndicator.setStrokeWidth(0.4);

                    Rectangle ewIndicator = new Rectangle(p.getX() - 8, p.getY() - 2, 16, 4);
                    ewIndicator.setFill(Color.GRAY);
                    ewIndicator.setStroke(Color.BLACK);
                    ewIndicator.setStrokeWidth(0.4);

                    trafficLightGroup.getChildren().addAll(nsIndicator, ewIndicator);
                    pane.getChildren().add(trafficLightGroup);

                    lightVisualsMap.put(node.getId(), new TrafficLightDisplay(nsIndicator, ewIndicator));
                }
            }
        }
    }

    /**
     * Atualiza os elementos dinâmicos na tela, como cores dos semáforos,
     * posições dos veículos e estatísticas da simulação.
     */
    private void atualizarElementosDinamicos() {
        if (pane == null || graph == null || simulator == null || !transformacaoCalculada) return;

        // 1. Atualizar Cores dos Semáforos
        if (graph.getTrafficLights() != null) {
            for (TrafficLight tl : graph.getTrafficLights()) {
                if (tl == null) continue;
                TrafficLightDisplay display = lightVisualsMap.get(tl.getNodeId());
                if (display != null) {
                    LightPhase phase = tl.getCurrentPhase();
                    Color nsColor = Color.DARKRED;
                    Color ewColor = Color.DARKRED;

                    if (phase != null) {
                        switch (phase) {
                            case NS_GREEN_EW_RED:
                                nsColor = Color.LIMEGREEN;
                                ewColor = Color.INDIANRED;
                                break;
                            case NS_YELLOW_EW_RED:
                                nsColor = Color.GOLD;
                                ewColor = Color.INDIANRED;
                                break;
                            case NS_RED_EW_GREEN:
                                nsColor = Color.INDIANRED;
                                ewColor = Color.LIMEGREEN;
                                break;
                            case NS_RED_EW_YELLOW:
                                nsColor = Color.INDIANRED;
                                ewColor = Color.GOLD;
                                break;
                        }
                    }
                    display.nsIndicator.setFill(nsColor);
                    display.ewIndicator.setFill(ewColor);
                }
            }
        }

        // 2. Atualizar Posições dos Veículos
        CustomLinkedList<Vehicle> currentVehicles = simulator.getVehicles();
        if (currentVehicles == null) return;

        Map<String, ImageView> newVehicleVisualsMap = new HashMap<>();
        List<javafx.scene.Node> childrenToAdd = new ArrayList<>();
        List<javafx.scene.Node> childrenToRemove = new ArrayList<>();

        for (Vehicle vehicle : currentVehicles) {
            if (vehicle == null || vehicle.getCurrentNode() == null) continue;

            Point2D vehiclePos;
            Node currentNodeObject = graph.getNode(vehicle.getCurrentNode());
            if (currentNodeObject == null) continue;

            if (vehicle.getPosition() == 0.0 || vehicle.getRoute() == null || vehicle.getRoute().isEmpty()) {
                vehiclePos = transformarCoordenadas(currentNodeObject.getLatitude(), currentNodeObject.getLongitude());
            } else {
                String nextNodeId = getNextNodeIdInRoute(vehicle, currentNodeObject.getId());
                if (nextNodeId == null) {
                    vehiclePos = transformarCoordenadas(currentNodeObject.getLatitude(), currentNodeObject.getLongitude());
                } else {
                    Node nextNodeObject = graph.getNode(nextNodeId);
                    if (nextNodeObject == null) {
                        vehiclePos = transformarCoordenadas(currentNodeObject.getLatitude(), currentNodeObject.getLongitude());
                    } else {
                        Point2D startScreenPos = transformarCoordenadas(currentNodeObject.getLatitude(), currentNodeObject.getLongitude());
                        Point2D endScreenPos = transformarCoordenadas(nextNodeObject.getLatitude(), nextNodeObject.getLongitude());
                        double interpolatedX = startScreenPos.getX() + vehicle.getPosition() * (endScreenPos.getX() - startScreenPos.getX());
                        double interpolatedY = startScreenPos.getY() + vehicle.getPosition() * (endScreenPos.getY() - startScreenPos.getY());
                        vehiclePos = new Point2D(interpolatedX, interpolatedY);
                    }
                }
            }

            ImageView vehicleImageView = vehicleVisuals.get(vehicle.getId());
            if (vehicleImageView == null) {
                vehicleImageView = new ImageView(carroImage);
                vehicleImageView.setFitWidth(14);
                vehicleImageView.setFitHeight(14);
                vehicleImageView.setPreserveRatio(true);
                childrenToAdd.add(vehicleImageView);
            }
            vehicleImageView.setX(vehiclePos.getX() - vehicleImageView.getFitWidth() / 2);
            vehicleImageView.setY(vehiclePos.getY() - vehicleImageView.getFitHeight() / 2);
            newVehicleVisualsMap.put(vehicle.getId(), vehicleImageView);

        }

        for (String existingId : vehicleVisuals.keySet()) {
            if (!newVehicleVisualsMap.containsKey(existingId)) {
                childrenToRemove.add(vehicleVisuals.get(existingId));
            }
        }

        pane.getChildren().removeAll(childrenToRemove);
        pane.getChildren().addAll(childrenToAdd);
        vehicleVisuals = newVehicleVisualsMap;

        // 3. Atualizar Texto de Estatísticas
        if (simulator != null && simulator.getStats() != null && statsText != null) {
            Statistics currentStats = simulator.getStats();
            String statsDisplay = String.format(
                    "Tempo: %.0fs | Veículos Ativos: %d | Congest.: %.0f\n" +
                            "Chegadas: %d | T Médio Viagem: %.1fs | T Médio Espera: %.1fs\n" +
                            "Comb. Total: %.2f L | Comb. Médio/Veículo: %.3f L",
                    currentStats.getCurrentTime(),
                    (simulator.getVehicles() != null ? simulator.getVehicles().size() : 0),
                    currentStats.getCurrentCongestionIndex(),
                    currentStats.getVehiclesArrived(),
                    currentStats.getAverageTravelTime(),
                    currentStats.getAverageWaitTime(),
                    currentStats.getTotalFuelConsumed(),
                    currentStats.getAverageFuelConsumptionPerVehicle()
            );
            statsText.setText(statsDisplay);
        }
    }

    /**
     * Retorna o ID do próximo nó na rota do veículo, dado o ID do nó atual do veículo.
     * @param vehicle O veículo cuja rota está sendo consultada.
     * @param currentVehicleNodeId O ID do nó atual do veículo.
     * @return O ID do próximo nó na rota, ou {@code null} se não houver um próximo nó.
     */
    private String getNextNodeIdInRoute(Vehicle vehicle, String currentVehicleNodeId) {
        CustomLinkedList<String> route = vehicle.getRoute();
        if (route == null || route.isEmpty()) return null;

        int currentIndex = route.indexOf(currentVehicleNodeId);
        if (currentIndex != -1 && currentIndex + 1 < route.size()) {
            return route.get(currentIndex + 1);
        }
        return null;
    }
}
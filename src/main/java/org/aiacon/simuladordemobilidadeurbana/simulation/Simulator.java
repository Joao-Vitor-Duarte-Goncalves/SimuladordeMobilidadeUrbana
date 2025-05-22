package org.aiacon.simuladordemobilidadeurbana.simulation;

import org.aiacon.simuladordemobilidadeurbana.model.*;
import java.util.HashMap;
import java.util.Map;

/**
 * A classe {@code Simulator} é o coração da simulação de mobilidade urbana.
 * Ela gerencia o fluxo de tempo, a geração e o movimento de veículos, a atualização
 * dos semáforos e a coleta de estatísticas. Implementa {@code Runnable} para
 * executar a simulação em uma thread separada.
 */
public class Simulator implements Runnable {
    private Graph graph;
    private Configuration config;
    private CustomLinkedList<Vehicle> vehicles;
    private Statistics stats;
    private VehicleGenerator generator;
    private double time;
    private volatile boolean running = true;
    private boolean generationStopped = false;

    /**
     * Constrói uma nova instância do {@code Simulator}.
     *
     * @param graph O objeto {@code Graph} que representa a malha viária da cidade.
     * @param config O objeto {@code Configuration} que contém os parâmetros da simulação.
     * @throws IllegalStateException se o grafo estiver vazio ou não estiver totalmente conectado.
     */
    public Simulator(Graph graph, Configuration config) {
        this.graph = graph;
        this.config = config;
        this.vehicles = new CustomLinkedList<>();
        this.stats = new Statistics();
        this.generator = new VehicleGenerator(graph, config.getVehicleGenerationRate());
        this.time = 0.0;

        validateGraph();
        if (!isGraphConnected()) {
            throw new IllegalStateException("Erro: O grafo não está totalmente conectado. Nem todos os nós podem ser alcançados.");
        }
    }

    /**
     * O método principal de execução da simulação.
     * Este método contém o loop de simulação que avança o tempo, gera veículos,
     * atualiza semáforos e move veículos. A simulação continua até que a duração
     * configurada seja atingida ou a simulação seja explicitamente parada.
     */
    @Override
    public void run() {
        System.out.println("SIMULATOR_RUN: Iniciando loop de simulação. Duração: " + config.getSimulationDuration() + "s");
        double deltaTime = 1.0;

        while (running && time < config.getSimulationDuration()) {
            time += deltaTime;
            stats.updateCurrentTime(time);

            if (Thread.currentThread().isInterrupted()) {
                System.out.println("SIMULATOR_RUN: Thread de simulação interrompida, encerrando loop.");
                this.running = false;
                break;
            }

            if (!generationStopped && time > config.getVehicleGenerationStopTime()) {
                System.out.println("SIMULATOR_RUN: Tempo limite de geração de veículos (" + String.format("%.2f", config.getVehicleGenerationStopTime()) + "s) atingido. Nenhum veículo novo será gerado.");
                generationStopped = true;
            }

            if (!generationStopped) {
                generateVehicles(deltaTime);
            }

            updateTrafficLights(deltaTime);
            moveVehicles(deltaTime);
            logSimulationState();
            stats.calculateCurrentCongestion(this.vehicles, this.graph);

            if (running) {
                sleep(deltaTime);
            }
        }
        System.out.println("SIMULATOR_RUN: Loop de simulação terminado. Tempo final: " + String.format("%.2f", time));
        stats.printSummary();
    }

    /**
     * Sinaliza para a simulação parar sua execução.
     */
    public void stopSimulation() {
        System.out.println("SIMULATOR_STOPSIMULATION: Sinalizando para parar a simulação.");
        this.running = false;
    }

    /**
     * Valida a integridade do grafo, verificando se não é nulo ou vazio.
     * @throws IllegalStateException se o grafo estiver vazio ou não tiver sido carregado corretamente.
     */
    private void validateGraph() {
        if (graph == null || graph.getNodes() == null || graph.getNodes().isEmpty()) {
            throw new IllegalStateException("Erro: O grafo está vazio ou não foi carregado corretamente.");
        }
        System.out.println("Grafo validado com sucesso! Nós carregados: " + graph.getNodes().size());
    }

    /**
     * Verifica se o grafo está totalmente conectado usando uma busca em largura (BFS).
     * @return {@code true} se o grafo estiver conectado, {@code false} caso contrário.
     */
    private boolean isGraphConnected() {
        if (graph == null || graph.getNodes() == null || graph.getNodes().isEmpty()) {
            System.out.println("BFS: Grafo nulo ou sem nós.");
            return false;
        }
        CustomLinkedList<String> visited = new CustomLinkedList<>();
        CustomLinkedList<String> queue = new CustomLinkedList<>();

        Node startNode = graph.getNodes().get(0);
        if (startNode == null) {
            System.out.println("BFS: Primeiro nó do grafo é nulo!");
            return false;
        }

        queue.add(startNode.getId());
        visited.add(startNode.getId());

        int iterations = 0;
        int maxIterations = graph.getNodes().size();

        while (!queue.isEmpty() && iterations < maxIterations) {
            iterations++;
            String currentNodeId = queue.removeFirst();
            Node currentNodeObject = graph.getNode(currentNodeId);

            if (currentNodeObject != null && currentNodeObject.getEdges() != null) {
                for (Edge edge : currentNodeObject.getEdges()) {
                    if (edge == null) continue;
                    String neighborId = edge.getDestination();
                    if (neighborId != null && !neighborId.isEmpty() && !visited.contains(neighborId)) {
                        visited.add(neighborId);
                        queue.add(neighborId);
                    }
                }
            }
        }
        boolean connected = visited.size() == graph.getNodes().size();
        System.out.println("BFS RESULTADO: Nós visitados: " + visited.size() + " de " + graph.getNodes().size() + ". Grafo conectado: " + connected);
        return connected;
    }

    /**
     * Gera novos veículos com base na taxa de geração e no tempo decorrido.
     * @param deltaTime O incremento de tempo da simulação.
     */
    private void generateVehicles(double deltaTime) {
        double numExpectedVehicles = deltaTime * config.getVehicleGenerationRate();
        int numToGenerate = (int) numExpectedVehicles;
        if (Math.random() < (numExpectedVehicles - numToGenerate)) {
            numToGenerate++;
        }

        for (int i = 0; i < numToGenerate; i++) {
            int vehicleId = stats.getTotalVehiclesGenerated() + 1;
            Vehicle vehicle = generator.generateVehicle(vehicleId);

            if (vehicle != null) {
                vehicles.add(vehicle);
                stats.vehicleGenerated();
            }
        }
    }

    /**
     * Atualiza o estado de todos os semáforos no grafo.
     * @param deltaTime O incremento de tempo da simulação.
     */
    private void updateTrafficLights(double deltaTime) {
        if (graph.getTrafficLights() == null) return;
        for (TrafficLight tl : graph.getTrafficLights()) {
            if (tl != null) {
                tl.update(deltaTime, config.isPeakHour());
            }
        }
    }

    /**
     * Move todos os veículos ativos no grafo e remove os veículos que chegaram ao destino.
     * @param deltaTime O incremento de tempo da simulação.
     */
    private void moveVehicles(double deltaTime) {
        CustomLinkedList<Vehicle> vehiclesStillActive = new CustomLinkedList<>();
        for (Vehicle vehicle : vehicles) {
            if (vehicle == null) continue;
            updateVehicle(vehicle, deltaTime);

            if (running && vehicle.getCurrentNode().equals(vehicle.getDestination()) && vehicle.getPosition() == 0.0) {
                stats.vehicleArrived(vehicle.getTravelTime(), vehicle.getWaitTime(), vehicle.getFuelConsumed());
            } else if (running) {
                vehiclesStillActive.add(vehicle);
            }
        }
        if (running) {
            vehicles = vehiclesStillActive;
        }
    }

    /**
     * Determina a direção cardeal de um nó de origem para um nó de destino.
     * @param fromNodeId O ID do nó de origem.
     * @param toNodeId O ID do nó de destino.
     * @return Uma string representando a direção ("north", "south", "east", "west") ou "unknown".
     */
    private String determineCardinalDirection(String fromNodeId, String toNodeId) {
        if (fromNodeId == null || toNodeId == null || fromNodeId.equals(toNodeId)) {
            return "unknown";
        }
        Node fromNode = graph.getNode(fromNodeId);
        Node toNode = graph.getNode(toNodeId);
        if (fromNode == null || toNode == null) {
            return "unknown";
        }

        double deltaLat = toNode.getLatitude() - fromNode.getLatitude();
        double deltaLon = toNode.getLongitude() - fromNode.getLongitude();
        double absDeltaLat = Math.abs(deltaLat);
        double absDeltaLon = Math.abs(deltaLon);
        double threshold = 0.000001;

        if (absDeltaLat > absDeltaLon + threshold) {
            return (deltaLat > 0) ? "north" : "south";
        } else if (absDeltaLon > absDeltaLat + threshold) {
            return (deltaLon > 0) ? "east" : "west";
        }
        return "unknown";
    }

    /**
     * Atualiza a posição de um veículo, sua contagem de tempo de viagem/espera
     * e consumo de combustível. Lida com a lógica de travessia de semáforos.
     * @param vehicle O veículo a ser atualizado.
     * @param deltaTime O incremento de tempo da simulação.
     */
    private void updateVehicle(Vehicle vehicle, double deltaTime) {
        if (!running) return;

        vehicle.incrementTravelTime(deltaTime);
        boolean vehicleIsMoving = false;

        if (vehicle.getPosition() == 0.0) {
            String currentVehicleNodeId = vehicle.getCurrentNode();
            String previousNodeIdInRoute = getPreviousNodeInRoute(vehicle);

            TrafficLight tl = getTrafficLight(currentVehicleNodeId);

            String nextNodeIdInRoute = getNextNodeInRoute(vehicle);

            if (nextNodeIdInRoute == null) {
                if(!currentVehicleNodeId.equals(vehicle.getDestination())){
                    System.err.println("UPDATE_VEHICLE: Veículo " + vehicle.getId() + " em " + currentVehicleNodeId + " sem próximo nó, mas não está no destino " + vehicle.getDestination() + ". Rota: " + vehicle.getRoute());
                }
                vehicle.incrementFuelConsumption(vehicle.getFuelConsumptionRateIdle() * deltaTime);
                return;
            }

            if (tl != null) {
                String approachToLightDirection = "unknown";
                if (previousNodeIdInRoute != null) {
                    approachToLightDirection = determineCardinalDirection(previousNodeIdInRoute, currentVehicleNodeId);
                } else {
                    approachToLightDirection = determineCardinalDirection(currentVehicleNodeId, nextNodeIdInRoute);
                }

                String lightState = tl.getLightStateForApproach(approachToLightDirection);

                if (!"green".equalsIgnoreCase(lightState)) {
                    vehicle.incrementWaitTime(deltaTime);
                    vehicle.incrementFuelConsumption(vehicle.getFuelConsumptionRateIdle() * deltaTime);
                    tl.addVehicleToQueue(approachToLightDirection, vehicle);
                    return;
                }
            }

            Edge edgeToTraverse = findEdge(currentVehicleNodeId, nextNodeIdInRoute);
            if (edgeToTraverse == null) {
                System.err.println("UPDATE_VEHICLE (EM NÓ): Veículo " + vehicle.getId() + " no nó " + currentVehicleNodeId +
                        ". Não foi possível encontrar a aresta para o PRÓXIMO nó da rota: " + nextNodeIdInRoute +
                        ". Rota: " + (vehicle.getRoute() != null ? vehicle.getRoute().toString() : "NULA"));
                this.running = false;
                return;
            }
            double edgeTravelTime = edgeToTraverse.getTravelTime();
            if (edgeTravelTime <= 0) edgeTravelTime = deltaTime;

            vehicle.setPosition(deltaTime / edgeTravelTime);
            vehicleIsMoving = true;

            if (vehicle.getPosition() >= 1.0) {
                vehicle.setCurrentNode(nextNodeIdInRoute);
                vehicle.setPosition(0.0);
                vehicleIsMoving = false;
            }

        } else {
            vehicleIsMoving = true;
            String sourceNodeOfCurrentSegment = vehicle.getCurrentNode();
            String targetNodeOfCurrentSegment = getNextNodeInRoute(vehicle);

            if (targetNodeOfCurrentSegment == null) {
                System.err.println("UPDATE_VEHICLE (EM ARESTA): Veículo " + vehicle.getId() + " na aresta de " + sourceNodeOfCurrentSegment +
                        " mas getNextNodeInRoute é nulo. Posição: " + String.format("%.2f",vehicle.getPosition()));
                vehicle.setPosition(0.0);
                vehicleIsMoving = false;
                if (!sourceNodeOfCurrentSegment.equals(vehicle.getDestination())) {
                    System.err.println("    Veículo " + vehicle.getId() + " parou em " + sourceNodeOfCurrentSegment + " pois a rota terminou inesperadamente.");
                }
                if (!vehicle.getCurrentNode().equals(vehicle.getDestination())) {
                    vehicle.incrementFuelConsumption(vehicle.getFuelConsumptionRateIdle() * deltaTime);
                }
                return;
            }

            Edge currentEdge = findEdge(sourceNodeOfCurrentSegment, targetNodeOfCurrentSegment);
            if (currentEdge == null) {
                System.err.println("UPDATE_VEHICLE (EM ARESTA): Veículo " + vehicle.getId() +
                        ". Não foi possível encontrar a aresta entre " + sourceNodeOfCurrentSegment + " e " + targetNodeOfCurrentSegment);
                this.running = false;
                return;
            }
            double edgeTravelTime = currentEdge.getTravelTime();
            if (edgeTravelTime <= 0) edgeTravelTime = deltaTime;

            vehicle.setPosition(vehicle.getPosition() + (deltaTime / edgeTravelTime));

            if (vehicle.getPosition() >= 1.0) {
                vehicle.setCurrentNode(targetNodeOfCurrentSegment);
                vehicle.setPosition(0.0);
                vehicleIsMoving = false;
            }
        }

        if (vehicleIsMoving) {
            vehicle.incrementFuelConsumption(vehicle.getFuelConsumptionRateMoving() * deltaTime);
        } else {
            if (!vehicle.getCurrentNode().equals(vehicle.getDestination()) || vehicle.getPosition() > 0) {
                vehicle.incrementFuelConsumption(vehicle.getFuelConsumptionRateIdle() * deltaTime);
            }
        }
    }

    /**
     * Encontra uma aresta entre dois nós especificados.
     * @param sourceNodeId O ID do nó de origem da aresta.
     * @param targetNodeId O ID do nó de destino da aresta.
     * @return O objeto {@code Edge} se encontrado, ou {@code null} caso contrário.
     */
    private Edge findEdge(String sourceNodeId, String targetNodeId) {
        if (sourceNodeId == null || targetNodeId == null) return null;
        Node sourceNode = graph.getNode(sourceNodeId);
        if (sourceNode == null || sourceNode.getEdges() == null) return null;
        for (Edge edge : sourceNode.getEdges()) {
            if (edge != null && edge.getDestination() != null && edge.getDestination().equals(targetNodeId)) {
                return edge;
            }
        }
        return null;
    }

    /**
     * Registra o estado atual da simulação no console.
     */
    private void logSimulationState() {
        System.out.println("Tempo: " + String.format("%.2f", time) + "s, Veículos: " + (vehicles != null ? vehicles.size() : 0) +
                ", Congestionamento: " + String.format("%.0f", stats.getCurrentCongestionIndex()));
    }

    /**
     * Pausa a thread de simulação por um curto período de tempo.
     * @param deltaTime O incremento de tempo da simulação (usado para calcular o tempo de sono).
     */
    private void sleep(double deltaTime) {
        try {
            Thread.sleep((long) (deltaTime * 100));
        } catch (InterruptedException e) {
            System.out.println("SIMULATOR_SLEEP: A thread foi interrompida durante o sleep.");
            this.running = false;
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Redireciona um veículo para uma rota alternativa se o seu caminho atual
     * estiver congestionado e houver uma alternativa melhor disponível.
     *
     * @param vehicle O veículo a ser potencialmente redirecionado.
     * @param trafficLightNodeId O ID do nó do semáforo onde o redirecionamento pode ocorrer.
     * @param graph O grafo da cidade.
     */
    private void redirectIfNeeded(Vehicle vehicle, String trafficLightNodeId, Graph graph) {
        if (vehicle == null || trafficLightNodeId == null || graph == null || config == null || config.getRedirectThreshold() <= 0) {
            return;
        }

        TrafficLight tl = getTrafficLight(trafficLightNodeId);
        if (tl == null) return;

        String nextNodeInOriginalRoute = getNextNodeInRoute(vehicle);
        if (nextNodeInOriginalRoute == null) return;

        String currentRouteOutgoingDirection = determineCardinalDirection(trafficLightNodeId, nextNodeInOriginalRoute);
        if ("unknown".equals(currentRouteOutgoingDirection)) return;

        Integer currentDirectionIndex = tl.getDirectionIndex(currentRouteOutgoingDirection);
        int[] queueSizes = tl.getAllQueueSizes();

        if (currentDirectionIndex != null && currentDirectionIndex >= 0 && currentDirectionIndex < queueSizes.length &&
                queueSizes[currentDirectionIndex] > config.getRedirectThreshold()) {

            System.out.println("REDIRECT_IF_NEEDED: Veículo " + vehicle.getId() + " no nó " + trafficLightNodeId +
                    ". Rota atual via " + currentRouteOutgoingDirection + " (nó " + nextNodeInOriginalRoute + ") congestionada (fila: " + queueSizes[currentDirectionIndex] + "). Procurando alternativa...");

            String bestAlternativeOutgoingDirection = null;
            int minQueueSizeForAlternative = queueSizes[currentDirectionIndex];
            Node bestAlternativeNextNode = null;
            String oppositeOfCurrentOutgoing = getOppositeDirection(currentRouteOutgoingDirection);

            String[] tryDirections = {"north", "east", "south", "west"};

            for (String potentialOutgoingDir : tryDirections) {
                if (potentialOutgoingDir.equalsIgnoreCase(currentRouteOutgoingDirection) ||
                        (oppositeOfCurrentOutgoing != null && potentialOutgoingDir.equalsIgnoreCase(oppositeOfCurrentOutgoing))) {
                    continue;
                }
                Node potentialNextNode = findNeighborInDirection(trafficLightNodeId, potentialOutgoingDir, graph);
                if (potentialNextNode != null && (getPreviousNodeInRoute(vehicle) == null || !potentialNextNode.getId().equals(getPreviousNodeInRoute(vehicle)))) {
                    Integer potentialDirIndex = tl.getDirectionIndex(potentialOutgoingDir);
                    if (potentialDirIndex != null && potentialDirIndex >= 0 && potentialDirIndex < queueSizes.length &&
                            queueSizes[potentialDirIndex] < minQueueSizeForAlternative) {
                        minQueueSizeForAlternative = queueSizes[potentialDirIndex];
                        bestAlternativeOutgoingDirection = potentialOutgoingDir;
                        bestAlternativeNextNode = potentialNextNode;
                    }
                }
            }

            if (bestAlternativeNextNode != null) {
                System.out.println("  -> Redirecionando Veículo " + vehicle.getId() + " para direção '" + bestAlternativeOutgoingDirection +
                        "' (nó: " + bestAlternativeNextNode.getId() + ") com fila: " + minQueueSizeForAlternative);

                CustomLinkedList<String> newRouteFromAlternative = Dijkstra.calculateRoute(graph, bestAlternativeNextNode.getId(), vehicle.getDestination());

                if (newRouteFromAlternative != null && !newRouteFromAlternative.isEmpty()) {
                    CustomLinkedList<String> finalNewRoute = new CustomLinkedList<>();
                    finalNewRoute.add(trafficLightNodeId);
                    finalNewRoute.add(bestAlternativeNextNode.getId());

                    boolean firstNodeSkipped = false;
                    for(String routeNode : newRouteFromAlternative) {
                        if (!firstNodeSkipped && routeNode.equals(bestAlternativeNextNode.getId())) {
                            firstNodeSkipped = true;
                            continue;
                        }
                        finalNewRoute.add(routeNode);
                    }
                    vehicle.setRoute(finalNewRoute);
                    System.out.println("  -> Nova rota para V" + vehicle.getId() + ": " + finalNewRoute.toString());
                } else {
                    System.out.println("  -> Não foi possível calcular rota alternativa para Veículo " + vehicle.getId() + " via " + bestAlternativeNextNode.getId());
                }
            } else {
                System.out.println("  -> Nenhuma direção alternativa viável encontrada para Veículo " + vehicle.getId());
            }
        }
    }

    /**
     * Retorna a direção cardeal oposta à direção fornecida.
     * @param direction A direção cardeal.
     * @return A direção oposta, ou "unknown" se a direção não for reconhecida.
     */
    private String getOppositeDirection(String direction) {
        if (direction == null) return "unknown";
        switch (direction.toLowerCase()) {
            case "north": return "south";
            case "south": return "north";
            case "east": return "west";
            case "west": return "east";
            default: return "unknown";
        }
    }

    /**
     * Retorna o objeto {@code TrafficLight} associado a um determinado ID de nó.
     * @param nodeId O ID do nó.
     * @return O objeto {@code TrafficLight} se encontrado, ou {@code null} caso contrário.
     */
    public TrafficLight getTrafficLight(String nodeId) {
        if (graph == null || graph.getTrafficLights() == null || nodeId == null) return null;
        for (TrafficLight tl : graph.getTrafficLights()) {
            if (tl != null && nodeId.equals(tl.getNodeId())) {
                return tl;
            }
        }
        return null;
    }

    /**
     * Retorna o ID do próximo nó na rota do veículo.
     * @param vehicle O veículo.
     * @return O ID do próximo nó, ou {@code null} se não houver um próximo nó.
     */
    private String getNextNodeInRoute(Vehicle vehicle) {
        if (vehicle == null || vehicle.getRoute() == null || vehicle.getRoute().isEmpty()) return null;
        String currentNode = vehicle.getCurrentNode();
        if (currentNode == null) return null;
        int currentIndex = vehicle.getRoute().indexOf(currentNode);
        if (currentIndex == -1 || currentIndex + 1 >= vehicle.getRoute().size()) return null;
        return vehicle.getRoute().get(currentIndex + 1);
    }

    /**
     * Retorna o ID do nó anterior na rota do veículo.
     * @param vehicle O veículo.
     * @return O ID do nó anterior, ou {@code null} se não houver um nó anterior.
     */
    private String getPreviousNodeInRoute(Vehicle vehicle) {
        if (vehicle == null || vehicle.getRoute() == null || vehicle.getRoute().isEmpty() || vehicle.getCurrentNode() == null) return null;
        String currentVehicleNodeId = vehicle.getCurrentNode();
        int currentIndex = vehicle.getRoute().indexOf(currentVehicleNodeId);
        if (currentIndex > 0) {
            return vehicle.getRoute().get(currentIndex - 1);
        }
        return null;
    }

    /**
     * Encontra um nó vizinho na direção cardeal especificada a partir de um nó de origem.
     * @param sourceNodeId O ID do nó de origem.
     * @param targetDirection A direção cardeal alvo ("north", "south", "east", "west").
     * @param graph O grafo da cidade.
     * @return O nó vizinho na direção especificada, ou {@code null} se nenhum for encontrado.
     */
    private Node findNeighborInDirection(String sourceNodeId, String targetDirection, Graph graph) {
        Node sourceNode = graph.getNode(sourceNodeId);
        if (sourceNode == null || sourceNode.getEdges() == null || targetDirection == null || targetDirection.equals("unknown")) {
            return null;
        }
        Node bestNeighbor = null;
        double bestScore = -Double.MAX_VALUE;

        for (Edge edge : sourceNode.getEdges()) {
            if (edge == null) continue;
            Node neighbor = graph.getNode(edge.getDestination());
            if (neighbor == null) continue;

            double deltaLat = neighbor.getLatitude() - sourceNode.getLatitude();
            double deltaLon = neighbor.getLongitude() - sourceNode.getLongitude();
            double score = 0.0;

            switch (targetDirection.toLowerCase()) {
                case "north":
                    if (deltaLat > 0) score = deltaLat - Math.abs(deltaLon);
                    break;
                case "south":
                    if (deltaLat < 0) score = -deltaLat - Math.abs(deltaLon);
                    break;
                case "east":
                    if (deltaLon > 0) score = deltaLon - Math.abs(deltaLat);
                    break;
                case "west":
                    if (deltaLon < 0) score = -deltaLon - Math.abs(deltaLat);
                    break;
                default: continue;
            }

            if (score > 0 && score > bestScore) {
                bestScore = score;
                bestNeighbor = neighbor;
            }
        }
        return bestNeighbor;
    }

    /**
     * Retorna a lista atual de veículos na simulação.
     * @return Uma {@code CustomLinkedList} de objetos {@code Vehicle}.
     */
    public CustomLinkedList<Vehicle> getVehicles() {
        return this.vehicles;
    }

    /**
     * Retorna o objeto {@code Statistics} que coleta e gerencia os dados da simulação.
     * @return O objeto {@code Statistics}.
     */
    public Statistics getStats() {
        return this.stats;
    }

    /**
     * Retorna o tempo atual da simulação.
     * @return O tempo atual da simulação em segundos.
     */
    public double getCurrentTime() { return this.time; }
}
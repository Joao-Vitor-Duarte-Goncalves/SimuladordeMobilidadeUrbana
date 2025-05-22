package org.aiacon.simuladordemobilidadeurbana.model;

/**
 * Representa um veículo na simulação de mobilidade urbana.
 * Cada veículo possui um ID único, uma origem, um destino, uma rota,
 * e rastreia seu tempo de viagem, tempo de espera, posição atual na aresta
 * e consumo de combustível.
 */
public class Vehicle {
    private String id;
    private String origin;
    private String destination;
    private CustomLinkedList<String> route;
    private String currentNode;
    private double travelTime;
    private double waitTime;
    private double position;
    public Vehicle next;
    private double fuelConsumed;
    private double fuelConsumptionRateMoving;
    private double fuelConsumptionRateIdle;

    /**
     * Constrói uma nova instância de {@code Vehicle}.
     *
     * @param id O identificador único do veículo.
     * @param origin O ID do nó de origem do veículo.
     * @param destination O ID do nó de destino do veículo.
     * @param route A rota calculada para o veículo, como uma lista de IDs de nós.
     */
    public Vehicle(String id, String origin, String destination, CustomLinkedList<String> route) {
        this.id = id;
        this.origin = origin;
        this.destination = destination;
        this.route = (route != null) ? route : new CustomLinkedList<>();
        this.currentNode = origin;
        this.travelTime = 0.0;
        this.waitTime = 0.0;
        this.position = 0.0;
        this.next = null;
        this.fuelConsumed = 0.0;
        this.fuelConsumptionRateMoving = 0.0005;
        this.fuelConsumptionRateIdle = 0.0002;
    }

    public String getId() {
        return id;
    }

    public String getOrigin() {
        return origin;
    }

    public String getDestination() {
        return destination;
    }

    public CustomLinkedList<String> getRoute() {
        return route;
    }

    /**
     * Define uma nova rota para o veículo.
     * Se a rota fornecida for nula, a rota do veículo será definida como uma lista vazia.
     * @param route A nova {@code CustomLinkedList} de IDs de nós para a rota.
     */
    public void setRoute(CustomLinkedList<String> route) {
        if (route == null) {
            System.err.println("Atribuição de rota nula para o veículo " + id);
            this.route = new CustomLinkedList<>();
        } else {
            this.route = route;
        }
    }

    public String getCurrentNode() {
        return currentNode;
    }

    public void setCurrentNode(String currentNode) {
        this.currentNode = currentNode;
    }

    public double getTravelTime() {
        return travelTime;
    }

    public void incrementTravelTime(double deltaTime) {
        this.travelTime += deltaTime;
    }

    public double getWaitTime() {
        return waitTime;
    }

    public void incrementWaitTime(double deltaTime) {
        this.waitTime += deltaTime;
    }

    public double getPosition() {
        return position;
    }

    public void setPosition(double position) {
        this.position = position;
    }

    public double getFuelConsumed() {
        return fuelConsumed;
    }

    public void incrementFuelConsumption(double consumption) {
        this.fuelConsumed += consumption;
    }

    public double getFuelConsumptionRateMoving() {
        return fuelConsumptionRateMoving;
    }

    public double getFuelConsumptionRateIdle() {
        return fuelConsumptionRateIdle;
    }
}
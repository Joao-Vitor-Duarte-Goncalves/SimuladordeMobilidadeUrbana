package org.aiacon.simuladordemobilidadeurbana.model;

/**
 * Representa uma aresta (segmento de via) na rede urbana.
 * Uma aresta conecta dois nós e possui propriedades como comprimento, tempo de viagem,
 * direção (mão única ou dupla), velocidade máxima e capacidade.
 */
public class Edge {
    private String id;
    private String source;
    private String target;
    private double length;
    private double travelTime;
    private boolean oneway;
    private double maxspeed;
    private int capacity;
    public Edge next;

    /**
     * Constrói uma nova instância de {@code Edge}.
     *
     * @param id O identificador único da aresta.
     * @param source O ID do nó de origem da aresta.
     * @param target O ID do nó de destino da aresta.
     * @param length O comprimento da aresta em metros.
     * @param travelTime O tempo de travessia esperado da aresta em segundos.
     * @param oneway {@code true} se a aresta for de mão única, {@code false} se for de mão dupla.
     * @param maxspeed A velocidade máxima permitida na aresta em km/h.
     * @param capacity A capacidade de fluxo da aresta em veículos.
     */
    public Edge(String id, String source, String target, double length, double travelTime,
                boolean oneway, double maxspeed, int capacity) {
        this.id = id;
        this.source = source;
        this.target = target;
        this.length = length;
        this.travelTime = travelTime;
        this.oneway = oneway;
        this.maxspeed = maxspeed;
        this.capacity = capacity;
        this.next = null;
    }

    public String getId() {
        return id;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public double getLength() {
        return length;
    }

    public void setLength(double length) {
        this.length = length;
    }

    public double getTravelTime() {
        return travelTime;
    }

    public void setTravelTime(double travelTime) {
        this.travelTime = travelTime;
    }

    public boolean isOneway() {
        return oneway;
    }

    public void setOneway(boolean oneway) {
        this.oneway = oneway;
    }

    public double getMaxspeed() {
        return maxspeed;
    }

    public void setMaxspeed(double maxspeed) {
        this.maxspeed = maxspeed;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public double getAverageSpeed() {
        return length / travelTime;
    }

    public boolean isBidirectional() {
        return !oneway;
    }

    public String getDestination() {
        return target;
    }
}
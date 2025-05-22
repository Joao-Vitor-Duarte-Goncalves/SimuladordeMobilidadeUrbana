package org.aiacon.simuladordemobilidadeurbana.model;

/**
 * Representa uma interseção ou ponto de interesse na rede urbana, como um nó em um grafo.
 * Cada nó possui um identificador único, coordenadas geográficas e pode indicar
 * a presença de um semáforo.
 */
public class Node {
    public String id;
    public double latitude;
    public double longitude;
    public boolean isTrafficLight;
    public Node next;

    private CustomLinkedList<Edge> edges;

    /**
     * Constrói uma nova instância de {@code Node}.
     *
     * @param id O identificador único do nó (geralmente do OpenStreetMap).
     * @param latitude A coordenada latitudinal do nó.
     * @param longitude A coordenada longitudinal do nó.
     * @param isTrafficLight Um booleano indicando se este nó possui um semáforo.
     */
    public Node(String id, double latitude, double longitude, boolean isTrafficLight) {
        this.id = id;
        this.latitude = latitude;
        this.longitude = longitude;
        this.isTrafficLight = isTrafficLight;
        this.next = null;
        this.edges = new CustomLinkedList<>();
    }

    /**
     * Retorna o identificador único do nó.
     */
    public String getId() {
        return id;
    }

    /**
     * Adiciona uma aresta que se conecta a este nó.
     */
    public void addEdge(Edge edge) {
        if (edge != null) {
            edges.add(edge);
        }
    }

    /**
     * Retorna a lista de todas as arestas que se originam ou chegam a este nó.
     */
    public CustomLinkedList<Edge> getEdges() {
        return edges;
    }

    /**
     * Define se este nó possui ou não um semáforo.
     */
    public void setIsTrafficLight(boolean isTrafficLight) {
        this.isTrafficLight = isTrafficLight;
    }

    /**
     * Retorna a coordenada latitudinal do nó.
     */
    public double getLatitude() {
        return latitude;
    }

    /**
     * Retorna a coordenada longitudinal do nó.
     */
    public double getLongitude() {
        return longitude;
    }
}
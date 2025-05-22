package org.aiacon.simuladordemobilidadeurbana.model;

import java.util.HashMap;
import java.util.Map;

/**
 * Representa o grafo da rede urbana, contendo nós, arestas e semáforos.
 * Fornece métodos para adicionar e recuperar esses elementos, além de funcionalidades de busca.
 */
public class Graph {
    private CustomLinkedList<Node> nodesList;
    private CustomLinkedList<Edge> edgesList;
    private CustomLinkedList<TrafficLight> trafficLightsList;

    private Map<String, Node> nodeMap;

    /**
     * Constrói uma nova instância de {@code Graph} com listas vazias de nós, arestas e semáforos,
     * e um mapa de nós vazio para busca rápida.
     */
    public Graph() {
        this.nodesList = new CustomLinkedList<>();
        this.edgesList = new CustomLinkedList<>();
        this.trafficLightsList = new CustomLinkedList<>();
        this.nodeMap = new HashMap<>();
    }

    /**
     * Adiciona um nó ao grafo. Se o nó for válido e seu ID não for duplicado,
     * ele será adicionado à lista de nós e ao mapa de nós para acesso rápido.
     *
     * @param node O objeto {@code Node} a ser adicionado.
     */
    public void addNode(Node node) {
        if (node != null && node.getId() != null && !node.getId().isEmpty()) {
            if (!this.nodeMap.containsKey(node.getId())) {
                this.nodesList.add(node);
                this.nodeMap.put(node.getId(), node);
            }
        } else {
            System.err.println("GRAPH_ADD_NODE: Tentativa de adicionar um nó nulo ou com ID inválido.");
        }
    }

    /**
     * Retorna a lista de todos os nós no grafo.
     * @return Uma {@code CustomLinkedList} de objetos {@code Node}.
     */
    public CustomLinkedList<Node> getNodes() {
        return this.nodesList;
    }

    /**
     * Busca e retorna um nó específico pelo seu ID.
     * Este método utiliza um mapa interno para um acesso eficiente (complexidade O(1) em média).
     *
     * @param nodeId O ID do nó a ser buscado.
     * @return O objeto {@code Node} correspondente ao ID, ou {@code null} se não for encontrado.
     */
    public Node getNode(String nodeId) {
        if (nodeId == null || nodeId.isEmpty()) {
            return null;
        }
        return this.nodeMap.get(nodeId);
    }

    /**
     * Adiciona uma aresta ao grafo.
     * @param edge O objeto {@code Edge} a ser adicionado.
     */
    public void addEdge(Edge edge) {
        if (edge != null) {
            this.edgesList.add(edge);
        } else {
            System.err.println("GRAPH_ADD_EDGE: Tentativa de adicionar uma aresta nula.");
        }
    }

    /**
     * Retorna a lista de todas as arestas no grafo.
     * @return Uma {@code CustomLinkedList} de objetos {@code Edge}.
     */
    public CustomLinkedList<Edge> getEdges() {
        return this.edgesList;
    }

    /**
     * Adiciona um semáforo ao grafo.
     * @param trafficLight O objeto {@code TrafficLight} a ser adicionado.
     */
    public void addTrafficLight(TrafficLight trafficLight) {
        if (trafficLight != null) {
            this.trafficLightsList.add(trafficLight);
        } else {
            System.err.println("GRAPH_ADD_TRAFFIC_LIGHT: Tentativa de adicionar um semáforo nulo.");
        }
    }

    /**
     * Retorna a lista de todos os semáforos no grafo.
     * @return Uma {@code CustomLinkedList} de objetos {@code TrafficLight}.
     */
    public CustomLinkedList<TrafficLight> getTrafficLights() {
        return this.trafficLightsList;
    }

    /**
     * Verifica se o grafo contém um nó com o ID especificado.
     *
     * @param nodeId O ID do nó a ser verificado.
     * @return {@code true} se o nó estiver presente, {@code false} caso contrário.
     */
    public boolean containsNode(String nodeId) {
        if (nodeId == null || nodeId.isEmpty()) {
            return false;
        }
        return this.nodeMap.containsKey(nodeId);
    }

    /**
     * Verifica se o grafo contém uma aresta que conecta o nó de origem ao nó de destino.
     *
     * @param sourceId O ID do nó de origem da aresta.
     * @param targetId O ID do nó de destino da aresta.
     * @return {@code true} se a aresta estiver presente, {@code false} caso contrário.
     */
    public boolean containsEdge(String sourceId, String targetId) {
        if (sourceId == null || targetId == null || sourceId.isEmpty() || targetId.isEmpty()) {
            return false;
        }
        for (Edge edge : this.edgesList) {
            if (edge == null) continue;
            if (edge.getSource().equals(sourceId) && edge.getDestination().equals(targetId)) {
                return true;
            }
        }
        return false;
    }
}
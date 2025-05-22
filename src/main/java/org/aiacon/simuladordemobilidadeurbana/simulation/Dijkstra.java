package org.aiacon.simuladordemobilidadeurbana.simulation;

import org.aiacon.simuladordemobilidadeurbana.model.Graph;
import org.aiacon.simuladordemobilidadeurbana.model.CustomLinkedList;
import org.aiacon.simuladordemobilidadeurbana.model.Node;
import org.aiacon.simuladordemobilidadeurbana.model.Edge;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 * A classe {@code Dijkstra} fornece um método estático para calcular
 * a rota mais curta entre dois nós em um grafo, utilizando o algoritmo de Dijkstra.
 * A "distância" neste contexto é baseada no tempo de viagem das arestas.
 */
public class Dijkstra {

    /**
     * Calcula a rota de menor tempo de viagem entre um nó de origem e um nó de destino
     * em um grafo. Utiliza o algoritmo de Dijkstra para encontrar o caminho mais eficiente.
     *
     * @param graph O grafo da cidade, contendo nós e arestas com tempos de viagem.
     * @param originId O ID do nó de origem.
     * @param destinationId O ID do nó de destino.
     * @return Uma {@code CustomLinkedList} de Strings representando a sequência de IDs de nós
     * na rota mais curta, incluindo a origem e o destino. Retorna uma lista vazia
     * se o grafo, IDs, ou nós forem nulos, ou se nenhuma rota puder ser encontrada.
     */
    public static CustomLinkedList<String> calculateRoute(Graph graph, String originId, String destinationId) {
        if (graph == null || originId == null || destinationId == null || graph.getNodes() == null || graph.getNodes().isEmpty()) {
            System.err.println("DIJKSTRA_ROUTE: Grafo nulo, IDs nulos, ou grafo sem nós.");
            return null;
        }

        Map<String, Integer> distances = new HashMap<>();
        Map<String, String> previousNodeIds = new HashMap<>();
        HashSet<String> visitedNodeIds = new HashSet<>();

        Map<String, Integer> unvisitedNodesWithDistance = new HashMap<>();

        Node originNode = graph.getNode(originId);
        Node destinationNode = graph.getNode(destinationId);

        if (originNode == null) {
            System.err.println("DIJKSTRA_ROUTE_ERROR: Nó de origem com ID '" + originId + "' não encontrado no grafo.");
            return null;
        }
        if (destinationNode == null) {
            System.err.println("DIJKSTRA_ROUTE_ERROR: Nó de destino com ID '" + destinationId + "' não encontrado no grafo.");
            return null;
        }

        for (Node node : graph.getNodes()) {
            if (node != null) {
                distances.put(node.getId(), Integer.MAX_VALUE);
                unvisitedNodesWithDistance.put(node.getId(), Integer.MAX_VALUE);
            }
        }

        distances.put(originId, 0);
        unvisitedNodesWithDistance.put(originId, 0);

        while (!unvisitedNodesWithDistance.isEmpty()) {
            String currentNodeId = getClosestUnvisitedNode(unvisitedNodesWithDistance, visitedNodeIds);

            if (currentNodeId == null || distances.get(currentNodeId) == Integer.MAX_VALUE) {
                break;
            }

            if (currentNodeId.equals(destinationId)) {
                break;
            }

            visitedNodeIds.add(currentNodeId);
            unvisitedNodesWithDistance.remove(currentNodeId);

            Node currentNodeObject = graph.getNode(currentNodeId);
            if (currentNodeObject == null || currentNodeObject.getEdges() == null) {
                continue;
            }

            for (Edge edge : currentNodeObject.getEdges()) {
                if (edge == null) continue;

                String neighborNodeId = edge.getTarget();
                if (visitedNodeIds.contains(neighborNodeId)) {
                    continue;
                }

                Node neighborNodeObject = graph.getNode(neighborNodeId);
                if (neighborNodeObject == null) {
                    continue;
                }

                double edgeTravelTime = edge.getTravelTime();
                if (edgeTravelTime <= 0 || edgeTravelTime == Double.POSITIVE_INFINITY) {
                    continue;
                }

                int newDist = distances.get(currentNodeId) + (int) (edgeTravelTime * 100.0);

                if (newDist < distances.getOrDefault(neighborNodeId, Integer.MAX_VALUE)) {
                    distances.put(neighborNodeId, newDist);
                    previousNodeIds.put(neighborNodeId, currentNodeId);
                    unvisitedNodesWithDistance.put(neighborNodeId, newDist);
                }
            }
        }

        if (!previousNodeIds.containsKey(destinationId) && !originId.equals(destinationId)) {
            System.err.println("DIJKSTRA_ROUTE_ERROR: Caminho para o destino " + destinationId + " não pôde ser construído (não está em 'previousNodeIds').");
            return new CustomLinkedList<>();
        }

        return buildPath(previousNodeIds, originId, destinationId);
    }

    /**
     * Encontra o nó não visitado com a menor distância na fila de prioridade simulada.
     *
     * @param unvisitedNodesWithDistance Um mapa de IDs de nós para suas distâncias atuais.
     * @param visitedNodeIds Um conjunto de IDs de nós que já foram visitados.
     * @return O ID do nó não visitado com a menor distância, ou {@code null} se não houver mais nós alcançáveis.
     */
    private static String getClosestUnvisitedNode(Map<String, Integer> unvisitedNodesWithDistance, HashSet<String> visitedNodeIds) {
        String closestNodeId = null;
        int minDistance = Integer.MAX_VALUE;

        for (Map.Entry<String, Integer> entry : unvisitedNodesWithDistance.entrySet()) {
            String nodeId = entry.getKey();
            int distance = entry.getValue();
            if (!visitedNodeIds.contains(nodeId) && distance < minDistance) {
                minDistance = distance;
                closestNodeId = nodeId;
            }
        }
        return closestNodeId;
    }

    /**
     * Reconstrói o caminho mais curto a partir do mapa de antecessores gerado por Dijkstra.
     *
     * @param previousNodeIds Um mapa onde a chave é um ID de nó e o valor é o ID do nó que o precede no caminho mais curto.
     * @param originId O ID do nó de origem.
     * @param destinationId O ID do nó de destino.
     * @return Uma {@code CustomLinkedList} de Strings representando a rota do origem ao destino.
     * Retorna uma lista vazia se não for possível construir o caminho.
     */
    private static CustomLinkedList<String> buildPath(Map<String, String> previousNodeIds, String originId, String destinationId) {
        CustomLinkedList<String> path = new CustomLinkedList<>();
        String currentNodeId = destinationId;

        if (!previousNodeIds.containsKey(currentNodeId) && !currentNodeId.equals(originId)) {
            return path;
        }

        while (currentNodeId != null) {
            path.addFirst(currentNodeId);
            if (currentNodeId.equals(originId)) {
                break;
            }
            currentNodeId = previousNodeIds.get(currentNodeId);
            if (currentNodeId == null && !path.getFirst().equals(originId)) {
                return new CustomLinkedList<>();
            }
        }

        if (path.isEmpty() || !path.getFirst().equals(originId)) {
            if (!originId.equals(destinationId)) {
                return new CustomLinkedList<>();
            }
        }

        return path;
    }
}
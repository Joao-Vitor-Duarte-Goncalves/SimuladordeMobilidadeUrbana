package org.aiacon.simuladordemobilidadeurbana.simulation;

import org.aiacon.simuladordemobilidadeurbana.model.Graph;
import org.aiacon.simuladordemobilidadeurbana.model.CustomLinkedList;
import org.aiacon.simuladordemobilidadeurbana.model.Node;
import org.aiacon.simuladordemobilidadeurbana.model.Vehicle;

import java.util.Random;

/**
 * {@code VehicleGenerator} é responsável por gerar veículos aleatoriamente
 * dentro do grafo da simulação, definindo suas origens, destinos e rotas.
 */
public class VehicleGenerator {
    private Graph graph;
    private double generationRate;
    private Random random;

    /**
     * Constrói uma nova instância de {@code VehicleGenerator}.
     *
     * @param graph O grafo da cidade no qual os veículos serão gerados.
     * @param generationRate A taxa de geração de veículos por segundo.
     */
    public VehicleGenerator(Graph graph, double generationRate) {
        this.graph = graph;
        this.generationRate = generationRate;
        this.random = new Random();
    }

    /**
     * Gera um novo objeto {@code Vehicle} com origem, destino e rota aleatórios.
     * A rota é calculada usando o algoritmo de Dijkstra.
     *
     * @param id O ID único para o novo veículo.
     * @return Um objeto {@code Vehicle} recém-gerado, ou {@code null} se não for possível gerar o veículo
     * (por exemplo, grafo vazio, rota não encontrada, etc.).
     */
    public Vehicle generateVehicle(int id) {
        if (graph == null || graph.getNodes() == null || graph.getNodes().isEmpty()) {
            System.err.println("Erro: Grafo está vazio ou não foi inicializado. Não é possível gerar veículo.");
            return null;
        }

        CustomLinkedList<String> nodeIds = new CustomLinkedList<>();
        for (Node node : graph.getNodes()) {
            nodeIds.add(node.getId());
        }

        int size = nodeIds.size();
        if (size <= 1) {
            System.err.println("Erro: Grafo não possui nós suficientes para origem e destino. Não é possível gerar veículo.");
            return null;
        }

        String origin = getRandomNodeId(nodeIds, size);
        String destination = getRandomNodeId(nodeIds, size);

        int retries = 0;
        while (destination.equals(origin) && retries < 100) {
            destination = getRandomNodeId(nodeIds, size);
            retries++;
        }

        if (!nodeIds.contains(origin) || !nodeIds.contains(destination)) {
            System.err.println("Erro: Nó de origem ou destino não encontrado no grafo.");
            return null;
        }

        System.out.println("Gerando veículo V" + id + " com origem " + origin + " e destino " + destination);

        CustomLinkedList<String> route = Dijkstra.calculateRoute(graph, origin, destination);

        if (route == null || route.isEmpty()) {
            System.err.println("Erro ao calcular rota para veículo V" + id + ": nenhuma rota encontrada entre " + origin + " e " + destination);
            return null;
        }

        Vehicle vehicle = new Vehicle("V" + id, origin, destination, route);
        System.out.println("Veículo V" + id + " gerado com sucesso: Rota = " + route);
        return vehicle;
    }

    /**
     * Retorna um ID de nó aleatório da lista fornecida.
     *
     * @param nodeIds A lista de IDs de nós disponíveis.
     * @param size O número total de IDs na lista.
     * @return Um ID de nó selecionado aleatoriamente.
     * @throws IllegalArgumentException se a lista de IDs de nós estiver vazia.
     * @throws IllegalStateException se ocorrer um erro na seleção do nó aleatório.
     */
    private String getRandomNodeId(CustomLinkedList<String> nodeIds, int size) {
        if (size <= 0) {
            throw new IllegalArgumentException("Lista de IDs de nós está vazia. Não é possível selecionar um nó aleatório.");
        }

        int index = random.nextInt(size);
        int currentIndex = 0;

        for (String nodeId : nodeIds) {
            if (currentIndex++ == index) {
                return nodeId;
            }
        }
        throw new IllegalStateException("Erro na seleção de nó aleatório: índice fora do intervalo.");
    }

    /**
     * Retorna a taxa de geração de veículos.
     *
     * @return A taxa de geração de veículos por segundo.
     */
    public double getGenerationRate() {
        return generationRate;
    }

    /**
     * Define uma nova taxa de geração de veículos.
     *
     * @param rate A nova taxa de geração de veículos por segundo.
     */
    public void setGenerationRate(double rate) {
        this.generationRate = rate;
    }
}
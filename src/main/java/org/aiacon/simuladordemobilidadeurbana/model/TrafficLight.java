package org.aiacon.simuladordemobilidadeurbana.model;

import org.aiacon.simuladordemobilidadeurbana.control.AdaptiveQueueStrategy;
import org.aiacon.simuladordemobilidadeurbana.control.EnergySavingStrategy;
import org.aiacon.simuladordemobilidadeurbana.control.FixedTimeStrategy;
import org.aiacon.simuladordemobilidadeurbana.control.TrafficLightControlStrategy;
import org.aiacon.simuladordemobilidadeurbana.simulation.Configuration;
import org.aiacon.simuladordemobilidadeurbana.simulation.NextPhaseDecision;

import java.util.HashMap;
import java.util.Map;

/**
 * Representa um semáforo em um nó específico no grafo da simulação.
 * Gerencia as fases de luz, as filas de veículos por direção e delega
 * a lógica de controle a uma estratégia configurável (tempo fixo, adaptativa, etc.).
 */
public class TrafficLight {
    private String nodeId;
    private int mode;
    private String initialJsonDirection;

    private LightPhase currentPhase;
    private double phaseTimer;

    private Queue[] directionQueues;
    private Map<String, Integer> directionNameToIndexMap;

    private TrafficLightControlStrategy controlStrategy;
    private boolean peakHourStatus = false;
    private Configuration config;

    /**
     * Constrói uma nova instância de {@code TrafficLight}.
     * Inicializa as filas de veículos para cada direção (Norte, Leste, Sul, Oeste)
     * e configura a estratégia de controle do semáforo com base no modo definido na configuração.
     *
     * @param nodeId O ID do nó ao qual este semáforo está associado.
     * @param jsonOriginalDirection A direção inicial definida no JSON (usado para referência).
     * @param config O objeto de {@code Configuration} que contém os parâmetros de simulação, incluindo o modo do semáforo.
     */
    public TrafficLight(String nodeId, String jsonOriginalDirection, Configuration config) {
        this.nodeId = nodeId;
        this.initialJsonDirection = jsonOriginalDirection != null ? jsonOriginalDirection.toLowerCase() : "unknown";
        this.config = config;
        this.mode = config.getTrafficLightMode();

        this.directionQueues = new Queue[4];
        for (int i = 0; i < 4; i++) {
            this.directionQueues[i] = new Queue();
        }

        this.directionNameToIndexMap = new HashMap<>();
        directionNameToIndexMap.put("north", 0);
        directionNameToIndexMap.put("east", 1);
        directionNameToIndexMap.put("south", 2);
        directionNameToIndexMap.put("west", 3);

        switch (this.mode) {
            case 1:
                this.controlStrategy = new FixedTimeStrategy(
                        config.getFixedGreenTime(),
                        config.getFixedYellowTime()
                );
                break;
            case 2:
                this.controlStrategy = new AdaptiveQueueStrategy(
                        config.getAdaptiveBaseGreen(),
                        config.getAdaptiveYellowTime(),
                        config.getAdaptiveMaxGreen(),
                        config.getAdaptiveQueueThreshold(),
                        config.getAdaptiveMinGreenTime(),
                        config.getAdaptiveIncrement()
                );
                break;
            case 3:
                this.controlStrategy = new EnergySavingStrategy(
                        config.getEnergySavingBaseGreen(),
                        config.getEnergySavingYellowTime(),
                        config.getEnergySavingMinGreen(),
                        config.getEnergySavingThreshold(),
                        config.getEnergySavingMaxGreenTime()
                );
                break;
            default:
                System.err.println("TRAFFIC_LIGHT_INIT: Modo de semáforo inválido (" + mode + ") para nó " + nodeId + ". Usando FixedTime por padrão.");
                this.controlStrategy = new FixedTimeStrategy(
                        config.getFixedGreenTime(),
                        config.getFixedYellowTime()
                );
                break;
        }

        if (this.controlStrategy != null) {
            this.controlStrategy.initialize(this);
        } else {
            System.err.println("TRAFFIC_LIGHT_INIT_FATAL: controlStrategy não foi instanciada para o nó " + nodeId);
            setCurrentPhase(LightPhase.NS_GREEN_EW_RED, config.getFixedGreenTime());
        }

        if (this.currentPhase == null) {
            System.err.println("TRAFFIC_LIGHT_INIT_WARN: Estratégia não definiu fase inicial para o nó " + nodeId + ". Definindo NS_GREEN_EW_RED como padrão.");
            setCurrentPhase(LightPhase.NS_GREEN_EW_RED, config.getFixedGreenTime());
            logPhaseChange();
        }
    }

    public String getNodeId() { return nodeId; }

    public LightPhase getCurrentPhase() { return currentPhase; }

    public String getInitialJsonDirection() { return initialJsonDirection; }

    public boolean isPeakHourEnabled() { return peakHourStatus; }

    public Configuration getConfiguration() { return config; }

    public void setCurrentPhase(LightPhase phase, double duration) {
        this.currentPhase = phase;
        this.phaseTimer = duration;
    }

    /**
     * Retorna o índice interno da fila para um nome de direção fornecido.
     * @param directionName O nome da direção (ex: "north", "east").
     * @return O índice inteiro correspondente, ou {@code null} se a direção não for reconhecida.
     */
    public Integer getDirectionIndex(String directionName) {
        if (directionName == null) return null;
        return directionNameToIndexMap.get(directionName.toLowerCase());
    }

    /**
     * Retorna o tamanho de todas as filas de veículos nas direções controladas pelo semáforo.
     * @return Um array de inteiros onde cada elemento representa o tamanho da fila para uma direção.
     */
    public int[] getAllQueueSizes() {
        int[] sizes = new int[4];
        for (int i = 0; i < 4; i++) {
            sizes[i] = (directionQueues[i] != null) ? directionQueues[i].size() : 0;
        }
        return sizes;
    }

    /**
     * Adiciona um veículo à fila de espera de uma direção específica.
     * @param directionName O nome da direção da fila (ex: "north").
     * @param vehicle O veículo a ser adicionado à fila.
     */
    public void addVehicleToQueue(String directionName, Vehicle vehicle) {
        Integer index = getDirectionIndex(directionName);
        if (index != null && index >= 0 && index < directionQueues.length) {
            if (directionQueues[index] == null) {
                directionQueues[index] = new Queue();
            }
            directionQueues[index].enqueue(vehicle);
        } else {
            System.err.println("TrafficLight " + nodeId + ": Não foi possível encontrar índice para direção '" + directionName + "' ao tentar enfileirar veículo " + vehicle.getId());
        }
    }

    /**
     * Remove e retorna o primeiro veículo da fila de uma direção específica.
     * @param directionName O nome da direção da fila.
     * @return O veículo removido da frente da fila, ou {@code null} se a fila estiver vazia ou a direção for inválida.
     */
    public Vehicle popVehicleFromQueue(String directionName) {
        Integer index = getDirectionIndex(directionName);
        if (index != null && index >= 0 && index < directionQueues.length &&
                directionQueues[index] != null && !directionQueues[index].isEmpty()) {
            return directionQueues[index].dequeue();
        }
        return null;
    }

    /**
     * Atualiza o estado do semáforo com base no tempo decorrido.
     * Se o timer da fase atual expirar, uma nova fase é decidida pela estratégia de controle.
     *
     * @param deltaTime O incremento de tempo da simulação em segundos.
     * @param isPeakHour Um booleano indicando se a simulação está em horário de pico.
     */
    public void update(double deltaTime, boolean isPeakHour) {
        this.peakHourStatus = isPeakHour;
        this.phaseTimer -= deltaTime;

        if (this.phaseTimer <= 0) {
            if (this.controlStrategy == null) {
                System.err.println("TrafficLight " + nodeId + ": ERRO FATAL - controlStrategy é nula no método update.");
                setCurrentPhase(LightPhase.NS_GREEN_EW_RED, config.getFixedGreenTime());
                logPhaseChange();
                return;
            }
            NextPhaseDecision decision = controlStrategy.decideNextPhase(this, deltaTime, getAllQueueSizes(), this.peakHourStatus);

            if (decision != null && decision.nextPhase != null) {
                setCurrentPhase(decision.nextPhase, decision.duration);
                logPhaseChange();
            } else {
                System.err.println("TrafficLight " + nodeId + ": Estratégia retornou decisão/fase nula. Mantendo fase atual ("+this.currentPhase+") e resetando timer para um valor seguro.");
                this.phaseTimer = config.getFixedGreenTime();
                if (this.currentPhase == null) {
                    setCurrentPhase(LightPhase.NS_GREEN_EW_RED, this.phaseTimer);
                    logPhaseChange();
                }
            }
        }
    }

    /**
     * Retorna o estado da luz (verde, amarelo, vermelho) para uma direção de aproximação específica.
     * @param approachDirection A direção da aproximação (ex: "north", "south").
     * @return Uma string representando o estado da luz ("green", "yellow", "red").
     */
    public String getLightStateForApproach(String approachDirection) {
        if (controlStrategy == null) {
            System.err.println("TrafficLight " + nodeId + ": Estratégia de controle não inicializada ao chamar getLightStateForApproach.");
            return "red";
        }
        return controlStrategy.getLightStateForApproach(this, approachDirection);
    }

    /**
     * Registra uma mudança de fase do semáforo no console.
     */
    private void logPhaseChange() {
        String phaseStr = (this.currentPhase != null) ? this.currentPhase.toString() : "INDEFINIDA";
        System.out.println("Semáforo " + nodeId + ": Nova FASE -> " + phaseStr +
                ". Duração programada: " + String.format("%.1f", this.phaseTimer) + "s.");
    }

    /**
     * Registra o estado interno atual do semáforo no console, incluindo fase e tempo restante.
     */
    public void logCurrentInternalState() {
        String phaseStr = (this.currentPhase != null) ? this.currentPhase.toString() : "INDEFINIDA";
        System.out.printf("Semáforo no nó %s -> Fase: %s, Timer Restante: %.1f%n",
                nodeId, phaseStr, phaseTimer);
    }

    /**
     * Retorna o número total de veículos em todas as filas de espera do semáforo.
     * @return O número total de veículos enfileirados.
     */
    public synchronized int getTotalVehiclesInQueues() {
        int total = 0;
        if (directionQueues != null) {
            for (int i = 0; i < directionQueues.length; i++) {
                if (directionQueues[i] != null) {
                    total += directionQueues[i].size();
                }
            }
        }
        return total;
    }
}
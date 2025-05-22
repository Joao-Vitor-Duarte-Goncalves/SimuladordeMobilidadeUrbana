package org.aiacon.simuladordemobilidadeurbana.control;

import org.aiacon.simuladordemobilidadeurbana.model.LightPhase;
import org.aiacon.simuladordemobilidadeurbana.model.TrafficLight;
import org.aiacon.simuladordemobilidadeurbana.simulation.Configuration;
import org.aiacon.simuladordemobilidadeurbana.simulation.NextPhaseDecision;

/**
 * Implementa uma estratégia de controle de semáforo adaptativa baseada no tamanho das filas de veículos.
 * Esta estratégia ajusta dinamicamente a duração da fase verde para otimizar o fluxo de tráfego,
 * buscando conceder mais tempo às direções com maior demanda, respeitando limites mínimos e máximos.
 */
public class AdaptiveQueueStrategy implements TrafficLightControlStrategy {
    private double baseGreenTimeParam;
    private double yellowTimeParam;
    private double maxGreenTimeParam;
    private int queueThresholdParam;
    private double minGreenTimeParam;
    private double incrementPerVehicleParam;

    /**
     * Constrói uma nova instância de {@code AdaptiveQueueStrategy}.
     *
     * @param baseGreen A duração base da fase verde em segundos.
     * @param yellow A duração da fase amarela em segundos.
     * @param maxGreen A duração máxima absoluta que uma fase verde pode atingir em segundos.
     * @param threshold O número de veículos na fila a partir do qual o tempo verde começa a ser estendido.
     * @param minGreen A duração mínima da fase verde em segundos.
     * @param incrementPerVehicle O tempo em segundos a ser adicionado à fase verde para cada veículo acima do limiar.
     */
    public AdaptiveQueueStrategy(double baseGreen, double yellow, double maxGreen,
                                 int threshold, double minGreen, double incrementPerVehicle) {
        this.baseGreenTimeParam = baseGreen;
        this.yellowTimeParam = yellow;
        this.maxGreenTimeParam = maxGreen;
        this.queueThresholdParam = threshold;
        this.minGreenTimeParam = minGreen;
        this.incrementPerVehicleParam = incrementPerVehicle;
    }

    /**
     * Inicializa a fase inicial do semáforo com base na sua direção JSON original.
     * A duração inicial é definida considerando o horário de pico e os limites de tempo verde.
     *
     * @param light O semáforo a ser inicializado.
     */
    @Override
    public void initialize(TrafficLight light) {
        String initialJsonDir = light.getInitialJsonDirection().toLowerCase();
        LightPhase startPhase = LightPhase.NS_GREEN_EW_RED;

        if (initialJsonDir.contains("east") || initialJsonDir.contains("west")) {
            startPhase = LightPhase.NS_RED_EW_GREEN;
        }

        double initialDuration = light.isPeakHourEnabled() ? this.baseGreenTimeParam + 5.0 : this.baseGreenTimeParam;
        initialDuration = Math.max(initialDuration, this.minGreenTimeParam);
        initialDuration = Math.min(initialDuration, this.maxGreenTimeParam);

        light.setCurrentPhase(startPhase, initialDuration);
    }

    /**
     * Decide qual será a próxima fase do semáforo e por quanto tempo ela deve durar.
     * A decisão é baseada no ciclo de fases, e a duração da fase verde é calculada
     * de forma adaptativa com base nos tamanhos das filas.
     *
     * @param light O semáforo para o qual a decisão está sendo tomada.
     * @param deltaTime O incremento de tempo da simulação (não utilizado diretamente nesta decisão).
     * @param queueSizes Um array contendo o tamanho das filas de veículos em cada direção.
     * @param isPeakHour Um booleano indicando se a simulação está em horário de pico.
     * @return Uma {@code NextPhaseDecision} contendo a próxima fase e sua duração.
     */
    @Override
    public NextPhaseDecision decideNextPhase(TrafficLight light, double deltaTime, int[] queueSizes, boolean isPeakHour) {
        LightPhase currentPhase = light.getCurrentPhase();
        LightPhase nextPhaseDetermined;
        double durationDetermined;

        switch (currentPhase) {
            case NS_GREEN_EW_RED:
                nextPhaseDetermined = LightPhase.NS_YELLOW_EW_RED;
                durationDetermined = this.yellowTimeParam;
                break;
            case NS_YELLOW_EW_RED:
                nextPhaseDetermined = LightPhase.NS_RED_EW_GREEN;
                durationDetermined = calculateAdaptiveGreenTime(light, queueSizes, true, isPeakHour);
                break;
            case NS_RED_EW_GREEN:
                nextPhaseDetermined = LightPhase.NS_RED_EW_YELLOW;
                durationDetermined = this.yellowTimeParam;
                break;
            case NS_RED_EW_YELLOW:
                nextPhaseDetermined = LightPhase.NS_GREEN_EW_RED;
                durationDetermined = calculateAdaptiveGreenTime(light, queueSizes, false, isPeakHour);
                break;
            default:
                System.err.println("AdaptiveQueueStrategy: Fase atual desconhecida " + currentPhase + " para nó " + light.getNodeId() +". Resetando para NS_GREEN_EW_RED.");
                nextPhaseDetermined = LightPhase.NS_GREEN_EW_RED;
                durationDetermined = calculateAdaptiveGreenTime(light, queueSizes, false, isPeakHour);
                break;
        }
        return new NextPhaseDecision(nextPhaseDetermined, durationDetermined);
    }

    /**
     * Calcula a duração adaptativa da fase verde com base no tamanho das filas relevantes.
     * O tempo verde é inicialmente baseado no valor base, com um bônus para horário de pico.
     * Se a fila mais longa exceder um limiar, o tempo verde é estendido por um incremento
     * por veículo. O resultado é sempre limitado pelos tempos verdes mínimo e máximo.
     *
     * @param light O semáforo para o qual o tempo verde está sendo calculado.
     * @param queueSizes Um array contendo o tamanho das filas de veículos em cada direção.
     * @param isEastWestGreenPhase {@code true} se a fase verde atual ou futura for Leste-Oeste, {@code false} se for Norte-Sul.
     * @param isPeakHour {@code true} se a simulação estiver em horário de pico, {@code false} caso contrário.
     * @return A duração calculada da fase verde em segundos.
     */
    private double calculateAdaptiveGreenTime(TrafficLight light, int[] queueSizes, boolean isEastWestGreenPhase, boolean isPeakHour) {
        double adaptiveGreenDuration = isPeakHour ? this.baseGreenTimeParam + 5.0 : this.baseGreenTimeParam;

        Integer relevantIndex1 = isEastWestGreenPhase ? light.getDirectionIndex("east") : light.getDirectionIndex("north");
        Integer relevantIndex2 = isEastWestGreenPhase ? light.getDirectionIndex("west") : light.getDirectionIndex("south");

        int relevantQueue1Size = (relevantIndex1 != null && relevantIndex1 >= 0 && relevantIndex1 < queueSizes.length) ? queueSizes[relevantIndex1] : 0;
        int relevantQueue2Size = (relevantIndex2 != null && relevantIndex2 >= 0 && relevantIndex2 < queueSizes.length) ? queueSizes[relevantIndex2] : 0;
        int maxRelevantQueue = Math.max(relevantQueue1Size, relevantQueue2Size);

        if (maxRelevantQueue == 0 && !isPeakHour) {
            return Math.max(this.minGreenTimeParam, adaptiveGreenDuration * 0.66);
        }

        if (maxRelevantQueue > this.queueThresholdParam) {
            double extension = (maxRelevantQueue - this.queueThresholdParam) * this.incrementPerVehicleParam;
            adaptiveGreenDuration += extension;
        }

        adaptiveGreenDuration = Math.min(adaptiveGreenDuration, this.maxGreenTimeParam);
        adaptiveGreenDuration = Math.max(adaptiveGreenDuration, this.minGreenTimeParam);

        return adaptiveGreenDuration;
    }

    /**
     * Retorna o estado da luz (verde, amarelo, vermelho) para uma direção de aproximação específica,
     * com base na fase atual do semáforo.
     *
     * @param light O semáforo cujo estado da luz será consultado.
     * @param approachDirection A direção de aproximação (ex: "north", "east").
     * @return Uma string que representa o estado da luz ("green", "yellow", ou "red").
     */
    @Override
    public String getLightStateForApproach(TrafficLight light, String approachDirection) {
        LightPhase currentPhase = light.getCurrentPhase();
        if (currentPhase == null || approachDirection == null) return "red";
        String dir = approachDirection.toLowerCase();

        switch (currentPhase) {
            case NS_GREEN_EW_RED: return (dir.equals("north") || dir.equals("south")) ? "green" : "red";
            case NS_YELLOW_EW_RED: return (dir.equals("north") || dir.equals("south")) ? "yellow" : "red";
            case NS_RED_EW_GREEN: return (dir.equals("east") || dir.equals("west")) ? "green" : "red";
            case NS_RED_EW_YELLOW: return (dir.equals("east") || dir.equals("west")) ? "yellow" : "red";
            default: return "red";
        }
    }
}
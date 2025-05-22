package org.aiacon.simuladordemobilidadeurbana.control;

import org.aiacon.simuladordemobilidadeurbana.model.LightPhase;
import org.aiacon.simuladordemobilidadeurbana.model.TrafficLight;
import org.aiacon.simuladordemobilidadeurbana.simulation.NextPhaseDecision;

/**
 * Implementa uma estratégia de controle de semáforo com foco em economia de energia.
 * Esta estratégia ajusta a duração da fase verde com base no volume de tráfego nas filas,
 * permitindo tempos de verde mais curtos em condições de baixo tráfego para economizar energia,
 * mas sempre respeitando um tempo verde mínimo e máximo.
 */
public class EnergySavingStrategy implements TrafficLightControlStrategy {
    private double strategyBaseGreenDuration;
    private double strategyYellowDuration;
    private double strategyMinGreenDuration;
    private int strategyLowTrafficThreshold;
    private double strategyMaxGreenDuration;

    /**
     * Constrói uma nova instância de {@code EnergySavingStrategy}.
     *
     * @param baseGreen A duração base da fase verde em segundos.
     * @param yellow A duração da fase amarela em segundos.
     * @param minGreen A duração mínima da fase verde em segundos.
     * @param threshold O número máximo de veículos na fila para que a fase verde seja reduzida ao mínimo.
     * @param maxGreen A duração máxima da fase verde em segundos.
     */
    public EnergySavingStrategy(double baseGreen, double yellow, double minGreen, int threshold, double maxGreen) {
        this.strategyBaseGreenDuration = baseGreen;
        this.strategyYellowDuration = yellow;
        this.strategyMinGreenDuration = minGreen;
        this.strategyLowTrafficThreshold = threshold;
        this.strategyMaxGreenDuration = maxGreen;
    }

    /**
     * Constrói uma estratégia de economia de energia com valores padrão.
     */
    public EnergySavingStrategy() {
        this(20.0, 3.0, 7.0, 1, 40.0);
    }

    /**
     * Inicializa a fase inicial do semáforo com base na sua direção JSON original.
     * Ajusta a duração inicial considerando o horário de pico e os limites mínimo e máximo de verde.
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

        double initialDuration = light.isPeakHourEnabled() ? this.strategyBaseGreenDuration + 2.0 : this.strategyBaseGreenDuration;
        initialDuration = Math.max(initialDuration, this.strategyMinGreenDuration);
        initialDuration = Math.min(initialDuration, this.strategyMaxGreenDuration);

        light.setCurrentPhase(startPhase, initialDuration);
    }

    /**
     * Calcula o tempo de verde para a próxima fase, considerando o tráfego atual e se é horário de pico.
     * Em condições de baixo tráfego e fora do horário de pico, o tempo verde pode ser reduzido ao mínimo.
     * O tempo verde sempre respeita os limites mínimo e máximo configurados.
     *
     * @param light O semáforo para o qual o tempo verde está sendo calculado.
     * @param queueSizes Um array contendo o tamanho das filas de veículos em cada direção.
     * @param isEastWestPhase {@code true} se a fase atual for Leste-Oeste, {@code false} se for Norte-Sul.
     * @param isPeakHour {@code true} se a simulação estiver em horário de pico, {@code false} caso contrário.
     * @return A duração calculada da fase verde em segundos.
     */
    private double calculateEnergySavingGreenTime(TrafficLight light, int[] queueSizes, boolean isEastWestPhase, boolean isPeakHour) {
        double greenTime = isPeakHour ? this.strategyBaseGreenDuration + 2.0 : this.strategyBaseGreenDuration;

        Integer relevantIndex1 = isEastWestPhase ? light.getDirectionIndex("east") : light.getDirectionIndex("north");
        Integer relevantIndex2 = isEastWestPhase ? light.getDirectionIndex("west") : light.getDirectionIndex("south");

        int trafficCount = ((relevantIndex1 != null && relevantIndex1 < queueSizes.length) ? queueSizes[relevantIndex1] : 0) +
                ((relevantIndex2 != null && relevantIndex2 < queueSizes.length) ? queueSizes[relevantIndex2] : 0);

        if (trafficCount <= this.strategyLowTrafficThreshold && !isPeakHour) {
            greenTime = this.strategyMinGreenDuration;
        }
        return Math.min(greenTime, this.strategyMaxGreenDuration);
    }

    /**
     * Decide qual será a próxima fase do semáforo e por quanto tempo ela deve durar.
     * A decisão é baseada no ciclo de fases e o tempo de verde é ajustado dinamicamente
     * pela estratégia de economia de energia.
     *
     * @param light O semáforo para o qual a decisão está sendo tomada.
     * @param deltaTime O incremento de tempo da simulação (não utilizado diretamente para a decisão de fase).
     * @param queueSizes Um array contendo o tamanho das filas de veículos em cada direção.
     * @param isPeakHour Um booleano indicando se a simulação está em horário de pico.
     * @return Uma {@code NextPhaseDecision} contendo a próxima fase e sua duração.
     */
    @Override
    public NextPhaseDecision decideNextPhase(TrafficLight light, double deltaTime, int[] queueSizes, boolean isPeakHour) {
        LightPhase currentPhase = light.getCurrentPhase();
        LightPhase nextPhase;
        double duration;

        switch (currentPhase) {
            case NS_GREEN_EW_RED:
                nextPhase = LightPhase.NS_YELLOW_EW_RED;
                duration = this.strategyYellowDuration;
                break;
            case NS_YELLOW_EW_RED:
                nextPhase = LightPhase.NS_RED_EW_GREEN;
                duration = calculateEnergySavingGreenTime(light, queueSizes, true, isPeakHour);
                break;
            case NS_RED_EW_GREEN:
                nextPhase = LightPhase.NS_RED_EW_YELLOW;
                duration = this.strategyYellowDuration;
                break;
            case NS_RED_EW_YELLOW:
                nextPhase = LightPhase.NS_GREEN_EW_RED;
                duration = calculateEnergySavingGreenTime(light, queueSizes, false, isPeakHour);
                break;
            default:
                System.err.println("EnergySavingStrategy: Fase atual desconhecida " + currentPhase + " para o nó " + light.getNodeId() + ". Resetando para NS_GREEN_EW_RED.");
                nextPhase = LightPhase.NS_GREEN_EW_RED;
                duration = calculateEnergySavingGreenTime(light, queueSizes, false, isPeakHour);
                break;
        }
        return new NextPhaseDecision(nextPhase, duration);
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
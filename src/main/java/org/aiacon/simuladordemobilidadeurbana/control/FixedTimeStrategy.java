package org.aiacon.simuladordemobilidadeurbana.control;

import org.aiacon.simuladordemobilidadeurbana.model.LightPhase;
import org.aiacon.simuladordemobilidadeurbana.model.TrafficLight;
import org.aiacon.simuladordemobilidadeurbana.simulation.NextPhaseDecision;

/**
 * Implementa uma estratégia de controle de semáforo de tempo fixo.
 * As durações das fases verde e amarela são predefinidas e não mudam
 * com base nas condições do tráfego (como tamanho da fila).
 * No entanto, pode haver ajustes para horários de pico.
 */
public class FixedTimeStrategy implements TrafficLightControlStrategy {

    private double strategyGreenDuration;
    private double strategyYellowDuration;

    /**
     * Constrói uma estratégia de tempo fixo com durações de verde e amarelo especificadas.
     *
     * @param greenTime A duração da fase verde em segundos.
     * @param yellowTime A duração da fase amarela em segundos.
     */
    public FixedTimeStrategy(double greenTime, double yellowTime) {
        this.strategyGreenDuration = greenTime;
        this.strategyYellowDuration = yellowTime;
    }

    /**
     * Constrói uma estratégia de tempo fixo com durações padrão (verde: 15s, amarelo: 3s).
     */
    public FixedTimeStrategy() {
        this.strategyGreenDuration = 15.0;
        this.strategyYellowDuration = 3.0;
    }

    /**
     * Inicializa a fase inicial do semáforo com base na sua direção JSON original.
     * Ajusta a duração inicial se a simulação estiver em horário de pico.
     *
     * @param light O semáforo a ser inicializado.
     */
    @Override
    public void initialize(TrafficLight light) {
        String initialJsonDir = light.getInitialJsonDirection().toLowerCase();
        LightPhase startPhase = LightPhase.NS_GREEN_EW_RED;
        double initialDuration = light.isPeakHourEnabled() ? 20.0 : this.strategyGreenDuration;

        if (initialJsonDir.contains("east") || initialJsonDir.contains("west")) {
            startPhase = LightPhase.NS_RED_EW_GREEN;
        }

        light.setCurrentPhase(startPhase, initialDuration);
    }

    /**
     * Decide qual será a próxima fase do semáforo e por quanto tempo ela deve durar.
     * A decisão é baseada em um ciclo de fases fixo, com possível ajuste de duração
     * para o horário de pico. As filas de veículos não influenciam a decisão neste modo.
     *
     * @param light O semáforo para o qual a decisão está sendo tomada.
     * @param deltaTime O incremento de tempo da simulação (não utilizado nesta estratégia).
     * @param queueSizes Um array contendo o tamanho das filas de veículos em cada direção (não utilizado nesta estratégia).
     * @param isPeakHour Um booleano indicando se a simulação está em horário de pico.
     * @return Uma {@code NextPhaseDecision} contendo a próxima fase e sua duração.
     */
    @Override
    public NextPhaseDecision decideNextPhase(TrafficLight light, double deltaTime, int[] queueSizes, boolean isPeakHour) {
        LightPhase currentPhase = light.getCurrentPhase();
        LightPhase nextPhase;
        double duration;

        double activeGreenDuration = isPeakHour ? 20.0 : this.strategyGreenDuration;
        double activeYellowDuration = this.strategyYellowDuration;

        switch (currentPhase) {
            case NS_GREEN_EW_RED:
                nextPhase = LightPhase.NS_YELLOW_EW_RED;
                duration = activeYellowDuration;
                break;
            case NS_YELLOW_EW_RED:
                nextPhase = LightPhase.NS_RED_EW_GREEN;
                duration = activeGreenDuration;
                break;
            case NS_RED_EW_GREEN:
                nextPhase = LightPhase.NS_RED_EW_YELLOW;
                duration = activeYellowDuration;
                break;
            case NS_RED_EW_YELLOW:
                nextPhase = LightPhase.NS_GREEN_EW_RED;
                duration = activeGreenDuration;
                break;
            default:
                System.err.println("FixedTimeStrategy: Fase atual desconhecida (" + currentPhase + ") para o nó " + light.getNodeId() + ". Resetando para NS_GREEN_EW_RED.");
                nextPhase = LightPhase.NS_GREEN_EW_RED;
                duration = activeGreenDuration;
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
            case NS_GREEN_EW_RED:
                return (dir.equals("north") || dir.equals("south")) ? "green" : "red";
            case NS_YELLOW_EW_RED:
                return (dir.equals("north") || dir.equals("south")) ? "yellow" : "red";
            case NS_RED_EW_GREEN:
                return (dir.equals("east") || dir.equals("west")) ? "green" : "red";
            case NS_RED_EW_YELLOW:
                return (dir.equals("east") || dir.equals("west")) ? "yellow" : "red";
            default:
                return "red";
        }
    }
}
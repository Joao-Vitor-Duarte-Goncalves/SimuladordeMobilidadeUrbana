
package org.aiacon.simuladordemobilidadeurbana.simulation; // Ou .control

import org.aiacon.simuladordemobilidadeurbana.model.LightPhase; // Supondo que LightPhase está em TrafficLight

/**
 * Representa uma decisão sobre qual será a próxima fase de um semáforo
 * e por quanto tempo essa fase deve durar.
 * Esta classe é imutável, garantindo que a decisão de fase e duração não mude após a criação.
 */

public class NextPhaseDecision {
    public final LightPhase nextPhase;
    public final double duration;

    public NextPhaseDecision(LightPhase nextPhase, double duration) {
        this.nextPhase = nextPhase;
        this.duration = duration;
    }
}
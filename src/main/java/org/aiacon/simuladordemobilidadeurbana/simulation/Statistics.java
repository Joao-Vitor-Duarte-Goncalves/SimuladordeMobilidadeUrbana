package org.aiacon.simuladordemobilidadeurbana.simulation;

import org.aiacon.simuladordemobilidadeurbana.model.CustomLinkedList;
import org.aiacon.simuladordemobilidadeurbana.model.Graph;
import org.aiacon.simuladordemobilidadeurbana.model.TrafficLight;
import org.aiacon.simuladordemobilidadeurbana.model.Vehicle;

public class Statistics {
    private int vehiclesGenerated;
    private int vehiclesArrived;
    private double totalTravelTime;
    private double totalWaitTime;
    private double totalFuelConsumed;
    private double currentTime;

    private double currentCongestionIndex;
    private double maxRecordedCongestionRatio;

    private double totalCongestionSum;
    private int congestionSamples;

    private static final double SMOOTHING_FACTOR = 0.3;

    public Statistics() {
        this.vehiclesGenerated = 0;
        this.vehiclesArrived = 0;
        this.totalTravelTime = 0.0;
        this.totalWaitTime = 0.0;
        this.totalFuelConsumed = 0.0;
        this.currentTime = 0.0;
        this.currentCongestionIndex = 0.0;
        this.maxRecordedCongestionRatio = 0.0;
        this.totalCongestionSum = 0.0;
        this.congestionSamples = 0;
    }

    public synchronized void vehicleGenerated() {
        this.vehiclesGenerated++;
    }

    public synchronized void vehicleArrived(double travelTime, double waitTime, double fuelConsumedByVehicle) {
        this.vehiclesArrived++;
        this.totalTravelTime += travelTime;
        this.totalWaitTime += waitTime;
        this.totalFuelConsumed += fuelConsumedByVehicle;
    }

    public synchronized void updateCurrentTime(double time) {
        this.currentTime = time;
    }

    public synchronized void calculateCurrentCongestion(CustomLinkedList<Vehicle> activeVehicles, Graph graph) {
        if (graph == null || graph.getNodes() == null || graph.getNodes().isEmpty() || activeVehicles == null) {
            this.currentCongestionIndex = 0.0;
            return;
        }

        int numberOfActiveVehicles = activeVehicles.size();
        int totalNodes = graph.getNodes().size();
        int totalQueuedVehicles = 0;

        if (graph.getTrafficLights() != null) {
            for (TrafficLight tl : graph.getTrafficLights()) {
                if (tl != null) {
                    totalQueuedVehicles += tl.getTotalVehiclesInQueues();
                }
            }
        }

        if (totalNodes == 0) {
            this.currentCongestionIndex = numberOfActiveVehicles + totalQueuedVehicles;
            return;
        }

        double vehicleDensityRatio = (double) numberOfActiveVehicles / totalNodes;
        double queuedVehicleRatio = (numberOfActiveVehicles > 0) ?
                (double) totalQueuedVehicles / numberOfActiveVehicles : 0.0;

        double rawCongestionScore = (0.4 * vehicleDensityRatio) + (0.6 * queuedVehicleRatio);
        double newCongestionIndex = Math.min(1.0, rawCongestionScore) * 100;

        this.currentCongestionIndex = (SMOOTHING_FACTOR * newCongestionIndex) +
                ((1 - SMOOTHING_FACTOR) * this.currentCongestionIndex);

        if (this.currentCongestionIndex > this.maxRecordedCongestionRatio) {
            this.maxRecordedCongestionRatio = this.currentCongestionIndex;
        }

        // Acumula para cálculo da média
        this.totalCongestionSum += this.currentCongestionIndex;
        this.congestionSamples++;
    }

    public synchronized double getCurrentCongestionIndex() {
        return this.currentCongestionIndex;
    }

    public synchronized double getMaxRecordedCongestionRatio() {
        return maxRecordedCongestionRatio;
    }

    public synchronized double getAverageCongestionIndex() {
        if (congestionSamples == 0) return 0.0;
        return totalCongestionSum / congestionSamples;
    }

    public synchronized int getTotalVehiclesGenerated() {
        return vehiclesGenerated;
    }

    public synchronized int getArrivedCount() {
        return vehiclesArrived;
    }

    public synchronized double getAverageTravelTime() {
        if (vehiclesArrived == 0) return 0.0;
        return totalTravelTime / vehiclesArrived;
    }

    public synchronized double getAverageWaitTime() {
        if (vehiclesArrived == 0) return 0.0;
        return totalWaitTime / vehiclesArrived;
    }

    public synchronized double getTotalFuelConsumed() {
        return totalFuelConsumed;
    }

    public synchronized double getAverageFuelConsumptionPerVehicle() {
        if (vehiclesArrived == 0) return 0.0;
        return totalFuelConsumed / vehiclesArrived;
    }

    public int getVehiclesArrived() {
        return vehiclesArrived;
    }

    public double getCurrentTime() {
        return currentTime;
    }

    public synchronized void printSummary() {
        System.out.println("\n--- RESUMO DA SIMULAÇÃO ---");
        System.out.printf("Tempo Total de Simulação: %.2fs%n", currentTime);
        System.out.printf("Total de Veículos Gerados: %d%n", vehiclesGenerated);
        System.out.printf("Total de Veículos Chegados ao Destino: %d%n", vehiclesArrived);

        if (vehiclesArrived > 0) {
            System.out.printf("Tempo Médio de Viagem por Veículo: %.2fs%n", getAverageTravelTime());
            System.out.printf("Tempo Médio de Espera por Veículo: %.2fs%n", getAverageWaitTime());
            System.out.printf("Consumo Total de Combustível (veículos chegados): %.3f L%n", totalFuelConsumed);
            System.out.printf("Consumo Médio de Combustível por Veículo Chegado: %.3f L%n", getAverageFuelConsumptionPerVehicle());
        } else {
            System.out.println("Nenhum veículo chegou ao destino para calcular médias detalhadas.");
        }

        System.out.printf("Média de Congestionamento (Índice Percentual): %.2f%%%n", getAverageCongestionIndex());
        System.out.printf("Pico de Congestionamento Registrado (Índice Percentual): %.2f%%%n", maxRecordedCongestionRatio);
        System.out.println("---------------------------\n");
    }
}

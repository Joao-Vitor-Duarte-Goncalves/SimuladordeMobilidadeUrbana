package org.aiacon.simuladordemobilidadeurbana;

import org.aiacon.simuladordemobilidadeurbana.model.Graph;
import org.aiacon.simuladordemobilidadeurbana.simulation.Configuration;
import org.aiacon.simuladordemobilidadeurbana.simulation.Simulator;
import org.aiacon.simuladordemobilidadeurbana.visualization.Visualizer;
import org.aiacon.simuladordemobilidadeurbana.io.JsonParser;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.io.InputStream;

public class HelloApplication extends Application {

    private Simulator simulator;
    private Thread simulationThread;

    @Override
    public void start(Stage primaryStage) {
        System.out.println("HELLO_APPLICATION_START: Iniciando a aplicação...");
        Graph graph;
        Configuration config = new Configuration(); // << MOVER A CRIAÇÃO DA CONFIG PARA ANTES DO TRY-CATCH

        // Definir os parâmetros da simulação ANTES de carregar o grafo,
        // pois o JsonParser agora precisa do objeto config.
        config.setTrafficLightMode(2);           // 1:Fixo, 2:AdaptativoFila, 3:EconomiaEnergia
        config.setVehicleGenerationRate(0.5);    // Veículos por segundo
        // config.setRedirectThreshold(10);      // Comentado pois redirectIfNeeded foi desabilitado no Simulator
        config.setPeakHour(true);               // Simular horário de pico ou não

        double totalSimulationTime = 3600.0;
        double stopGeneratingVehiclesAfter = 1500.0;

        config.setSimulationDuration(totalSimulationTime);
        config.setVehicleGenerationStopTime(stopGeneratingVehiclesAfter);

        // Exemplo para Modo Fixo (se trafficLightMode fosse 1)
        config.setFixedGreenTime(8);
        config.setFixedYellowTime(3);

        // Exemplo para Modo Adaptativo
        config.setAdaptiveBaseGreen(8.0);
        config.setAdaptiveMaxGreen(9.0);
        config.setAdaptiveMinGreenTime(6.0);
        config.setAdaptiveIncrement(0.5);
        config.setAdaptiveQueueThreshold(7);

        // Exemplo para Modo Economia
        // config.setEnergySavingBaseGreen(22.0);
        // config.setEnergySavingMaxGreenTime(45.0);
        // config.setEnergySavingMinGreen(8.0);
        // config.setEnergySavingThreshold(2);


        try {
            String resourcePath = "/mapa/CentroTeresinaPiauiBrazil.json";
            InputStream jsonInputStream = getClass().getResourceAsStream(resourcePath);
            if (jsonInputStream == null) {
                String errorMessage = "Erro Crítico: Não foi possível localizar o arquivo JSON do mapa: " + resourcePath;
                System.err.println(errorMessage);
                mostrarErroFatal(primaryStage, errorMessage);
                return;
            }
            // << PASSAR O OBJETO 'config' PARA O MÉTODO loadGraphFromStream
            graph = JsonParser.loadGraphFromStream(jsonInputStream, config);
            System.out.println("HELLO_APPLICATION_START: Grafo carregado com sucesso.");

        } catch (Exception e) {
            String errorMessage = "Erro Crítico ao carregar o grafo do JSON: " + e.getMessage();
            System.err.println(errorMessage);
            e.printStackTrace();
            mostrarErroFatal(primaryStage, errorMessage);
            return;
        }

        if (graph == null || graph.getNodes() == null || graph.getNodes().isEmpty()) {
            String errorMessage = "Erro Crítico: Falha ao carregar o grafo ou o grafo está vazio.";
            System.err.println(errorMessage);
            mostrarErroFatal(primaryStage, errorMessage);
            return;
        }

        // A configuração já foi feita antes de carregar o grafo.
        System.out.println("HELLO_APPLICATION_START: Configuração da simulação carregada. Modo Semáforo: " + config.getTrafficLightMode() +
                ", Duração Total: " + config.getSimulationDuration() + "s, Parar Geração em: " + config.getVehicleGenerationStopTime() + "s");

        this.simulator = new Simulator(graph, config); // Simulator também usa o mesmo objeto config
        Visualizer visualizer = new Visualizer(graph, this.simulator);

        try {
            visualizer.start(primaryStage);
            System.out.println("HELLO_APPLICATION_START: Visualizador iniciado.");
        } catch (Exception e) {
            String errorMessage = "Erro Crítico ao iniciar o Visualizer: " + e.getMessage();
            System.err.println(errorMessage);
            e.printStackTrace();
            mostrarErroFatal(primaryStage, errorMessage);
            return;
        }

        simulationThread = new Thread(this.simulator);
        simulationThread.setName("SimulationLoopThread");
        simulationThread.setDaemon(true);
        simulationThread.start();
        System.out.println("HELLO_APPLICATION_START: Thread da simulação iniciada.");
    }

    private void mostrarErroFatal(Stage stage, String mensagem) {
        Pane errorPane = new Pane(new Text(20, 50, mensagem));
        Scene errorScene = new Scene(errorPane, Math.max(400, mensagem.length() * 7), 100);
        stage.setTitle("Erro na Aplicação");
        stage.setScene(errorScene);
        stage.show();
        Platform.exit();
    }

    @Override
    public void stop() {
        System.out.println("HELLO_APPLICATION_STOP: Método stop() chamado, encerrando a aplicação...");
        if (simulator != null) {
            System.out.println("HELLO_APPLICATION_STOP: Chamando stopSimulation() do simulador.");
            simulator.stopSimulation();
        }
        if (simulationThread != null && simulationThread.isAlive()) {
            System.out.println("HELLO_APPLICATION_STOP: Tentando interromper a thread da simulação...");
            simulationThread.interrupt();
            try {
                simulationThread.join(1000);
                if (simulationThread.isAlive()) {
                    System.err.println("HELLO_APPLICATION_STOP: Thread da simulação ainda está ativa após join().");
                } else {
                    System.out.println("HELLO_APPLICATION_STOP: Thread da simulação terminada.");
                }
            } catch (InterruptedException e) {
                System.err.println("HELLO_APPLICATION_STOP: Thread principal interrompida enquanto esperava pela thread da simulação.");
                Thread.currentThread().interrupt();
            }
        } else {
            System.out.println("HELLO_APPLICATION_STOP: Thread da simulação não estava ativa ou era nula.");
        }
        System.out.println("HELLO_APPLICATION_STOP: Processo de encerramento da aplicação JavaFX concluído.");
    }

    public static void main(String[] args) {
        launch(args);
    }
}
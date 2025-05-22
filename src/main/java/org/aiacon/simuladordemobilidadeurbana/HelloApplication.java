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

/**
 * A aplicação principal para o Simulador de Mobilidade Urbana.
 * Esta classe estende {@code javafx.application.Application} e é responsável por
 * inicializar a simulação, carregar o grafo da cidade, configurar os parâmetros
 * da simulação e iniciar a visualização.
 */
public class HelloApplication extends Application {

    private Simulator simulator;
    private Thread simulationThread;

    /**
     * O método de entrada principal para a aplicação JavaFX.
     * Este método é chamado após o sistema estar pronto para a aplicação.
     *
     * @param primaryStage O palco principal para esta aplicação, em que a cena da aplicação pode ser definida.
     */
    @Override
    public void start(Stage primaryStage) {
        System.out.println("HELLO_APPLICATION_START: Iniciando a aplicação...");
        Graph graph;
        Configuration config = new Configuration();

        config.setTrafficLightMode(2);
        config.setVehicleGenerationRate(0.3);
        config.setPeakHour(true);

        double totalSimulationTime = 600.0;
        double stopGeneratingVehiclesAfter = 300.0;

        config.setSimulationDuration(totalSimulationTime);
        config.setVehicleGenerationStopTime(stopGeneratingVehiclesAfter);

        try {
            String resourcePath = "/mapa/CentroTeresinaPiauiBrazil.json";
            InputStream jsonInputStream = getClass().getResourceAsStream(resourcePath);
            if (jsonInputStream == null) {
                String errorMessage = "Erro Crítico: Não foi possível localizar o arquivo JSON do mapa: " + resourcePath;
                System.err.println(errorMessage);
                mostrarErroFatal(primaryStage, errorMessage);
                return;
            }
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

        System.out.println("HELLO_APPLICATION_START: Configuração da simulação carregada. Modo Semáforo: " + config.getTrafficLightMode() +
                ", Duração Total: " + config.getSimulationDuration() + "s, Parar Geração em: " + config.getVehicleGenerationStopTime() + "s");

        this.simulator = new Simulator(graph, config);
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

    /**
     * Exibe uma mensagem de erro fatal e encerra a aplicação.
     *
     * @param stage O palco principal da aplicação.
     * @param mensagem A mensagem de erro a ser exibida.
     */
    private void mostrarErroFatal(Stage stage, String mensagem) {
        Pane errorPane = new Pane(new Text(20, 50, mensagem));
        Scene errorScene = new Scene(errorPane, Math.max(400, mensagem.length() * 7), 100);
        stage.setTitle("Erro na Aplicação");
        stage.setScene(errorScene);
        stage.show();
        Platform.exit();
    }

    /**
     * Este método é chamado quando a aplicação é encerrada.
     * Garante que a thread da simulação seja interrompida de forma limpa.
     */
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

    /**
     * O método principal que inicia a aplicação JavaFX.
     *
     * @param args Argumentos da linha de comando.
     */
    public static void main(String[] args) {
        launch(args);
    }
}
package org.aiacon.simuladordemobilidadeurbana.model;

/**
 * Implementa uma fila simples (FIFO - First-In, First-Out) para armazenar objetos {@link Vehicle}.
 * Esta fila é otimizada para o caso de uso de semáforos, onde a ordem dos veículos é crucial.
 * A implementação utiliza uma lista encadeada simples, onde cada {@code Vehicle} atua como um nó,
 * utilizando seu campo {@code next} para apontar para o próximo veículo na fila.
 */
public class Queue {
    private Vehicle front;
    private Vehicle rear;
    private int size;

    /**
     * Constrói uma nova fila vazia.
     */
    public Queue() {
        this.front = null;
        this.rear = null;
        this.size = 0;
    }

    /**
     * Adiciona um veículo ao final da fila.
     * Se o veículo fornecido for nulo, uma mensagem de aviso será impressa e o veículo não será adicionado.
     *
     * @param vehicle O veículo a ser enfileirado.
     */
    public void enqueue(Vehicle vehicle) {
        if (vehicle == null) {
            System.err.println("QUEUE_ENQUEUE_WARN: Tentativa de enfileirar um veículo nulo.");
            return;
        }
        vehicle.next = null;

        if (isEmpty()) {
            front = vehicle;
            rear = vehicle;
        } else {
            rear.next = vehicle;
            rear = vehicle;
        }
        size++;
    }

    /**
     * Remove e retorna o veículo da frente da fila.
     *
     * @return O veículo que estava na frente da fila, ou {@code null} se a fila estiver vazia.
     */
    public Vehicle dequeue() {
        if (isEmpty()) {
            return null;
        }
        Vehicle vehicleToDequeue = front;
        front = front.next;

        if (front == null) {
            rear = null;
        }

        vehicleToDequeue.next = null;
        size--;
        return vehicleToDequeue;
    }

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return front == null;
    }

    public Vehicle peek() {
        return front;
    }
}
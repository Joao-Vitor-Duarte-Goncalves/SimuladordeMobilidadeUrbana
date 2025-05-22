package org.aiacon.simuladordemobilidadeurbana.model;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Uma implementação de lista encadeada genérica que suporta operações básicas
 * como adicionar, remover, obter elementos e iterar sobre a lista.
 * Esta lista mantém ponteiros para o primeiro e último nós para otimizar
 * as operações de adição e remoção no início e no final para O(1).
 *
 * @param <T> O tipo de elementos que a lista armazenará.
 */
public class CustomLinkedList<T> implements Iterable<T> {

    /**
     * Classe interna estática que representa um nó individual na lista encadeada.
     * Cada nó contém os dados e uma referência para o próximo nó.
     *
     * @param <T> O tipo de dados armazenados no nó.
     */
    private static class Node<T> {
        T data;
        Node<T> next;

        /**
         * Constrói um novo nó com os dados especificados.
         *
         * @param data Os dados a serem armazenados neste nó.
         */
        Node(T data) {
            this.data = data;
            this.next = null;
        }
    }

    private Node<T> first;
    private Node<T> last;
    private int size;

    /**
     * Constrói uma lista encadeada vazia.
     */
    public CustomLinkedList() {
        this.first = null;
        this.last = null;
        this.size = 0;
    }

    /**
     * Adiciona um item ao final da lista.
     * Esta operação tem complexidade de tempo O(1).
     *
     * @param item O item a ser adicionado.
     */
    public void add(T item) {
        Node<T> newNode = new Node<>(item);
        if (isEmpty()) {
            first = newNode;
            last = newNode;
        } else {
            last.next = newNode;
            last = newNode;
        }
        size++;
    }

    /**
     * Adiciona um item ao início da lista.
     * Esta operação tem complexidade de tempo O(1).
     *
     * @param item O item a ser adicionado.
     */
    public void addFirst(T item) {
        Node<T> newNode = new Node<>(item);
        newNode.next = first;
        first = newNode;
        if (last == null) {
            last = newNode;
        }
        size++;
    }

    /**
     * Retorna o primeiro item da lista sem removê-lo.
     * Esta operação tem complexidade de tempo O(1).
     *
     * @return O primeiro item da lista.
     * @throws NoSuchElementException se a lista estiver vazia.
     */
    public T getFirst() {
        if (isEmpty()) {
            throw new NoSuchElementException("A lista está vazia.");
        }
        return first.data;
    }

    /**
     * Retorna o último item da lista sem removê-lo.
     * Esta operação tem complexidade de tempo O(1).
     *
     * @return O último item da lista.
     * @throws NoSuchElementException se a lista estiver vazia.
     */
    public T getLast() {
        if (isEmpty()) {
            throw new NoSuchElementException("A lista está vazia.");
        }
        return last.data;
    }

    public int size() {
        return size;
    }

    /**
     * Verifica se a lista está vazia.
     * Esta operação tem complexidade de tempo O(1).
     */
    public boolean isEmpty() {
        return size == 0;
    }

    /**
     * Retorna o item no índice especificado.
     * Esta operação tem complexidade de tempo O(N), onde N é o tamanho da lista.
     *
     * @param index O índice do item a ser retornado (base zero).
     * @return O item no índice especificado.
     * @throws IndexOutOfBoundsException se o índice for inválido.
     */
    public T get(int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException("Índice inválido: " + index + " para tamanho " + size);
        }
        Node<T> current = first;
        for (int i = 0; i < index; i++) {
            current = current.next;
        }
        return current.data;
    }

    /**
     * Retorna o índice da primeira ocorrência do item especificado nesta lista.
     * Esta operação tem complexidade de tempo O(N).
     *
     * @param item O item a ser pesquisado.
     * @return O índice da primeira ocorrência do item, ou -1 se o item não for encontrado.
     */
    public int indexOf(T item) {
        Node<T> current = first;
        int index = 0;
        while (current != null) {
            if (item == null ? current.data == null : item.equals(current.data)) {
                return index;
            }
            current = current.next;
            index++;
        }
        return -1;
    }

    public boolean contains(T item) {
        return indexOf(item) != -1;
    }

    /**
     * Remove e retorna o primeiro item da lista.
     * Esta operação tem complexidade de tempo O(1).
     *
     * @return O item que foi removido do início da lista.
     * @throws NoSuchElementException se a lista estiver vazia.
     */
    public T removeFirst() {
        if (isEmpty()) {
            throw new NoSuchElementException("Não é possível remover de uma lista vazia.");
        }
        T data = first.data;
        first = first.next;
        size--;
        if (isEmpty()) {
            last = null;
        }
        return data;
    }

    /**
     * Remove a primeira ocorrência do item especificado desta lista, se estiver presente.
     * Esta operação tem complexidade de tempo O(N).
     *
     * @param item O item a ser removido.
     * @return {@code true} se o item foi removido com sucesso, {@code false} caso contrário.
     */
    public boolean remove(T item) {
        if (isEmpty()) {
            return false;
        }

        if ((item == null && first.data == null) || (item != null && item.equals(first.data))) {
            removeFirst();
            return true;
        }

        Node<T> current = first;
        while (current.next != null) {
            if ((item == null && current.next.data == null) || (item != null && item.equals(current.next.data))) {
                if (current.next == last) {
                    last = current;
                }
                current.next = current.next.next;
                size--;
                return true;
            }
            current = current.next;
        }
        return false;
    }

    /**
     * Retorna um iterador sobre os elementos desta lista na sequência correta.
     *
     * @return Um {@code Iterator} sobre os elementos desta lista.
     */
    @Override
    public Iterator<T> iterator() {
        return new Iterator<T>() {
            private Node<T> current = first;

            @Override
            public boolean hasNext() {
                return current != null;
            }

            @Override
            public T next() {
                if (!hasNext()) {
                    throw new NoSuchElementException("Não há mais elementos na lista.");
                }
                T data = current.data;
                current = current.next;
                return data;
            }
        };
    }

    /**
     * Retorna uma representação em string da lista.
     *
     * @return Uma string formatada como "[item1 -> item2 -> ... -> itemN]".
     */
    @Override
    public String toString() {
        if (isEmpty()) {
            return "[]";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        Node<T> current = first;
        while (current != null) {
            sb.append(current.data == null ? "null" : current.data.toString());
            if (current.next != null) {
                sb.append(" -> ");
            }
            current = current.next;
        }
        sb.append("]");
        return sb.toString();
    }
}
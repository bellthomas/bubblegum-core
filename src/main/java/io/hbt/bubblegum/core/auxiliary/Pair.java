package io.hbt.bubblegum.core.auxiliary;


/**
 * Helper class providing a generics Pair.
 * @param <U> The firs type.
 * @param <V> The second type.
 */
public class Pair<U,V> {
    private U first;
    private V second;

    /**
     * Constructor.
     * @param first The first element.
     * @param second The second element.
     */
    public Pair(U first, V second){
        this.first = first;
        this.second = second;
    }

    /**
     * Retrieve the first elements.
     * @return The first elements.
     */
    public U getFirst() {
        return this.first;
    }

    /**
     * Retrieve the second element.
     * @return The second element.
     */
    public V getSecond() {
        return this.second;
    }

    /**
     * Set the first element.
     * @param newFirst The new first element.
     */
    public void setFirst(U newFirst) {
        this.first = newFirst;
    }

    /**
     * Set the second element.
     * @param newSecond The new second element.
     */
    public void setSecond(V newSecond) {
        this.second = newSecond;
    }

} // end Pair class

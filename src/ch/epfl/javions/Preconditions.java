package ch.epfl.javions;

/**
 * Immuable class to ensure the validity of the arguments passed to methods throughout the project.
 *
 * @author : Alexei Thornber (363088)
 * @author : Zachary Doll (356458)
 */
public final class Preconditions {
    private Preconditions() {
    } // Prevents instantiation

    /**
     * Used exclusively to check if a given boolean is true, and throws an IllegalArgumentException if it is not.
     * Does nothing if the boolean is true.
     *
     * @param shouldBeTrue the boolean to check
     * @throws IllegalArgumentException if the boolean is false
     */
    public static void checkArgument(boolean shouldBeTrue) {
        if (!shouldBeTrue) {
            throw new IllegalArgumentException();
        }
    }
}

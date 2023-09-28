package ch.epfl.javions;

import static java.lang.Math.log;

/**
 * This class contains utility methods for mathematical operations that are not provided by the standard
 * library.
 *
 * @author : Alexei Thornber (363088)
 * @author : Zachary Doll (356458)
 */
public final class Math2 {
    private Math2() {
    }

    /**
     * Bounds an inputted value by a given maximum and minimum number.
     *
     * @param min the minimum value of the interval (cannot be bigger than max)
     * @param v   the value to be bounded
     * @param max the maximum value of the interval
     * @return min if v is smaller or equal to min,
     * max if v is bigger or equal to max,
     * v if v is between min and max
     * @throws IllegalArgumentException if min is bigger than max
     */

    public static int clamp(int min, int v, int max) {
        Preconditions.checkArgument(min <= max);
        return Math.max(min, Math.min(v, max));
    }

    /**
     * This method computes the reciprocal function of the hyperbolic sinus of a given value.
     *
     * @param x inserted value
     * @return the reciprocal of the hyperbolic sin function of x.
     */
    public static double asinh(double x) {
        return log(x + Math.hypot(1, x));
    }
}

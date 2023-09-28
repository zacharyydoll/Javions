/**
 * @author : Alexei Thornber (363088)
 * @author : Zachary Doll (356458)
 */

package ch.epfl.javions.gui;

import ch.epfl.javions.Preconditions;
import javafx.scene.paint.Color;

/**
 * Represents a gradient of colors as a function mapping a value in [0, 1] to a color.
 */
public final class ColorRamp {

    /**
     * The array of colors used to represent the gradient.
     *
     * @see Color#valueOf(String) for more infomration.
     */
    public static final ColorRamp PLASMA = new ColorRamp(
            Color.valueOf("0x0d0887ff"), Color.valueOf("0x220690ff"),
            Color.valueOf("0x320597ff"), Color.valueOf("0x40049dff"),
            Color.valueOf("0x4e02a2ff"), Color.valueOf("0x5b01a5ff"),
            Color.valueOf("0x6800a8ff"), Color.valueOf("0x7501a8ff"),
            Color.valueOf("0x8104a7ff"), Color.valueOf("0x8d0ba5ff"),
            Color.valueOf("0x9814a0ff"), Color.valueOf("0xa31d9aff"),
            Color.valueOf("0xad2693ff"), Color.valueOf("0xb6308bff"),
            Color.valueOf("0xbf3984ff"), Color.valueOf("0xc7427cff"),
            Color.valueOf("0xcf4c74ff"), Color.valueOf("0xd6556dff"),
            Color.valueOf("0xdd5e66ff"), Color.valueOf("0xe3685fff"),
            Color.valueOf("0xe97258ff"), Color.valueOf("0xee7c51ff"),
            Color.valueOf("0xf3874aff"), Color.valueOf("0xf79243ff"),
            Color.valueOf("0xfa9d3bff"), Color.valueOf("0xfca935ff"),
            Color.valueOf("0xfdb52eff"), Color.valueOf("0xfdc229ff"),
            Color.valueOf("0xfccf25ff"), Color.valueOf("0xf9dd24ff"),
            Color.valueOf("0xf5eb27ff"), Color.valueOf("0xf0f921ff"));
    private final Color[] colors;

    /**
     * Constructs a new ColorRamp with the given array of colors.
     *
     * @param colors the array of {@link Color} that specifies the function representing the gradient.
     * @throws IllegalArgumentException if the given array has less than 2 elements
     */
    public ColorRamp(Color... colors) {
        //check that there are at least two colors
        Preconditions.checkArgument(colors.length >= 2);
        this.colors = colors;
    }

    /**
     * Returns a color on the gradient corresponding to the given value.
     *
     * @param value the value to associate with a color
     * @return the {@link Color} from the {@link #colors} associated with the given value
     */
    public Color at(double value) {
        final double lowerBound = 0;
        final double upperBound = 1;

        if (value < lowerBound) {
            return colors[0];
        } else if (value > upperBound) {
            return colors[colors.length - 1];
        } else {
            int idx = (int) Math.floor((value - lowerBound) / ((upperBound - lowerBound) / (colors.length - 1)));
            double proportion = (value - lowerBound) * (colors.length - 1) - idx;
            return colors[idx].interpolate(colors[idx + 1], proportion);
        }
    }
}
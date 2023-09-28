package ch.epfl.javions.aircraft;

import ch.epfl.javions.Preconditions;

import java.util.regex.Pattern;

/**
 * Represents the Aircraft Description of an Aircraft, a 3-character string that describes the type of
 * aircraft and its engine type.
 *
 * @author : Alexei Thornber (363088)
 * @author : Zachary Doll (356458)
 */
public record AircraftDescription(String string) {
    private static final Pattern AIRCRAFT_DESC_FORMAT = Pattern.compile("[ABDGHLPRSTV-][0123468][EJPT-]");

    /**
     * Creates a new Aircraft Description from a string.
     *
     * @param string Aircraft Description of designated Aircraft (must match Aircraft Description format)
     * @throws IllegalArgumentException if the inserted Aircraft Description does not match expected format
     */
    public AircraftDescription {
        Preconditions.checkArgument(string.isEmpty() || AIRCRAFT_DESC_FORMAT.matcher(string).matches());
    }
}

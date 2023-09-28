package ch.epfl.javions.aircraft;

import ch.epfl.javions.Preconditions;

import java.util.regex.Pattern;

/**
 * Represents the Aircraft Type Designator of an aircraft.
 *
 * @author : Alexei Thornber (363088)
 * @author : Zachary Doll (356458)
 */
public record AircraftTypeDesignator(String string) {
    private static final Pattern TYPE_DESIGNATOR_FORMAT = Pattern.compile("[A-Z0-9]{2,4}");

    /**
     * Instantiates a new Aircraft Type Designator.
     *
     * @param string Aircraft Type Designator of designated Aircraft (must match Aircraft Type Designator format)
     * @throws IllegalArgumentException if the inserted Aircraft Type Designator is invalid
     */
    public AircraftTypeDesignator {
        Preconditions.checkArgument(string.isEmpty() || TYPE_DESIGNATOR_FORMAT.matcher(string).matches());
    }
}

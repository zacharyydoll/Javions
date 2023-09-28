package ch.epfl.javions.aircraft;

import ch.epfl.javions.Preconditions;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Represents the Aircraft Registration of an Aircraft, a String that uniquely identifies an aircraft.
 *
 * @author : Alexei Thornber (363088)
 * @author : Zachary Doll (356458)
 */
public record AircraftRegistration(String string) {

    private static final Pattern REGISTRATION_FORMAT = Pattern.compile("[A-Z0-9 .?/_+-]+");

    /**
     * Creates a new Aircraft Registration from a string.
     *
     * @param string Aircraft Registration of designated Aircraft (must match Registration format, cannot be null)
     * @throws IllegalArgumentException the inserted registration is invalid or null
     * @throws NullPointerException     if the inserted registration is null
     */
    public AircraftRegistration {
        Objects.requireNonNull(string, "registration cannot be null");
        Preconditions.checkArgument(!(string.isEmpty()) && REGISTRATION_FORMAT.matcher(string).matches());
    }
}

package ch.epfl.javions.aircraft;

import ch.epfl.javions.Preconditions;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Represents the ICAO address of an aircraft, a 6 character hexadecimal string.
 *
 * @author : Alexei Thornber (363088)
 * @author : Zachary Doll (356458)
 */
public record IcaoAddress(String string) {
    private final static Pattern ICAO_FORMAT = Pattern.compile("[0-9A-F]{6}");

    /**
     * Instantiates a new Icao address from a string.
     *
     * @param string the ICAO address of an Aircraft (must match expected ICAO address format, cannot be null)
     * @throws IllegalArgumentException if the inserted ICAO address is invalid or null
     * @throws NullPointerException     if the inserted ICAO address is null
     */
    public IcaoAddress {
        Objects.requireNonNull(string, "Icao address cannot be null");
        Preconditions.checkArgument(ICAO_FORMAT.matcher(string).matches() && !(string.equals("")));
    }
}

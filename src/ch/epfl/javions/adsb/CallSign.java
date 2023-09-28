package ch.epfl.javions.adsb;

import ch.epfl.javions.Preconditions;

import java.util.regex.Pattern;

/**
 * Represents the call sign of an aircraft, an 8 character string.
 *
 * @author : Alexei Thornber (363088)
 * @author : Zachary Doll (356458)
 */
public record CallSign(String string) {
    private final static Pattern CALL_SIGN_FORMAT = Pattern.compile("[A-Z0-9 ]{0,8}");

    /**
     * The constructor of the call sign.
     *
     * @param string the call sign of designated Aircraft (must match call sign format)
     * @throws IllegalArgumentException if the inserted call sign does not match expected format
     */
    public CallSign {
        Preconditions.checkArgument((CALL_SIGN_FORMAT.matcher(string).matches()) || (string.equals("")));
    }
}

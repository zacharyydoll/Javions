package ch.epfl.javions.adsb;

import ch.epfl.javions.Bits;
import ch.epfl.javions.Preconditions;
import ch.epfl.javions.aircraft.IcaoAddress;

import java.util.Objects;

/**
 * Represents a message sent by an aircraft to identify itself, characterized by the type codes 1, 2, 3, or 4.
 * An identification message contains the time at which it was sent, the ICAO address of the aircraft,
 * the category of the aircraft, and the call sign of the aircraft.
 *
 * @author : Alexei Thornber (363088)
 * @author : Zachary Doll (356458)
 */
public record AircraftIdentificationMessage(long timeStampNs,
                                            IcaoAddress icaoAddress,
                                            int category,
                                            CallSign callSign) implements Message {
    private static final int NB_CHARACTERS = 8;
    private static final int BITS_PER_CHAR = 6;
    private static final int NB_BITS = BITS_PER_CHAR * NB_CHARACTERS; // 48
    private static final int CA_SIZE = 3;

    //--------------------------------------------------------------------------------
    /**
     * The ASCII codes for the characters used in the call sign.
     */
    private static final int A = 1;
    private static final int Z = 26;
    private static final int SPACE = 32;
    private static final int ZERO = 48;
    private static final int NINE = 57;
    private static final int UPPER_CASES = 64;


    /**
     * Instantiates a new message sent by an aircraft to identify itself.
     *
     * @param timeStampNs the times at which the message was sent, in nanoseconds (must be positive)
     * @param icaoAddress the sent ICAO address of the given Aircraft (cannot be null)
     * @param category    the category of the given Aircraft (plane, helicopter, glider, ect...)
     * @param callSign    the CallSign of the Aircraft, a series of 8 characters (cannot be null)
     * @throws NullPointerException     if the ICAO address is null or if the CallSign is null
     * @throws IllegalArgumentException if the timestamp is negative
     */
    public AircraftIdentificationMessage {
        Preconditions.checkArgument(timeStampNs >= 0);
        Objects.requireNonNull(icaoAddress, "The ICAO address is null");
        Objects.requireNonNull(callSign, "The call sign is null");
    }

    /**
     * Provides an Aircraft Identification Message corresponding to the given RawMessage.
     *
     * @param rawMessage the Raw ADS-B message of the Aircraft
     * @return the Aircraft Identification Message of the Raw Message, or null if at least one of the characters
     * of the call sign is invalid
     * @see CallSign for valid call sign characters
     */
    public static AircraftIdentificationMessage of(RawMessage rawMessage) {

        int CA = Bits.extractUInt(rawMessage.payload(), NB_BITS, CA_SIZE);
        int category = ((RawMessage.LENGTH - rawMessage.typeCode()) << 4) | CA;

        CallSign callSign = getCallSign(rawMessage);
        if (callSign == null) return null;

        return new AircraftIdentificationMessage(rawMessage.timeStampNs(),
                rawMessage.icaoAddress(),
                category,
                callSign);
    }

    /**
     * Returns the CallSign of the Aircraft corresponding to the given RawMessage.
     *
     * @param rawMessage the Raw ADS-B message of the Aircraft
     * @return the CallSign of the Aircraft, a series of 8 characters, or null if at least one of the characters
     * of the call sign is invalid
     * @see CallSign for valid call sign characters
     */
    private static CallSign getCallSign(RawMessage rawMessage) {
        int idx = NB_BITS;
        var callSignBuilder = new StringBuilder(NB_CHARACTERS);

        for (int i = 0; i < NB_CHARACTERS; i++) {
            int character = Bits.extractUInt(rawMessage.payload(), idx - BITS_PER_CHAR, BITS_PER_CHAR);

            if (character >= A && character <= Z) { // A-Z
                callSignBuilder.append((char) (character + UPPER_CASES));
            } else if (character == SPACE) { // Space
                callSignBuilder.append(" ");
            } else if (character >= ZERO && character <= NINE) { // 0-9
                callSignBuilder.append((char) character);
            } else {
                return null;
            }
            idx -= BITS_PER_CHAR;
        }
        return new CallSign(callSignBuilder.toString().trim()); // Trim to remove trailing spaces
    }
}





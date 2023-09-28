package ch.epfl.javions.adsb;

import ch.epfl.javions.Bits;
import ch.epfl.javions.Preconditions;
import ch.epfl.javions.Units;
import ch.epfl.javions.aircraft.IcaoAddress;

import java.util.Objects;

/**
 * Represents a message sent by an aircraft to report its position, characterized by the type codes 9 to 18 (included)
 * or 20 to 22 (included). A position message contains the time at which it was sent, the ICAO address of the aircraft,
 * the altitude of the aircraft, and the position of the aircraft.
 *
 * @author : Alexei Thornber (363088)
 * @author : Zachary Doll (356458)
 */
public record AirbornePositionMessage(long timeStampNs,
                                      IcaoAddress icaoAddress,
                                      double altitude,
                                      int parity,
                                      double x,
                                      double y) implements Message {

    private static final int ALTITUDE_SIZE = 12;
    private static final int ALTITUDE_START = 36;
    private static final int LAT_START = 17;
    private static final int LON_START = 0;
    private static final int PARITY_IDX = 34;
    private static final int COORD_LENGTH = 17;

    //Q is defined as the 4th bit (from the right) of the 12-bit altitude attribute of the message.
    private static final int Q_IDX = 4;
    private static final int STRONG_SIZE = (ALTITUDE_SIZE - Q_IDX) + 1;
    private static final int WEAK_SIZE = ALTITUDE_SIZE - STRONG_SIZE;

    /**
     * Instantiates a new message sent by an aircraft to report its position.
     *
     * @param timeStampNs the timestamp of the message in nanoseconds (must be positive)
     * @param icaoAddress the sent ICAO address of the given Aircraft (cannot be null)
     * @param altitude    the current altitude of the aircraft given in meters
     * @param parity      the parity of the message, can only be 0 or 1 (must be 1 or 0)
     * @param x           the normalized local longitude of the aircraft at time of sending, between 0 and 1 (excluded)
     * @param y           the normalized local latitude of the aircraft at time of sending, between 0 and 1 (excluded)
     * @throws NullPointerException     if the ICAO address is null
     * @throws IllegalArgumentException if the time stamp is strictly negative,
     *                                  if the parity is not equal to 0 or 1, or
     *                                  if the longitude and latitude are not between 0 (included) and 1 (excluded).
     */
    public AirbornePositionMessage {
        Preconditions.checkArgument(timeStampNs >= 0);
        Preconditions.checkArgument((x >= 0 && x < 1) && (y >= 0 && y < 1));
        Preconditions.checkArgument(parity == 0 || parity == 1);
        Objects.requireNonNull(icaoAddress, "icaoAddress is null");
    }

    /**
     * Provide an AirbornePositionMessage corresponding to the given raw message.
     *
     * @param rawMessage the Raw ADS-B message of the aircraft
     * @return the AircraftPositionMessage of the raw message if the altitude contained in the raw message is valid,
     * null otherwise
     * (see {@link AirbornePositionMessage#decodeAltitudeQ0(int, int, double, double, RawMessage)} for
     * information on the validity of the altitude)
     */
    public static AirbornePositionMessage of(RawMessage rawMessage) {

        int alt = Bits.extractUInt(rawMessage.payload(), ALTITUDE_START, ALTITUDE_SIZE);
        int parity = Bits.testBit(rawMessage.payload(), PARITY_IDX) ? 1 : 0;
        //the longitude and latitude need to be normalized.
        double lat_cpr = Math.scalb(Bits.extractUInt(rawMessage.payload(), LAT_START, COORD_LENGTH), -17);
        double lon_cpr = Math.scalb(Bits.extractUInt(rawMessage.payload(), LON_START, COORD_LENGTH), -17);

        if (Bits.testBit(alt, Q_IDX))
            return decodeAltitudeQ1(alt, parity, lat_cpr, lon_cpr, rawMessage);
        else
            return decodeAltitudeQ0(alt, parity, lat_cpr, lon_cpr, rawMessage);

    }

    /**
     * Returns the AirbornePositionMessage for the specific case where the bit at index {@link #Q_IDX} of the altitude
     * attribute is equal to 1.
     *
     * @param alt        the altitude of the aircraft attribute of the message
     * @param parity     the parity of the raw message
     * @param lat        the normalized local latitude of the aircraft at the time the message was sent
     * @param lon        the normalized local longitude of the aircraft at time the message was sent
     * @param rawMessage the raw message
     * @return the AirbornePositionMessage corresponding to the given raw message
     */
    private static AirbornePositionMessage decodeAltitudeQ1(int alt,
                                                            int parity,
                                                            double lat,
                                                            double lon,
                                                            RawMessage rawMessage) {

        int left = Bits.extractUInt(alt, 5, ALTITUDE_SIZE - Q_IDX);
        int right = Bits.extractUInt(alt, 0, Q_IDX);
        alt = (left << 4) | right;
        //formula from the handout
        double altitude = Units.convertFrom((-1000 + (alt * 25)), Units.Length.FOOT);

        return new AirbornePositionMessage(rawMessage.timeStampNs(),
                rawMessage.icaoAddress(),
                altitude,
                parity,
                lon,
                lat);
    }

    /**
     * Returns the AirbornePositionMessage for the specific case where the bit at index {@link #Q_IDX} of the altitude
     * attribute is equal to 0.
     *
     * @param alt        the altitude of the aircraft attribute of the message
     * @param parity     the parity of the raw message
     * @param lat        the normalized local latitude of the aircraft at the time the message was sent
     * @param lon        the normalized local longitude of the aircraft at time the message was sent
     * @param rawMessage the raw message
     * @return the AirbornePositionMessage if the altitude is valid (if the gray code value of the 3 least significant
     * bits of the alt attribute is not 0, 5 or 6), null otherwise
     */
    private static AirbornePositionMessage decodeAltitudeQ0(int alt,
                                                            int parity,
                                                            double lat,
                                                            double lon,
                                                            RawMessage rawMessage) {

        alt = reorder(alt);
        //taking the 9 most significant bits of alt attribute
        int strongBits = decodeGray(Bits.extractUInt(alt, WEAK_SIZE, STRONG_SIZE));
        //taking the 3 least significant bits of alt attribute
        int weakBits = decodeGray(Bits.extractUInt(alt, 0, WEAK_SIZE));

        //if the weak bits are 0, 5 or 6, the altitude is defined as invalid
        if (weakBits == 0 || weakBits == 5 || weakBits == 6)
            return null;
        else if (weakBits == 7)
            weakBits = 5;

        //if the Gray code value of the 9 most significant bits is odd, the weak bits are reflected
        if (strongBits % 2 != 0)
            weakBits = 6 - weakBits;
        //formula from the handout
        double altitude = Units.convertFrom(-1300 + (weakBits * 100) + (strongBits * 500), Units.Length.FOOT);

        return new AirbornePositionMessage(rawMessage.timeStampNs(),
                rawMessage.icaoAddress(),
                altitude,
                parity,
                lon,
                lat);
    }

    /**
     * Reorders the 12 bits of an inputted integer in the order specified by the indices array.
     *
     * @param n the number to rearrange in specified format
     * @return the rearranged number
     */
    //TODO: unnecessary to use a string to do this 35.9
    /*private static int reorder(int n) {
        //ensures that the number is 12 bits long, replacing leading spaces with 0.
        var binaryString = String.format("%12s", Integer.toBinaryString(n)).replace(' ', '0');
        var reorderedBits = new char[ALTITUDE_SIZE];
        //order in which the bits are to be rearranged
        int[] indices = {7, 9, 11, 1, 3, 5, 6, 8, 10, 0, 2, 4};

        for (int i = 0; i < indices.length; i++) {
            reorderedBits[i] = binaryString.charAt(indices[i]);
        }
        return Integer.parseInt(new String(reorderedBits), 2);
    }*/
    private static int reorder(int n) {
        int[] indices = {7, 9, 11, 1, 3, 5, 6, 8, 10, 0, 2, 4};
        int reordered = 0;

        for (int i = 0; i < indices.length; i++) {
            int bit = (n >> indices[i]) & 1;
            reordered |= bit << i;
        }

        return reordered;
    }

    /**
     * Interprets a given integer as a gray code and decodes it.
     *
     * @param gray the gray code to decode
     * @return the decoded gray code value of the given integer
     */
    private static int decodeGray(int gray) {
        for (int i = 1; i < Integer.SIZE; i <<= 1) {
            gray ^= gray >> i;
        }
        return gray;
    }

}


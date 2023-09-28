package ch.epfl.javions.adsb;

/**
 * Parses a RawMessage into a Message.
 *
 * @author : Alexei Thornber (363088)
 * @author : Zachary Doll (356458)
 */
public class MessageParser {
    private static final int APM_START1 = 9;
    private static final int APM_END1 = 18;
    private static final int APM_START2 = 20;
    private static final int APM_END2 = 22;
    private static final int AIM_START = 1;
    private static final int AIM_END = 4;
    private static final int AVM_TC = 19;

    private MessageParser() {
    } // Prevents instantiation

    /**
     * Parses a raw message into a message.
     *
     * @param rawMessage the raw message to parse
     * @return the messaged passed according to its type code (and the information it holds),
     * or null if the type code is not recognized
     * @see RawMessage#typeCode() for the type codes supported by this method and the messages they correspond to.
     */

    public static Message parse(RawMessage rawMessage) {
        int typeCode = rawMessage.typeCode();

        if ((typeCode >= APM_START1 && typeCode <= APM_END1) || (typeCode >= APM_START2 && typeCode <= APM_END2)) {
            return AirbornePositionMessage.of(rawMessage);
        } else if (typeCode >= AIM_START && typeCode <= AIM_END) {
            return AircraftIdentificationMessage.of(rawMessage);
        } else if (typeCode == AVM_TC) {
            return AirborneVelocityMessage.of(rawMessage);
        } else {
            return null;
        }
    }
}

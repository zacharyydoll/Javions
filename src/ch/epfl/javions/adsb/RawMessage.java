package ch.epfl.javions.adsb;

import ch.epfl.javions.Bits;
import ch.epfl.javions.ByteString;
import ch.epfl.javions.Crc24;
import ch.epfl.javions.Preconditions;
import ch.epfl.javions.aircraft.IcaoAddress;

/**
 * Represents a raw ADS-B message transmitted by an aircraft, containing information about the position, velocity
 * or identification of the aircraft, depending on its type code.
 *
 * @author : Alexei Thornber (363088)
 * @author : Zachary Doll (356458)
 */
public record RawMessage(long timeStampNs, ByteString bytes) {
    public final static int LENGTH = 14;
    private final static int TYPE_CODE_SIZE = 5;
    private final static int TYPE_CODE_START = 51;
    private final static int TYPE_CODE_BYTE_START = 3;
    private final static int TYPE_CODE_BYTE_IDX = 4;

    private static final int PAYLOAD_END_IDX = 11;
    private static final int ICAO_END = 8;
    private static final int ICAO_CHAR_LENGTH = 6;

    private final static int DF_SIZE = 5;
    private final static int DF_START = 3;
    private final static int DF_BYTE_IDX = 0;
    private final static int ADSB_DF = 17;

    private static final Crc24 CRC = new Crc24(Crc24.GENERATOR);

    /**
     * Instantiates a new Raw message containing the information transmitted by an aircraft.
     *
     * @param timeStampNs the timestamp of the raw message in nanoseconds (must be positive)
     * @param bytes       the ADS-B message in an Array of bytes (must be of length 14)
     * @throws IllegalArgumentException if the timestamps is negative, or if the length of the Array of bytes
     *                                  is not equal to the length of an ADS-B message
     */
    public RawMessage {
        Preconditions.checkArgument(timeStampNs >= 0 && bytes.size() == LENGTH);
    }

    /**
     * @param timeStampNs the timestamp of the message in nanoseconds
     * @param bytes       the (possible valid) ADS-B message in an array of bytes
     * @return The raw message from provided data if the CRC is equal to zero, meaning the message has not been
     * corrupted, returns null otherwise
     */
    public static RawMessage of(long timeStampNs, byte[] bytes) {
        return CRC.crc(bytes) == 0 ? new RawMessage(timeStampNs, new ByteString(bytes)) : null;
    }

    /**
     * Returns the size of the ADS-B message in bytes.
     *
     * @param byte0 the DF attribute of the ADS-B message (first 5 bits)
     * @return the {@link #LENGTH} in bytes of an ADS-B message if the DF attribute is valid and equal to 17, or
     * 0 if not the case
     */
    public static int size(byte byte0) {
        int DF = Bits.extractUInt(byte0, DF_START, DF_SIZE);
        return DF == ADSB_DF ? LENGTH : 0;
    }

    /**
     * Returns the payload of the ADS-B message, which is the 56 bits of the message containing its exploitable
     * information.
     *
     * @param payload the Payload of the ADS-B message containing all of its exploitable information
     * @return the typeCode of the ADS-B message (i.e, the 5 msb of its payload)
     */
    public static int typeCode(long payload) {
        return Bits.extractUInt(payload, TYPE_CODE_START, TYPE_CODE_SIZE);
    }

    /**
     * Returns the DownLink Format (DF) of the ADS-B message, which is the first 5 bits of the message.
     *
     * @return the format of the ADS-B message.
     */
    public int downLinkFormat() {
        return Bits.extractUInt(bytes.byteAt(DF_BYTE_IDX), DF_START, DF_SIZE);
    }

    /**
     * Returns the typeCode of the ADS-B message, which is the 5 most significant bits (msb) of its payload,
     * and is used to identify the type of the message.
     * Supported messages in this and their type codes in this project are:
     * <ul>
     *      <li>Identification messages (type codes 1, 2, 3 or 4)
     *      <li>Position messages (type codes 9 through 18 or 20 through 22)
     *      <li>Velocity Messages (type code 19)
     * </ul>
     *
     * @return the Type Code of the ADS-B Message.
     */
    public int typeCode() {
        return Bits.extractUInt(bytes.byteAt(TYPE_CODE_BYTE_IDX), TYPE_CODE_BYTE_START, TYPE_CODE_SIZE);
    }

    /**
     * Returns the payload of the ADS-B message, which is the 56 bits of the message containing its exploitable
     * information.
     *
     * @return the payload of the ADS-B message.
     * The PayLoad (ME attribute) is the 7 bytes containing the exploitable information of the ADS-B message.
     */
    public long payload() {
        return bytes.bytesInRange(TYPE_CODE_BYTE_IDX, PAYLOAD_END_IDX);
    }

    /**
     * Returns the {@link IcaoAddress} of the aircraft.
     *
     * @return the ICAO address of the given Aircraft.
     */
    public IcaoAddress icaoAddress() {
        String hexString = bytes.toString().substring(ICAO_END - ICAO_CHAR_LENGTH, ICAO_END);
        return new IcaoAddress(hexString);
    }
}

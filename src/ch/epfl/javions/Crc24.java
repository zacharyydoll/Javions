package ch.epfl.javions;

/**
 * Represents 24 bit CRC calculator of a message, allowing to check whether a message has been corrupted.
 * A message is considered to be corrupted if its CRC is not equal to 0.
 *
 * @author : Alexei Thornber (363088)
 * @author : Zachary Doll (356458)
 */
public final class Crc24 {

    /**
     * The generator used to decode the CRC messages.
     */
    public final static int GENERATOR = 0xFFF409;
    private static final int FILE_ENTRIES = 256;
    private static final int CRC_SIZE = 24;
    private final int[] generatorTable;

    /**
     * Instantiates a 24 bit CRC calculator given a generator, and stores its corresponding table in an attribute.
     *
     * @param generator the designed generator to decode our CRC messages
     */
    public Crc24(int generator) {
        generatorTable = buildTable(generator);
    }

    /**
     * Calculates the CRC of a given message, bit by bit.
     *
     * @param generator the generator used to decode the CRC messages
     * @param bytes     the message to calculate the CRC of
     * @return the 24 bit CRC of the inputted message
     */
    private static int crcBitwise(int generator, byte[] bytes) {
        int[] table = {0, generator};
        int crc = 0;
        for (byte curr : bytes) {
            for (int j = Byte.SIZE - 1; j >= 0; j--) {
                int tabIdx = table[Bits.extractUInt(crc, (CRC_SIZE - 1), 1)];
                crc = ((crc << 1) | Bits.extractUInt(curr, j, 1)) ^ tabIdx;
            }
        }
        for (int j = 0; j < CRC_SIZE; j++) {
            crc = ((crc << 1) ^ table[Bits.extractUInt(crc, (CRC_SIZE - 1), 1)]);
        }
        crc = Bits.extractUInt(crc, 0, CRC_SIZE);
        return crc;
    }

    /**
     * Builds an array of size 256, corresponding to a generator.
     *
     * @param generator the generator used to decode the CRC messages
     * @return the array of size 256 corresponding to the generator
     */
    private static int[] buildTable(int generator) {
        var output = new int[FILE_ENTRIES];

        for (int i = 0; i < FILE_ENTRIES; i++)
            output[i] = crcBitwise(generator, new byte[]{(byte) i});
        return output;
    }

    /**
     * Calculates the 24 bit CRC of a message, byte by byte.
     *
     * @param bytes the message to calculate the CRC of
     * @return the 24 bit CRC of the inputted message
     */
    public int crc(byte[] bytes) {
        int crc = 0;
        for (byte curr : bytes) {
            int tabIdx = generatorTable[Bits.extractUInt(crc, (CRC_SIZE - Byte.SIZE), Byte.SIZE)];
            crc = ((crc << Byte.SIZE) | Byte.toUnsignedInt(curr)) ^ tabIdx;
        }
        for (int j = 0; j < 3; j++) {
            crc = ((crc << Byte.SIZE) ^ generatorTable[Bits.extractUInt(crc, (CRC_SIZE - Byte.SIZE), Byte.SIZE)]);
        }
        return Bits.extractUInt(crc, 0, CRC_SIZE);
    }
}

package ch.epfl.javions;

/**
 * This class contains methods to extract bits from a long value.
 *
 * @author : Alexei Thornber (363088)
 * @author : Zachary Doll (356458)
 */
public class Bits {
    private Bits() {
    } // Prevents instantiation

    /**
     * Extracts an integer from a long value in a given range of bits.
     *
     * @param value the long type of which we want to extract a value of up to 32 bits
     * @param start the starting index of the desired extraction (in little endian format)
     * @param size  the size of the extracted value, must be between 0 (excluded) and 32 (excluded)
     * @return the unsigned extracted value (integer)
     * @throws IllegalArgumentException  if the extracted size is negative or exceeds the size of an integer
     * @throws IndexOutOfBoundsException if start+size exceeds the size of an integer or is negative
     */
    public static int extractUInt(long value, int start, int size) {
        Preconditions.checkArgument(size > 0 && size < Integer.SIZE);

        if ((start + size) <= 0 || (start + size) > Long.SIZE) {
            throw new IndexOutOfBoundsException("interval must be between 0 and 64");
        }

        value >>>= start;
        value = value & ((1 << (size)) - 1);
        return (int) value;
    }

    /**
     * Determines the value of a bit at a given index in a long value.
     *
     * @param value the long type of which we want to test a bit
     * @param index the desired position of the bit in the long value (must be between 0 and 63)
     * @return true if the bit at index "index" is set to 1, false otherwise
     * @throws IndexOutOfBoundsException if the index is not between 0 (excluded) and the size of a long (excluded)
     */
    public static boolean testBit(long value, int index) {
        if (index < 0 || index >= Long.SIZE) {
            throw new IndexOutOfBoundsException("The index must be between 0 and 64");
        }
        long mask = 1L << index;
        return (value & mask) != 0;
    }
}

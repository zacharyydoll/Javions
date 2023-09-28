package ch.epfl.javions;

import java.util.Arrays;
import java.util.HexFormat;

/**
 * Represents an immutable sequence of unsigned bytes.
 *
 * @author : Alexei Thornber (363088)
 * @author : Zachary Doll (356458)
 */
public final class ByteString {
    private final byte[] bytes;

    /**
     * Builds a new ByteString, given an inputted byte array.
     *
     * @param bytes an array of bytes
     */
    public ByteString(byte[] bytes) {
        this.bytes = bytes.clone();
    }

    /**
     * Converts a String representing a Hexadecimal number to an array of bytes.
     *
     * @param hexString the Hexadecimal number in String format (must have an even amount of characters)
     * @return an array of bytes equals to the hexadecimal number inputted as a String
     * @throws IllegalArgumentException if the inputted String is of odd length
     */
    public static ByteString ofHexadecimalString(String hexString) {
        Preconditions.checkArgument((hexString.length() + 1) % 2 == 1);

        byte[] bytes = HexFormat.of().parseHex(hexString);
        return new ByteString(bytes);
    }

    /**
     * Returns the size of the byte array.
     *
     * @return the size of the byte array
     */
    public int size() {
        return bytes.length;
    }

    /**
     * Gets the unsigned byte at the given index.
     *
     * @param index the index of the desired byte
     * @return the byte at index "index" in the stored byte array
     */
    public int byteAt(int index) {
        return bytes[index] & 0xFF;
    }

    /**
     * Returns the extracted long value of the ByteString, from a given start and end index.
     *
     * @param fromIndex the start index of the extraction
     * @param toIndex   the end index of the extraction
     * @return the extracted long in range of fromIndex (included) to toIndex (excluded)
     * @throws IndexOutOfBoundsException if the span is smaller negative or larger than the length of the byte array
     * @throws IllegalArgumentException  if the span is larger than the authorized maximum of 8 bytes (64 bits)
     */
    public long bytesInRange(int fromIndex, int toIndex) {
        int span = Math.abs(fromIndex - toIndex);
        Preconditions.checkArgument(span < Byte.SIZE);

        if (span < 0 || span > bytes.length - 1) {
            throw new IndexOutOfBoundsException("span must be between zero and ByteString size");
        } else {
            long result = 0;
            for (int i = fromIndex; i < toIndex; i++) {
                result = (result << Byte.SIZE) | byteAt(i);
            }
            return result;
        }
    }

    /**
     * Tests if the inputted object is an instance of ByteString and that their arrays are equal.
     *
     * @param o the object we want to compare
     * @return true if the inputted Object is an instance as ByteString and has the same Array, false otherwise.
     */
    @Override
    public boolean equals(Object o) {
        return (o instanceof ByteString bString) && Arrays.equals(bytes, bString.bytes);
    }

    /**
     * Applies the HashCode method from Arrays to the array of bytes.
     *
     * @return the hashCode value of the array of bytes
     * @see Arrays#hashCode(byte[])
     */
    @Override
    public int hashCode() {
        return Arrays.hashCode(bytes);
    }

    /**
     * Transforms the array of bytes to a Hex number (in String format).
     *
     * @return the HexString representation of the ByteString
     * @see HexFormat#formatHex(byte[])
     */
    @Override
    public String toString() {
        HexFormat hf = HexFormat.of().withUpperCase();
        return hf.formatHex(bytes);
    }
}

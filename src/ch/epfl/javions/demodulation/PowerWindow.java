package ch.epfl.javions.demodulation;

import ch.epfl.javions.Preconditions;

import java.io.IOException;
import java.io.InputStream;

/**
 * Represents a fixed size window over a sequence of power samples produced by a {@link PowerComputer}.
 *
 * @author : Alexei Thornber (363088)
 * @author : Zachary Doll (356458)
 */
public final class PowerWindow {

    //The size of a batch is 2^16
    private final static int BATCH_SIZE = 1 << 16;
    private final int windowSize;
    private final InputStream stream;
    private final PowerComputer computer;
    private int[] EvenBatch;
    private int[] OddBatch;
    private long posOfWindow;
    private int size;


    /**
     * Builds a new window of the given size over the given stream of power samples.
     *
     * @param stream     the stream to read from (cannot be null)
     * @param windowSize the size of the window (must be positive and less than or equal to the batch size)
     * @throws IllegalArgumentException if the window size is not positive or greater than the batch size
     * @throws IOException              if the stream cannot be read from (see {@link InputStream#readNBytes(byte[], int, int)})
     */
    public PowerWindow(InputStream stream, int windowSize) throws IOException {
        Preconditions.checkArgument(windowSize > 0 && windowSize <= BATCH_SIZE);
        this.windowSize = windowSize;
        this.stream = stream;
        EvenBatch = new int[BATCH_SIZE];
        OddBatch = new int[BATCH_SIZE];
        posOfWindow = 0; //Window starts at index 0

        //instantiates a new PowerComputer to read calculated power samples from the stream
        this.computer = new PowerComputer(stream, BATCH_SIZE);
        size = computer.readBatch(EvenBatch);
    }

    /**
     * Returns the size of the window.
     *
     * @return the size of the window {@link #windowSize} provided in the constructor
     */
    public int size() {
        return windowSize;
    }

    /**
     * Returns the position of the window in the batch.
     *
     * @return the position of the window in the batch.
     */
    public long position() {
        return posOfWindow;
    }

    /**
     * Determines whether the window is full or not.
     *
     * @return true if the window is full, false otherwise
     */
    public boolean isFull() {
        return windowSize + posOfWindow <= size;
    }

    /**
     * Returns the value at a given index of the window.
     *
     * @param i the index of the element to get (must be non-negative and less than the window size)
     * @return the element at the given index
     * @throws IndexOutOfBoundsException if the index is negative or greater than or equal to the window size
     */
    public int get(int i) {
        if (i < 0 || i >= windowSize) {
            throw new IndexOutOfBoundsException();
        }
        if (((posOfWindow % BATCH_SIZE) + i) >= BATCH_SIZE) {
            return OddBatch[(int) ((posOfWindow % BATCH_SIZE) + i) % BATCH_SIZE];
        } else {
            return EvenBatch[(int) (posOfWindow % BATCH_SIZE) + i];
        }
    }

    /**
     * Advances the window by an offset of 1.
     *
     * @throws IOException if the stream cannot be read from (see {@link InputStream#readNBytes(byte[], int, int)})
     */
    public void advance() throws IOException {
        posOfWindow++;
        if ((posOfWindow + windowSize) % BATCH_SIZE == 1) {
            size += computer.readBatch(OddBatch);
        }
        if ((posOfWindow > 1) && ((posOfWindow % BATCH_SIZE) == 1)) {
            int[] temp = EvenBatch;
            EvenBatch = OddBatch;
            OddBatch = temp;
        }
    }

    /**
     * Advances the window by a given offset.
     *
     * @param offset the offset to advance the window by (must be non-negative)
     * @throws IllegalArgumentException if the offset is negative
     * @throws IOException              if the stream cannot be read from
     *                                  (see {@link InputStream#readNBytes(byte[], int, int)})
     */
    public void advanceBy(int offset) throws IOException {
        Preconditions.checkArgument(offset >= 0);
        for (int i = 0; i < offset; i++) {
            advance();
        }
    }
}
